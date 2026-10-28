# Employee Management Service

A standalone employee CRUD application (Spring Boot + Angular) intended to be **deployed through EnvGuard**. This is **not** EnvGuard itself.

| Component | Local port | Notes |
|-----------|------------|--------|
| Backend API | **8081** | Avoids EnvGuard on 8080 |
| Angular UI | **4201** | `ng serve --port 4201` |
| MySQL (Docker) | **3307** | Avoids EnvGuard MySQL on 3306 |

## Prerequisites

- Java 21, Maven 3.9+
- Node 24+, Angular CLI 22+
- Docker (for MySQL / full stack)
- Python 3.10+ (optional, for `scripts/validate-config.py`)

## Project layout

```
employee-management-service/
├── backend/                 # Spring Boot 3.3.5 API
├── frontend/                # Angular SPA
├── deploy/env/              # Non-secret EnvGuard env YAML + schema
├── scripts/                 # validate-config.py, deploy-ec2.sh
├── .github/workflows/       # CI/CD + secret scan
├── docker-compose.yml
└── Dockerfile
```

## Quick start (PowerShell)

### 1. Start MySQL

```powershell
cd C:\Users\DELL\Desktop\Task\employee-management-service
Copy-Item .env.example .env
# Edit .env and set DB_PASSWORD
docker compose up -d mysql
```

### 2. Run the backend

```powershell
cd backend
$env:APP_PORT="8081"
$env:DB_HOST="localhost"
$env:DB_PORT="3307"
$env:DB_NAME="employee_db"
$env:DB_USER="employee"
$env:DB_PASSWORD="changeme"   # match .env / compose
mvn spring-boot:run
```

Health check: http://localhost:8081/api/health

### 3. Run the frontend

```powershell
cd frontend
npm install
npm start
# serves on http://localhost:4201 — API base http://localhost:8081/api
```

## Quick start (Linux / macOS)

```bash
cd employee-management-service
cp .env.example .env
docker compose up -d mysql

cd backend
export APP_PORT=8081 DB_HOST=localhost DB_PORT=3307 DB_NAME=employee_db DB_USER=employee DB_PASSWORD=changeme
mvn spring-boot:run

# another terminal
cd frontend && npm install && npm start
```

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_PORT` | `8081` | HTTP port |
| `LOG_LEVEL` | `INFO` | Root / app log level |
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3307` | MySQL port |
| `DB_NAME` | `employee_db` | Database name |
| `DB_USER` | `employee` | DB user |
| `DB_PASSWORD` | _(empty)_ | DB password — **required** for real MySQL |

No credentials are hardcoded in source.

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/employees` | Create |
| `GET` | `/api/employees` | List (`q`, `department`, pagination) |
| `GET` | `/api/employees/{id}` | Get by id |
| `PUT` | `/api/employees/{id}` | Update |
| `DELETE` | `/api/employees/{id}` | Delete |
| `GET` | `/api/health` | Public health |

## Tests & build

```powershell
cd backend
mvn -B test

cd ..\frontend
npm install
npm run build
```

Validate EnvGuard YAML:

```powershell
pip install pyyaml jsonschema
python scripts/validate-config.py
```

## Docker Compose (API + DB)

```powershell
# Build and start MySQL + backend (frontend profile optional)
docker compose up -d --build mysql backend

# Optional static frontend after `npm run build` in frontend/
docker compose --profile frontend up -d frontend
```

## How EnvGuard integrates

This app is **separate** from EnvGuard (sibling folder `../envguard`). EnvGuard must already be running, and you should create a project named `employee-management-service` (or set `ENVGUARD_PROJECT_ID`).

1. CI builds JAR + frontend, scans secrets, validates `deploy/env/*.yaml` against `schema.json`.
2. CI creates an artifact tarball and SHA256 digest.
3. CI registers a build: `POST {ENVGUARD_URL}/api/pipeline/builds` with `X-EnvGuard-Pipeline-Token`.
4. CI creates a deployment: `POST .../api/pipeline/deployments` with `environment`, `idempotencyKey`, `requesterIdentity`, `configYaml`, digests, and `projectName` / `projectId`.
5. CI polls `.../deployments/{id}/status` until `APPROVED` / `REJECTED` / `TIMED_OUT` / `FAILED` (responses use `{ success, data }`).
6. On `APPROVED`, CI (if AWS secrets are configured) assumes an OIDC role, resolves Secrets Manager refs, and deploys via SSM.

**Required CI secrets:** `ENVGUARD_URL`, `ENVGUARD_PIPELINE_TOKEN`  
**Optional:** `ENVGUARD_PROJECT_ID`  
**Required for EC2 deploy:** `AWS_ROLE_ARN`, `AWS_REGION`, `EC2_INSTANCE_ID`, `ARTIFACT_S3_BUCKET`, `DEPLOY_HEALTH_URL`

If AWS or EnvGuard secrets are missing, the job **fails clearly** — it does not invent success.

Deploy YAML only stores **secret references** (paths), never values. See [SECURITY.md](SECURITY.md) and `../envguard/docs/SETUP_GUIDE.md`.
