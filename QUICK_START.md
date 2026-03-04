# Wild Beyond — Quick Start

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| Java (JDK) | 17+ | `java -version` |
| Maven | 3.9+ | already cached in `~/.m2/wrapper/dists/` |
| Docker Desktop | any | `docker --version` |
| PowerShell 7 | 7.x | `pwsh --version` |

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
| Password | `wildbeyond123` |

---

## 2. Run the Application

**Option A — VS Code Task (recommended):**
`Ctrl+Shift+B` → **App: Run**  *(default build task)*

**Option B — Terminal (PowerShell) — ttawh's machine:**
```pwsh
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
& 'C:\Users\ttawh\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' `
    -f 'D:\Wild-Beyond\pom.xml' spring-boot:run
```

**Option C — Terminal (PowerShell) — 88017's machine:**
```pwsh
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot'
& 'C:\Users\88017\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' `
    -f 'E:\Wild-Beyond\pom.xml' spring-boot:run
```

Wait for:
```
Tomcat started on port 8080 (http)
Started WildBeyondApplication in X seconds
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

Accounts are created automatically on first startup if they don't already exist.
Check your `DataSeeder.java` for the exact emails and passwords.

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
