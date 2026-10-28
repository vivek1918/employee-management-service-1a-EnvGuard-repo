#!/usr/bin/env bash
# Deploy employee-management-service JAR and frontend static files to EC2 via SSM + systemd/nginx.
# Requires: AWS CLI, jq, and IAM permissions for SSM / S3 artifact access.
# This script FAILS clearly when required env vars are missing — it does not invent success.

set -euo pipefail

required_vars=(
  AWS_REGION
  EC2_INSTANCE_ID
  ARTIFACT_S3_URI
  ARTIFACT_SHA256
  DEPLOY_ENV
)

missing=()
for v in "${required_vars[@]}"; do
  if [[ -z "${!v:-}" ]]; then
    missing+=("$v")
  fi
done

if ((${#missing[@]} > 0)); then
  echo "ERROR: Missing required environment variables:" >&2
  printf '  - %s\n' "${missing[@]}" >&2
  echo "Configure CI secrets / AWS OIDC before deploying." >&2
  exit 1
fi

APP_NAME="employee-management-service"
REMOTE_APP_DIR="/opt/${APP_NAME}"
REMOTE_WEB_DIR="/var/www/${APP_NAME}"
SERVICE_UNIT="${APP_NAME}.service"

echo "==> Deploying ${APP_NAME} to instance ${EC2_INSTANCE_ID} (${DEPLOY_ENV})"
echo "==> Artifact: ${ARTIFACT_S3_URI}"
echo "==> Expected SHA256: ${ARTIFACT_SHA256}"

COMMAND_ID=$(aws ssm send-command \
  --region "${AWS_REGION}" \
  --instance-ids "${EC2_INSTANCE_ID}" \
  --document-name "AWS-RunShellScript" \
  --comment "Deploy ${APP_NAME} ${DEPLOY_ENV}" \
  --parameters commands="[
    \"set -euo pipefail\",
    \"mkdir -p ${REMOTE_APP_DIR} ${REMOTE_WEB_DIR} /tmp/${APP_NAME}\",
    \"aws s3 cp '${ARTIFACT_S3_URI}' /tmp/${APP_NAME}/artifact.tar.gz\",
    \"echo '${ARTIFACT_SHA256}  /tmp/${APP_NAME}/artifact.tar.gz' | sha256sum -c -\",
    \"tar -xzf /tmp/${APP_NAME}/artifact.tar.gz -C /tmp/${APP_NAME}\",
    \"install -m 0644 /tmp/${APP_NAME}/*.jar ${REMOTE_APP_DIR}/app.jar\",
    \"if [ -d /tmp/${APP_NAME}/frontend ]; then rsync -a --delete /tmp/${APP_NAME}/frontend/ ${REMOTE_WEB_DIR}/; fi\",
    \"systemctl daemon-reload\",
    \"systemctl restart ${SERVICE_UNIT}\",
    \"systemctl is-active ${SERVICE_UNIT}\",
    \"curl -fsS http://127.0.0.1:8081/api/health\"
  ]" \
  --query "Command.CommandId" \
  --output text)

echo "==> SSM CommandId: ${COMMAND_ID}"

for i in $(seq 1 60); do
  STATUS=$(aws ssm get-command-invocation \
    --region "${AWS_REGION}" \
    --command-id "${COMMAND_ID}" \
    --instance-id "${EC2_INSTANCE_ID}" \
    --query "Status" \
    --output text 2>/dev/null || echo "Pending")
  echo "    poll ${i}: ${STATUS}"
  case "${STATUS}" in
    Success)
      echo "==> Deploy succeeded"
      exit 0
      ;;
    Failed|Cancelled|TimedOut)
      echo "ERROR: SSM command ${STATUS}" >&2
      aws ssm get-command-invocation \
        --region "${AWS_REGION}" \
        --command-id "${COMMAND_ID}" \
        --instance-id "${EC2_INSTANCE_ID}" \
        --output json >&2 || true
      exit 1
      ;;
  esac
  sleep 5
done

echo "ERROR: Timed out waiting for SSM deploy" >&2
exit 1
