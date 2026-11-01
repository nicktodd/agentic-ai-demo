# agentic-ai-demo — portfolio valuation

A small Spring Boot 3.5 / Java 21 REST API used in a 60-minute session on **agentic AI**
for graduate engineers. The same task is solved three ways:

| Stage | Tool | Who drives | Where it runs |
|-------|------|-----------|---------------|
| 1. General AI | Copilot Chat (ask mode) | You, line by line | Your IDE |
| 2. Agent | Copilot agent mode in VS Code | You approve each step | Your IDE |
| 3. Agentic AI | Copilot coding agent on a GitHub issue → PR → Jenkins → review → AWS | You review the PR | GitHub + Jenkins + AWS |

The presenter script is in [demo/RUNBOOK.md](demo/RUNBOOK.md). The seeded issues are in
[demo/issues](demo/issues).

> **Not accepting external pull requests.** This is a teaching repository. The open issues are
> deliberately left unresolved so they can be used as live exercises during sessions, so PRs from
> outside the repo are closed automatically (see
> [.github/workflows/close-external-prs.yml](.github/workflows/close-external-prs.yml)). Fork it
> freely and use it for your own sessions instead.

## The service in 5 minutes

```
controller/   PortfolioController  GET  /api/portfolios, /{id}, /{id}/holdings, /{id}/value
              PriceController      GET  /api/prices, /api/prices/{ticker}
              FxController         GET  /api/fx/convert?amount=100&from=GBP&to=EUR
service/      PortfolioService     values holdings via PricingService + FxService
              PricingService       latest price per ticker
              FxService            converts between GBP / USD / EUR (rates pivot via USD)
repository/   Spring Data JPA interfaces over in-memory H2
model/        JPA entities (Portfolio, Holding, Price, FxRate) and response records
```

Seed data (`src/main/resources/data.sql`): three portfolios (UK Growth in GBP, US Tech in
USD, Euro Income in EUR), seven prices, two FX rates. Health: `/actuator/health`.

## Run locally

Prerequisites: JDK 21, Maven 3.9+ (or use `./mvnw` if you add the wrapper).

```bash
mvn spring-boot:run
```

```bash
curl -s localhost:8080/api/portfolios/2/value | jq
```

```bash
curl -s "localhost:8080/api/fx/convert?amount=100&from=USD&to=GBP"
```

H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:portfolio`, user `sa`).

## Build and quality gates

```bash
mvn -B verify
```

`verify` runs, in order: compile → JUnit 5 tests (Surefire) → JaCoCo report → **JaCoCo line
coverage gate (85%)** → **Spotless check** (google-java-format). Fix formatting with:

```bash
mvn spotless:apply
```

Coverage report: `target/site/jacoco/index.html`.

## Repository map

```
.github/copilot-instructions.md          conventions the Copilot coding agent must follow
.github/workflows/copilot-setup-steps.yml JDK 21 + Maven cache for the coding agent
demo/RUNBOOK.md                           timed presenter script
demo/issues/*.md                          seeded issues (bug, feature, coverage, breakouts)
scripts/create_issues.sh                  creates the issues + labels with gh
scripts/triage_agent.py                   stretch: Claude diagnoses a failed build → PR comment
scripts/deploy_manual.sh                  build, push to ECR, apply OpenTofu from a laptop
scripts/teardown.sh                       destroys the AWS sandbox resources
Jenkinsfile                               declarative pipeline: verify → image → ECR → ECS
jenkins/                                  run Jenkins locally in Docker (JDK 21, Maven, Docker, AWS CLI)
Dockerfile                                multi-stage image for the service
infra/terraform/                          ECR + ECS Fargate + ALB (OpenTofu/Terraform HCL)
```

## Deliberately planted problems

Do not fix these before the session; they are the demo.

1. **FX rounding bug** – `FxService.convert` does the arithmetic in `double` and truncates
   before "rounding", so some conversions are a penny short. Existing tests use round rates
   and pass. Issue: `demo/issues/01-fx-rounding-bug.md`.
2. **Missing endpoint** – `GET /api/portfolios/{id}/value?currency=EUR` does not exist.
   Issue: `demo/issues/02-portfolio-value-in-requested-currency.md`. This is the one assigned
   to the Copilot coding agent live.
3. **Coverage headroom** – coverage is ~90% against an 85% gate, and `addHolding` has no
   direct tests. Any new code without tests trips the gate in Jenkins.
   Issue: `demo/issues/03-coverage-gap-addholding-and-fx.md`.
4. **Six breakout tasks** – validation, `@ControllerAdvice`, query endpoint, top-N, health
   indicator, delete endpoint. Issues 04–09.

## One-time setup (manual steps)

### Keeping outsiders' PRs out

A public repo whose issues are labelled as beginner-friendly attracts drive-by pull requests
within hours, which would consume the demo exercises. Three layers stop that:

1. **Interaction limit** (Settings → Moderation options → Interaction limits → *Collaborators
   only*). Blocks new issues/PRs/comments from non-collaborators. GitHub caps it at six months,
   so re-apply it before each run of the session:

   ```bash
   gh api -X PUT repos/<owner>/agentic-ai-demo/interaction-limits -f limit=collaborators_only -f expiry=six_months
   ```

2. **Auto-close workflow** (`.github/workflows/close-external-prs.yml`) comments and closes any
   PR from someone without write access, as a backstop for when the limit lapses. Copilot's PRs
   are left alone.
3. **No `good first issue` label.** The seeded issues use `demo` / `breakout` only, so they
   stay out of GitHub's "contribute" feeds.


### GitHub

1. Create an empty repo, e.g. `agentic-ai-demo`, in an org that has **Copilot coding agent**
   enabled (Copilot Business/Enterprise, agent enabled in org policies).
2. Push this repo:

```bash
git remote add origin git@github.com:<org>/agentic-ai-demo.git && git push -u origin main
```

3. Create the issues and labels:

```bash
scripts/create_issues.sh <org>/agentic-ai-demo
```

4. Branch protection on `main`: require a PR, require the Jenkins status check, require one
   approving review. This is what makes "a human reviews the agent's PR" real.
5. Confirm `.github/workflows/copilot-setup-steps.yml` ran green under Actions.
6. Create a classic PAT (or fine-grained token) for Jenkins with `repo` and
   `admin:repo_hook`; the triage script can reuse it.

### Jenkins (on EC2 in the sandbox account)

See [jenkins/README.md](jenkins/README.md). In short: an EC2 instance with Jenkins, JDK 21,
Maven, Docker and the AWS CLI, and an **IAM instance profile role** carrying
`infra/jenkins-iam-policy.json`. The pipeline needs no AWS keys; the CLI uses the role and
looks the account id up with STS. In Jenkins add the `github-token` credential (and
optionally `anthropic-api-key`), create a **Multibranch Pipeline** pointing at the GitHub
repo, and add a GitHub webhook pointing at `http://<jenkins-host>:8080/github-webhook/`.

A local Docker alternative (`jenkins/docker-compose.yml`) is documented in the same file; it
needs access keys and a smee.io webhook relay.

### AWS sandbox

See [infra/README.md](infra/README.md). In short: the Jenkins instance role described above,
then from your laptop (with your own sandbox admin credentials):

```bash
scripts/deploy_manual.sh
```

which builds the image, creates the ECR repo, pushes, and applies the OpenTofu config for ECS
Fargate + ALB. After the session:

```bash
scripts/teardown.sh
```

### Copilot

- VS Code with the GitHub Copilot and Copilot Chat extensions, signed in to the org.
- In the GitHub repo: Settings → Copilot → coding agent enabled; the agent uses
  `copilot-setup-steps.yml` automatically.
- Optional: Settings → Copilot → coding agent → allow the Jenkins check to run on
  Copilot's PRs (CI on agent PRs needs a human's approval the first time by default).

### Optional: triage agent

```bash
pip install -r scripts/requirements.txt
```

```bash
python3 scripts/triage_agent.py --log-file scripts/sample-failed-build.log --repo <org>/agentic-ai-demo --pr 1 --dry-run
```

Needs `ANTHROPIC_API_KEY`; see [scripts/README.md](scripts/README.md).

## Licence

MIT. See [LICENSE](LICENSE). Reuse it for your own sessions; a mention is appreciated.
