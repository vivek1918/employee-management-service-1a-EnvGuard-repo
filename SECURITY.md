# Security Policy — Employee Management Service

This application is deployed **through EnvGuard**. Secrets never belong in git or in `deploy/env/*.yaml`.

## Secrets handling

| What | Where it lives |
|------|----------------|
| Non-secret config (ports, hosts, log levels) | `deploy/env/*.yaml` |
| Secret *references* (paths only) | `secretReferences` in those YAML files |
| Actual secret values | AWS Secrets Manager / EnvGuard-managed store |

Example reference (safe to commit):

```yaml
secretReferences:
  dbPassword: employee-service/staging/db-password
  jwtSecret: employee-service/staging/jwt-secret
```

Local development uses a `.env` file copied from `.env.example`. `.env` is gitignored.

## Never commit

- `.env`, `*.pem`, keystores, credential JSON, API tokens
- Real database passwords or JWT secrets
- Cloud access keys

CI runs **Gitleaks** on every push/PR (see `.github/workflows/secret-scan.yml` and `ci-cd.yml`).

## If a secret was committed

1. **Rotate immediately** — invalidate the leaked credential in Secrets Manager / IdP / DB.
2. **Purge history** — remove the secret from git history (`git filter-repo` or BFG) and force-push only after team agreement.
3. **Verify** — re-run Gitleaks; confirm the secret no longer appears in any branch or tag.
4. **Document** — note the incident and rotation time in your ops channel.

## Runtime hardening notes

- Backend reads credentials only from environment variables (`DB_PASSWORD`, etc.).
- API error responses never include stack traces (see `GlobalExceptionHandler`).
- Actuator exposes only `health` and `info` by default.
- Docker images run as a non-root user.

## Reporting

Report suspected secret leaks or vulnerabilities to your security contact / EnvGuard operators. Do not open a public issue that includes secret values.
