# Bevoya environment and secrets

Version 1 · 21 October 2026
Sources: architecture blueprint (sections 8, 9, 13), ADR-0002, ADR-0004, ADR-0006, AI_RULES.md.

## 1. Rules

1. **Names only in the repo.** `.env.example` files hold variable names and safe placeholders. Real values live in `.env.local` (git-ignored), Render environment groups and GitHub Actions secrets.
2. **Anything prefixed `EXPO_PUBLIC_` is compiled into the app and is public.** Never put a secret there.
3. **The service-role key and the AI key never reach the app or the repo.** `SUPABASE_SERVICE_ROLE_KEY` is Java-only (Auth Admin and Storage, never database access). `GEMINI_API_KEY` is Python-only.
4. **Model names are variables, never code.** Use `GEMINI_FAST_MODEL`, `GEMINI_TEXT_MODEL`, `GEMINI_STRONG_MODEL`, `GEMINI_EMBEDDING_MODEL`.
5. **Migrations use admin credentials from CI or your laptop only.** Never set `FLYWAY_*` on Render. The runtime app uses the `bevoya_app` role.
6. **Synthetic data only** in the dev database and with the Gemini free tier (see AI_RULES.md, section 7).

## 2. Where each value lives

| Place | Holds | Notes |
|---|---|---|
| `mobile/.env.local` | `EXPO_PUBLIC_*` | Public values only |
| `backend-java/.env.local` | Java runtime variables | Copy from `backend-java/.env.example` |
| `ai-python/.env.local` | Python variables | Copy from `ai-python/.env.example` |
| Render environment groups | Production runtime secrets | Set per service; `sync: false` in `render.yaml` |
| GitHub Environments `dev` and `prod` | `FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD` | Used by the manual `migrate` workflow |
| GitHub repository secrets | Only what CI needs | No Gemini key in CI; Gemini is mocked |

## 3. Variables by service

### Mobile (all public)

`EXPO_PUBLIC_API_BASE_URL`, `EXPO_PUBLIC_SUPABASE_URL`, `EXPO_PUBLIC_SUPABASE_ANON_KEY` (the public key; check the dashboard for the current key name), `EXPO_PUBLIC_SENTRY_DSN`, `EXPO_PUBLIC_POSTHOG_KEY`, `EXPO_PUBLIC_POSTHOG_HOST`.

### Java (`bevoya-api`)

| Variable | Secret | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | no | `local` or `prod` |
| `DATABASE_URL` | no | JDBC URL to the **session pooler**, `sslmode=require` |
| `DB_USER`, `DB_PASSWORD` | yes | The `bevoya_app` role only |
| `DB_POOL_MAX` | no | 5 on free tiers |
| `SUPABASE_JWKS_URL`, `JWT_ISSUER`, `JWT_AUDIENCE` | no | JWT validation by JWKS. There is no `SUPABASE_JWT_SECRET` |
| `SUPABASE_URL` | no | For Admin API and Storage |
| `SUPABASE_SERVICE_ROLE_KEY` | yes | Never used for database access |
| `AI_SERVICE_URL`, `INTERNAL_SERVICE_TOKEN` | no / yes | Token shared with Python; long, random, rotated |
| `AI_FREE_DAILY_REQUESTS`, `AI_PRO_DAILY_REQUESTS`, `AI_MONTHLY_TOKEN_LIMIT` | no | Per-user quotas and the global kill-switch |
| `SENTRY_DSN` | no | |

### Python (`bevoya-ai`)

| Variable | Secret | Notes |
|---|---|---|
| `ENVIRONMENT`, `PORT` | no | |
| `INTERNAL_SERVICE_TOKEN` | yes | Same value as Java |
| `GEMINI_API_KEY` | yes | |
| `GEMINI_FAST_MODEL`, `GEMINI_TEXT_MODEL`, `GEMINI_STRONG_MODEL` | no | See AI_RULES.md for current IDs |
| `GEMINI_EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS` | no | `gemini-embedding-2` at `768` (ADR-0004). Changing it means re-embedding every row |
| `AI_MAX_INPUT_TOKENS`, `AI_MAX_OUTPUT_TOKENS`, `AI_MAX_CONCURRENCY`, `AI_TIMEOUT_SECONDS` | no | Per-request ceilings |
| `SENTRY_DSN` | no | |

## 4. Connections

- Render has no Mumbai region, so Java and Python run in Singapore while Supabase is in Mumbai (ADR-0002). Keep database round trips per request low.
- Supabase's direct connection is IPv6-only without the IPv4 add-on. Use the **session-mode pooler** string for the Java runtime and Flyway.
- No local Docker for now (ADR-0006): local work uses the hosted Supabase **dev** project. Testcontainers tests run in GitHub Actions.

## 5. Working with Claude Code

- Claude Code's shell does not see variables set only in another terminal or tool. If a command needs one, put it in the git-ignored `.env.local` for that service or export it in the same shell session.
- Never paste real secrets into a prompt or a chat. If one leaks into a commit, rotate it first, then clean the history.
- The secret-scan job (`security.yml`) must stay green.

## 6. Adding a variable

1. Add the name to the right `.env.example` with a placeholder.
2. Add it to the table above.
3. Add it to `render.yaml` with `sync: false` if it is a secret.
4. Read it through the service's config class, never directly in business code.