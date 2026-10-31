#!/usr/bin/env bash
# Creates the demo issues in demo/issues/*.md as GitHub issues, with labels.
#
# Usage:
#   scripts/create_issues.sh                # uses the repo of the current directory
#   scripts/create_issues.sh owner/repo     # explicit repo
#
# Requires: gh (authenticated: `gh auth login`). Idempotent-ish: skips an issue if one
# with the same title already exists (open or closed).
set -euo pipefail

REPO="${1:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"
ISSUE_DIR="$(cd "$(dirname "$0")/.." && pwd)/demo/issues"

echo "Creating issues in $REPO from $ISSUE_DIR"

# --- labels --------------------------------------------------------------
ensure_label() { # name colour description
  gh label create "$1" --repo "$REPO" --color "$2" --description "$3" --force >/dev/null
}
ensure_label "bug"                 "d73a4a" "Something isn't working"
ensure_label "enhancement"         "a2eeef" "New feature or request"
ensure_label "tests"               "0e8a16" "Test coverage work"
ensure_label "demo"                "fbca04" "Used live in the presenter demo"
ensure_label "breakout"            "c5def5" "Hands-on exercise for graduate pairs"
ensure_label "stage-1-general-ai"  "bfd4f2" "Solved with Copilot Chat (ask mode)"
ensure_label "stage-2-agent"       "bfd4f2" "Solved with Copilot agent mode in the IDE"
ensure_label "stage-3-agentic"     "bfd4f2" "Assigned to the Copilot coding agent"

# --- issues --------------------------------------------------------------
existing="$(gh issue list --repo "$REPO" --state all --limit 200 --json title -q '.[].title')"

for file in "$ISSUE_DIR"/*.md; do
  # Front matter: title: "..." and labels: a, b, c
  title="$(sed -n 's/^title: *"\(.*\)"$/\1/p' "$file" | head -1)"
  labels="$(sed -n 's/^labels: *//p' "$file" | head -1 | sed 's/, */,/g')"
  # Body = everything after the closing '---' of the front matter
  body="$(awk 'BEGIN{c=0} /^---$/{c++; next} c>=2{print}' "$file")"

  if grep -Fxq "$title" <<<"$existing"; then
    echo "  skip (exists): $title"
    continue
  fi
  url="$(gh issue create --repo "$REPO" --title "$title" --label "$labels" --body "$body")"
  echo "  created: $url"
done

echo "Done. Assign the 'stage-3-agentic' issue to Copilot in the GitHub UI when you reach that demo step."
