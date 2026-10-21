# Bevoya progress tracker

Last updated: 21 October 2026
Update this file at the end of every work session: move items from "Next" to "Done", add a dated line, and refresh the status snapshot.

## Status snapshot

| Area | State |
|---|---|
| Docs and ADRs | Written and committed |
| Repo scaffold | Done (folders, README, .gitignore files, .gitattributes) |
| CI | On branch `chore/ci-pipelines`, not merged yet |
| Database | Not started |
| Java API | Not started (folders only) |
| Python AI service | Not started (folders only) |
| Mobile app | Route stubs, colour tokens, jest and lint working. No screens yet |

Current milestone: **M1, the authenticated vertical slice.** Sign up, confirm email, sign in, create a goal, restart the app and still see it. A second account gets `404` on that goal.

## M1 exit criteria

- [ ] Sign-up, confirm email, sign-in and sign-out work on a physical device
- [ ] Expired, wrong-audience, wrong-issuer and unsigned tokens are rejected by tests
- [ ] Cross-user test: every goal endpoint returns `404` for another user's goal
- [ ] Schema test: every `public` table has RLS enabled; Supabase Data API is off
- [ ] Logs and Sentry events contain no goal text or tokens
- [ ] Goal list has loading, empty and error states; works at 200% font scale with TalkBack labels
- [ ] All CI workflows pass on `develop`; `bevoya-api` deployed to Render Singapore and `/health` returns 200
- [ ] No secret in repository history (gitleaks clean)

## Done

| Date | What |
|---|---|
| 20 Oct 2026 | Product, design, architecture, security and privacy docs and ADR-0001 to ADR-0006 written |
| 20 Oct 2026 | `CLAUDE.md` added at repo root, next to `README.md` |
| 20 Oct 2026 | Decisions: launch 18+ only (ADR-0005); embeddings with `gemini-embedding-2` at 768 dimensions |
| 21 Oct 2026 | README redesigned with language bar and badges; `.gitattributes`; `.gitignore` for `backend-java` and `ai-python` committed |
| 21 Oct 2026 | Jest and ESLint set up in `mobile/`; brand colour contrast test passes (17 tests) |
| 21 Oct 2026 | `app.json` updated: Android package, brand colours for icon and splash |

## In progress

- Branch `chore/ci-pipelines`: CI workflows, `docs/engineering/` files, tsconfig `types` fix. Next action: pass typecheck, commit, push, open PR to `develop`, merge when checks are green.

## Next (in order)

1. Merge `chore/ci-pipelines`; confirm all checks are green on GitHub
2. `feature/db-schema-migrations`: V1 baseline with `secure_table()` and grants, V2 identity, goals; run against the Supabase dev project
3. `feature/java-skeleton`: Spring Boot, health endpoint, RFC 7807 errors, request ID; first Render deploy
4. `feature/java-auth`: JWKS validation, `CurrentUser`, `GET /me`, cross-user tests
5. `feature/mobile-design-system`: `typography.ts`, `lightTheme` and `darkTheme`, `useTheme()`, Button, Card, TextField, EmptyState, ErrorState
6. `feature/mobile-navigation`: sign-in screens, auth gate, goals list and create form
7. `feature/ai-service-skeleton` (after M1)

## Small tasks to fit in

- [ ] Mark ADR-0004 as Accepted (embedding model chosen)
- [ ] Update `docs/product/AI_RULES.md` section 5 to say `gemini-embedding-2`
- [ ] Check the doc links in `README.md` match the real folder layout
- [ ] Remove the CI gates in `ci-java.yml` and `ci-ai.yml` when those projects exist
- [ ] Add Maven, uv and Docker entries to `.github/dependabot.yml` when scaffolded
- [ ] Replace placeholder launcher icon and splash image with the real mark

## Decisions still open

- [ ] Android application id (currently `app.bevoya.android`; cannot change after publishing)
- [ ] Gemini billing: enable pay-as-you-go before any real person's data is used
- [ ] GitHub plan: pay for branch protection on a private repo, or rely on discipline
- [ ] Weekly hours available (decides where the cut line goes)
- [ ] Keep `develop`, or collapse to `main` plus short branches
- [ ] Confirm the Supabase project uses JWT signing keys (needed before Java auth)
- [ ] Support and security email; domain and trademark check for "Bevoya"

## Before private beta (not yet started)

Privacy settings, data export and account deletion, Sentry and PostHog with redaction, SMTP provider for sign-up emails, restore drill, privacy policy and terms, Gemini billing on, paid always-on API instance, Play developer account.

## Notes and problems found

- 21 Oct 2026: TypeScript 6 needs `"types": ["jest"]` in `tsconfig.json`; without it test globals are unknown.