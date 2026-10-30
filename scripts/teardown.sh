#!/usr/bin/env bash
# Destroy every AWS resource created for the demo.
#
#   ./scripts/teardown.sh          # asks for confirmation
#   ./scripts/teardown.sh --yes    # no prompt (e.g. from a cron/CI job)
#
# Environment:
#   AWS_REGION  (default eu-west-2)
set -euo pipefail

AWS_REGION="${AWS_REGION:-eu-west-2}"
APP_NAME="${APP_NAME:-portfolio-valuation}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TF_DIR="${REPO_ROOT}/infra/terraform"

echo "Account: $(aws sts get-caller-identity --query Account --output text)  Region: ${AWS_REGION}"

if [[ "${1:-}" != "--yes" ]]; then
  read -r -p "This will DESTROY the ${APP_NAME} stack in the account above. Type 'yes' to continue: " answer
  [[ "${answer}" == "yes" ]] || { echo "Aborted."; exit 1; }
fi

echo "==> terraform destroy"
cd "${TF_DIR}"
terraform init -input=false >/dev/null
terraform destroy -auto-approve -var "region=${AWS_REGION}"

# ECS can recreate the log group after Terraform deleted it (a task that was
# still shutting down writes a final line).  Remove it if it came back.
echo "==> removing leftover log group /ecs/${APP_NAME} (if any)"
aws logs delete-log-group --region "${AWS_REGION}" --log-group-name "/ecs/${APP_NAME}" 2>/dev/null \
  && echo "    deleted" || echo "    none left"

cat <<MSG

Done.  Remaining manual steps:
  * Remove the Jenkins credentials (aws-sandbox, aws-account-id, anthropic-api-key, github-token)
    if the Jenkins instance is staying around.
  * Deactivate/delete the jenkins-demo IAM user's access key in the sandbox account.
  * Optionally: cd jenkins && docker compose down -v
MSG
