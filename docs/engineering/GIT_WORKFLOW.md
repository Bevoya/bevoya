# Bevoya Git workflow

Version 1 · 20 October 2026
The repository is `bevoya`, owned by your GitHub organisation. Local path: `C:\Projects\bevoya`, using Git Bash in VS Code.

## 1. Model

- `main`: stable, deployable. Render deploys from it. Release tags go here.
- `develop`: integration branch and the repository default. Every pull request targets it.
- Topic branches are cut from `develop` and merged back with a pull request. `release/*` stabilises a version. `hotfix/*` is cut from `main`.
- With one or two developers `develop` adds ceremony. It stays because the project brief specifies it; collapse to `main` plus short branches if it costs you time.

## 2. Branches and their jobs

| Branch | Job |
|---|---|
| `main` | Stable code and release tags |
| `develop` | Integration; default branch |
| `chore/ci-pipelines` | GitHub Actions: Java, Python, mobile, security scan, Dependabot |
| `chore/render-deploy` | `render.yaml`, Dockerfiles, production env checklist |
| `feature/db-schema-migrations` | Flyway migrations, RLS, schema tests |
| `feature/java-skeleton` | Spring Boot project, `/health`, error format, request IDs |
| `feature/java-auth` | JWT validation, `CurrentUser`, `/me`, profile creation |
| `feature/ai-service-skeleton` | FastAPI project, internal token, Gemini adapter |
| `feature/mobile-design-system` | Theme tokens, fonts, UI components |
| `feature/mobile-navigation` | Router, tabs, auth gate, Supabase sign-in |
| `feature/mobile-onboarding` | Onboarding, 18+ confirmation, consents |
| `feature/goals-roadmaps` | Goals and milestones: API and screens |
| `feature/daily-planner` | Tasks and today's plan |
| `feature/session-tracking` | Focus timer and session summary |
| `feature/learning-hub` | Notes, skills, evidence |
| `feature/journal-reflection` | Journal and reflections |
| `feature/offline-drafts` | SQLite drafts and offline queue |
| `feature/ai-coach` | Reflection extraction, roadmap proposals, AI insight cards |
| `feature/ai-quizzes` | Quiz generation and attempts |
| `feature/weekly-review` | Daily and weekly review, basic analytics |
| `feature/notifications-reminders` | Push notifications and reminder jobs |
| `feature/privacy-settings` | Consents, AI-memory toggles, privacy screens |
| `feature/account-export-deletion` | Data export and full account deletion |
| `feature/observability` | Sentry and PostHog with redaction and allow-list |
| `feature/subscriptions` | Google Play Billing and entitlements |
| `test/jwt-and-auth-security` | Token security tests |
| `test/cross-user-authorization` | User B cannot read user A's data, on every endpoint |
| `test/schema-rls-assertions` | RLS on every table; foreign keys reject cross-user links |
| `test/ai-safety-fixtures` | Prompt injection, malformed output, no-advice tests |
| `test/mobile-e2e-maestro` | End-to-end Android flows |
| `test/accessibility-checks` | TalkBack, 200% font scale, contrast checks |
| `docs/privacy-and-legal` | Privacy policy, terms, data-safety answers |
| `docs/security-threat-model` | Threat model work |

Created only when needed: `fix/<name>`, `hotfix/<name>`, `release/v0.1.0`, `refactor/<name>`. Short-lived scaffolding branches (for example `chore/mobile-scaffold`, `docs/project-docs`) are deleted after merging.

## 3. Rules

- Names: `type/short-kebab-description`, lowercase. Types: `feature chore docs test fix refactor hotfix release`.
- Commits: Conventional Commits, `type(scope): summary`. Types: `feat fix chore docs refactor test perf ci`. Scopes: `mobile api ai db infra docs`.
- Topic branch to `develop`: pull request, squash merge, delete the branch after merging. Keep PRs small.
- `release/vX.Y.Z` to `main`: merge commit, tag, then merge `main` back into `develop`.
- Delete a branch only after its PR is merged. Before starting on an idle branch, run `git merge develop`.
- Tags: `v0.1.0` local alpha; `v0.2.0` private beta; `v0.3.0` public Android beta; `v1.0.0` first public release.

## 4. Protection

Branch protection and required checks on a **private** repository need a paid GitHub plan; on a free organisation plan they cannot be enforced. Check your organisation's plan. Without enforcement, CI still reports and you follow the rules by discipline.

## 5. Start-of-work routine

```bash
git checkout develop && git pull
git checkout feature/<name> && git merge develop
```

Then work, commit, `git push`, open a pull request against `develop`.
