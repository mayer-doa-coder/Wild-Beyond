# Wild Beyond CI/CD Pipeline

## Branch Strategy

- main: production-ready branch with deployment enabled.
- dev: integration branch for team development.
- feature/**: feature branches for isolated development.

## CI Triggers

GitHub Actions workflow file: .github/workflows/ci.yml

CI runs on:

- Push to main, dev, and feature/**.
- Pull requests targeting main and dev.

## Build and Test Workflow

Build job executes in this order:

1. Checkout repository.
2. Setup Java 17 with Maven cache.
3. Build with Maven wrapper.
4. Run test suite.
5. Build Docker image.

Deployment is blocked automatically if build or tests fail because deploy depends on a successful build job.

## Conditional Deployment Rules

Render deploy trigger runs only when all conditions are true:

1. Event is push.
2. Target branch is main or dev.
3. Build job completed successfully.

No deployment occurs on feature/** pushes or on pull requests.

Note:
- The deploy job itself always completes (no skipped check status).
- On non-deploy events it records an informational "policy skip" message and exits successfully.

## Render Integration

Deployment is triggered through Render Deploy Hook.

Required GitHub repository secret:

- RENDER_DEPLOY_HOOK

Deploy step:

- Sends POST request to secret deploy hook URL.
- Fails safely if secret is missing.
- Does not print secret value in logs.

## Security Notes

- Deploy hook URL is stored only in GitHub Secrets.
- No deploy URLs are hardcoded in repository files.
- Workflow uses secret environment variable only at runtime.

## CI/CD Flow

Developer -> Push -> CI (Build + Test + Docker build) -> Push to dev/main -> Deploy job -> Render

## Validation Checklist

1. Push to feature branch: CI runs, deploy does not run.
2. Open PR to dev/main: CI runs, deploy does not run.
3. Push to dev or main: CI runs, deploy runs after successful build/tests.
4. Confirm updated service revision in Render dashboard.
