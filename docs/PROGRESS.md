# Bevoya progress tracker

Last updated: 1 November 2026
Update this file at the end of every work session: move items from "Next" to "Done", add a dated line, and refresh the status snapshot. Record only results that were actually seen. Mark anything else "reported" (you said it) or "unverified".

**Starting a new conversation?** Read "Status snapshot", "In progress", "Next (in order)" and "Remember for later" first, then "Notes and problems found" (newest at the bottom). **State on 1 Nov: V1 to V9 are applied (V1 to V7 seen; V8 and V9 reported), merged and deployed. Reflections (V9, PR #46) were written, passed all six checks (seen) and were reported merged, applied and tested. A second real-token script run against Render passed 38 of 38 lines (seen) for sessions, evidence, learning entries and tasks; it did not call `/reflections`, so the live reflections check is reported, not seen. The CI `Tests run:` lines for the learning PR (expected 122) and PR #46 (expected 131) were not read. The next step is the tracker PR, then `feature/java-learning` part 2 (V10: skills), see "In progress".** Decision (29 Oct): finish the whole Java backend end to end before starting the mobile or Python services; the order is under "Next".

## Status snapshot

| Area | State |
|---|---|
| Docs and ADRs | Written and committed |
| Repo scaffold | Done (folders, README, .gitignore files, .gitattributes). The dead `com/bevoya/*` scaffold tree is gone (seen 29 Oct) |
| CI | PR #4, #16, #18, #19, #20 and #21 merged into `develop`, plus the Render deploy PR, the database connection PR, the `feature/java-auth` PR and the goals PR (numbers not recorded). Then #36 (idempotency), #37 (profile, consents, AI settings), #38 (milestones), #41 (planning), #42 (tracker), #43 (sessions), the learning PR (number not recorded) and #46 (reflections). **Seen in CI logs:** run 41123649310 (#38): 69 tests, 0 failures, 0 skipped. Run 41227946713 (#41): 85 tests, 0 failures, 0 skipped. Run 41232900131 (#43): 102 tests, 0 failures, 0 skipped. **Not read:** the CI test line for the learning PR (expected 122) and for PR #46 (expected 131). **Seen on 1 Nov:** `gh pr checks 46`: `security/audit-ai`, `security/audit-mobile`, `ci-db/db`, `ci-java/java` (1m3s), `ci-mobile/mobile`, `security/secrets` all successful. The merge of #46 is reported (not seen) |
| Security incident | Closed for now. Token revoked, `.mcp.json` untracked, finding ignored by fingerprint. The revoked token is still in git history (see Notes) |
| Database | Supabase dev project (Mumbai). **V1 to V7 applied (seen 1 Nov).** **V8 `evidence and learning entries` and V9 `reflections` applied (reported; `flyway info` output not pasted).** The RLS check after V8 returned 10 rows (reported); after V9 it was reported as expected (expect 11 `public` tables, all true: `consent_records`, `evidence`, `flyway_schema_history`, `goals`, `idempotency_keys`, `learning_entries`, `milestones`, `profiles`, `reflections`, `sessions`, `tasks`); output not pasted. All migrations apply cleanly on a fresh `postgres:17` in CI (`ci-db` green). Zero table grants to `anon`, `authenticated`, `service_role` as of V3; `anon` has no execute on `secure_table()`; Data API checked off (reported). `profiles.id` is a primary key that references `auth.users(id)`: the profile id is the auth user id. `profiles.time_zone` is the time zone column. `bevoya_app` is not superuser, has no RLS bypass and has its own password (SCRAM verifier, in the password manager and in Render). The DB admin password was rotated on 24 Oct (reported); the Render runtime never holds it. The RLS policy for `bevoya_app` is `using (true)`: **row ownership is enforced only by `user_id` in the Java queries and the composite foreign keys**. Seen on 28 Oct: `bevoya_app` on the real database wrote a `goals` row whose `user_id` equals the real auth user id |
| Java API (on `develop`) | Spring Boot 4.1.2, Java 21, Maven wrapper. `GET /health` (database-free), RFC 7807 problem responses with a request ID, generic 500 that leaks no exception text, JDBC startup check that does not fail the start. Spring Security resource server (Supabase tokens by JWKS, ES256 pinned, issuer and audience checked), `CurrentUser`. **Goals** (`POST`, `GET`, `GET /{id}`, `PUT`, `DELETE`). **Idempotency** (`Idempotency-Key`, 8 to 100 characters of `A-Za-z0-9._:-`). **Profile, consents, AI settings** (`GET` and `PATCH /me`, `GET` and `POST /me/consents`, `GET` and `PUT /me/ai-settings`). **Milestones** (CRUD, order, idempotent create). **Tasks** (`POST`, `GET` with filters, `GET /{id}`, `PATCH`, `DELETE`, `GET /plans/today`). **Sessions** (`POST /sessions`, `POST /sessions/{id}/complete`, `GET /sessions/active`, `GET /sessions`, `GET /{id}`, `DELETE`; one running session per user). **Evidence** (`POST`, `GET` with `sessionId` and `activityLevel`, `GET /{id}`, `DELETE`; immutable). **Learning entries** (`POST`, `GET`, `GET /{id}`, `PUT`, `DELETE`). **Reflections (PR #46, reported merged):** `POST /reflections` (idempotent), `GET /reflections` (optional `from` date, newest entry date first, capped at 100), `GET`, `PUT` and `DELETE` on `/reflections/{id}`. **Seen on Render with a real token on 1 Nov (38 of 38 lines):** health, `GET /me`, the no-token 401, sessions (start, replay, active, 409, complete, complete again 409, delete), evidence (create, replay, filter, bad filter 400, unknown session 404, `http://` url 400, unlink on session delete, delete), learning entries (create, `PUT`, `GET`, `http://` source 400, delete), tasks (create, `GET /plans/today`, `PATCH` done with `completedAt`, bad status 400, delete). **Not verified on Render:** reflections (reported only), milestone `PUT`, `DELETE` and reorder, goal `PUT` and `DELETE`, consents, AI settings, and cross-user isolation with two real users (CI covers it) |
| Deployment | `bevoya-api` on Render (Docker, region `singapore`, free plan, `autoDeploy: false`), deploying from `develop`. **1 Nov (seen):** `/health` 200 at 08:47 UTC. After the V8 and V9 merges `develop` was deployed manually (reported). **Second real-token run on 1 Nov (seen):** sign-in 200, `GET /me` 200 in 5.4 s, first `POST /sessions` 2.8 s, then 0.13 to 1.1 s for almost everything, with one unexplained slow response: `POST /evidence` with an `http://` url returned its 400 in 5.2 s. Token verification settings (`SUPABASE_JWKS_URL`, `JWT_ISSUER`) are proven by behaviour; `JWT_AUDIENCE` is unset and the default `authenticated` is enforced and accepted. `render.yaml` still contains `SPRING_DATASOURCE_HIKARI_MINIMUMIDLE`, which should be deleted. Dashboard region and plan were not shown in output. Free instances sleep after 15 minutes idle |
| Python AI service | Not started (folders only) |
| Mobile app | Route stubs, colour tokens, jest, lint and typecheck working under `npm ci` (17 tests pass). No screens yet |

Current milestone: **M1, the authenticated vertical slice.** Sign up, confirm email, sign in, create a goal, restart the app and still see it. A second account gets `404` on that goal. The API half works with a real token; the mobile half has not started. Java is being completed end to end first (29 Oct decision, see Next).

## M1 exit criteria

- [ ] Sign-up, confirm email, sign-in and sign-out work on a physical device (email and Google, see "Remember for later"). Sign-in through the Supabase password grant works against the real project (seen again 1 Nov, status 200); the app screens do not exist yet
- [x] Expired, wrong-audience, wrong-issuer and unsigned tokens are rejected by tests (`AuthTokenTest`, 12 of 12 passed in CI, seen in runs 41123649310, 41227946713 and 41232900131)
- [x] Cross-user test: every goal endpoint returns `404` for another user's goal (`GoalApiTest` 18 of 18 passed in CI, seen again in run 41232900131)
- [ ] Schema test: every `public` table has RLS enabled; Supabase Data API is off (CI schema test merged and green in `ci-db`; dev RLS check after V9 reported as expected, output not pasted; re-check after V10; Data API still to re-verify in the dashboard)
- [ ] Logs and Sentry events contain no goal text or tokens
- [ ] Goal list has loading, empty and error states; works at 200% font scale with TalkBack labels
- [ ] All CI workflows pass on `develop`; `bevoya-api` deployed to Render Singapore and `/health` returns 200 (`/health` returned 200 on Render on 31 Oct and 1 Nov; tick when the current `develop` has been deployed and its runs are green)
- [ ] No live secret in the repository (gitleaks clean). Note: one revoked token remains in history and is ignored by fingerprint

## Done

| Date | What |
|---|---|
| 20 Oct 2026 | Product, design, architecture, security and privacy docs and ADR-0001 to ADR-0006 written. `CLAUDE.md` added at repo root. Decisions: launch 18+ only (ADR-0005); embeddings with `gemini-embedding-2` at 768 dimensions |
| 21 Oct 2026 | README redesigned; `.gitattributes`; `.gitignore` for `backend-java` and `ai-python`. Jest and ESLint set up in `mobile/`; brand colour contrast test passes (17 tests); typecheck fixed with `"types": ["jest"]`. `app.json` updated (Android package, brand colours). CI workflows, Dependabot and `docs/engineering/` written; PR #4 opened. GitHub CLI authenticated; gitleaks installed locally |
| 21 Oct 2026 | Finding identified: `github-fine-grained-pat` in `.mcp.json` line 7, commit `b777b4e` (18 Oct). Token revoked; `.mcp.json` untracked and git-ignored; `.mcp.json.example` added; fingerprint added to `.gitleaksignore`. Local scan clean |
| 22 Oct 2026 | PR #4 found merged with `security / secrets` red; cause confirmed from the CI log. PR #16 (`chore/ci-pipelines`) opened, all five checks green, squash-merged |
| 22 Oct 2026 | Supabase project created (first attempt landed in Tokyo by mistake; recreated in Mumbai, Tokyo deleted). JWT keys checked: ECC P-256 (ES256). Flyway CLI available through Flyway Desktop with a `flyway` alias; settings in git-ignored `backend-java/.env.local` |
| 22 Oct 2026 | `V1__baseline.sql` and `afterMigrate__lock_history.sql` written and applied. Connection test passed through the session pooler (PostgreSQL 17.11, SSL on, `pgvector` 0.8.2 available). V2 and V3 (`profiles`, `consent_records`, `goals`) written with `secure_table()`; `ci-bootstrap.sql`, `schema-check.sql` and the `ci-db` workflow added and green on a fresh Postgres |
| 22 Oct 2026 | Found `ci-mobile` failing at Lint on `develop` (`typescript ~7.0.2` next to `~6.0.3`) |
| 23 Oct 2026 | PR #19 merged (TypeScript 7 removed, `eslint .` as the lint script; reproduced CI locally with `npm ci`). Checked that `eslint .` is a real lint (`import/namespace` canary fails). PR #18 `feat(db): identity and goals schema` merged after six green checks. `flyway migrate` applied V2 and V3 to the dev project; dev checks passed (3 history rows, RLS on 4 tables, zero grants to `anon`, `authenticated`, `service_role`, `bevoya_app` not superuser, `anon` cannot execute `secure_table()`). PR #20 (tracker) merged |
| 24 Oct 2026 | Data API switch checked and DB admin password stored in a password manager (reported). `feature/java-skeleton` recut and built (health, `RequestIdFilter`, `ApiExceptionHandler`, `application.properties`, `ApiBaselineTest`); PR #21 merged after six green checks; `ci-java` ran in CI for the first time |
| 24 Oct 2026 | `chore/render-deploy` built: `Dockerfile`, `.dockerignore`, `render.yaml`; first Render build succeeded and `/health` returned 200. Service switched to `develop` |
| 24 Oct 2026 | `feature/java-database` merged (JDBC, datasource settings, `DatabaseStartupCheck`). `bevoya_app` given its own password with the one-off `set-app-db-password.sh` (not committed). First Render deploy with database settings failed because the admin password had been entered as `DB_PASSWORD`; a further deploy hit the pooler circuit breaker; with the correct credentials and a wait the app connected. Admin password rotated (reported), confirmed working with `flyway info` |
| 24 Oct 2026 | PR #24 (tracker) merged; local `develop` divergence recovered with `backup/develop-local`, `git reset --hard origin/develop`; `git config pull.ff only` set |
| 25 Oct 2026 | Supabase JWKS checked (one ES256 key, no HS256). `.githooks/pre-push` inspected and working. Database first-connection delay investigated on Render (about 9.7 to 11.2 s for the first query); ruled out IPv6, the IPv4 flag, `connectTimeout=3`, pool size; parked (see Notes). Initializr checked for Boot 4.1.2 artifact names |
| 25 Oct 2026 | `feature/java-auth` built (security artifacts, JWT properties, `SecurityConfig`, `ProblemJsonSecurityHandlers`, `CurrentUser`, `MeController`, `AuthTokenTest`, `scripts/load-env.sh`). Local verify: 19 tests, 0 failures. Merged and live on Render (inferred from a 401 problem+json on `/me`; PR number not recorded). JPA, profile bootstrap and Testcontainers harness written (24 run, 3 skipped locally). pom slip (dependencies pasted outside `<dependencies>`) fixed |
| 26 Oct 2026 | Package layout checked: code belongs under `app.bevoya.api`. Goals API written (35 tests, 14 skipped locally). **First CI run of Docker tests (seen):** 35 run, 1 failure (`anotherUsersGoalIsIndistinguishableFromAMissingOne`: the two 404 bodies differed only in `instance`; the API was right, the test now ignores `requestId` and `instance`). Fix pushed, goals PR merged (reported) |
| 28 Oct 2026 | **First real-token check on Render (seen):** sign-in 200; `GET /me` 200 in 6.0 s; `POST /goals` 201 in 3.5 s; `GET /goals` 200 in 4.9 s; the SQL Editor showed the goals row with `user_id` equal to the real auth user id. Tracker PR merged |
| 29 Oct 2026 | Post-merge checks (green push runs, dead scaffold tree gone). `feature/java-idempotency` merged as #36. `feature/java-profiles-consent` written; first local verify failed in `AuthTokenTest` |
| 30 Oct 2026 | `AuthTokenTest` fixed; profiles PR merged as #37. `feature/java-milestones` written and merged as #38 (V5 and 7 files, `MilestoneApiTest` 14 tests); CI run 41123649310 success |
| 31 Oct 2026 | CI evidence read in full for #38 (69 tests, 0 skipped). `flyway info`: V1 to V5 Success (V4 and V5 installed 30 Oct 20:46 UTC). **Render deploy and real-token check (seen):** `/health` 200; `GET /me` 200 in 5.7 s; `PATCH /me` 200 in 2.3 s; `POST /goals` 201 in 1.8 s; same `Idempotency-Key` again 201 in 2.3 s with the same id; milestone `POST` 201 in 0.86 s; `GET` 200 in 0.40 s |
| 31 Oct 2026 | `feature/java-planning` written (V6, 8 files, `TaskApiTest` 16). First CI run (run 41170096808): 85 run, 2 failures (two 500s instead of 400s from a class-level `@Validated` on `TaskController`) |
| 1 Nov 2026 | Planning PR #41 fixed (`@Validated` removed) and merged; run 41227946713: 85 tests, 0 failures, 0 skipped. `feature/java-sessions` written (V7, 6 files, `SessionApiTest` 17) and merged as #43; tracker PR #42 merged; run 41232900131: 102 tests, 0 failures, 0 skipped |
| 1 Nov 2026 | **Ops sweep (seen):** `flyway migrate` applied V6 and V7 at 08:42 UTC ("now at version v7"); second `flyway info` showed V1 to V7 Success; `/health` 200 at 08:47 UTC. RLS check reported as expected |
| 1 Nov 2026 | `feature/java-learning` part 1 written (V8, evidence and learning entries, 8 main files, 20 Docker tests) and merged (reported; PR number and CI test line not recorded). Local verify (seen): 122 run, 0 failures, 101 skipped |
| 1 Nov 2026 | **V8 applied, RLS and deploy (reported):** `flyway migrate` applied V8, the RLS query returned 10 rows, `develop` deployed on Render. Outputs were not pasted |
| 1 Nov 2026 | **First real-token script run against Render (seen in full, 28 of 28 lines ok):** sessions, evidence and learning entries, first calls 3.0 s and 4.5 s, then 0.2 to 0.7 s. Proved V7 and V8 on the real database |
| 1 Nov 2026 | `feature/java-reflections` written (V9, `ReflectionRequest`, `ReflectionResponse`, `ReflectionController`, `ReflectionService`, `ReflectionApiTest` with 9 Docker tests). **Seen:** local `./mvnw -B verify`: 131 run, 0 failures, 110 skipped; `gh pr checks 46 --watch`: all six checks successful. Merged as #46, V9 applied, `develop` deployed and a reflections check done (all **reported**; no merge, `flyway`, RLS or `/reflections` output pasted; the CI test line was not read) |
| 1 Nov 2026 | **Second real-token script run against Render (seen in full, 38 passed, 0 failed):** sign-in 200; `GET /me` 200 in 5.4 s; no-token `GET /sessions` 401. *Sessions:* `POST` 201 in 2.8 s; same key 201 in 0.49 s with the same id; `GET /active` 200; start while running 409 with `activeSessionId`; `complete` 200 with `actualMinutes`; `complete` again 409; `GET /active` 204; `DELETE` 204; `GET` deleted 404. *Evidence:* `POST` linked to the running session 201; same key 201 with the same id; `GET ?activityLevel=built` 200 and listed; `?activityLevel=nope` 400; random `sessionId` 404; `http://` url 400 (took 5.2 s); after the session was deleted the evidence was kept with `sessionId` null; `DELETE` 204; `GET` deleted 404. *Learning entries:* `POST` 201; `PUT` 200; `GET` 200 with the new title and `confidence` cleared to null; `http://` source 400; `DELETE` 204; `GET` deleted 404. *Tasks (first time seen on Render):* `POST` 201; `GET /plans/today` 200; `PATCH` done 200 with `completedAt` set; `?status=nope` 400; `DELETE` 204. Everything after the first calls took 0.13 to 1.1 s except the one 5.2 s response noted above. This proves V6, V7 and V8 and the tasks, sessions, evidence and learning-entry code on the real database. It did **not** call `/reflections` |

## In progress

### Now: close out 1 Nov, then `feature/java-learning` part 2

1. **Merge this tracker update** (one small PR from `develop`, check `git status --short` shows only `docs/PROGRESS.md`, merge only on green).
2. **Confirm the reported items with output, if you can** (each is one command and makes a "reported" into "seen"): `flyway info` (V1 to V9 Success), the RLS query (11 rows, all true), and the `ci-java` test lines for the learning PR and #46 (`gh run list --branch develop --workflow ci-java --limit 4 --json databaseId,conclusion,displayTitle`, then `gh run view <run> --log | grep -a -E "Tests run:" | cut -c1-200`; want 122 and 131, 0 skipped).
3. **Live check for `/reflections`** (never paste the token or password; use synthetic text; status codes and timings only): `POST /reflections` `{"entryDate":"2026-10-30","rawText":"Test entry","mood":4}` with a key (201); same key again (201, same `id`); `GET /reflections` (listed); `GET /reflections?from=2026-10-31` (not listed); `GET /reflections?from=yesterday` (400); `PUT` `{"rawText":"Changed"}` (200, date unchanged, mood null); `GET` (shows "Changed"); `DELETE` (204), then `GET` (404). Extend the pasted script with these lines.
4. **Start `feature/java-learning` part 2 (V10: skills):** `git checkout develop && git pull`, delete the stale remote and local `feature/java-learning` branch if it has no unique commits, cut it fresh. Quiz storage moves to its own later branch (V11).

If CI is red on any branch: paste the failing test name and the full `Unhandled exception` line (`cut -c1-600`). The reflection tests deliberately print no response body on failure, so a journal entry cannot reach a CI log; to see a body you may need to reproduce the failure locally against a real Postgres.

### Other

- `chore/git-guards` (status not rechecked since 25 Oct): adds `.githooks/pre-commit` and `.githooks/pre-merge-commit` (block commits on `main` and `develop`), fixes the required-check names in `GIT_WORKFLOW.md`, adds a "Local guards" section. Next: `git add`, set the executable bit on the two hooks, one push, one PR, merge only on green. Until it is merged the pre-commit guard does not exist, so check `git branch --show-current` before committing.
- Render cleanup after the timing experiments (dashboard, Environment tab): set `DATABASE_URL` back to the documented form ending in `?sslmode=require`, delete `LOGGING_LEVEL_COM_ZAXXER_HIKARI`, make sure `JAVA_TOOL_OPTIONS` is not set. Also delete `SPRING_DATASOURCE_HIKARI_MINIMUMIDLE` from the dashboard and from `render.yaml`.
- Clear the Flyway variables from the shell after use. The pasted real-token script clears its own token and password variables when you run its cleanup lines.
- **Do not run `schema-check.sql` or `ci-bootstrap.sql` against Supabase.** They belong only in CI's throwaway Postgres.

## Next (in order)

**Rule (29 Oct): finish the Java backend end to end, with real code, tests and a cross-user test per user-data endpoint, before starting the mobile or Python services.** Layout is `app/bevoya/api/<module>`, flat and package-private. Each step is one branch; branch from `develop` when you start (the pre-created branches are stale).

1. ~~`feature/java-profiles-consent`~~ done (#37)
2. ~~Confirm V4 and V5; test idempotency, profile and milestones with a real token~~ done 31 Oct
3. Render cleanup (environment variables and `render.yaml`)
4. Merge `chore/git-guards`
5. ~~`feature/java-milestones`~~ done (#38). `PLAN_LIMIT_REACHED` (Free plan: one active goal) moves to the billing step (15)
6. ~~`feature/java-planning`~~ done (#41, V6 applied, real-token check seen 1 Nov)
7. ~~`feature/java-sessions`~~ done (#43, V7 applied, real-token check seen 1 Nov)
8. `feature/java-learning`: **part 1 (V8, evidence and learning entries) done: merged, applied, real-token checked (seen 1 Nov)**; **part 2 (V10): skills, `skill_id` on learning entries (and evidence), skill level progression** is next; **quiz storage and attempts (V11)** follow in their own branch
9. ~~`feature/java-reflections` (V9)~~ done (#46, reported merged and applied; live check not seen). `structured`, `ai_generated` and `edited_by_user` are not in V9; they arrive in a small migration with the AI branch
10. `feature/java-jobs`: jobs table, dispatcher with `SKIP LOCKED`, retries, lease recovery, purge of old idempotency keys
11. `feature/java-privacy`: export job, `DELETE /me` account deletion job, access log, audit events
12. `feature/java-notifications`: preferences, device tokens, reminder jobs
13. `feature/java-ai-gateway`: `AiClient` (Java to Python, stubbed until the Python service exists), daily quota from `usage_counters`, recommendations accept and dismiss
14. `feature/java-billing`: Play purchase verification, entitlements (can wait until before the public beta)
15. `chore/java-hardening`: ArchUnit boundary test, Java schema test (every `public` table has RLS on; `bevoya_app` cannot update or delete `consent_records`; no grants to `anon`, `authenticated`, `service_role`), a test that an unknown path with a valid token gives a `404` problem+json, rate limits, `docs/api/openapi.json`, Sentry scrubbing
16. Revisit the first-connection delay with a timing probe, or decide on a paid always-on instance
17. `feature/mobile-design-system`: `typography.ts`, `lightTheme` and `darkTheme`, `useTheme()`, Button, Card, TextField, EmptyState, ErrorState
18. `feature/mobile-navigation`: sign-in screens (**email and Google**), auth gate, goals list and create form (uses `Idempotency-Key`)
19. `feature/ai-service-skeleton`, then the first AI feature (reflection extraction as an editable proposal)

Risk to keep in view: the blueprint's cut line says that if M1 slips, drop the AI feature and finish the non-AI loop. Doing all of Java first delays anything you can run on a phone, so keep each step small. After V10 the core loop data (goals, tasks, sessions, evidence, notes, reflections) is complete on the server, so steps 10 to 14 can reasonably wait until after a first mobile slice. Decide that at the end of the next module.

## Remember for later

- **Sign-up and sign-in must offer both email and Google (decided 26 Oct).** Build it in `feature/mobile-navigation`. Settle first:
  - Supabase needs the Google provider enabled, with an OAuth client created in Google Cloud. The Android client is tied to the **package name and the signing certificate's SHA-1**, so the open decision on the Android application id (`app.bevoya.android`, unchangeable after publishing) is a dependency.
  - The Java API should need no change. An email-password token is proven; still confirm one real Google token.
  - ADR-0005 requires a recorded 18+ confirmation. A Google sign-up skips the email form, so the app must still show the 18+ and terms step and write the `consent_records` rows before the account is used.
  - Check what Supabase does when the same address signs up with email and later uses Google (account linking). Not verified.
- **Embeddings and deletion (ADR-0004, `DATA_INVENTORY.md`):** when embeddings are added, deleting or updating a learning entry, a piece of evidence or a reflection must delete or refresh its chunks in the same transaction, and reflection chunks only with the journal-retrieval consent. The V8 and V9 services carry a comment saying so; no chunk table exists yet.
- **Journal privacy:** reflection text must never appear in logs, exception messages, Sentry events, analytics or CI output. The reflection tests print no response bodies for that reason. Keep that rule for every later reflection test, and never paste real journal text into a live check.

## Small tasks to fit in

- [ ] Update `ENVIRONMENT.md`: loading `.env.local` is `source scripts/load-env.sh` from `backend-java` (unquoted values), **but run `flyway` from the repo root**. Add `SUPABASE_JWKS_URL`, `JWT_ISSUER`, `JWT_AUDIENCE` to the Render variable list
- [ ] Add an explicit startup check for the required settings (`SUPABASE_JWKS_URL`, `JWT_ISSUER`, `DATABASE_URL`): Spring does not fail on an unresolved `${NAME}`
- [ ] **Save the real-token script** as a file outside the repo (or in a private tools folder, never committed). It currently exists only as pasted text in a chat; the next conversation will not have it. Extend it with `/reflections`, milestones, goals `PUT` and `DELETE`, and a **second user** for a live cross-user check (user B gets 404 on user A's session, evidence, entry and task)
- [ ] **Investigate the 5.2 s response** to `POST /evidence` with an `http://` url on 1 Nov (a plain 400 that normally takes 0.1 to 0.5 s). Likely the free instance (garbage collection or a CPU pause) but not measured; compare against later runs before changing anything
- [ ] Add a Dependabot `ignore` for major `typescript` updates (Expo pins the version). First check who added 7.0.2: `git log -p -3 -- mobile/package.json | cat`
- [ ] Run `npx expo install --check` in `mobile/` and read what it reports
- [ ] Investigate why `expo lint` fails on Windows while `eslint .` passes (unexplained; `eslint .` is the workaround)
- [ ] Review the 52 npm audit findings separately. Do not run `npm audit fix --force`
- [ ] Review the npm `allowScripts` warning for `@parcel/watcher` and `unrs-resolver`
- [ ] ESLint 9.39.5 is reported deprecated by npm. Check Expo's supported ESLint version before upgrading
- [ ] Confirm in the Render dashboard that `bevoya-api` shows region Singapore and plan Free
- [ ] Decide whether `set-app-db-password.sh` should live in a private tools folder (never committed)
- [ ] Pin the base images in `backend-java/Dockerfile` and let Dependabot's Docker entry track them
- [ ] Tidy `backend-java/pom.xml` after future Initializr merges
- [ ] Comment `@dependabot rebase` on the open Dependabot PRs
- [ ] Mark ADR-0004 as Accepted; update `docs/product/AI_RULES.md` section 5 to say `gemini-embedding-2`
- [ ] Check the doc links in `README.md` match the real folder layout
- [ ] Remove the CI gate in `ci-ai.yml` when the AI project exists
- [ ] Add Maven (`backend-java`), uv and Docker entries to `.github/dependabot.yml`
- [ ] Replace placeholder launcher icon and splash image with the real mark
- [ ] Create the new minimum-scope GitHub token for the MCP server and store it as a user environment variable, never in a file
- [ ] Set the repository default branch to `develop` (`origin/HEAD` points to `main`)
- [ ] Prune the roughly 30 stale pre-created branches. When you reach one, check `git log origin/develop..origin/<branch> --oneline | cat`; if it has no unique commits, delete and recut from `develop`
- [ ] Still to do: `git config --global core.pager cat`
- [ ] `ENVIRONMENT.md`: record the Render variables (`DATABASE_URL`, `DB_USER`, `DB_PASSWORD`, `DB_POOL_MAX`), the pooler login form `bevoya_app.<project-ref>`, `FLYWAY_LOCATIONS`, the session-pooler URL form, the rule that Flyway never runs on Render, `.mcp.json` handling, and the rename to `EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY`
- [ ] Update the `gitleaks` Docker image pin deliberately when needed
- [ ] Rename the Supabase project to `bevoya-dev` if it is not already
- [ ] Check `backend-java/.gitignore` and `ai-python/.gitignore`: add `!.env.example`
- [ ] Add the vector extension in its own migration when embeddings start (ADR-0004)
- [ ] If `security / secrets` flags the `ci-db` throwaway Postgres URL, allow it narrowly by fingerprint
- [ ] A dedicated Java schema assertion test (every `public` table has RLS on) is still open
- [ ] Confirm in CI (`has_function_privilege`) that a function created after V2 is not executable by `anon` or PUBLIC
- [ ] Update docs if the module layout is documented as `api/dto/domain/persistence/service` sub-packages: the code uses flat package-private feature packages
- [ ] CI log noise, probably harmless: Maven `401` warnings, Hibernate `HHH90000025`, Mockito agent warning
- [ ] Decide whether to keep the Supabase test user or delete it. Its rows (goals, tasks, sessions, evidence, notes, reflections) are in the dev database; dev data must stay synthetic. Check V3 before relying on cascade
- [ ] Flyway CLI 13.12.0 is offered (installed 13.11.0-rc3035); update when convenient
- [ ] Use `cut -c1-600` on `Unhandled exception` lines when reading CI failures
- [ ] Decide whether sessions, evidence, learning and reflections lists need a `to` date and pagination before the mobile history screens (all are newest first, capped at 100)

## Decisions still open

- [ ] Android application id (currently `app.bevoya.android`; cannot change after publishing). Also needed for the Google sign-in client
- [ ] Gemini billing: enable pay-as-you-go before any real person's data is used
- [x] GitHub plan: staying on the free plan, so no branch protection. Rule: never merge a red check
- [ ] Branch protection: upgrade to Team, or enforce by tooling (pre-push hook plus the `gh pr checks` merge gate). Do not make the repo public while the revoked token remains in history
- [ ] Weekly hours available (decides where the cut line goes)
- [ ] Keep `develop`, or collapse to `main` plus short branches
- [x] Supabase JWT signing key is asymmetric (ES256). Revoke the old HS256 key later, after checking nothing uses the legacy `anon` or `service_role` keys
- [x] (25 Oct) Unauthenticated requests to unknown paths answer `401`, not `404`
- [x] (25 Oct) The profile id is the auth user id (V3). The profile row is created by `ProfileService.ensureExists`, called on `GET /me` and before every create that inserts user data (goals, tasks, sessions, evidence, learning entries, reflections). Reads do not call it
- [x] (25 Oct) `ProfileService` is an interface so no-database tests can swap in a stub
- [x] (25 Oct) JPA does not connect at boot; the CI schema checks are the schema validation
- [x] (25 Oct) Docker-dependent Java tests are named `*Test` and use `@Testcontainers(disabledWithoutDocker = true)`. They skip locally and run in CI
- [x] (26 Oct) Java code lives under `app.bevoya.api.<module>`, flat and package-private. Enforce the boundary with ArchUnit later (ADR-0001)
- [x] (26 Oct) Goals API: `PUT` replaces everything; `DELETE` is a hard delete (204); list newest first, capped at 100, optional `status` filter; a missing goal and another user's goal give the same 404
- [x] (26 Oct) Profile insert and goal insert share one transaction on create; reads run without `@Transactional`
- [x] (26 Oct) A deleted account (valid token, no `auth.users` row) is detected inside `JdbcProfileService` by SQLState `23503` and answered with 401
- [x] (26 Oct) Sign-up and sign-in will offer both email and Google
- [x] (29 Oct) Finish the whole Java backend end to end before mobile or Python (to be revisited after V10, see Next)
- [x] (29 Oct) Idempotency: one generic `idempotency_keys` table keyed by `(user_id, scope, key)` holding the created `resource_id`. A replay returns the original with `201` even if the body differs. If the resource was deleted since, the key is released. Keys are not purged until the jobs module exists
- [x] (29 Oct) Consents are append-only; `GET /me/consents` returns the newest row per type; `POST /me/consents` accepts `Idempotency-Key`
- [x] (29 Oct) `PUT /me/ai-settings` writes consent rows only for a value that changes, and rejects journal retrieval on while AI memory is off
- [x] (29 Oct) `GET /me` lives in `profiles` and returns the full profile
- [x] (30 Oct) Milestones: `position` 0-based, composite foreign key to `goals` with cascade, status `not_started`, `in_progress`, `completed`; reorder takes the full list of ids; a reused key under a different goal gives `409`
- [x] (30 Oct) `PLAN_LIMIT_REACHED` deferred to the billing step
- [x] (31 Oct, #41) Tasks: `PATCH` with null meaning "leave as it is"; `completed_at` set by the server; missing, foreign, and unowned goal or milestone all give the same 404; deleting a goal or milestone sets the link to null; `GET /plans/today` computes today from `profiles.time_zone`
- [x] (31 Oct) Controllers do not get class-level `@Validated`: it turned bad parameters into 500s in the first planning CI run. `@Valid` on bodies is fine
- [x] (1 Nov, #43) Sessions: one running session per user (partial unique index), second start gives `409` with `activeSessionId`; `startedAt` set by the database; `complete` floors whole minutes; `GET /sessions/active` returns the session or `204`; activity types `study`, `build`, `revise`, `practice`, `create`, `work`, `read`, `reflect`; no `version` column and no edit endpoint yet
- [x] (1 Nov, V8, merged) Evidence is immutable (no edit); `type` and `activity_level` vocabularies as in `EvidenceVocabulary`; `externalUrl` and `sourceUrl` must be `https://`, never fetched; deleting a session keeps the evidence with `session_id` null. Learning entries: full CRUD with `PUT` replacing everything (an omitted `confidence` is cleared, seen live); `content` 1 to 20000 characters; `confidence` 1 to 5; no `skill_id` yet (V10)
- [x] (1 Nov, V9, PR #46) Reflections: private, hard delete, no `visibility` column. Columns `entry_date`, `raw_text` (1 to 20000 characters), `mood` and `energy` (1 to 5, optional), timestamps. **Several entries per day are allowed** (no unique constraint). No `entryDate` on create means today in the person's time zone, worked out in the database; `PUT` replaces text, mood and energy (omitted mood or energy is cleared) and keeps the old date when none is given. List: optional `from` date, newest entry date first, capped at 100. `structured`, `ai_generated`, `edited_by_user` are left out until the AI branch. Create accepts `Idempotency-Key` (scope `reflection.create`). A missing entry and another user's entry give the same 404. Reflection tests print no response body on failure
- [ ] V10 skills, to decide when the branch starts: skill level progression follows the blueprint ladder (`discovered`, `studying`, `explained`, `practiced`, `built`, `tested`, `applied`); whether a skill's level is stored or derived from linked evidence (derived is more honest, stored is simpler); whether `skill_id` links learning entries only or evidence too; `skill_dependencies` stays deferred
- [ ] Should the API refuse users with no granted `age_18_plus` consent row? Currently recorded, not enforced; leaning towards mobile onboarding first, server check later
- [ ] V3's `profiles.user_type` check has no `parent` (the blueprint lists it). Adding it needs a new migration; decide before onboarding
- [ ] Support and security email; domain and trademark check for "Bevoya"

## Before private beta (not yet started)

Privacy settings, data export and account deletion, Sentry and PostHog with redaction, SMTP provider for sign-up emails, restore drill, privacy policy and terms, Gemini billing on, paid always-on API instance, Play developer account.

## Notes and problems found

- 21 Oct 2026: TypeScript 6 needs `"types": ["jest"]` in `tsconfig.json`.
- 21 Oct 2026: Docker does not work on the dev laptop (ADR-0006). Testcontainers tests run in GitHub Actions, not locally. The gitleaks binary does not need Docker.
- 21 Oct 2026: with `--redact` and no `--verbose`, gitleaks hides the file; use `--verbose --redact`.
- 21 Oct 2026: **Incident.** `.mcp.json` was committed on 18 Oct with a live GitHub fine-grained token. It reached `main`, `develop` and about 30 other branches, so history was not rewritten. Response: token revoked, file untracked, fingerprint `b777b4e46f605f903e8a895130366c7d9175725b:.mcp.json:github-fine-grained-pat:7` kept in `.gitleaksignore`. Rule: MCP and tool config files that hold credentials are never committed. If the repo ever goes public, scrub the history first.
- 21 Oct 2026: a `git` command that opens a pager needs `q`; set `core.pager cat`.
- 22 Oct 2026: **Process slip.** PR #4 was merged while `security / secrets` was red. Always run `gh pr checks N && gh pr merge N --squash --delete-branch`. A PR whose branch gets new commits after the merge does not re-run; open a new PR.
- 22 Oct 2026: In Git Bash, `git show origin/develop:path` can be mangled; prefix with `MSYS_NO_PATHCONV=1`.
- 22 Oct 2026: The Supabase direct host is IPv6-only and does not resolve on the dev laptop. Use the session pooler; the pooler user is `postgres.<project-ref>`. Do not enable the paid IPv4 add-on.
- 22 Oct 2026: Quick connection check without a virtualenv: a short `check.py` outside the repo, `PG*` variables, password via `read -s`, `uv run --with "psycopg[binary]" python check.py`, then `unset`. Never type a placeholder such as `<host>` into an `export`.
- 22 Oct 2026: The first `flyway migrate` of V1 stalled when it altered Flyway's own history table (suspected, not confirmed). Rule: never alter `flyway_schema_history` in a versioned migration; once a version has applied, never edit that file.
- 22 Oct 2026: The Flyway CLI lives in `C:\Program Files\Red Gate\Flyway Desktop\flyway\flyway.cmd`. Migrations are plain SQL, so the runner can change.
- 22 Oct 2026: `gh pr merge --squash --delete-branch` fails its local cleanup if the working tree has uncommitted changes. After a squash merge, delete the old local branch with `git branch -D`.
- 23 Oct 2026: **`ci-mobile` was red on `develop`** (TypeScript 7 beside 6; `npm ci` follows the lockfile). Fixed in PR #19. Expo pins TypeScript (`~6.0.3` for SDK 58); do not accept a major bump.
- 23 Oct 2026: `expo lint` fails locally on Windows with `Cannot find module '@typescript-eslint/parser'`; `eslint .` passes. Cause not identified.
- 23 Oct 2026: A local `npm install` can hide problems that CI's `npm ci` shows. Before pushing dependency changes run `npm ci` then lint, typecheck and tests.
- 23 Oct 2026: After a rebase and force-push, `gh pr checks` can report "no checks reported" until the run registers. Do not merge on an empty result.
- 23 Oct 2026: `schema-check.sql` and `ci-bootstrap.sql` are for CI's throwaway Postgres only.
- 23 Oct 2026: `flyway` from a new shell found no settings; load them with `source scripts/load-env.sh` (see 31 Oct for the working directory).
- 23 Oct 2026: V3 printed `policy ... does not exist, skipping` notices. Expected on first apply (`drop policy if exists`); the same appeared for later tables.
- 24 Oct 2026: Spring Boot 4.x renamed the web starters and moved test annotations; the tests use `@SpringBootTest` on a random port, the JDK `HttpClient` and `${local.server.port}`.
- 24 Oct 2026: The skeleton uses `ResponseEntityExceptionHandler` for RFC 7807 and logs only the exception type for unexpected errors (messages and stack traces can contain fragments of request bodies). The `ERROR ... Unhandled exception` line in `./mvnw verify` comes from the test-only `/test-only/boom` endpoint.
- 24 Oct 2026: `ls` in Git Bash marks executables with `*`. Never type a placeholder path into a command. `mvnw` needs the executable bit in git.
- 24 Oct 2026: Render build worked with `dockerfilePath: ./backend-java/Dockerfile` and `dockerContext: ./backend-java`. The Blueprint has no `branch` field; change the branch in the dashboard after a merge deletes the feature branch.
- 24 Oct 2026: The Supabase project password is the admin `postgres` password. `bevoya_app` has its own. The admin password must never go on Render. Through the pooler the login is `bevoya_app.<project-ref>`; `current_user` shows `bevoya_app`.
- 24 Oct 2026: **Pooler circuit breaker.** After repeated failed logins Supavisor blocks the origin IP for up to 2 minutes. Fix the credentials, wait about 3 minutes, redeploy once. When rotating a database password, stop the services that use it first.
- 24 Oct 2026: Rotating the admin password means updating `FLYWAY_PASSWORD` in `.env.local` and the password manager. `set-app-db-password.sh` sets a pre-hashed SCRAM verifier and copies the password to the clipboard.
- 24 Oct 2026: Second query from Render to Mumbai: about 100 ms, one round trip. Keep round trips per request low (ADR-0002).
- 25 Oct 2026: **Database first-connection delay, unresolved.** First query on Render about 9.7 to 11.2 s; the second is fast. Ruled out: IPv6, the IPv4 flag, a 3 s connect timeout, pool size. Open theories: low CPU share on the free instance slowing TLS and SCRAM in a cold JVM, or the pooler's handling of new connections. Next step if it matters: a probe that times DNS, TCP, TLS and login separately; a paid instance would show whether it is CPU. The first request after a wake can take about 10 s; the mobile app needs a retry, which is why idempotency matters. (31 Oct: startup check timed out at 10 s again, app start 90 s; real-token calls afterwards 0.4 to 5.7 s. 1 Nov: first calls 2.8 to 5.4 s, then mostly 0.13 to 1.1 s, with one 5.2 s outlier on a 400.)
- 25 Oct 2026: Render-only settings (`JAVA_TOOL_OPTIONS`) never belong in `.env.local`.
- 25 Oct 2026: `git pull` opening vim means a merge commit; quit with `:q!`, then `git status` and `git log --oneline --graph --decorate -8 | cat`. `pull.ff only` now refuses instead.
- 25 Oct 2026: **A missing environment variable does not stop Spring from starting.** Check `SUPABASE_JWKS_URL` and `JWT_ISSUER` by behaviour (a real token). `JWT_AUDIENCE` unset still enforces `authenticated`.
- 25 Oct 2026: Quoted values in `.env.local` break `load-env.sh`; keep them unquoted. An earlier odd output (`command not found`) was not explained.
- 25 Oct 2026: In VS Code a new `.java` file can be pre-filled with a class stub and a `package` line; delete the stub. Check the file is under `src/test/java` before pasting test code.
- 25 Oct 2026: Piping Maven output through `grep` printed "Binary file matches"; use `grep -a`.
- 25 Oct 2026: Spring Security 7 on Boot 4.1.2: JWT validation with `jwk-set-uri`, `issuer-uri`, `audiences` and `jws-algorithms=ES256` works; all rejection cases pass. `AuthTokenTest` serves a generated ES256 JWKS from an in-process `HttpServer`.
- 25 Oct 2026: Once `spring-boot-starter-security` is on the classpath every endpoint is locked until a `SecurityFilterChain` permits it. A test-only chain must use `securityMatcher(...)` (here `/test-only/**`), or Spring Security 7 throws `UnreachableFilterChainException`.
- 25 Oct 2026: The Initializr pom preview endpoint is `https://start.spring.io/pom.xml?...`.
- 25 Oct 2026: **pom slip.** Dependency blocks pasted outside `<dependencies>` make Maven report `Malformed POM` and turn unrelated test files red. Run `./mvnw -B -q test-compile 2>&1 | grep -a -E "ERROR|cannot find|does not exist"` before touching the file.
- 25 Oct 2026: Flyway is a test-scope plain dependency on purpose (`spring-boot-starter-flyway` would make every `@SpringBootTest` try to migrate).
- 25 Oct 2026: With Docker unavailable, Testcontainers logs `Could not find a valid Docker environment` and the tests skip. A skipped test proves nothing; the CI log is the evidence.
- 25 Oct 2026: A valid token for an account deleted from `auth.users` becomes `UnknownAccountException` (SQLState `23503`, caught only in `JdbcProfileService`), mapped to 401. Confirmed in CI.
- 25 Oct 2026: Hibernate 7 with metadata access off and an explicit dialect makes no connection at boot; JPA runs on Render (28 Oct).
- 26 Oct 2026: **Package location.** Spring only scans `app.bevoya.api` and below. The `com/bevoya/*` tree was dead scaffold.
- 26 Oct 2026: **Row isolation relies on Java.** The `bevoya_app_all` policy is `using (true)`, so RLS only blocks `anon` and `authenticated`. Repositories expose no `findById`, `findAll` or `deleteById`; every user-data endpoint needs a cross-user test.
- 26 Oct 2026: Goals set both timestamps in the service from a `Clock` truncated to microseconds. Later modules use `now()` in the database.
- 26 Oct 2026: **Correction.** Spring fills `instance` on any `ProblemDetail`, so a missing and a foreign goal differ in `instance` (each echoes the caller's path). That reveals nothing about the record; the test comparison was wrong, not the API.
- 26 Oct 2026: A skipped local run is not evidence; read the `ci-java` log for `Tests run: N, Failures: 0, ... Skipped: 0`.
- 26 Oct 2026: `#73` on the Actions page is the run number, not the PR number. Use `gh pr list --head <branch>`.
- 28 Oct 2026: Real-token timings above 3 s are consistent with the unresolved database delay and a free instance's small CPU share; not measured. Cross-user isolation with two real users has not been tested live; CI covers it.
- 28 Oct 2026: The Supabase SQL Editor shows only the result of the last statement. Never paste an access token or the test password into chat, the tracker or a commit.
- 29 Oct 2026: **Local verify caught a regression before pushing.** After `MeController` was replaced by `ProfileController`, two `AuthTokenTest` tests failed with 500 (`CannotGetJdbcConnectionException`). Rule: when an endpoint starts reading the database directly, check which no-database tests call it; always read the `Tests run:` line, not only BUILD SUCCESS.
- 29 Oct 2026: A local `./mvnw -B verify` takes about 2 minutes.
- 29 Oct 2026: `GET /me` changed from `{"authUserId": ...}` to the full profile; update any client or test comparing the old body.
- 30 Oct 2026: A service that an endpoint calls and that touches the database should be an interface, so no-database tests can stub it.
- 31 Oct 2026: Three merges went to `develop` without a deploy or migration run between them. Do the ops sweep before the next feature so a problem is found against one change.
- 31 Oct 2026: **Flyway working directory.** `FLYWAY_LOCATIONS` is relative to the repo root. Source the loader from `backend-java`, then run `flyway` from the repo root (otherwise Flyway finds no migrations and shows V1 to V3 as `Future`; do not run `migrate` there). `scripts/load-env.sh` must be sourced, not executed.
- 31 Oct 2026: Reading a CI run: `gh run list --branch develop --workflow ci-java --limit 3 --json databaseId,conclusion,displayTitle,createdAt`, then `gh run view <number> --log | grep -a -E "Tests run:" | cut -c1-200`. For a failure: `gh run view <number> --log-failed | grep -a -E "ERROR|FAIL|Tests run:|expected|Caused by" | cut -c1-250 | head -60 | grep -a -v skipping`.
- 31 Oct 2026: Git Bash `node -e` with piped stdin fails; extract the token with `sed`. The test user must be synthetic.
- 31 Oct 2026: **Slip, VS Code stub again** (`TaskCategory.java`). After creating several files: `grep -c "^package" <files> | grep -v ":1$"` must print nothing.
- 31 Oct 2026: **Second time a first CI run of a new Docker test file found failures the local run could not** (goals, then tasks). Expect it; read the log, fix, push again. Sessions passed first time (seen); learning and reflections passed all six checks first time (seen for #46, reported for learning), with the count lines not read.
- 31 Oct 2026: **Two 500s instead of 400s in `TaskApiTest`:** cause inferred, not proven: a class-level `@Validated` made method validation throw `ConstraintViolationException`. Removing it fixed it (settled 1 Nov by run 41227946713).
- 1 Nov 2026: Slips in the sessions module: `ErrorResponseException(status, problem)` does not exist (the form is `(status, problem, cause)`); the test folder `session` did not match the package `sessions`; a miscount (17 tests, not 16).
- 1 Nov 2026: Sessions assumptions settled by CI and now by the live run (409 body with `activeSessionId`, empty-body `complete`, idempotent replay). The lost-race path in `IdempotencyStore.claim` is not covered by a test.
- 1 Nov 2026: **Correction.** An earlier version of this tracker said V6 had been applied before 1 Nov 08:42 UTC. It had not: `flyway info` showed V6 and V7 Pending. Rule: record "applied" or "checked" only from command output; a verbal "done" is "reported".
- 1 Nov 2026: **Miscount.** I predicted 121 tests for the learning branch; the real total was 122 (20 new tests, not 19). Trust `grep -c "^  @Test$"` and the CI log, not my arithmetic. (For reflections the prediction of 131 matched the local run.)
- 1 Nov 2026: Learning assumptions: the `https://\S+` request pattern against the database `~ '^https://'` check is now exercised live (`http://` urls gave 400 for both `externalUrl` and `sourceUrl`, seen); the evidence ordering test relying on `created_at = now()` differing between requests is still only covered by CI.
- 1 Nov 2026: **Slip in the first real-token script:** a stray `\"` in the escaped password expression. Fixed in later versions. The pasted script keeps the token and password in shell variables, never prints them, and clears them (and its temp file) in the cleanup lines.
- 1 Nov 2026: Reflections assumptions to settle from the CI log or the live check: dates passed as text with `cast(:date as date)`; bad dates (`2026-13-45`, `30-10-2026`) giving 400 through Jackson; the default-date path using `profiles.time_zone` (same pattern as `/plans/today`, which passed live). CI was green, so these held in the tests, but the log line was not read and the live reflections check was not shown.
- 1 Nov 2026: **Scope of the second live run.** The 38-line script covers tasks, sessions, evidence and learning entries only. It did not exercise reflections, milestones, goals `PUT` and `DELETE`, consents, AI settings, or a second user. Do not read "real token tested" as covering those.
- 1 Nov 2026: Saving to Claude's memory was blocked for part of the 1 Nov session, so this tracker is the only record of the 1 Nov stages.