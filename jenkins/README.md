# Jenkins for the demo

The `Jenkinsfile` expects a Jenkins with JDK 21, Maven, the Docker CLI, AWS CLI v2, jq and
Python 3 available on the agent. There are two ways to get one:

* **Option A (recommended): Jenkins on an EC2 instance in the sandbox account** with an IAM
  *instance profile role*. The AWS CLI picks the role up automatically, so Jenkins stores no
  AWS keys at all, and GitHub can reach the instance directly for webhooks. See section 0.
* **Option B: Jenkins locally in Docker** using the compose file in this folder. You would
  then need AWS access keys in Jenkins (see *Local Jenkins and AWS keys* at the end) and a
  webhook relay such as smee.io.

## 0. Option A: Jenkins on EC2 with an instance role

1. **IAM role.** Create a role for EC2 (trusted entity: `ec2.amazonaws.com`) named e.g.
   `jenkins-demo-role`. Attach [`infra/jenkins-iam-policy.json`](../infra/jenkins-iam-policy.json)
   as an inline policy, with `ACCOUNT_ID` replaced. Attach the role to the instance
   (*EC2 -> Instance -> Actions -> Security -> Modify IAM role*). No access keys are created.
2. **Tools on the instance** (Amazon Linux 2023 shown):

   ```bash
   sudo dnf install -y java-21-amazon-corretto-devel maven docker jq python3 python3-pip git
   sudo systemctl enable --now docker
   sudo usermod -aG docker jenkins       # the Docker Build stage runs `docker build` as the jenkins user
   curl -s "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip && unzip -q awscliv2.zip && sudo ./aws/install
   sudo systemctl restart jenkins
   ```

   Check the role is visible to the Jenkins user:

   ```bash
   sudo -u jenkins aws sts get-caller-identity
   ```

   It should print the sandbox account id and an `assumed-role/jenkins-demo-role/...` ARN.
3. **Metadata hop limit.** If Jenkins (or its agents) ever run inside Docker *on* the instance,
   raise the IMDSv2 hop limit to 2 or the containers cannot reach the credential endpoint:
   `aws ec2 modify-instance-metadata-options --instance-id <id> --http-put-response-hop-limit 2`.
   Jenkins installed directly on the host does not need this.
4. **Plugins.** Install the plugins listed in [`plugins.txt`](plugins.txt) via
   *Manage Jenkins -> Plugins* (the `aws-credentials` plugin is not needed for Option A).
5. **Security group.** Open the Jenkins port (8080 by default) to your IP, and to GitHub's
   webhook IP ranges (`https://api.github.com/meta`, key `hooks`) if you want webhooks.
6. Set *Manage Jenkins -> System -> Jenkins URL* to the instance's public URL so the GitHub
   status-check links work.

Then continue from section 2 (credentials) below.

## 1. Option B: start Jenkins locally in Docker

```bash
cd jenkins
cp .env.example .env          # edit the admin password
docker compose up -d --build  # first build: ~5 min (downloads plugins + tools)
docker compose logs -f        # wait for "Jenkins is fully up and running"
```

Open <http://localhost:9090> and log in with the user/password from `.env`.
(The setup wizard is skipped; `casc/jenkins.yaml` created the admin user for you.)

## 2. Add credentials

*Manage Jenkins -> Credentials -> System -> Global credentials -> Add Credentials*

| ID                  | Kind             | Value                                                              | Needed for |
|---------------------|------------------|--------------------------------------------------------------------|------------|
| `github-token`      | Secret text      | GitHub PAT (classic) with scopes `repo`, `admin:repo_hook`          | Multibranch scanning, PR status checks, triage comments |
| `anthropic-api-key` | Secret text      | Anthropic API key (optional)                                        | `scripts/triage_agent.py` on failed PR builds |

The IDs must match exactly; the `Jenkinsfile` refers to them by ID.

**No AWS credentials are stored in Jenkins.** The `Push to ECR` and `Deploy to ECS` stages call
the AWS CLI with no keys set, so it uses the EC2 instance role, and the account id is looked
up with `aws sts get-caller-identity` at the start of the push stage.

## 3. Create the Multibranch Pipeline job

1. *New Item* -> name `portfolio-valuation` -> **Multibranch Pipeline** -> OK.
2. *Branch Sources* -> *Add source* -> **GitHub**.
   * Credentials: `github-token` (for the "Secret text" kind pick it in the *Credentials* dropdown;
     if the plugin insists on username/password, create a *Username with password* credential
     using your GitHub username and the PAT as the password).
   * Repository HTTPS URL: `https://github.com/<owner>/agentic-ai-demo`.
3. *Behaviours* (defaults are fine, check they read):
   * Discover branches – *Exclude branches that are also filed as PRs*
   * Discover pull requests from origin – **Merging the pull request with the current target branch revision**
     (this builds what `main` *would* look like after the merge, which is what you want to test)
   * Discover pull requests from forks – remove it for the demo (fork PRs run untrusted code).
4. *Build Configuration*: by Jenkinsfile, script path `Jenkinsfile`.
5. *Scan Multibranch Pipeline Triggers*: tick *Periodically if not otherwise run* -> **1 minute**
   (this is the fallback if webhooks aren't set up).
6. Save. Jenkins scans the repo and starts building `main` and any open PRs.

PR builds show up as `PR-<number>`; their result is posted back to GitHub as a commit status
check automatically by the GitHub Branch Source plugin, so the PR page shows a green tick / red cross.

## 4. Webhooks (optional, makes builds start within seconds)

**Jenkins on EC2 (Option A):** GitHub can call Jenkins directly. In the GitHub repo,
*Settings -> Webhooks -> Add webhook*: Payload URL `http://<jenkins-host>:8080/github-webhook/`,
content type `application/json`, events **Pushes** and **Pull requests**. The security group
must allow GitHub's hook IP ranges on that port.

**Local Jenkins (Option B):** GitHub can't reach `localhost`, so relay the webhook through
[smee.io](https://smee.io):

```bash
# 1. Visit https://smee.io and click "Start a new channel"; copy the URL.
# 2. In a terminal on your laptop, forward it to Jenkins:
npx smee-client --url https://smee.io/<your-channel> --target http://localhost:9090/github-webhook/
```

Then in the GitHub repo: *Settings -> Webhooks -> Add webhook*

* Payload URL: `https://smee.io/<your-channel>`
* Content type: `application/json`
* Events: *Let me select individual events* -> **Pushes** and **Pull requests**

(`ngrok http 9090` works the same way; use the ngrok URL + `/github-webhook/`.)

If you skip this, the 1-minute periodic scan from step 3 picks up changes instead, or click
**Scan Multibranch Pipeline Now** on the job page.

## 5. Walk through a build

* Branch `main` -> all 5 stages; it ends by printing the ALB URL.
* A PR -> Checkout, Build & Verify, Docker Build only. Break a test on the PR to show the
  red cross on GitHub and (if configured) the AI triage comment.

## Troubleshooting

**`permission denied while trying to connect to the Docker daemon socket`** (Linux)
The jenkins user inside the container can't write to `/var/run/docker.sock`. Either add the
host's docker group id under `group_add:` in `docker-compose.yml`
(`stat -c %g /var/run/docker.sock`) or, for a local demo, set `user: root`. On macOS/Windows
with Docker Desktop this just works.

**Port already in use**
Something else is on 9090 or 50000. Change the left-hand side of the `ports:` mapping, and
update `unclassified.location.url` in `casc/jenkins.yaml` to match.

**First build is very slow**
Maven downloads the whole dependency tree the first time. The `maven_repo` named volume
(mounted at `/var/jenkins_home/.m2`) caches it so later builds take seconds, not minutes.
Don't `docker compose down -v` unless you want to lose that cache.

**`aws: command not found` / `jq: not found`**
You're running a stale image. `docker compose up -d --build` to rebuild.

**Jenkins can't see the GitHub repo**
Check the PAT has `repo` scope and hasn't expired; re-run *Scan Multibranch Pipeline Now*
and read the scan log on the job page.

**Reset everything**
```bash
docker compose down -v   # removes jenkins_home and maven_repo volumes
```

## Appendix: local Jenkins and AWS keys (Option B only)

A Jenkins running on your laptop has no instance role. If you must deploy from there, create
an IAM user with `infra/jenkins-iam-policy.json`, store its access key in Jenkins as an
**AWS Credentials** entry with ID `aws-sandbox`, and wrap the `Push to ECR` and `Deploy to ECS`
stage bodies in the `Jenkinsfile` with:

```groovy
withCredentials([aws(credentialsId: 'aws-sandbox',
                     accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                     secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {
  sh '''...'''
}
```

The STS account lookup keeps working with keys. For the demo, prefer Option A: it is the
"least privilege, nothing to leak" talking point from the runbook.
