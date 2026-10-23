# Bevoya progress tracker

Last updated: 24 October 2026
Update this file at the end of every work session: move items from "Next" to "Done", add a dated line, and refresh the status snapshot. Record only results that were actually seen. Mark anything else "unverified".

## Status snapshot

| Area | State |
|---|---|
| Docs and ADRs | Written and committed |
| Repo scaffold | Done (folders, README, .gitignore files, .gitattributes) |
| CI | PR #4, #16, #18, #19, #20 and #21 merged into `develop`. PR #19 (`fix/ci-mobile-lint`) had five green checks; PR #18 (`feature/db-identity-goals`) and PR #21 (`feature/java-skeleton`) had all six (audit-ai, audit-mobile, ci-db, java, mobile, secrets) before merge. `ci-java` now really builds and tests the app (the gate is gone). **Not yet verified:** the `push` runs on `develop` after the #18, #19 and #21 merges |
| Security incident | Closed for now. Token revoked, `.mcp.json` untracked, finding ignored by fingerprint. The revoked token is still in git history (see Notes) |
| Database | Supabase dev project (Mumbai). V1, V2 and V3 applied through Flyway (`flyway info`: all Success; history table has 3 rows, all `success = true`). `public` has 4 tables (`consent_records`, `flyway_schema_history`, `goals`, `profiles`), all with RLS on. Zero table grants to `anon`, `authenticated`, `service_role`. `bevoya_app`: not superuser, no RLS bypass, can log in. `anon` has no execute privilege on `secure_table()`. **Still to check:** the Supabase Data API switch in the dashboard |
| Java API | Skeleton merged (PR #21): Spring Boot 4.1.2, Java 21, Maven wrapper; `GET /health`, RFC 7807 problem responses with a request ID, request ID in log lines, generic 500 that leaks no exception text; 7 tests pass locally and in CI. No database, security, JPA or Flyway dependencies yet |
| Deployment | `bevoya-api` created on Render from `render.yaml` (Docker, region `singapore`, free plan, `autoDeploy: false`), deployed from branch `chore/render-deploy`. `curl -i https://bevoya-api.onrender.com/health` returned `200`, body `{"status":"ok"}` and an `x-request-id` header. Dashboard region and plan were not shown in output. Free instances sleep after 15 minutes idle. **Not yet done:** merge the branch and switch the service to `develop` |
| Python AI service | Not started (folders only) |
| Mobile app | Route stubs, colour tokens, jest, lint and typecheck working under `npm ci` (17 tests pass). No screens yet |

Current milestone: **M1, the authenticated vertical slice.** Sign up, confirm email, sign in, create a goal, restart the app and still see it. A second account gets `404` on that goal.

## M1 exit criteria

- [ ] Sign-up, confirm email, sign-in and sign-out work on a physical device
- [ ] Expired, wrong-audience, wrong-issuer and unsigned tokens are rejected by tests
- [ ] Cross-user test: every goal endpoint returns `404` for another user's goal
- [ ] Schema test: every `public` table has RLS enabled; Supabase Data API is off (CI schema test merged and green in `ci-db`; dev shows RLS on for all 4 tables; Data API still to re-verify in the dashboard)
- [ ] Logs and Sentry events contain no goal text or tokens
- [ ] Goal list has loading, empty and error states; works at 200% font scale with TalkBack labels
- [ ] All CI workflows pass on `develop`; `bevoya-api` deployed to Render Singapore and `/health` returns 200 (`/health` returned 200 on Render from the deploy branch on 24 Oct; the service must still be switched to `develop` and the `develop` push runs checked)
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
| 22 Oct 2026 | V2 (default function privileges) and V3 (`profiles`, `consent_records`, `goals`) written with `secure_table()`; `infrastructure/db/ci-bootstrap.sql`, `infrastructure/db/schema-check.sql` and the `ci-db` workflow added. `ci-db` run on a fresh Postgres: bootstrap roles created, "Successfully applied 3 migrations ... now at version v3", all six check blocks returned `DO` with no exception |
| 22 Oct 2026 | Found `ci-mobile` failing at the Lint step on `develop` (`TypeError: Cannot read properties of undefined (reading 'Intrinsic')` in `ts-api-utils`). Cause: `package.json` had `typescript ~7.0.2` in `devDependencies` next to `~6.0.3`; `typescript-estree` supports `>=4.8.4 <6.1.0` |
| 23 Oct 2026 | PR #19 `fix(mobile): drop typescript 7, lint with eslint` merged after five green checks. Removed the TypeScript 7 devDependency, kept `typescript ~6.0.3` in `dependencies`, added `@typescript-eslint/parser` as a devDependency, changed the `lint` script from `expo lint` to `eslint .`. Reproduced CI locally with `npm ci`: lint, typecheck and 17 tests pass |
| 23 Oct 2026 | Checked that `eslint .` is a real lint: with `--print-config`, `import/namespace` is active on `_layout.tsx`, and a canary file importing a missing export fails with `import/namespace` (exit 1) |
| 23 Oct 2026 | `feature/db-identity-goals` rebased on `origin/develop` (after #19) and pushed with `--force-with-lease` |
| 23 Oct 2026 | PR #18 `feat(db): identity and goals schema` merged after six green checks, including `ci-db`. The local fast-forward of `develop` then failed because of uncommitted edits to `docs/PROGRESS.md`; the merge on GitHub was not affected |
| 23 Oct 2026 | `flyway migrate` applied V2 and V3 to the dev project ("Successfully applied 2 migrations ... now at version v3") and the `afterMigrate` lock-history callback ran. Flyway shows times in UTC: V1 at 21 Oct 20:08, V2 and V3 at 22 Oct 19:11 |
| 23 Oct 2026 | Dev checks in the Supabase SQL Editor passed: `flyway_schema_history` has 3 rows, all `success = true`; RLS is on for all 4 `public` tables; zero grants to `anon`, `authenticated`, `service_role`; `bevoya_app` has `rolsuper` false, `rolbypassrls` false, `rolcanlogin` true; `has_function_privilege('anon', 'public.secure_table(regclass)', 'execute')` returned false |
| 23 Oct 2026 | PR #20 (tracker update) merged. The first attempt committed the wrong file because the `cp` used a placeholder path; the second commit put the intended `PROGRESS.md` on the branch before the merge |
| 24 Oct 2026 | Data API switch checked in the Supabase dashboard and the DB admin password stored in a password manager (both reported by you; not shown in output) |
| 24 Oct 2026 | `feature/java-skeleton` recut from `develop` (the old pre-created branch had no unique commits and was deleted). Project generated from Spring Initializr (Boot 4.1.2, only the `web` dependency) and merged into `backend-java/` without touching `.gitignore` or the migrations. Added `HealthController`, `RequestIdFilter`, `ApiExceptionHandler`, `application.properties` (port from `PORT`, graceful shutdown, request ID in logs, no error text in responses) and `ApiBaselineTest` with a test-only failing endpoint. Local `./mvnw -B verify`: `Tests run: 7, Failures: 0, Errors: 0`, BUILD SUCCESS |
| 24 Oct 2026 | PR #21 `feat(api): Spring Boot skeleton` merged after six green checks; `ci-java/java` ran the Maven build and tests in CI for the first time. `backend-java/mvnw` is stored with mode `100755` |
| 24 Oct 2026 | `chore/render-deploy` recut from `develop` (the pre-created branch had no unique commits). Added `backend-java/Dockerfile` (Temurin 21, JRE-only run stage, non-root user, heap capped for small instances), `backend-java/.dockerignore` and `render.yaml`. Blueprint applied in Render; first build succeeded and `/health` returned 200 with `{"status":"ok"}` and an `x-request-id` header |

## In progress

- `chore/render-deploy` (commit `8cdeb45` pushed, plus this tracker update): deployed and verified on Render from this branch. Next: push the tracker commit, open one PR to `develop`, merge with `gh pr checks N && gh pr merge N --squash --delete-branch` only when every check is green. Then in the Render dashboard change the service's branch from `chore/render-deploy` to `develop`, run **Manual Deploy**, and check `/health` again, because `autoDeploy` is off and the branch is deleted on merge.
- Clear the Flyway variables from the shell after use: `unset FLYWAY_PASSWORD FLYWAY_USER FLYWAY_URL FLYWAY_LOCATIONS`.
- **Do not run `schema-check.sql` or `ci-bootstrap.sql` against Supabase.** Check 6 inserts into `auth.users`, and the bootstrap creates roles that already exist there. They belong only in CI's throwaway Postgres.

## Next (in order)

1. Merge `chore/render-deploy` and switch the Render service to `develop` (see In progress)
2. Add `ci-db` as a required check name in `GIT_WORKFLOW.md`, and fix `ai` to `audit-ai`
3. Proposed split, `feature/java-database`: give `bevoya_app` its own password out of band, add the PostgreSQL and Flyway modules and JPA at the versions Spring Boot manages (not the rc used locally); connect through the session pooler; keep Flyway migrations off Render; set the database variables in Render with `sync: false`; add the Testcontainers empty-database migration test in CI
4. `feature/java-auth`: JWKS validation (pin ES256), `CurrentUser`, `GET /me`, cross-user tests
5. `feature/mobile-design-system`: `typography.ts`, `lightTheme` and `darkTheme`, `useTheme()`, Button, Card, TextField, EmptyState, ErrorState
6. `feature/mobile-navigation`: sign-in screens, auth gate, goals list and create form
7. `feature/ai-service-skeleton` (after M1)

## Small tasks to fit in

- [ ] Add a Dependabot `ignore` for major `typescript` updates in `.github/dependabot.yml` (Expo pins the version). First check who added 7.0.2: `git log -p -3 -- mobile/package.json | cat`. Origin not yet checked
- [ ] Run `npx expo install --check` in `mobile/` and read what it reports. It was not run after the fix; it may flag the explicit `@typescript-eslint/parser` pin
- [ ] Investigate why `expo lint` fails on Windows (`Parse errors in imported module ... Cannot find module '@typescript-eslint/parser'`) while `eslint .` passes on the same config. Unexplained; `eslint .` is the workaround. Retry `expo lint` after future Expo or eslint-config-expo updates
- [ ] Review the 52 npm audit findings (21 moderate, 31 high) separately. Do not run `npm audit fix --force`; it can move Expo-pinned packages
- [ ] Review the npm `allowScripts` warning for `@parcel/watcher` and `unrs-resolver` (`npm install-scripts ls`)
- [ ] ESLint 9.39.5 is reported deprecated by npm. Check Expo's supported ESLint version before upgrading
- [ ] Confirm in the Render dashboard that `bevoya-api` shows region Singapore and plan Free (not visible in the `curl` output; the region cannot be changed after creation)
- [ ] Pin the base images in `backend-java/Dockerfile` (`eclipse-temurin:21-jdk` and `21-jre` are floating tags) and let Dependabot's Docker entry track them
- [ ] Tidy `backend-java/pom.xml`: Initializr left empty `<licenses>`, `<developers>`, `<scm>` and `<url/>` elements and an empty `<description/>`
- [ ] Comment `@dependabot rebase` on the open Dependabot PRs so they pick up the fixed workflows
- [ ] Mark ADR-0004 as Accepted (embedding model chosen)
- [ ] Update `docs/product/AI_RULES.md` section 5 to say `gemini-embedding-2`
- [ ] Check the doc links in `README.md` match the real folder layout
- [ ] Remove the CI gate in `ci-ai.yml` when the AI project exists (the `ci-java.yml` gate is removed on `feature/java-skeleton`)
- [ ] Add Maven (`backend-java`), uv and Docker entries to `.github/dependabot.yml`. The Maven project now exists
- [ ] Replace placeholder launcher icon and splash image with the real mark
- [ ] Create the new, minimum-scope GitHub token for the MCP server and store it as a user environment variable (`GITHUB_PERSONAL_ACCESS_TOKEN`), never in a file
- [ ] Set the repository default branch to `develop` (`origin/HEAD` currently points to `main`, which contradicts `GIT_WORKFLOW.md`)
- [ ] Every branch the plan names already exists, pre-created and stale. When you reach one, do not create it. Check `git log origin/develop..origin/<branch> --oneline | cat`; if it has no unique commits, delete it (`git push origin --delete <branch>`, `git branch -D <branch>`) and cut it again from `develop`. Prune the roughly 30 pre-created branches you are not about to use; they drift from `develop`, and `GIT_WORKFLOW.md` says to branch when work starts
- [ ] Confirm the `.githooks/pre-push` guard is committed and `core.hooksPath` is set; set `git config --global core.pager cat`
- [ ] Add `.mcp.json` handling to `ENVIRONMENT.md` (secrets in MCP config are secrets too)
- [ ] Update the `gitleaks` Docker image pin deliberately when a new version is needed (Dependabot cannot bump it)
- [ ] Rename the Supabase project to `bevoya-dev` if it is not already, so dev and a later prod project cannot be confused
- [ ] `ENVIRONMENT.md`: rename `EXPO_PUBLIC_SUPABASE_ANON_KEY` to `EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY` (Supabase's current name for the public key)
- [ ] Check `backend-java/.gitignore` and `ai-python/.gitignore`: the `.env.*` rule also hides `.env.example`; add `!.env.example` so the example files can be committed
- [ ] Give `bevoya_app` its own password out of band (never in a migration or repo file). The admin `postgres` password is for Flyway only and never goes on Render
- [ ] `ENVIRONMENT.md`: add `FLYWAY_LOCATIONS`, the session-pooler JDBC URL form and the rule that Flyway never runs on Render
- [ ] Add the vector extension in its own migration when embeddings start (ADR-0004); V1 does not include it
- [ ] The `ci-db` job uses a throwaway `postgres:postgres` connection URL. If `security / secrets` flags it, allow it narrowly (by fingerprint) with a comment saying it is a CI-only throwaway
- [ ] The empty-database Testcontainers test now comes with the database wiring branch, not the skeleton; until then `ci-db` is the only empty-database migration test

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
- 23 Oct 2026: **`ci-mobile` was red on `develop`.** `typescript ~7.0.2` sat in `devDependencies` beside `~6.0.3` in `dependencies`. CI uses `npm ci`, which follows the lockfile and installed 7.0.2. `ts-api-utils` reads `ts.TypeFlags.Intrinsic`, which is undefined there, so ESLint crashed. Local `node_modules` had been stale at 6.0.3, which hid it. Fixed in PR #19. Rule: Expo pins the TypeScript version (`~6.0.3` for SDK 58); do not accept a major bump. Also, an earlier note said PR #16 had five green checks; that was true for the PR, but `develop` was red afterwards.
- 23 Oct 2026: `expo lint` fails locally on Windows with `Cannot find module '@typescript-eslint/parser'` from `eslint-plugin-import`, even though the parser loads from the project root, from ESLint's location and through the plugin's own loader. `eslint .` passes on the same config and catches a real `import/namespace` error. Cause of the wrapper failure is **not identified**; the `lint` script now uses `eslint .`. An early `import/named` canary passed with exit 0 because that rule is not the one in play for TypeScript files; use `import/namespace` for canaries.
- 23 Oct 2026: A local `npm install` can hide dependency problems that CI's `npm ci` shows. Before pushing dependency changes, run `npm ci` then `npm run lint && npm run typecheck && npm test -- --ci`.
- 23 Oct 2026: After a rebase and force-push, `gh pr checks` can report "no checks reported" until the new run registers. Wait and re-run; do not merge on an empty result.
- 23 Oct 2026: `infrastructure/db/schema-check.sql` and `ci-bootstrap.sql` are for CI's throwaway Postgres only. Never run them against Supabase (the bootstrap creates roles that already exist, and check 6 inserts into `auth.users`).
- 23 Oct 2026: `flyway` run from a new shell found no connection settings and no locations. The `FLYWAY_*` values are in the git-ignored `backend-java/.env.local` and are not loaded automatically. Load them with `set -a; source <(tr -d '\r' < backend-java/.env.local); set +a` from the repo root (the `tr` strips Windows line endings), run Flyway, then `unset` them. Run `flyway info` before `flyway migrate`.
- 23 Oct 2026: V3 printed `policy "bevoya_app_all" for relation "public.<table>" does not exist, skipping` three times. This looks like a `drop policy if exists` notice on a fresh table and is expected on first apply (inferred from the message, not checked against the `secure_table()` source). The `schema-check.sql` run in CI and the dev checks above both passed.
- 23 Oct 2026: `gh pr merge --squash --delete-branch` can merge on GitHub but fail the local fast-forward when `docs/PROGRESS.md` has uncommitted edits. Copy the file aside, `git checkout -- docs/PROGRESS.md`, then `git pull`.
- 23 Oct 2026: V1 to V3 were all found in the working tree on `develop` when Flyway validated "3 migrations", which means the V1 branch had reached `develop`.
- 24 Oct 2026: Spring Boot 4.x renamed the web starters (`spring-boot-starter-webmvc`, `spring-boot-starter-webmvc-test`) and moved test annotations into new packages. The skeleton tests avoid those annotations: `@SpringBootTest` on a random port, the JDK `HttpClient`, and `${local.server.port}`. Keep that in mind when generating code from older examples.
- 24 Oct 2026: The skeleton uses `ResponseEntityExceptionHandler` for RFC 7807 (`application/problem+json`) and logs only the exception type for unexpected errors. Exception messages and stack traces can contain fragments of request bodies, so this is deliberate; revisit when Sentry scrubbing exists. The `ERROR ... Unhandled exception` line printed during `./mvnw verify` comes from the test-only `/test-only/boom` endpoint and is expected.
- 24 Oct 2026: `ls` in Git Bash on Windows marks executable files with `*`; a `$(ls ...)` path then ends in `*`. Strip it with `SRC="${SRC%\*}"`. Also never type a placeholder path such as `/path/to/file` into a command.
- 24 Oct 2026: `git add` warned `LF will be replaced by CRLF` for `mvnw.cmd`. Harmless on Windows. The shell script `mvnw` needs the executable bit in git (`git update-index --chmod=+x backend-java/mvnw`); CI also runs `chmod +x mvnw`.
- 24 Oct 2026: Render build and first deploy worked on the first try with `dockerfilePath: ./backend-java/Dockerfile` and `dockerContext: ./backend-java` (repo-root paths, no `rootDir`). The service was created from the feature branch before merging so the Dockerfile was proven before it reached `develop`. Because the Blueprint has no `branch` field, the branch is whatever was chosen in the dashboard; it must be changed to `develop` after the merge deletes the feature branch.
- 24 Oct 2026: The pre-created `chore/render-deploy` branch had the same commit hash as the other stale pre-created branches (`bfba0ee`) and no unique commits; it was deleted and recut, as the tracker rule says.