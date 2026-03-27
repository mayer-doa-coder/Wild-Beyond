# Wild Beyond — Quick Start

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| Java (JDK) | 17+ | `java -version` |
| Maven | 3.9+ | already cached in `~/.m2/wrapper/dists/` |
| Docker Desktop | any | `docker --version` |
| PowerShell 7 | 7.x | `pwsh --version` |

---

## 0. Configure Environment Variables

Create your local secret file from the template:

```pwsh
Copy-Item .env.example .env
```

Then edit `.env` and set strong values for:

- `DB_NAME`
- `DB_HOST`
- `DB_PORT`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `JWT_SECRET`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_PASSWORD`

---

## 1. Start the Database

**Option A — VS Code Task (recommended):**
`Ctrl+Shift+P` → `Tasks: Run Task` → **DB: Start**

**Option B — Terminal:**
```pwsh
docker-compose up -d
```

Verify it's ready:
```pwsh
docker exec wild_beyond_db pg_isready -U wildbeyond -d wild_beyond
```

Expected output: `localhost:5432 - accepting connections`

| Setting | Value |
|---------|-------|
| Host | `localhost` |
| Port | `5433` |
| Database | `wild_beyond` |
| Username | `wildbeyond` |
| Password | value from `.env` (`DB_PASSWORD`) |

---

## 2. Run the Application

The app now uses Flyway migrations at startup.
Schema is versioned in `src/main/resources/db/migration` and Hibernate runs in validate mode.

**Option A — VS Code Task (recommended):**
`Ctrl+Shift+B` → **App: Run**  *(default build task)*

**Option B — Terminal (PowerShell):**
```pwsh
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_HOST = "localhost"
$env:DB_PORT = "5433"
$env:DB_NAME = "<your-db-name>"
$env:DB_USERNAME = "<your-db-username>"
$env:DB_PASSWORD = "<your-db-password>"
$env:JWT_SECRET = "<your-jwt-secret>"
$env:APP_ADMIN_EMAIL = "<your-admin-email>"
$env:APP_ADMIN_PASSWORD = "<your-admin-password>"
./mvnw.cmd spring-boot:run
```

Wait for:
```
Tomcat started on port 8080 (http)
Started WildBeyondApplication in X seconds
```

You should also see Flyway migration logs on startup (or baseline if schema already exists).

Run with production profile locally (sanity check):
```pwsh
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_HOST = "localhost"
$env:DB_PORT = "5433"
$env:DB_NAME = "wild_beyond"
$env:DB_USERNAME = "wildbeyond"
$env:DB_PASSWORD = "<your-db-password>"
$env:JWT_SECRET = "<your-jwt-secret>"
$env:APP_ADMIN_EMAIL = "<your-admin-email>"
$env:APP_ADMIN_PASSWORD = "<your-admin-password>"
./mvnw.cmd spring-boot:run
```

---

## 3. Open in Browser

| URL | Description | Auth required |
|-----|-------------|---------------|
| http://localhost:8080 | Home / landing | No |
| http://localhost:8080/products | Product listing | No |
| http://localhost:8080/auth/register | Register new account | No |
| http://localhost:8080/auth/login | Login | No |
| http://localhost:8080/dashboard | Role-based dashboard | Yes |
| http://localhost:8080/admin/** | Admin area | ADMIN only |

---

## 4. Seeded Accounts (DataSeeder)

An admin account is created automatically on first startup if it does not already exist.
The credentials come from `.env` (`APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD`).

Query current users:
```pwsh
docker exec wild_beyond_db psql -U wildbeyond -d wild_beyond -c `
  "SELECT u.id, u.email, u.enabled, string_agg(r.name, ', ') AS roles FROM users u LEFT JOIN user_roles ur ON u.id = ur.user_id LEFT JOIN roles r ON ur.role_id = r.id GROUP BY u.id, u.email, u.enabled ORDER BY u.id;"
```

Or use the VS Code task: **DB: Query Users**

---

## 5. Stop Everything

```pwsh
# Stop the app
Ctrl+C  (in the terminal running spring-boot:run)

# Stop the database
docker-compose down

# Stop DB AND wipe all data (fresh DB on next start)
docker-compose down -v
```

---

## VS Code Tasks Reference

| Task | Shortcut / How to run |
|------|-----------------------|
| **App: Run** | `Ctrl+Shift+B` |
| **App: Build** | `Ctrl+Shift+P` → Run Task |
| **DB: Start** | `Ctrl+Shift+P` → Run Task |
| **DB: Stop** | `Ctrl+Shift+P` → Run Task |
| **DB: Recreate (wipe data)** | `Ctrl+Shift+P` → Run Task |
| **DB: Health Check** | `Ctrl+Shift+P` → Run Task |
| **DB: Query Users** | `Ctrl+Shift+P` → Run Task |

---

## Role-Based Dashboard Routing

After login, users are redirected to `/dashboard` which routes by role:

| Role | Redirects to |
|------|-------------|
| `ADMIN` | `templates/admin/dashboard.html` |
| `SELLER` | `templates/seller/dashboard.html` |
| `BUYER` | `templates/buyer/dashboard.html` |
