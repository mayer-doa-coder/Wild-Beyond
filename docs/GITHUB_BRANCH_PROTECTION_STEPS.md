# GitHub Branch Protection Enforcement (Phase 4)

Repository:

- https://github.com/mayer-doa-coder/Wild-Beyond

## Manual Configuration (GitHub UI)

1. Open repository Settings.
2. Open Branches.
3. Under Branch protection rules, click Add rule.
4. Branch name pattern: main.
5. Enable Require a pull request before merging.
6. Set required approvals to 1.
7. Enable Dismiss stale pull request approvals when new commits are pushed.
8. Enable Require status checks to pass before merging.
9. Enable Require branches to be up to date before merging.
10. Add required status check for CI workflow job (build job from .github/workflows/ci.yml).
11. Enable Include administrators (recommended).
12. Save changes.

## Expected Enforcement Behavior

- Direct push to main is blocked.
- Pull request cannot merge without at least one approval.
- Pull request cannot merge when CI fails.
- Pull request cannot merge when branch is out of date.

## Verification Procedure

1. Attempt direct push to main. Expected result: rejected by branch protection.
2. Open PR with no approval. Expected result: merge disabled.
3. Open PR with failing CI. Expected result: merge disabled.
4. Approve PR and ensure CI passes. Expected result: merge enabled.

## Evidence Capture Requirement

Capture screenshot from GitHub branch protection page showing enabled options:

- Require a pull request before merging
- Required approvals = 1
- Dismiss stale approvals
- Require status checks to pass
- Require branch to be up to date

Save screenshot as:

- docs/github-branch-protection.png
