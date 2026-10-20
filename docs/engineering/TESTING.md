# Bevoya testing strategy

Version 1 · 20 October 2026

## 1. Layers

| Layer | Tools | What must be tested |
|---|---|---|
| Java unit | JUnit 5, Mockito | Business rules, validation, quota logic |
| Java integration | Spring Boot Test, Testcontainers (pgvector image) | Every migration on an empty database; repositories; endpoints with a real database |
| Java security | Spring Security Test, a JWKS stub | Expired, wrong issuer, wrong audience, wrong algorithm, unsigned tokens; cross-user access on every endpoint |
| Architecture | ArchUnit | No entity in controllers; no cross-module repository use; no bare `findById` on user data |
| Schema | SQL assertions | Every `public` table has RLS on; composite foreign keys reject cross-user links |
| Python | pytest, respx (mocked Gemini) | Schema validation; malformed output; timeout and 429; token ceiling; internal token required |
| AI safety | pytest, synthetic fixtures | Prompt injection; no medical, legal or financial advice; "no evidence" case |
| AI quality | `eval/run_eval.py` | Extraction accuracy; recommendations cite real source ids; run before changing a prompt or model |
| Mobile unit | Jest, React Native Testing Library | Forms and Zod schemas; offline queue ordering and idempotency; token storage; analytics allow-list |
| Mobile end to end | Maestro | Sign in, create goal, plan a task, run a session, write a reflection |
| Accessibility | Role and label queries; manual TalkBack; 200% font scale | Primary flows |
| Contract | OpenAPI diff in CI | Committed spec equals generated spec |

## 2. Gates

No coverage percentage target. The gates are: a cross-user test exists for every user-data endpoint; migrations apply cleanly; AI paths have schema and failure tests; primary mobile flows pass on a device; no secret in the repository.

## 3. Without local Docker (ADR-0006)

Testcontainers tests need Docker, which is not available on the development laptop. They run in GitHub Actions on every push and pull request. Unit tests, Python tests and mobile tests run locally. Use a hosted dev database with synthetic data for manual checks.

## 4. Test data

Synthetic only until Gemini billing is enabled and a privacy review is done. Never use real people's journals in tests, previews or evaluations.

## 5. Install later (not yet in the mobile project)

`jest-expo`, `jest`, `@types/jest`, `react-test-renderer` (matching the React version) and `@testing-library/react-native` are added with the first mobile test, so install problems do not block scaffolding.
