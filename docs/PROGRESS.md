# Bevoya progress tracker

Last updated: 21 October 2026
Update this file at the end of every work session: move items from "Next" to "Done", add a dated line, and refresh the status snapshot.

## Status snapshot

| Area | State |
|---|---|
| Docs and ADRs | Written and committed |
| Repo scaffold | Done (folders, README, .gitignore files, .gitattributes) |
| CI | PR #4 open. mobile, java, audit-ai and audit-mobile checks passed earlier. The secret finding is identified and handled (see below). **Waiting for CI to re-run and confirm `security / secrets` is green** |
| Security incident | A GitHub fine-grained token was committed in `.mcp.json` (commit `b777b4e`). Token revoked, file untracked, finding ignored by fingerprint. Local scan is clean. The token is still in git history (see Notes) |
| Database | Not started |
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
| 21 Oct 2026 | CI workflows, Dependabot and `docs/engineering/` files written; PR #4 opened, four of five checks green |
| 21 Oct 2026 | GitHub CLI authenticated; gitleaks installed locally (winget). `--verbose` step pushed to `security.yml` |
| 21 Oct 2026 | Finding identified: `github-fine-grained-pat` in `.mcp.json` line 7, commit `b777b4e` (18 Oct). Token revoked; `.mcp.json` untracked and git-ignored; `.mcp.json.example` added (token read from an environment variable); fingerprint added to `.gitleaksignore`. Local scan: 25 commits, no leaks |

## In progress

- PR #4 (`chore/ci-pipelines` to `develop`): the last two pushes (`4068721`, `1c049c3`) did not show a new `security` run; `gh run list` still showed run `39595698813` (failed, old command). Next action: run `gh pr view 4 --json mergeable,mergeStateStatus`. If `CONFLICTING`, merge `origin/develop` into the branch and push (pull_request workflows do not run on a conflicting PR). When all checks are green: `gh pr checks 4 && gh pr merge 4 --squash --delete-branch`. Never merge a red or missing check.

## Next (in order)

1. Confirm CI green and merge PR #4; then confirm the `push` run on `develop` passes
2. Create the `bevoya-dev` Supabase project in Mumbai (`ap-south-1`); check JWT signing keys; turn off the Data API
3. `feature/db-schema-migrations`: V1 baseline with `secure_table()` and grants, V2 identity, goals; run against the Supabase dev project
4. `feature/java-skeleton`: Spring Boot, health endpoint, RFC 7807 errors, request ID; first Render deploy
5. `feature/java-auth`: JWKS validation, `CurrentUser`, `GET /me`, cross-user tests
6. `feature/mobile-design-system`: `typography.ts`, `lightTheme` and `darkTheme`, `useTheme()`, Button, Card, TextField, EmptyState, ErrorState
7. `feature/mobile-navigation`: sign-in screens, auth gate, goals list and create form
8. `feature/ai-service-skeleton` (after M1)

## Small tasks to fit in

- [ ] Mark ADR-0004 as Accepted (embedding model chosen)
- [ ] Update `docs/product/AI_RULES.md` section 5 to say `gemini-embedding-2`
- [ ] Check the doc links in `README.md` match the real folder layout
- [ ] Remove the CI gates in `ci-java.yml` and `ci-ai.yml` when those projects exist
- [ ] Add Maven, uv and Docker entries to `.github/dependabot.yml` when scaffolded
- [ ] Replace placeholder launcher icon and splash image with the real mark
- [ ] Create the new, minimum-scope GitHub token for the MCP server and store it as a user environment variable (`GITHUB_PERSONAL_ACCESS_TOKEN`), never in a file
- [ ] Set the repository default branch to `develop` (`origin/HEAD` currently points to `main`, which contradicts `GIT_WORKFLOW.md`)
- [ ] Prune the roughly 30 pre-created branches you are not about to use; they all contain `b777b4e`, drift from `develop`, and `GIT_WORKFLOW.md` says to branch when work starts. Recreate each from `develop` when needed
- [ ] Fix the required-check name mismatch: `GIT_WORKFLOW.md` says `ai`, the workflow job is `audit-ai`
- [ ] Add the local `pre-push` guard (`.githooks/pre-push`, `git config core.hooksPath .githooks`) and set `git config --global core.pager cat`
- [ ] Add `.mcp.json` handling to `ENVIRONMENT.md` (secrets in MCP config are secrets too)

## Decisions still open

- [ ] Android application id (currently `app.bevoya.android`; cannot change after publishing)
- [ ] Gemini billing: enable pay-as-you-go before any real person's data is used
- [x] GitHub plan: staying on the free plan, so no branch protection. Rule: never merge a red check
- [ ] Branch protection: a private repo on a free organisation cannot enforce rulesets. Choose: upgrade to Team (check current price), or enforce by tooling (pre-push hook plus the `gh pr checks` merge gate). Do not make the repo public while the revoked token remains in history
- [ ] Weekly hours available (decides where the cut line goes)
- [ ] Keep `develop`, or collapse to `main` plus short branches
- [ ] Confirm the Supabase project uses JWT signing keys (needed before Java auth)
- [ ] Support and security email; domain and trademark check for "Bevoya"

## Before private beta (not yet started)

Privacy settings, data export and account deletion, Sentry and PostHog with redaction, SMTP provider for sign-up emails, restore drill, privacy policy and terms, Gemini billing on, paid always-on API instance, Play developer account.

## Notes and problems found

- 21 Oct 2026: TypeScript 6 needs `"types": ["jest"]` in `tsconfig.json`; without it test globals are unknown.
- 21 Oct 2026: Docker does not work on the dev laptop (ADR-0006). Secret scans and Testcontainers tests run in GitHub Actions, not locally. The gitleaks binary does not need Docker, so scan locally with it.
- 21 Oct 2026: with `--redact` and no `--verbose`, gitleaks hides the file; use `--verbose --redact`. The first CI failure stayed opaque because the local fix had not been pushed, and the run log showed the old command.
- 21 Oct 2026: **Incident.** `.mcp.json` was committed on 18 Oct with a live GitHub fine-grained token. It reached `main`, `develop` and about 30 other branches, so history was not rewritten (it would not remove the copy on GitHub's servers anyway). Response: token revoked, file untracked, fingerprint `b777b4e46f605f903e8a895130366c7d9175725b:.mcp.json:github-fine-grained-pat:7` kept in `.gitleaksignore`. Rule: MCP and tool config files that hold credentials are never committed; use environment variables. If the repo ever goes public, scrub the history first.
- 21 Oct 2026: a `git` command that opens a pager (`(END)` prompt) needs `q`; set `core.pager cat` to avoid it.
