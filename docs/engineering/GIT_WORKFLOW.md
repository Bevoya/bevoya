# Bevoya Git workflow

Version 1 · 21 October 2026
Source: architecture blueprint, section 12.

## 1. Branches

| Branch | Purpose |
|---|---|
| `main` | Stable and deployable. Render deploys from it. Releases are tagged here |
| `develop` | Integration branch and the repository default. Every pull request targets it |
| `type/short-kebab-description` | Short-lived work branches cut from `develop` |
| `release/vX.Y.Z` | Stabilise a version before it goes to `main` |
| `hotfix/*` | Cut from `main`, merged back to `main` and `develop` |

Types: `feature/`, `fix/`, `chore/`, `docs/`, `refactor/`, `test/`, `hotfix/`, `release/`. Lowercase, under about 40 characters.

Create a branch **when you start the work**, not in advance. Early branches drift from `develop` and become merge pain.

If `develop` costs more than it helps while you work solo, collapse to `main` plus short branches. Nothing else in the blueprint depends on it. Record that change as an ADR.

## 2. Daily commands (Git Bash)

```bash
git checkout develop
git pull
git checkout -b feature/java-skeleton
# ...work...
git add -A
git commit -m "feat(api): add Spring Boot skeleton with health endpoint"
git push -u origin feature/java-skeleton
gh pr create --base develop --title "feat(api): Spring Boot skeleton" --body "Closes #12. Definition of done checked."
```

## 3. Commits

Conventional Commits: `type(scope): summary`.

- Types: `feat fix chore docs refactor test perf ci`
- Scopes: `mobile api ai db infra docs`
- Imperative, lowercase summary, no full stop, under about 72 characters.
- One logical change per commit. Never commit secrets, `.env.local`, or real user data.

## 4. Pull requests

- Keep them small and focused. Fill in the pull request template and tick the Definition of Done (`DEFINITION_OF_DONE.md`).
- Topic branch to `develop`: **squash merge**, delete the branch.
- `release/vX.Y.Z` to `main`: **merge commit**, then tag on `main`, then merge `main` back into `develop`.
- `hotfix/*`: cut from `main`, pull request to `main`, tag a patch version, merge back into `develop`.
- Solo: GitHub does not let you approve your own pull request, so require the pull request and green checks with zero approvals. Add one required review and `CODEOWNERS` when a second person joins.

## 5. Tags

```text
v0.1.0  local alpha: the core loop works for you and a few friends
v0.2.0  private beta in Pune (needs privacy, export, deletion, monitoring)
v0.3.0  public Android beta (billing on, Play testing track)
v1.0.0  first public release
```

## 6. Branch protection

Required checks on `main` and `develop`: `audit-ai`, `audit-mobile`, `db`, `java`, `mobile` and `secrets` (the job names; GitHub shows them as `security/audit-ai`, `security/audit-mobile`, `ci-db/db`, `ci-java/java`, `ci-mobile/mobile` and `security/secrets`). No force-push, no deletion, delete head branches after merge.

Protected branches and required status checks on a **private** repository need a paid GitHub plan. On a free plan, the checks still run and report, but nothing blocks a merge. Decide whether to upgrade or enforce by discipline.

## 7. First branches, in order

`chore/ci-pipelines`, `feature/db-schema-migrations`, `feature/java-skeleton`, `feature/java-auth`, `feature/ai-service-skeleton`, `feature/mobile-design-system`, `feature/mobile-navigation`, then the feature branches listed in the blueprint, section 12.

## 8. Local guards

After cloning, run these once on every machine:

```bash
git config core.hooksPath .githooks
git config pull.ff only
```

`core.hooksPath` is a local setting, so a fresh clone does not have it until you run the command.

The hooks in `.githooks/` block pushes to `main` and `develop` (`pre-push`) and commits or merge commits made while `main` or `develop` is checked out (`pre-commit`, `pre-merge-commit`). Always work on a branch. `git commit --no-verify` skips the commit guards in a genuine emergency; the only way to change `main` or `develop` is a pull request.

`pull.ff only` makes `git pull` refuse to create a merge commit instead of opening an editor.
