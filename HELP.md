# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.5/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.5/maven-plugin/build-image.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.5/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.5/reference/web/servlet.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

## Profiles and secret handling

The application now uses Spring profiles for environments:
- `dev`
- `test`
- `uat`
- `prod`

Profile files:
- `application-dev.yaml`
- `application-test.yaml`
- `application-uat.yaml`
- `application-prod.yaml`

No passwords are stored in any `application*.yaml`.  
Secrets are provided only via environment variables.

Credential validation applied at startup:

- `APP_BOOTSTRAP_ADMIN_USERNAME` must contain 1–100 letters, digits, dots,
  underscores, or hyphens.
- `APP_BOOTSTRAP_ADMIN_EMAIL`, when provided, must be a valid email address of at
  most 320 characters.
- `APP_BOOTSTRAP_ADMIN_PASSWORD` must contain 12–72 UTF-8 bytes and must not contain
  the rejected placeholder markers `change-me` or `replace-me`.
- `JWT_SECRET` must contain at least 32 UTF-8 bytes and must not contain
  `change-me` or `replace-me`.
- `JWT_EXPIRATION_MS` must be between 60,000 and 86,400,000 milliseconds.

User passwords supplied through the API use a separate policy: 8 or more characters
and no more than 72 UTF-8 bytes, matching bcrypt's input limit. Login accepts a
username or email; usernames are limited to letters, digits, `.`, `_`, and `-`, and
emails must be syntactically valid.

Authentication hardening:

- Ten failed logins against one account in five minutes are rejected with HTTP 429.
- Thirty failed logins from one address in five minutes are rejected with HTTP 429.
- Registration is limited to five attempts per address per hour.
- These development-safe counters are process-local. A multi-instance deployment
  should replace or supplement them with a shared gateway/Redis-backed limiter.
- Browser access tokens are kept in memory and are therefore cleared by a full page
  reload. A future persistent-login flow should use rotating refresh tokens in
  `HttpOnly`, `Secure`, `SameSite` cookies.

`frontend/Dockerfile` runs the Vite development server. Production builds should use
`frontend/Dockerfile.prod`, which serves static assets with nginx and security headers.

## Local (dev) run

### Run everything with Docker

After creating `.env` from `.env.example`, start PostgreSQL, Adminer, the Spring
Boot backend, and the Vite frontend with one command:

```bash
docker compose up --build
```

Compose waits for PostgreSQL to become healthy before starting the backend, runs
Flyway migrations as part of Spring Boot startup, and waits for the backend
readiness check before starting the frontend. The services are available at:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:9000/api/v1`
- Adminer: `http://localhost:8090`

Stop the stack with `Ctrl+C`, or use `docker compose down` if it was started in
detached mode.

### Run the backend on the host

1. Create env file:

```bash
cp .env.example .env
```

2. Review the local-only values in `.env`:
- `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (at least 32 chars)
- `APP_BOOTSTRAP_ADMIN_PASSWORD` (at least 12 characters and at most 72 UTF-8 bytes)

The `dev` profile loads the extensionless `.env` file as a properties file. In
PowerShell, no manual import is required when running from the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

3. Start local PostgreSQL:

```bash
docker compose up -d postgres
```

4. Run the app with env vars:

```bash
set -a; source .env; set +a
./mvnw spring-boot:run
```

5. Optional DB UI:

```bash
docker compose up -d adminer
```

## Running specific environments

Use the same artifact and provide env vars per environment:

```bash
SPRING_PROFILES_ACTIVE=test ./mvnw spring-boot:run
SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

In CI/CD, inject these variables from your secret manager:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `APP_BOOTSTRAP_ADMIN_USERNAME`
- `APP_BOOTSTRAP_ADMIN_EMAIL`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`

## Health, readiness, liveness, startup probes (Actuator)

The app now exposes production-style probe endpoints via Spring Boot Actuator.

Public probe endpoints:
- `/livez` -> liveness
- `/readyz` -> readiness
- `/api/v1/actuator/health/startup` -> startup semantics

Additional actuator health endpoints:
- `/api/v1/actuator/health`
- `/api/v1/actuator/health/liveness`
- `/api/v1/actuator/health/readiness`
- `/api/v1/actuator/health/startup`

Example checks:

```bash
curl -i http://localhost:9000/api/v1/livez
curl -i http://localhost:9000/api/v1/readyz
curl -i http://localhost:9000/api/v1/actuator/health/startup
```

Suggested probe mapping (for orchestrators like Kubernetes):
- **startupProbe** -> `/api/v1/actuator/health/startup`
- **livenessProbe** -> `/livez`
- **readinessProbe** -> `/readyz`

## Restore maintenance API hooks (switch ON/OFF)

Restore mode can be toggled via API (admin-only), so scripts can switch traffic blocking on/off:

- `POST /ops/restore/enable`
- `POST /ops/restore/disable`
- `GET /ops/restore/status`

Typical restore flow:

```bash
# 1) login as admin and extract token
TOKEN="$(curl -sS -X POST 'http://localhost:9000/api/v1/auth/login' \
  -H 'Content-Type: application/json' \
  -d "{\"identifier\":\"${APP_BOOTSTRAP_ADMIN_USERNAME:-admin}\",\"password\":\"${APP_BOOTSTRAP_ADMIN_PASSWORD}\"}" \
  | jq -r '.accessToken')"

# 2) switch restore mode ON
curl -sS -X POST 'http://localhost:9000/api/v1/ops/restore/enable' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"reason":"database restore"}'

# ... run restore commands ...

# 3) switch restore mode OFF
curl -sS -X POST 'http://localhost:9000/api/v1/ops/restore/disable' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"reason":"restore complete"}'
```

