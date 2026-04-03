# Wild Beyond - Quick Start

## Prerequisites

1. Docker Desktop is installed and running.
2. Ports 5432 and 8080 are available.

## 1. Clone Repository

```bash
git clone <your-repo-url>
cd Wild-Beyond
```

## 2. Create Environment File

Copy the template and adjust values if needed.

```bash
cp .env.example .env
```

If copy command is unavailable on your shell, create .env manually with:

```env
DB_NAME=wild_beyond
DB_USERNAME=postgres
DB_PASSWORD=securepassword
DB_HOST=db
DB_PORT=5432
```

## 3. Start Full Stack

Run one command from the repository root:

```bash
docker compose up --build
```

This starts both:

1. PostgreSQL database container
2. Spring Boot application container

## 4. Verify Startup

Expected results:

1. Database container is healthy and running
2. Spring Boot application starts successfully
3. Application is accessible at http://localhost:8080

Useful checks:

```bash
docker compose ps
docker logs wild_beyond_app
docker logs wild_beyond_db
```

## 5. Stop or Reset

Stop containers:

```bash
docker compose down
```

Stop and wipe database volume:

```bash
docker compose down -v
```

## Troubleshooting

1. Confirm Docker is installed and Docker Desktop is running.
2. Check that ports 5432 and 8080 are free.
3. Verify .env exists and includes DB_NAME, DB_USERNAME, DB_PASSWORD, DB_HOST, DB_PORT.
4. If startup fails, run docker compose down -v then docker compose up --build.
