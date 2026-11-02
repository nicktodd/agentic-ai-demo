# Set up your own project for agentic AI

This guide takes the setup used in this demo repository and applies it to a project you work
on. It is written for a team that already uses GitHub for source control, GitHub Copilot in
VS Code, and Jenkins for CI (Continuous Integration). Nothing in it requires access to AWS
(Amazon Web Services) or changes to your Jenkins pipeline.

The guide follows the three stages from the session:

| Stage | What you set up | Who drives | Section |
|-------|-----------------|------------|---------|
| 1. General AI | Nothing beyond Copilot Chat | You, line by line | Already available |
| 2. Agent | A conventions file and a trustworthy build | You approve each step | Steps 1 to 4 |
| 3. Agentic AI | The Copilot coding agent, an issue template and a setup workflow | You review the pull request | Steps 5 to 7 |

Each stage builds on the previous one. Stage 2 is useful on its own, and most of its value
comes from the conventions file and the build gate, not from the agent.

**Time required:** about one hour for Steps 1 to 4, and a further hour for Steps 5 to 7 once
the coding agent is enabled for your organisation.

**Reference files in this repository:**

| File | Purpose |
|------|---------|
| [`.github/copilot-instructions.md`](../.github/copilot-instructions.md) | Conventions every Copilot mode reads |
| [`.github/workflows/copilot-setup-steps.yml`](../.github/workflows/copilot-setup-steps.yml) | Build environment for the coding agent |
| [`.github/pull_request_template.md`](../.github/pull_request_template.md) | Review checklist for humans and agents |
| [`demo/issues/`](../demo/issues) | Worked examples of agent-ready issues |
| [`Jenkinsfile`](../Jenkinsfile) | A pipeline where only `main` can deploy |

---

## Before you start

Your project belongs to your team and your firm, not to you. Complete this checklist before you
add any files.

- [ ] **Ask your tech lead.** Explain what you want to try and on which repository. Agree
      whether the first attempt happens on the team repository or on a sandbox branch.
- [ ] **Read your internal AI policy.** Confirm that GitHub Copilot is an approved tool for this
      codebase and its data classification. Some repositories (for example those holding client
      data, credentials or regulated reporting logic) may be out of scope.
- [ ] **Check what Copilot features your licence includes.** Copilot features are controlled by
      your organisation's administrators. Agent mode in VS Code and the Copilot coding agent are
      enabled separately. If you are unsure, ask the team that administers GitHub.
- [ ] **Check where the code runs.** Agent mode runs commands on your machine. The coding agent
      runs in a GitHub Actions environment that GitHub hosts or that your organisation provides.
      Confirm that your firm allows the repository's code to be processed in that environment.
- [ ] **Decide who reviews.** Every agent-written change needs a named human reviewer who did
      not request the change.

If any item is blocked, stop at Stage 2 or at the conventions file. Those steps still improve
results from Copilot Chat and agent mode, and they need no new permissions.

---

## Step 1: Make the build a trustworthy gate

An agent is only as safe as the checks that judge its work. In the demo, `mvn -B verify` runs
the tests, a formatter check (Spotless) and a coverage gate (JaCoCo at 85%). The agent can run
it, Jenkins runs it on every pull request, and a pull request cannot merge if it fails.

Confirm the following for your project.

1. **One command verifies everything.** A single command, for example `mvn -B verify` or
   `./gradlew check`, compiles the code, runs the unit tests and fails on lint or formatting
   errors. If a developer has to remember several commands, the agent will forget one.
2. **The command passes on `main` today.** An agent cannot distinguish a failure it caused from
   one that was already there. Fix or quarantine existing failures first.
3. **Failures are readable.** The output names the failing test, file and line. An agent fixes
   what it can read.
4. **Jenkins builds pull requests.** A Multibranch Pipeline (or equivalent) runs the same
   command on every pull request and reports a status check to GitHub.
5. **Branch protection is on.** In the repository settings, `main` requires a pull request, the
   Jenkins status check and at least one approving review. Direct pushes are blocked.
6. **Pull request builds cannot deploy.** Deployment stages run on `main` only, as in this
   repository's `Jenkinsfile` (`when { branch 'main' }`). Credentials for deployment are not
   available to pull request builds. This matters because an agent's branch is built by
   Jenkins like any other branch.

Optional, if your project supports it: a coverage gate. It turns "tests are required" from an
instruction into a check, which is the difference between telling an agent and stopping it.

---

## Step 2: Write a conventions file

GitHub Copilot reads `.github/copilot-instructions.md` from the repository root and applies it
to Chat, agent mode and the coding agent. It is how your team's judgement reaches the model.
Treat it as code: commit it, review changes to it in pull requests, and keep it short enough
that a new team member would read it.

### What to include

| Section | Contents | Example from this repository |
|---------|----------|------------------------------|
| Project layout | Modules or packages, and what belongs in each | `controller/` holds thin `@RestController` classes, no business logic |
| Conventions (must follow) | Rules a reviewer would reject a pull request for breaking | Money is always `BigDecimal` with an explicit `RoundingMode` |
| Testing | Which test type at which layer, and the libraries to use | `@WebMvcTest` for controllers, Mockito for services, AssertJ for assertions |
| Build | The verify command, and what the agent must not do to make it pass | Do not lower the coverage threshold or exclude classes from coverage |
| Out of bounds | Files and folders the agent must not change unless the issue asks | `Jenkinsfile`, `Dockerfile`, `infra/` |
| Working on an issue | The order of work you expect | Write the failing test first; make the smallest change in the right layer |

### Starter template

Copy this into `.github/copilot-instructions.md` and replace every line in angle brackets.

````markdown
# Copilot instructions for <project name>

<One or two sentences: what the service does and who uses it.>

## Project layout

- `<path>` - <what lives here>
- `<path>` - <what lives here>

## Conventions (must follow)

- <Rule a reviewer would reject a pull request for breaking.>
- <Dependency injection style, for example constructor injection only.>
- <Error handling: which exceptions map to which HTTP status codes.>
- Do not add new dependencies without a comment explaining why.

## Testing

- Every change in behaviour needs a test.
- <Unit test style and libraries.>
- <Integration test style, for example @SpringBootTest with Testcontainers.>

## Build

- `<verify command>` must pass before you finish. It runs <tests, lint, coverage>.
- Do not lower thresholds, skip tests or exclude files to make the build pass.

## Out of bounds

- Do not change `Jenkinsfile`, deployment configuration or anything under `<path>`
  unless the issue asks for it.
- Never add credentials, tokens or connection strings to the repository.

## Working on an issue

1. Read the acceptance criteria. Write the failing test first.
2. Make the smallest change in the correct layer.
3. Run `<verify command>` and make sure it passes.
4. In the pull request description, list the files changed and how you tested them.
````

### Guidance

- **Write rules, not aspirations.** "Use `BigDecimal` for all money" can be checked in review.
  "Write clean code" cannot.
- **Record the mistakes you see.** When an agent makes the same mistake twice, add a rule. The
  file grows from evidence, not from a brainstorm.
- **Do not put secrets or internal hostnames in it.** The file is sent to the model with every
  request.
- **Scope rules to folders when the file grows.** Copilot also supports path-specific
  instruction files under `.github/instructions/` (for example `*.instructions.md` files with
  an `applyTo` pattern). Use them when, for example, front-end and back-end rules differ.
  Check the current GitHub documentation for the supported file names in your Copilot version.

**Check it works:** open Copilot Chat in ask mode and ask "What conventions must you follow in
this repository?" The answer should summarise your file.

---

## Step 3: Add an issue template

The quality of an agent's pull request depends on the quality of the issue it was given. An
agent working asynchronously has nobody to ask, so it guesses whatever the issue leaves out. An
issue template puts the structure from the session into every new issue.

Create `.github/ISSUE_TEMPLATE/agent-task.md`:

````markdown
---
name: Agent-ready task
about: A scoped change that a person or an AI agent can complete from this issue alone
title: "<Type>: <one-line outcome>"
labels: agent-ready
---

## Context

<!-- The problem, and who it affects. -->

### Steps to reproduce (bugs only)

1.
2.

**Actual:**
**Expected:** <!-- Show the calculation or the source of the expected value. -->

## Contract

<!-- The interface after the change: endpoint, parameters, status codes, response shape. -->

### Edge cases

- <!-- What happens when the input is omitted? -->
- <!-- Case sensitivity, allowed values, limits. -->
- <!-- Invalid input: which status code and response body? -->

### Where the change belongs

<!-- Classes, packages or layers to change. Existing behaviour to keep unchanged. -->

### Out of scope

<!-- Files not to touch, follow-up work not to start. -->

## Definition of done

- [ ] <!-- Testable criterion, naming the test type and layer. -->
- [ ] <!-- Testable criterion. -->
- [ ] `<verify command>` passes, including all quality gates.
````

### Rules for writing the issue

1. **Keep it to one pull request.** A reviewer should be able to read the diff in one sitting.
   Split anything larger into several issues.
2. **Give calculations, not guesses.** State the arithmetic or the source of every expected
   value. Issue [#2](../demo/issues/02-portfolio-value-in-requested-currency.md) asks for
   assertions derived from seed data, so the agent does not hard-code values computed by
   buggy code and lock the bug into the tests.
3. **State the failure mode.** "Unsupported currency returns 400 with a `ProblemDetail` body,
   not 500" settles a decision the agent would otherwise make for you.
4. **Name the layer.** "Validation belongs in the service" prevents a correct change in the
   wrong place.
5. **Leave standing rules out.** Rules that apply to every change belong in the conventions
   file. The issue holds only what is specific to this task.
6. **Have it reviewed.** Ask a colleague to read the issue before it is assigned. Every
   question they ask is a guess the agent would have made.

### Compare a vague and an agent-ready issue

Vague:

```text
Add currency support to the valuation endpoint.
```

Agent-ready (condensed from issue #2):

```text
GET /api/portfolios/{id}/value?currency=EUR
- currency is optional; when omitted, behave exactly as today (base currency)
- case-insensitive; GBP, USD or EUR only
- unsupported currency (e.g. CHF) -> 400 with a ProblemDetail body, never 500
- validate in PortfolioService using FxService.SUPPORTED
- keep the existing value(Long) method delegating to the new one
- [ ] @WebMvcTest, service and @SpringBootTest cases
- [ ] mvn -B verify passes, including the 85% coverage gate
```

The vague version leaves four decisions to the agent: the default, the input rules, the error
code and the placement. The agent-ready version settles each one in terms a test can check.

---

## Step 4: Use agent mode in VS Code (Stage 2)

Agent mode needs no repository setup beyond Steps 1 to 3. It runs on your machine, under your
supervision.

### Workflow

1. Create a branch: `git checkout -b <your-name>/<issue-number>-<short-name>`.
2. Run the verify command once so dependencies are downloaded and you know the starting point
   is green.
3. Open Copilot Chat and switch the mode to **Agent**.
4. Paste the issue body, then add:

   ```text
   Follow .github/copilot-instructions.md. Write the tests first. Run <verify command>
   yourself and iterate until it passes. Do not change files outside the scope of the issue.
   ```

5. Read each proposed edit in the diff view before accepting it.
6. Approve terminal commands one at a time. Read the command before you click **Continue**.
7. When the build passes, review the full diff yourself (`git diff main`), then commit, push and
   open a pull request. Jenkins builds it like any other branch.

### Guidance

- **Keep command approval on.** VS Code can be configured to run some terminal commands without
  asking. Leave every command requiring approval until you have seen how the agent behaves on
  your codebase, and never auto-approve commands that touch Git remotes, deployment tools or
  cloud command-line tools.
- **Watch for gate-gaming.** If the agent proposes to lower a threshold, skip a test or add an
  exclusion, reject the edit and tell it the conventions file forbids it.
- **Ask why.** At least once per task, ask the agent why it chose an approach. A weak answer is
  a signal to read that part of the diff more closely.
- **Stop when it loops.** If the agent repeats the same failing fix twice, stop it, read the
  failure yourself, and give it a specific hint.

### Good first tasks

| Good first task | Why |
|-----------------|-----|
| Close a test coverage gap in one class | Low risk, the gate checks the result |
| Add input validation to one endpoint | Small, well-defined, easy to test |
| Add a read-only query endpoint | Additive, no change to existing behaviour |
| Replace a deprecated API call across a module | Mechanical, easy to review |

| Poor first task | Why |
|-----------------|-----|
| A change to a database migration | Hard to reverse once applied |
| Anything touching authentication or entitlements | High impact, hard to test fully |
| "Refactor the service layer" | No clear definition of done |
| A change that needs knowledge not in the repository | The agent will invent it |

---

## Step 5: Enable the Copilot coding agent (Stage 3)

The coding agent works from a GitHub issue without anyone at the keyboard. It runs in a GitHub
Actions environment, pushes to a branch, and opens a draft pull request. Your Jenkins build and
your reviewers decide what happens next.

### Prerequisites

- An administrator has enabled the coding agent for your organisation and for this repository.
  The setting and plan names change over time; ask your GitHub administrators, and see the
  current GitHub documentation for "Copilot coding agent".
- GitHub Actions is permitted on the repository, because the agent's environment is an Actions
  runner.
- Steps 1 to 3 are complete. The agent reads the conventions file and works from the issue.

### Add a setup workflow

The agent needs your build tools and dependencies to run the verify command. GitHub runs a
workflow file named `copilot-setup-steps.yml` before the agent starts. The job inside it must be
named `copilot-setup-steps`.

Create `.github/workflows/copilot-setup-steps.yml`. This version is for a Maven project; it is
the same as this repository's file.

```yaml
name: Copilot Setup Steps

on:
  workflow_dispatch:
  push:
    paths:
      - .github/workflows/copilot-setup-steps.yml
  pull_request:
    paths:
      - .github/workflows/copilot-setup-steps.yml

jobs:
  copilot-setup-steps:
    runs-on: ubuntu-latest
    permissions:
      contents: read
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven

      - name: Warm the Maven cache
        run: mvn -B -q -ntp dependency:go-offline

      - name: Prove the build works
        run: mvn -B -ntp -DskipTests compile
```

For a Gradle project, set `cache: gradle` and replace the last two steps with
`./gradlew --no-daemon dependencies` and `./gradlew --no-daemon compileJava`. Match the JDK
(Java Development Kit) version to the one your Jenkins build uses.

**Check it works:** push the file, open the **Actions** tab and run the workflow manually. If it
fails here, the agent will fail in the same way.

### Internal package repositories and network access

Most enterprise builds download dependencies from an internal mirror (for example Artifactory or
Nexus) rather than from Maven Central. The coding agent's environment needs the same access.

- **Credentials:** store mirror credentials as secrets in a GitHub environment named `copilot`,
  and reference them from the setup workflow (for example, by writing a Maven `settings.xml`).
  Never commit them. Check the current GitHub documentation for how the coding agent picks up
  environment secrets and variables.
- **Network:** the coding agent runs behind a firewall with an allowlist. If your mirror is not
  reachable, the setup workflow or the agent's build fails with connection errors. Ask your
  GitHub administrators to add the host to the allowlist, or to provide a runner inside your
  network if your organisation supports that.
- **Data:** if the build needs anything beyond source code and dependencies (test databases,
  internal services), the agent will not have it. Keep tasks to those the verify command can
  check on its own.

### Assign an issue

1. Create an issue from the **Agent-ready task** template and have it reviewed.
2. In the issue, set **Assignees** to **Copilot**.
3. Within a minute the agent acknowledges the issue and opens a draft pull request. Open it and
   select **View session** to watch its progress, including the commands it runs.
4. When it finishes, it marks you as a reviewer. The draft pull request lists the files changed
   and how it tested them, if your conventions file asks for that.

### Iterate through review comments

You steer the agent through pull request review comments, not by editing its branch.

- Leave a review comment that mentions `@copilot` and states exactly what to change, for
  example: "@copilot the coverage gate failed on PortfolioService. Add a unit test for the
  unsupported currency branch. Do not change the threshold."
- The agent picks up the comment, pushes a new commit, and Jenkins builds it again.
- If it misunderstands twice, close the pull request, improve the issue, and assign it again.
  The second issue is usually better than a third comment.

---

## Step 6: Connect the coding agent to Jenkins

Jenkins needs no changes. The agent pushes a branch to your repository and opens a pull
request, so a Multibranch Pipeline builds it like any other pull request.

Confirm the following.

1. **Jenkins discovers pull requests from branches in the repository.** The agent's branches
   are created in the same repository, typically under a `copilot/` prefix.
2. **The status check is required.** Branch protection lists the Jenkins check as required, so
   a red build blocks the merge.
3. **Pull request builds have no deployment credentials.** Agent-written code runs on your
   Jenkins agents during the build. Pull request builds must not be able to read deployment
   credentials or reach production. In this repository, only `main` can push images or deploy.
4. **Failures reach the agent.** The agent may not read Jenkins logs itself. When a build fails,
   copy the relevant lines (the failing test and assertion, not the whole log) into a review
   comment that mentions `@copilot`. Do not paste secrets, internal URLs or client data from
   logs.

Optional: this repository's `scripts/triage_agent.py` reads a failed Jenkins log, asks a model
for a diagnosis, and posts it as a pull request comment. Only adopt something similar if your
AI policy allows build logs to be sent to the model provider, and trim the log first.

---

## Step 7: Review an agent's pull request

Review the outcome, not the agent. The same standards apply as for a pull request from a new
colleague, with extra attention to the points below.

### Review checklist

- [ ] **Read the issue first.** Check each acceptance criterion against the diff, one by one.
- [ ] **Read every changed line.** A green build shows that the checks passed; it does not show
      the change is correct.
- [ ] **Check the tests test the right thing.** Look for assertions copied from the
      implementation's output, tests that cannot fail, and mocks that bypass the logic under
      test.
- [ ] **Check the gates were not weakened.** Look for changed thresholds, new exclusions,
      `@Disabled` tests, or edits to the build file the issue did not ask for.
- [ ] **Check scope.** Files outside the issue's scope should not change. Unrequested
      "improvements" belong in a separate issue.
- [ ] **Check dependencies.** Any new dependency needs a reason, and must come from your
      approved sources.
- [ ] **Check for secrets and data.** No credentials, hostnames, or real client data in code,
      tests or fixtures.
- [ ] **Run it.** For behaviour changes, check out the branch and exercise the change yourself.

### Accountability

GitHub requires an independent review for coding agent pull requests: the person who assigned
the issue to Copilot cannot be the one who approves the resulting pull request where approvals
are required. Keep that separation in your team's process even where the tool does not enforce
it. The person who approves the merge owns the change, exactly as for any other pull request.

---

## Improve the setup over time

Treat the first few weeks as an experiment and record what happens.

| Observation | Action |
|-------------|--------|
| The agent breaks the same convention twice | Add or sharpen a rule in the conventions file |
| Reviewers ask the same question about several pull requests | Add a section to the issue template |
| The agent stalls on dependencies or the network | Fix the setup workflow or the firewall allowlist |
| A pull request passed the build but was wrong | Add a test or a gate that would have caught it |
| Issues are too large to review | Lower the scope limit in your team's guidance |

Review the conventions file and the issue template together at a team retrospective once a
month. Both are part of the system, and both are reviewed in pull requests like code.

---

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|--------------|-----|
| Copilot does not appear in the issue's Assignees list | The coding agent is not enabled for the organisation or repository | Ask your GitHub administrators; use agent mode meanwhile |
| The agent's session fails before it starts work | The setup workflow fails | Run `copilot-setup-steps.yml` manually from the Actions tab and fix it there |
| Dependency downloads fail in the agent's session | Internal mirror unreachable or credentials missing | Add mirror credentials to the `copilot` environment; ask for the host to be allowlisted |
| The agent ignores a convention | The rule is vague, buried, or contradicts another rule | Rewrite it as a specific, checkable rule near the top of the file |
| The agent lowers a coverage threshold to pass | The rule is only stated, not enforced | Reject the change; protect the build file through code owners or review rules |
| Jenkins does not build the agent's pull request | Multibranch job does not discover the branch, or the webhook is not delivered | Run "Scan Repository Now" in Jenkins; check the webhook's recent deliveries in GitHub |
| Jenkins is green but the change is wrong | The issue's acceptance criteria were incomplete | Improve the issue template; add the missing test |
| Agent mode runs a command you did not expect | Auto-approval is enabled for that command | Turn auto-approval off and approve commands individually |

---

## Governance summary

Before you use the coding agent on a team repository, you should be able to answer each of
these questions. They are the questions an approval process is likely to ask.

| Question | Where the answer comes from in this setup |
|----------|-------------------------------------------|
| Who approved the use of AI on this repository? | Your tech lead and the internal AI policy (Before you start) |
| What can the agent change? | The issue's scope and the conventions file (Steps 2 and 3) |
| What stops a bad change? | The verify command, the Jenkins check and branch protection (Steps 1 and 6) |
| What credentials can the agent's code reach? | None for deployment: only `main` builds can deploy (Steps 1 and 6) |
| Who reviews and who is accountable? | An independent reviewer who approves the merge (Step 7) |
| What is the audit trail? | The issue, the pull request timeline, the agent's session log and the Jenkins build |
| What data leaves the firm? | Repository code and issue text sent to the Copilot service; build logs only if you send them |

If an answer is unclear for your project, resolve it before you assign the first issue.
