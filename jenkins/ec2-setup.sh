#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# ec2-setup.sh — prepare an Amazon Linux 2023 EC2 instance to run Jenkins for
# the portfolio-valuation demo pipeline (Jenkinsfile at the repo root).
#
# What it installs / configures (idempotent, safe to re-run):
#   * Jenkins LTS (official yum repo)            * Java 21 (Amazon Corretto), set as default
#   * Maven 3.9 (latest, from Maven Central)      * Docker, with the jenkins user in the docker group
#   * git, jq, python3 + pip (anthropic, requests for scripts/triage_agent.py)
#   * JAVA_HOME pinned for the Jenkins service     * AWS CLI v2 check (preinstalled on AL2023)
#
# It does NOT create the IAM instance role, security group or Jenkins credentials;
# see jenkins/README.md section 0 for those.
#
# Usage (on the instance, as a sudo-capable user):
#   curl -fsSLO https://raw.githubusercontent.com/<owner>/agentic-ai-demo/main/jenkins/ec2-setup.sh
#   sudo bash ec2-setup.sh
# -----------------------------------------------------------------------------
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "Run with sudo: sudo bash $0" >&2
  exit 1
fi

JAVA_HOME_21=/usr/lib/jvm/java-21-amazon-corretto.x86_64

log() { printf '\n==> %s\n' "$*"; }

# --- 1. Base packages ---------------------------------------------------------
log "Installing base packages (git, jq, python3, docker, Java 21)"
dnf install -y git jq python3 python3-pip docker java-21-amazon-corretto-devel

# --- 2. Jenkins ----------------------------------------------------------------
# The repo file and signing key are always refreshed: Jenkins moved its RPM
# repo from redhat-stable/ to rpm-stable/ and rotated the signing key, so a
# stale repo file makes "dnf update jenkins" fail with "GPG check FAILED".
log "Refreshing Jenkins yum repo and signing key"
curl -fsSL https://pkg.jenkins.io/redhat-stable/jenkins.repo -o /etc/yum.repos.d/jenkins.repo
rpm --import https://pkg.jenkins.io/rpm-stable/repodata/repomd.xml.key
if ! rpm -q jenkins >/dev/null 2>&1; then
  log "Installing Jenkins LTS"
  dnf install -y jenkins
else
  log "Jenkins already installed: $(rpm -q jenkins)"
fi
systemctl enable jenkins >/dev/null

# --- 3. Java 21 as the system default ------------------------------------------
# Jenkins' package pulls in Java 17; Maven would otherwise pick that up.
log "Making Java 21 the default java/javac"
alternatives --set java  "$JAVA_HOME_21/bin/java"
alternatives --set javac "$JAVA_HOME_21/bin/javac"

# Pin JAVA_HOME for the Jenkins service so builds don't depend on alternatives order.
mkdir -p /etc/systemd/system/jenkins.service.d
cat > /etc/systemd/system/jenkins.service.d/java.conf <<CONF
[Service]
Environment="JAVA_HOME=$JAVA_HOME_21"
CONF

# --- 4. Maven 3.9 (latest) from Maven Central ----------------------------------
# Apache's dlcdn mirror only keeps the newest 3.9.x, so resolve the version from
# Maven Central metadata and download from there; Central keeps every release.
MVN_VERSION=$(curl -fsSL https://repo1.maven.org/maven2/org/apache/maven/apache-maven/maven-metadata.xml \
  | grep -o '<version>3\.9\.[0-9]*</version>' | tail -1 | sed 's/<[^>]*>//g')
if [[ ! -x "/opt/apache-maven-${MVN_VERSION}/bin/mvn" ]]; then
  log "Installing Maven ${MVN_VERSION}"
  curl -fsSL "https://repo1.maven.org/maven2/org/apache/maven/apache-maven/${MVN_VERSION}/apache-maven-${MVN_VERSION}-bin.tar.gz" \
    -o /tmp/maven.tgz
  tar -xzf /tmp/maven.tgz -C /opt
  rm -f /tmp/maven.tgz
else
  log "Maven ${MVN_VERSION} already present"
fi
ln -sfn "/opt/apache-maven-${MVN_VERSION}" /opt/maven
ln -sf /opt/maven/bin/mvn /usr/local/bin/mvn

# --- 5. Docker -----------------------------------------------------------------
log "Enabling Docker and adding the jenkins user to the docker group"
systemctl enable --now docker
usermod -aG docker jenkins

# --- 6. Python deps for the optional triage agent -------------------------------
log "Installing Python packages for scripts/triage_agent.py"
pip3 install -q --upgrade anthropic requests 2>/dev/null \
  || pip3 install -q --upgrade --break-system-packages anthropic requests

# --- 7. Restart Jenkins to pick up JAVA_HOME and the docker group ---------------
log "Restarting Jenkins"
systemctl daemon-reload
systemctl restart jenkins

# --- 8. Verify as the jenkins user ---------------------------------------------
log "Verifying tools as the jenkins user"
sudo -u jenkins bash -c '
  set -e
  echo "java : $(java -version 2>&1 | head -1)"
  echo "mvn  : $(mvn -version 2>&1 | head -1)"
  echo "       $(mvn -version 2>&1 | grep "Java version")"
  echo "git  : $(git --version)"
  echo "jq   : $(jq --version)"
  echo "py   : $(python3 --version)"
  echo "aws  : $(aws --version 2>&1)"
  echo "docker: $(docker ps >/dev/null 2>&1 && echo ok || echo "FAILED - check docker group / daemon")"
  echo "role : $(aws sts get-caller-identity --query Arn --output text 2>/dev/null || echo "FAILED - no instance role attached?")"
'

cat <<MSG

Done. Next:
  * Open http://$(curl -fsS --max-time 2 http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "<instance-ip>"):8080
    and unlock with: sudo cat /var/lib/jenkins/secrets/initialAdminPassword
  * Install the plugins in jenkins/plugins.txt, add the github-token credential,
    set the Jenkins URL, then create the Multibranch Pipeline (jenkins/README.md).
  * If "role" shows FAILED above, attach the IAM instance role (infra/jenkins-iam-policy.json).
MSG
