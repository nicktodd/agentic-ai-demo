# RUNBOOK — Agentic AI for Spring Boot engineers (60 minutes, Zoom)

**Audience:** graduate engineers learning Spring Boot. Stack: GitHub + Copilot, Jenkins, AWS.
**Thesis:** the same task, three ways — *General AI* answers, an *Agent* acts in your IDE
under your supervision, *Agentic AI* owns a task end to end inside your governed pipeline.
The pipeline (tests, coverage gate, PR review, protected `main`) is what makes the third
one safe. **The guardrails are the story, not the model.**

---

## Pre-flight checklist (do the day before, re-check 30 min before)

- [ ] `git pull`, `mvn -B verify` is green locally (16 tests, coverage gate met).
- [ ] App runs: `mvn spring-boot:run`, then `curl localhost:8080/actuator/health` → `UP`.
- [ ] GitHub repo pushed, issues created (`scripts/create_issues.sh`), branch protection on.
- [ ] `copilot-setup-steps.yml` green in Actions.
- [ ] Jenkins up at http://localhost:9090, Multibranch job scanned, `main` built green once.
- [ ] smee client running: `npx smee-client --url https://smee.io/<id> --target http://localhost:9090/github-webhook/`.
- [ ] AWS: `scripts/deploy_manual.sh` run once; open `http://<alb-dns>/actuator/health` → `UP`.
      Keep the ALB URL in a text file for pasting.
- [ ] VS Code open on the repo, Copilot Chat panel visible, font size ≥ 16 for Zoom.
- [ ] Terminal font size ≥ 16. Close Slack/Outlook notifications.
- [ ] Backup material ready (see *Fallbacks*): screenshots of a completed agent PR,
      a Jenkins build page, and an ECS deployment; and the `demo/solutions` branch if you made one.
- [ ] Zoom: share *screen*, not *window*, so switching between VS Code, browser and terminal is smooth.
      Breakout rooms pre-configured for pairs.

**Hard rule:** never fix the planted bugs on `main` before the session. Demo branches go to
`demo/*` and get deleted afterwards (`git push origin --delete demo/stage-1`).

---

## Timeline

| Time | Segment | Mode |
|------|---------|------|
| 0:00–0:05 | Framing: three levels of autonomy | Slides / talk |
| 0:05–0:10 | Tour of the service and its guardrails | Live code |
| 0:10–0:18 | Stage 1: General AI (Copilot Chat ask mode) fixes the FX bug | Live |
| 0:18–0:28 | Stage 2: Agent (Copilot agent mode) closes the coverage gap | Live |
| 0:28–0:40 | Stage 3: Agentic AI (coding agent → PR → Jenkins → review → AWS) | Live, async |
| 0:40–0:52 | Breakouts: pairs take a good-first-issue with agent mode | Hands-on |
| 0:52–0:58 | Debrief, governance talking points, PR merge + deploy lands | Talk |
| 0:58–1:00 | Close: what to try this week | Talk |

Stage 3 is kicked off at **0:28 and runs in the background**. The coding agent typically
takes 5–15 minutes to open a PR, and Jenkins + ECS deploy another 5–8. Start it, then
move on; come back to it at 0:52.

---

## 0:00–0:05 Framing

Say:

> "We'll solve problems in one Spring Boot codebase three ways. Same model family, three
> levels of autonomy. Watch *who holds the keyboard* and *what stops a bad change reaching
> production*."

Draw or show the ladder:

1. **General AI** – you ask, it answers. You copy, paste, test, commit. Autonomy: none.
2. **Agent** – it reads your repo, edits files, runs the build, iterates. You approve every
   step. Autonomy: bounded by your attention.
3. **Agentic AI** – it is *assigned* work, operates in its own environment, opens a PR, and
   your pipeline and reviewers decide. Autonomy: bounded by the system, not by you watching.

Point out: Fidelity-style controls (four-eyes review, CI gates, protected branches, audit
trail) already exist. Agentic AI makes those controls *load-bearing*.

---

## 0:05–0:10 Tour of the service

Terminal:

```bash
mvn spring-boot:run
```

Browser or curl, in this order:

```bash
curl -s localhost:8080/api/portfolios | jq
```

```bash
curl -s localhost:8080/api/portfolios/3/value | jq
```

Point out: controller → service → repository, `BigDecimal` everywhere *except* one place
(don't reveal yet), `@WebMvcTest` and `@SpringBootTest` tests, `pom.xml` with Spotless and
the JaCoCo gate at 85%, `/actuator/health` used by AWS.

Open `.github/copilot-instructions.md`:

> "This file is how we give the AI our team's conventions. Every mode we use today reads it."

---

## 0:10–0:18 Stage 1 — General AI: Copilot Chat (ask mode)

**Task:** issue #1, the FX rounding bug.

1. Show the symptom:

```bash
curl -s "localhost:8080/api/fx/convert?amount=100&from=USD&to=GBP"
```

   Ask the room: 100 / 1.2715 = 78.647… — what should the answer be? (78.65). We get 78.64.

2. Open `FxService.java`. In Copilot Chat (ask mode, **not** agent), type:

   > Why does `convert` return 78.64 for 100 USD to GBP at rate 1.2715? Our convention is
   > BigDecimal with an explicit RoundingMode. Show the corrected method.

3. Read the answer with the room. Expected: it spots `double` + `Math.floor` (truncation,
   not rounding) and proposes `amount.multiply(fromRate).divide(toRate, 2, RoundingMode.HALF_UP)`.

4. **You** paste it in. **You** write the test — or ask Chat: "write a JUnit 5 test that
   would have failed before this fix using rates 1.2715 and 1.0845". Paste it into
   `FxServiceTest`.

5. Run:

```bash
mvn -B verify
```

   Spotless probably fails first (formatting). Say: "the AI didn't run anything — I did.
   That's the point." Fix with `mvn spotless:apply`, re-run.

**Point out:**
- Fast, great for understanding. Zero autonomy: it never saw the build, never ran a test.
- Quality depends entirely on *your* question and *your* verification.
- `git stash` or `git checkout .` the changes now — we want the bug back for Stage 3's
  diff story. (Or commit to `demo/stage-1` and leave `main` untouched.)

---

## 0:18–0:28 Stage 2 — Agent: Copilot agent mode in VS Code

**Task:** issue #3, the coverage gap. (Pick this because it touches tests and the pom, and
the loop "edit → run `mvn verify` → read failure → edit" is visible.)

1. Switch Copilot Chat to **Agent** mode. Paste the issue text, then add:

   > Work through the acceptance criteria. Run `mvn -B verify` yourself and iterate until
   > it passes, including the coverage gate. Follow .github/copilot-instructions.md.

2. Narrate as it works: it reads files, proposes edits (show the diff view), asks to run a
   terminal command — **you click Continue**. It reads the Maven output. If a test fails,
   it fixes and re-runs.

3. When it raises the gate to 0.92 and `verify` passes, open the JaCoCo report:
   `target/site/jacoco/index.html`.

**Point out:**
- It acts, but you're the approval step for every tool call. Autonomy bounded by attention.
- It used the *same* conventions file. Ask: "what would it have done without one?"
- Good for a 20-minute task. Not for "go and do these nine issues."
- Reset again: `git checkout . && git clean -fd` (or commit to `demo/stage-2`).

**If agent mode stalls:** re-prompt with "continue, and run mvn -B verify when done". If
VS Code's terminal approval dialog doesn't appear, check the Chat panel's *Continue* button.

---

## 0:28–0:40 Stage 3 — Agentic AI: coding agent → PR → Jenkins → review → AWS

**Task:** issue #2, the missing `?currency=` endpoint. Do this part in the browser.

1. Open issue #2 on GitHub. Walk through the acceptance criteria aloud, especially:
   *"`mvn -B verify` passes including the 85% coverage gate."*

2. **Assign the issue to Copilot** (Assignees → Copilot). Within ~30 s the agent reacts
   with 👀 and opens a draft PR titled from the issue.

3. Open the draft PR → "View session". Show: it is running in a GitHub Actions sandbox,
   using `copilot-setup-steps.yml` (JDK 21 + Maven cache), reading
   `copilot-instructions.md`, running `mvn verify`.

   Say: "Nobody is holding the keyboard. We go and do something else."

4. While it works (5–15 min), show what will happen next — open the `Jenkinsfile`:
   - PR build: checkout → `mvn -B verify` (tests + Spotless + coverage) → Docker build.
   - `main` only: push image to ECR → new ECS task definition → `ecs wait services-stable`.
   - Show `infra/terraform/alb.tf`: the health check is `/actuator/health`, the same
     endpoint we curl'd at the start.

5. Show the local Jenkins job (http://localhost:9090) with the `main` build green and the
   PR branch list. When the agent's PR appears as `PR-<n>`, open its build.

6. **Go to breakouts (0:40).** Come back at 0:52.

**What to point out when you return (0:52):**
- The PR: description lists files changed and how it tested. Jenkins check is green (or
  red — see below). Review the diff like a human PR: is `currency` validated in the
  service? `BigDecimal` only? Tests at all three layers?
- **If Jenkins is red on the coverage gate:** this is the best possible outcome. Leave a PR
  review comment: "Coverage gate failed — add tests for the new branch." The agent picks up
  review comments and pushes again. Say: "the pipeline just did the code review a senior
  would have done, and the agent responded without a human typing code."
- Approve and merge. Jenkins builds `main`, pushes to ECR, deploys to ECS. Open
  `http://<alb-dns>/api/portfolios/2/value?currency=GBP`. Note the GBP figures are a penny
  off *because bug #1 is still live in `main`* — "and that's why #1 is the first issue we'd
  actually merge."

---

## 0:40–0:52 Breakouts (pairs, 12 minutes)

Post in Zoom chat:

> **Hands-on (12 min, pairs):**
> 1. One of you clones the repo: `git clone <url> && cd agentic-ai-demo && mvn -B verify`
> 2. Pick an unassigned issue labelled `breakout` (#4–#9). Comment "taking this" on it.
> 3. Create a branch: `git checkout -b pair-<names>/issue-<n>`
> 4. In VS Code, open Copilot Chat in **Agent** mode. Paste the issue body and add:
>    "Follow .github/copilot-instructions.md. Write the tests first. Run `mvn -B verify`
>    and iterate until it passes."
> 5. Read every diff before you click Continue. Ask it *why* at least once.
> 6. Push and open a PR. Watch the Jenkins check. Paste the PR link in the chat.
>
> Stretch: get the PR green on first push.

Visit rooms. Typical snags: Maven download time (tell them to start `mvn verify` *before*
reading the issue), Spotless failures (`mvn spotless:apply`), agent lowering the coverage
threshold (call this out — it violates the conventions file; ask them to tell the agent so).

---

## 0:52–0:58 Debrief and governance talking points

Ask two pairs to show their PR for 60 seconds each. Then finish Stage 3 (merge, deploy).

Governance points, pick 4–5:

- **Identity and audit.** The coding agent commits as `Copilot`, in a PR, from a sandbox.
  Every action is in the PR timeline and the session log. Compare with a developer pasting
  Chat output: no trail of *what the AI suggested vs what the human changed*.
- **Least privilege.** The agent has no AWS credentials. Jenkins does, from its credential
  store, only on `main`, only after review. The agent can't deploy; the pipeline can.
- **Gates must be non-negotiable.** Coverage gate, Spotless, required status check, one
  approving review. The conventions file *tells* the agent not to lower the gate; branch
  protection *stops* it. Tell vs stop.
- **Conventions as code.** `copilot-instructions.md` is reviewed in PRs like any file. It's
  how the team's judgement scales to the agent.
- **Humans review outcomes, not keystrokes.** Stage 2 reviews every tool call; Stage 3
  reviews a PR. Which scales? Which is riskier if reviewers rubber-stamp?
- **Data boundary.** What left the building? Repo code went to GitHub's agent sandbox;
  with the triage agent, a trimmed build log goes to a model API. Know your approved tools.
- **Cost and blast radius.** Sandbox AWS account, tear down after. Agents are cheap to run
  and expensive to leave running.
- **Where it fails.** Ambiguous issues → confident wrong PRs. The quality of the issue
  (acceptance criteria) *is* the specification. Writing good issues is the new skill.

---

## 0:58–1:00 Close

> "This week: write one issue with acceptance criteria good enough to assign to an agent.
> Ask a senior to review the issue before the code. That's the habit."

Point them at `demo/issues/` as templates. Post the repo link.

---

## Optional stretch: build triage agent (if there's a red Jenkins build)

```bash
python3 scripts/triage_agent.py --build-url "<jenkins build url>" --repo <org>/agentic-ai-demo --pr <n>
```

Or offline:

```bash
python3 scripts/triage_agent.py --log-file scripts/sample-failed-build.log --repo <org>/agentic-ai-demo --pr <n> --dry-run
```

Talking point: a single LLM call with a strict output shape, read-only on the repo,
comment-only on the PR. "Agentic" doesn't have to mean autonomous — start with the
smallest useful loop.

---

## Fallbacks

| Failure | Do this |
|---------|---------|
| Copilot coding agent not available / not picking up the issue | Show the pre-recorded screenshots of a previous run; open an existing merged PR in the repo's history. Or do Stage 3 with agent mode in VS Code and push the PR yourself, narrating what the coding agent would have done differently (sandbox, no keyboard). |
| Agent PR takes > 15 min | Carry on with breakouts; it's async by design and that's a talking point. Return at 0:52. |
| Jenkins not receiving webhooks (smee down) | Click **Scan Multibranch Pipeline Now**. Or set the job to scan every minute. |
| Jenkins can't reach Docker / AWS | Skip image/deploy stages: comment out `when { branch 'main' }` stages or show the `main` build history. Open the ALB URL from the pre-flight deploy. |
| ECS deploy slow (`services-stable` waits ~3–5 min) | Expected. Show the ECS console *Deployments* tab while talking. |
| Local app port 8080 busy | `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
| Spotless fails on AI-generated code | Normal. `mvn spotless:apply`. Point out it's why lint is in `verify`. |
| Agent lowers `jacoco.minimum.line` | Perfect teaching moment: show the conventions file forbids it; a human reviewer catches it; branch protection would have blocked a direct push. |
| Zoom screen share dies | Keep the GitHub PR URL in chat so attendees can follow in their own browser. |
| Internet dies | Local only: Stage 1 and 2 still work with VS Code (Copilot needs network — fall back to reading `demo/issues` and walking through `Jenkinsfile`). |

After the session:

```bash
scripts/teardown.sh
```

and delete the `demo/*` branches and the Jenkins credentials if the sandbox is shared.
