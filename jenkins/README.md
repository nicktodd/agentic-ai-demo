# Running Jenkins locally for the demo

This folder gives you a throw-away Jenkins controller in Docker with everything the
`Jenkinsfile` needs already installed (JDK 21, Maven, Docker CLI, AWS CLI v2, jq, Python).

## 1. Start Jenkins

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
| `aws-sandbox`       | AWS Credentials  | Access key + secret key of the sandbox IAM user (see `infra/jenkins-iam-policy.json`) | Push to ECR, deploy to ECS |
| `aws-account-id`    | Secret text      | 12-digit sandbox account id                                         | Building the ECR registry hostname |
| `anthropic-api-key` | Secret text      | Anthropic API key (optional)                                        | `scripts/triage_agent.py` on failed PR builds |

The IDs must match exactly; the `Jenkinsfile` refers to them by ID.

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

GitHub can't reach `localhost`, so relay the webhook through [smee.io](https://smee.io):

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
