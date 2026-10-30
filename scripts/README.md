# scripts/

Helper scripts for the agentic-AI-in-CI demo.

| Script | Purpose |
|---|---|
| `triage_agent.py` | Build triage agent: reads a failed Jenkins log, asks Claude to diagnose it, posts the diagnosis as a PR comment. |
| `sample-failed-build.log` | A captured Jenkins console log (JUnit failure) for offline `--dry-run` demos. |
| `create_issues.sh` | Seeds the demo GitHub repo with example issues (written separately). |
| `teardown.sh` | Removes the demo resources created for the session (written separately). |

## Build triage agent

### Install

```bash
pip install -r scripts/requirements.txt
```

### Dry run (no Jenkins, no GitHub needed)

```bash
export ANTHROPIC_API_KEY=sk-ant-...
python3 scripts/triage_agent.py \
  --log-file scripts/sample-failed-build.log \
  --repo example/demo --pr 1 --dry-run
```

Prints the markdown comment to stdout instead of posting it.

### Live run against a Jenkins build

```bash
export ANTHROPIC_API_KEY=sk-ant-...
export GITHUB_TOKEN=ghp_...              # needs `issues: write` / `pull_requests: write`
export JENKINS_USER=ci-bot JENKINS_TOKEN=...   # optional, only if Jenkins needs auth
export ANTHROPIC_MODEL=claude-sonnet-5-5       # optional, this is the default

python3 scripts/triage_agent.py \
  --build-url "https://jenkins.example.com/job/agentic-ai-demo/job/PR-42/17/" \
  --repo my-org/agentic-ai-demo --pr 42
```

Options: `--max-chars N` (default 60000) caps how much log is sent; the script keeps the
*end* of the log because that is where Maven reports the failure.

Exit codes: `0` success, `2` configuration/API/GitHub error (message on stderr).

### How Jenkins calls it

In the `Jenkinsfile`, run the agent in a `post { failure { ... } }` block of the PR job so it
only fires when `mvn -B verify` fails. Jenkins already provides `BUILD_URL` and `CHANGE_ID`
(the PR number) for multibranch pipelines:

```groovy
post {
  failure {
    withCredentials([string(credentialsId: 'anthropic-api-key', variable: 'ANTHROPIC_API_KEY'),
                     string(credentialsId: 'github-token',       variable: 'GITHUB_TOKEN')]) {
      sh 'pip install -q -r scripts/requirements.txt'
      sh 'python3 scripts/triage_agent.py --build-url "$BUILD_URL" --repo my-org/agentic-ai-demo --pr "$CHANGE_ID"'
    }
  }
}
```

### Governance

- The agent **only comments**. It never pushes code, re-runs builds, approves or merges.
  The human reviewer stays in the loop and decides what to do with the diagnosis.
- Every comment ends with a footer saying it was machine-generated.
- Console logs can leak secrets. Jenkins masks `withCredentials` values in the log, and the
  script strips ANSI codes, keeps only error-like lines plus the tail, and truncates to
  `--max-chars`, but review what your pipeline prints before enabling this on real repos.
- API keys are read from environment variables and are never printed by the script.
