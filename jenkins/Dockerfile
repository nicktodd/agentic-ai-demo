# Jenkins controller image for the local demo.
#
# Starts from the official LTS image (JDK 21) and adds the tools our Jenkinsfile
# needs on the *same* machine, because we use "agent any" with no separate build
# agents to keep the demo simple:
#   * Maven          – mvn -B verify
#   * Docker CLI     – talks to the host's Docker daemon via the mounted socket
#   * AWS CLI v2     – ecr / ecs commands
#   * jq             – editing the ECS task definition JSON
#   * python3 + pip  – scripts/triage_agent.py (anthropic + requests libraries)
#   * curl, git      – general plumbing
FROM jenkins/jenkins:lts-jdk21

# Switch to root to install OS packages, then switch back at the end.
USER root

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      ca-certificates curl gnupg lsb-release unzip jq git \
      maven \
      python3 python3-pip python3-venv \
 && rm -rf /var/lib/apt/lists/*

# --- Docker CLI (client only; the daemon runs on the host) -------------------
RUN install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg \
      | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
      https://download.docker.com/linux/debian $(lsb_release -cs) stable" \
      > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli docker-buildx-plugin \
 && rm -rf /var/lib/apt/lists/*

# --- AWS CLI v2 (picks the right build for amd64 / arm64 hosts) --------------
RUN ARCH=$(uname -m) \
 && curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-${ARCH}.zip" -o /tmp/awscliv2.zip \
 && unzip -q /tmp/awscliv2.zip -d /tmp \
 && /tmp/aws/install \
 && rm -rf /tmp/aws /tmp/awscliv2.zip \
 && aws --version

# --- Python libraries for the AI triage script -------------------------------
# --break-system-packages is needed on Debian 12+ (PEP 668) for a system-wide pip install.
RUN pip3 install --no-cache-dir --break-system-packages anthropic requests

# --- Jenkins plugins -----------------------------------------------------------
# Pre-install plugins so the first start is ready to go (no plugin wizard).
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Back to the unprivileged jenkins user for running the controller.
USER jenkins
