# Bevoya progress tracker

Last updated: 22 October 2026 (evening)
Update this file at the end of every work session: move items from "Next" to "Done", add a dated line, and refresh the status snapshot.

## Status snapshot

| Area | State |
|---|---|
| Docs and ADRs | Written and committed |
| Repo scaffold | Done (folders, README, .gitignore files, .gitattributes) |
| CI | PR #4 and PR #16 merged into `develop`. PR #16 had all five checks green (mobile, java, audit-ai, audit-mobile, secrets). **Not yet verified:** the `push` run on `develop` after the #16 merge |
| Security incident | Closed for now. Token revoked, `.mcp.json` untracked, finding ignored by fingerprint. The revoked token is still in git history (see Notes) |
| Database | Supabase dev project (Mumbai) in use. Migration `V1__baseline` applied through Flyway: `bevoya_app` role, default privileges, `secure_table()`. Role attributes checked (not superuser, no RLS bypass, can log in). Remaining V1 checks listed under In progress. No tables yet |
| Java API | Not started (folders only) |
| Python AI service | Not started (folders only) |
| Mobile app | Route stubs, colour tokens, jest, lint and typecheck working. No screens yet |

Current milestone: **M1, the authenticated vertical slice.** Sign up, confirm email, sign in, create a goal, restart the app and still see it. A second account gets `404` on that goal.

## M1 exit criteria

- [ ] Sign-up, confirm email, sign-in and sign-out work on a physical device
- [ ] Expired, wrong-audience, wrong-issuer and unsigned tokens are rejected by tests
- [ ] Cross-user test: every goal endpoint returns `404` for another user's goal
- [ ] Schema test: every `public` table has RLS enabled; Supabase Data API is off
- [ ] Logs and Sentry events contain no goal text or tokens
- [ ] Goal list has loading, empty and error states; works at 200% font scale with TalkBack labels
- [ ] All CI workflows pass on `develop`; `bevoya-api` deployed to Render Singapore and `/health` returns 200
- [ ] No live secret in the repository (gitleaks clean). Note: one revoked token remains in history and is ignored by fingerprint

## Done

| Date | What |
|---|---|
| 20 Oct 2026 | Product, design, architecture, security and privacy docs and ADR-0001 to ADR-0006 written |
| 20 Oct 2026 | `CLAUDE.md` added at repo root, next to `README.md` |
| 20 Oct 2026 | Decisions: launch 18+ only (ADR-0005); embeddings with `gemini-embedding-2` at 768 dimensions |
| 21 Oct 2026 | README redesigned with language bar and badges; `.gitattributes`; `.gitignore` for `backend-java` and `ai-python` committed |
| 21 Oct 2026 | Jest and ESLint set up in `mobile/`; brand colour contrast test passes (17 tests); typecheck fixed with `"types": ["jest"]` |
| 21 Oct 2026 | `app.json` updated: Android package, brand colours for icon and splash |
| 21 Oct 2026 | CI workflows, Dependabot and `docs/engineering/` files written; PR #4 opened |
| 21 Oct 2026 | GitHub CLI authenticated; gitleaks installed locally (winget). `--verbose` step added to `security.yml` |
| 21 Oct 2026 | Finding identified: `github-fine-grained-pat` in `.mcp.json` line 7, commit `b777b4e` (18 Oct). Token revoked; `.mcp.json` untracked and git-ignored; `.mcp.json.example` added (token read from an environment variable); fingerprint added to `.gitleaksignore`. Local scan clean |
| 22 Oct 2026 | PR #4 found already merged with `security / secrets` red, so the fixes had not reached `develop`. Cause confirmed from the CI log: `develop` still ran the old workflow, tracked `.mcp.json` and had no `.gitleaksignore` |
| 22 Oct 2026 | PR #16 (`chore/ci-pipelines` to `develop`) opened with the five unmerged commits; all five checks green; squash-merged |
| 22 Oct 2026 | Supabase project created. First attempt landed in `ap-northeast-1` (Tokyo) by mistake; recreated in Mumbai. Data API switched off (reported done; re-verify after the first migration) |
| 22 Oct 2026 | JWT keys checked: current signing key is ECC P-256 (ES256); the previous HS256 key is only kept to verify older tokens. Data API reported off. Tokyo project deleted |
| 22 Oct 2026 | Flyway command line available through the Flyway Desktop installer (Community 13.11.0-rc3035) with a `flyway` alias in `~/.bashrc`; settings in git-ignored `backend-java/.env.local` (`FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD`, `FLYWAY_LOCATIONS`) |
| 22 Oct 2026 | `V1__baseline.sql` and `afterMigrate__lock_history.sql` written in `backend-java/src/main/resources/db/migration`; `flyway migrate` applied V1 and ran the callback |
| 22 Oct 2026 | Connection test passed from the laptop through the session pooler (`aws-0-ap-south-1.pooler.supabase.com`, port 5432, user `postgres.<project-ref>`): PostgreSQL 17.11, SSL on, `pgvector` 0.8.2 available, 0 tables in `public`. The `ap-south-1` pooler accepting the project ref confirms the region |

## In progress

- Branch `feature/db-schema-migrations` holds V1, the callback and this file. Next action: run the checks below, one statement at a time in the Supabase SQL Editor (it shows only the last result of a multi-statement paste), then commit and push once, open one PR to `develop`, and merge with `gh pr checks N && gh pr merge N --squash --delete-branch`.
  1. Tables in `public` with `relrowsecurity` (expect only `flyway_schema_history`, RLS on)
  2. Table grants to `anon`, `authenticated`, `service_role` (expect zero rows)
  3. `has_function_privilege('anon', 'public.secure_table(regclass)', 'execute')` (expect false)
  4. `bevoya_app` attributes: done, not superuser, no RLS bypass, can log in
- Before pushing: make sure the branch is based on current `develop` (`git merge-base --is-ancestor origin/develop HEAD && echo up-to-date`). The branch was pre-created and may predate the PR #16 fix; if it is behind, rebase it on `origin/develop`. Check `git status --short` does not list `.env.local`.

## Next (in order)

1. Finish the V1 checks; push the migration branch and merge it (one PR)
2. Put the DB admin password in a password manager (it is only in `backend-java/.env.local` as `FLYWAY_PASSWORD`)
3. V2 migration: identity, profiles, consent records and goals, with `secure_table()` on every table and composite foreign keys; add the schema test that fails when a `public` table lacks RLS
4. `feature/java-skeleton`: Spring Boot, health endpoint, RFC 7807 errors, request ID; first Render deploy. Use the Flyway version Spring Boot manages (not the rc used locally) and add the PostgreSQL Flyway module; set the `bevoya_app` password out of band when the app first connects
5. `feature/java-auth`: JWKS validation (pin ES256), `CurrentUser`, `GET /me`, cross-user tests
6. `feature/mobile-design-system`: `typography.ts`, `lightTheme` and `darkTheme`, `useTheme()`, Button, Card, TextField, EmptyState, ErrorState
7. `feature/mobile-navigation`: sign-in screens, auth gate, goals list and create form
8. `feature/ai-service-skeleton` (after M1)

## Small tasks to fit in

- [ ] Comment `@dependabot rebase` on the open Dependabot PRs so they pick up the fixed `security` workflow
- [ ] Mark ADR-0004 as Accepted (embedding model chosen)
- [ ] Update `docs/product/AI_RULES.md` section 5 to say `gemini-embedding-2`
- [ ] Check the doc links in `README.md` match the real folder layout
- [ ] Remove the CI gates in `ci-java.yml` and `ci-ai.yml` when those projects exist
- [ ] Add Maven, uv and Docker entries to `.github/dependabot.yml` when scaffolded
- [ ] Replace placeholder launcher icon and splash image with the real mark
- [ ] Create the new, minimum-scope GitHub token for the MCP server and store it as a user environment variable (`GITHUB_PERSONAL_ACCESS_TOKEN`), never in a file
- [ ] Set the repository default branch to `develop` (`origin/HEAD` currently points to `main`, which contradicts `GIT_WORKFLOW.md`)
- [ ] Every branch the plan names already exists, pre-created and stale. When you reach one, do not create it. Check `git log origin/develop..origin/<branch> --oneline | cat`; if it has no unique commits, delete it (`git push origin --delete <branch>`, `git branch -D <branch>`) and cut it again from `develop`. Prune the roughly 30 pre-created branches you are not about to use; they drift from `develop`, and `GIT_WORKFLOW.md` says to branch when work starts. Recreate each from `develop` when needed
- [ ] Fix the required-check name mismatch: `GIT_WORKFLOW.md` says `ai`, the workflow job is `audit-ai`
- [ ] Confirm the `.githooks/pre-push` guard is committed and `core.hooksPath` is set; set `git config --global core.pager cat`
- [ ] Add `.mcp.json` handling to `ENVIRONMENT.md` (secrets in MCP config are secrets too)
- [ ] Update the `gitleaks` Docker image pin deliberately when a new version is needed (Dependabot cannot bump it)
- [ ] Rename the Supabase project to `bevoya-dev` if it is not already, so dev and a later prod project cannot be confused
- [ ] `ENVIRONMENT.md`: rename `EXPO_PUBLIC_SUPABASE_ANON_KEY` to `EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY` (Supabase's current name for the public key)
- [ ] Check `backend-java/.gitignore` and `ai-python/.gitignore`: the `.env.*` rule also hides `.env.example`; add `!.env.example` so the example files can be committed
- [ ] Give `bevoya_app` its own password out of band (never in a migration or repo file). The admin `postgres` password is for Flyway only and never goes on Render
- [ ] `ENVIRONMENT.md`: add `FLYWAY_LOCATIONS`, the session-pooler JDBC URL form and the rule that Flyway never runs on Render
- [ ] Add the vector extension in its own migration when embeddings start (ADR-0004); V1 does not include it
- [ ] V1 has only been applied to the dev database. The empty-database test in CI (Testcontainers) comes with the Java skeleton

## Decisions still open

- [ ] Android application id (currently `app.bevoya.android`; cannot change after publishing)
- [ ] Gemini billing: enable pay-as-you-go before any real person's data is used
- [x] GitHub plan: staying on the free plan, so no branch protection. Rule: never merge a red check
- [ ] Branch protection: a private repo on a free organisation cannot enforce rulesets. Choose: upgrade to Team (check current price), or enforce by tooling (pre-push hook plus the `gh pr checks` merge gate). Do not make the repo public while the revoked token remains in history
- [ ] Weekly hours available (decides where the cut line goes)
- [ ] Keep `develop`, or collapse to `main` plus short branches
- [x] Supabase JWT signing key is asymmetric (ECC P-256, ES256). Revoke the old HS256 key later, after checking nothing uses the legacy `anon` or `service_role` keys
- [ ] Support and security email; domain and trademark check for "Bevoya"

## Before private beta (not yet started)

Privacy settings, data export and account deletion, Sentry and PostHog with redaction, SMTP provider for sign-up emails, restore drill, privacy policy and terms, Gemini billing on, paid always-on API instance, Play developer account.

## Notes and problems found

- 21 Oct 2026: TypeScript 6 needs `"types": ["jest"]` in `tsconfig.json`; without it test globals are unknown.
- 21 Oct 2026: Docker does not work on the dev laptop (ADR-0006). Secret scans and Testcontainers tests run in GitHub Actions, not locally. The gitleaks binary does not need Docker, so scan locally with it.
- 21 Oct 2026: with `--redact` and no `--verbose`, gitleaks hides the file; use `--verbose --redact`.
- 21 Oct 2026: **Incident.** `.mcp.json` was committed on 18 Oct with a live GitHub fine-grained token. It reached `main`, `develop` and about 30 other branches, so history was not rewritten (it would not remove the copy on GitHub's servers anyway). Response: token revoked, file untracked, fingerprint `b777b4e46f605f903e8a895130366c7d9175725b:.mcp.json:github-fine-grained-pat:7` kept in `.gitleaksignore`. Rule: MCP and tool config files that hold credentials are never committed; use environment variables. If the repo ever goes public, scrub the history first.
- 21 Oct 2026: a `git` command that opens a pager (`(END)` prompt) needs `q`; set `core.pager cat` to avoid it.
- 22 Oct 2026: **Process slip.** PR #4 was merged while `security / secrets` was red. The tracker rule says never merge red, and on the free plan nothing enforces it. Always run `gh pr checks N && gh pr merge N --squash --delete-branch` and merge only that way. A pull request whose branch gets new commits after the merge does not re-run; open a new PR instead.
- 22 Oct 2026: In Git Bash, `git show origin/develop:path` can be mangled into a Windows path. Prefix with `MSYS_NO_PATHCONV=1`.
- 22 Oct 2026: The Supabase direct host (`db.<ref>.supabase.co`) is IPv6-only and does not resolve on the dev laptop (`getaddrinfo failed`). Use the session pooler. The `Connect` dialog defaults to Direct; choose Session pooler. The pooler user is `postgres.<project-ref>`, not `postgres`. Do not enable the paid IPv4 add-on.
- 22 Oct 2026: Quick connection check without a virtualenv: put a short `check.py` outside the repo, set `PGHOST`, `PGUSER`, `PGPORT`, `PGDATABASE`, `PGSSLMODE`, read the password with `read -s` into `PGPASSWORD`, run `uv run --with "psycopg[binary]" python check.py`, then `unset` the variables. Never type a placeholder such as `<host>` into an `export`.
- 22 Oct 2026: The first `flyway migrate` of V1 stalled with no result line. V1 then contained a step that altered Flyway's own history table; that is the suspected cause (a lock fight with Flyway's own write), not confirmed. After stopping the run, the step moved to `afterMigrate__lock_history.sql` and the migration applied cleanly. Rule: never alter `flyway_schema_history` inside a versioned migration. Once a version has applied, never edit that file; add a new version.
- 22 Oct 2026: The Flyway CLI installed with Flyway Desktop lives in `C:\Program Files\Red Gate\Flyway Desktop\flyway\flyway.cmd`. It is the local runner only. Migrations are plain SQL in `backend-java/src/main/resources/db/migration`, so the runner can change without touching them.
- 22 Oct 2026: `gh pr merge --squash --delete-branch` fails its local cleanup if the working tree has uncommitted changes. Stash or commit first. After a squash merge, delete the old local branch with `git branch -D`.
