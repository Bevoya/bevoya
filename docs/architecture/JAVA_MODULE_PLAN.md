# Bevoya Java module plan

Version 2 · 1 November 2026
Replaces the original 13-row plan. Status is taken from `docs/PROGRESS.md`; "reported" means not yet seen in command output.

Rule (29 Oct): each module is one branch cut from `develop`, with real code, tests, and a cross-user test for every user-data endpoint. Code lives in `app/bevoya/api/<module>`, flat and package-private. Migrations are forward-only and each table ends with `select secure_table('public.<table>')`.

## 1. Modules, in build order

| # | Branch | Module | Migration | What it adds | Status |
|---|---|---|---|---|---|
| 1 | `feature/java-idempotency` | `common/idempotency` | V4 | `idempotency_keys`, `Idempotency-Key` on creates | Done (#36) |
| 2 | `feature/java-profiles-consent` | `profiles`, `identity` | V3 | `GET`/`PATCH /me`, `GET`/`POST /me/consents`, `GET`/`PUT /me/ai-settings` | Done (#37) |
| 3 | `feature/java-goals-api` | `goals` | V3 | goals CRUD | Done |
| 4 | `feature/java-milestones` | `goals` | V5 | milestones CRUD, ordering. `PLAN_LIMIT_REACHED` moved to billing (row 14) | Done (#38) |
| 5 | `feature/java-planning` | `planning` | V6 | tasks CRUD with filters, `GET /plans/today` | Done (#41) |
| 6 | `feature/java-sessions` | `sessions` | V7 | start, complete, active, list, delete; one running session per user | Done (#43) |
| 7 | `feature/java-learning` | `learning` | V8 | evidence, learning entries | Done (merged, real-token checked) |
| 8 | `feature/java-reflections` | `reflections` | V9 | journal CRUD | Done (#46; merged and applied reported) |
| 9 | `feature/java-skills` | `learning` | V10 | skills, `skill_id` on learning entries and evidence, derived level | Next |
| 10 | `feature/java-quizzes` | `learning` | V11 | quiz storage and attempts (with the AI work) | Planned |
| 11 | `feature/java-jobs` | `jobs` | later | jobs table, dispatcher with `SKIP LOCKED`, retries, lease recovery, key purge | Planned |
| 12 | `feature/java-privacy` | `privacy`, `common/audit` | later | export, `DELETE /me` deletion job, access log, audit events | Planned |
| 13 | `feature/java-notifications` | `notifications` | later | preferences, device tokens, reminder jobs | Planned |
| 14 | `feature/java-ai-gateway` | `recommendations`, `common/ai` | later | `AiClient`, daily quota from `usage_counters`, accept and dismiss | Planned |
| 15 | `feature/java-billing` | `billing` | later | Play verification, entitlements, `PLAN_LIMIT_REACHED` | Planned |
| 16 | `chore/java-hardening` | `common` | none | ArchUnit, Java schema test, rate limits, `docs/api/openapi.json`, Sentry scrubbing | Planned |

Open question after V10: the core loop data is then complete on the server. Rows 11 to 15 could wait until after a first mobile slice.

## 2. Skills design (V10)

- **Level is derived, not stored.** `discovered` (nothing linked), `studying` (watched or read evidence), `explained` (a linked learning entry), `practiced`, `built`, `tested`, `applied` (the highest linked evidence level). The result is the highest of these. Reason: the product rewards evidence, not time, so a person cannot claim a level they have no evidence for.
- **`target_level` is stored**, set by the person.
- **Links:** nullable `skill_id` on `learning_entries` and `evidence`, composite foreign key `(skill_id, user_id)` with `on delete set null`, so deleting a skill keeps the notes and evidence.
- **Fields:** `name` (1 to 100 characters, unique per user ignoring case), `category`, `description`, `target_level`, `next_revision_at` (set by the person; spaced repetition comes later). `last_practiced_at` is derived from the newest linked evidence.
- **Endpoints:** `POST /skills` (idempotent), `GET /skills`, `GET`, `PUT` and `DELETE /skills/{id}`. A duplicate name gives 409. A missing skill and another user's skill give the same 404.
- Deferred: `skill_dependencies`, spaced repetition scheduling.

## 3. Rules every module follows

- User id only from `CurrentUser`; every query has `user_id`; another user's record gives the same 404 as a missing one.
- No private text (journals, notes, prompts) in logs, exceptions or test failure messages.
- Create endpoints accept `Idempotency-Key`; a replay returns the original resource.
- No class-level `@Validated` on controllers.
- Docker tests are named `*Test`, run as `bevoya_app`, and are only proven by the CI log.
- When embeddings arrive, update and delete must refresh or remove chunks in the same transaction.