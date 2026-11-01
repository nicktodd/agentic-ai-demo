#!/usr/bin/env bash
# Build the Docker image on your laptop and push it to ECR, bypassing Jenkins.
# Used for the very first image (see infra/README.md) or when Jenkins is down.
#
#   ./scripts/deploy_manual.sh            # build + push <git-sha> and latest
#   ./scripts/deploy_manual.sh --apply    # ...then `tofu apply -var image_tag=<sha>`
#
# Environment:
#   AWS_REGION  (default eu-west-2)
#   TF_BIN      (default: tofu if installed, else terraform)
set -euo pipefail

AWS_REGION="${AWS_REGION:-eu-west-2}"
APP_NAME="${APP_NAME:-portfolio-valuation}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TF_DIR="${REPO_ROOT}/infra/terraform"
# OpenTofu is the default; plain Terraform works too (same HCL, same state format).
TF_BIN="${TF_BIN:-$(command -v tofu || command -v terraform || true)}"
[[ -n "${TF_BIN}" ]] || { echo "Neither tofu nor terraform found on PATH" >&2; exit 1; }

# Who are we and where are we pushing?
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
ECR_REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
ECR_IMAGE="${ECR_REGISTRY}/${APP_NAME}"
IMAGE_TAG="$(git -C "${REPO_ROOT}" rev-parse --short=7 HEAD)"

echo "Account ${ACCOUNT_ID} / ${AWS_REGION}  ->  ${ECR_IMAGE}:${IMAGE_TAG}"

echo "==> docker build"
# --platform: Fargate runs x86_64; without this an Apple-silicon Mac would build an arm64 image.
docker build --platform linux/amd64 \
  -t "${APP_NAME}:${IMAGE_TAG}" -t "${APP_NAME}:latest" "${REPO_ROOT}"

echo "==> ecr login"
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

echo "==> push"
docker tag "${APP_NAME}:${IMAGE_TAG}" "${ECR_IMAGE}:${IMAGE_TAG}"
docker tag "${APP_NAME}:${IMAGE_TAG}" "${ECR_IMAGE}:latest"
docker push "${ECR_IMAGE}:${IMAGE_TAG}"
docker push "${ECR_IMAGE}:latest"

if [[ "${1:-}" == "--apply" ]]; then
  echo "==> $(basename "${TF_BIN}") apply (image_tag=${IMAGE_TAG})"
  cd "${TF_DIR}"
  "${TF_BIN}" init -input=false >/dev/null
  "${TF_BIN}" apply -var "region=${AWS_REGION}" -var "image_tag=${IMAGE_TAG}"
  "${TF_BIN}" output health_url
else
  echo
  echo "Pushed. To (re)create the infrastructure with this image:"
  echo "  cd infra/terraform && tofu apply -var image_tag=${IMAGE_TAG}"
fi
