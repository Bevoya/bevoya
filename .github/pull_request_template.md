## What and why

## Definition of done
- [ ] It works on a real Android device (if it touches the app).
- [ ] Tests exist and pass, including a cross-user test for any endpoint that touches user data.
- [ ] Loading, empty, error and offline states are handled.
- [ ] It uses theme tokens, 44 dp minimum (48 preferred) touch targets, accessibility labels, and works at 200% font scale.
- [ ] No private text (journals, notes, prompts, responses) appears in logs, analytics, Sentry or notifications.
- [ ] New tables call `secure_table()`; migrations apply to an empty database.
- [ ] The OpenAPI spec and TypeScript types are regenerated.
- [ ] Any AI path has a per-user quota, a token ceiling and schema-validated output.
- [ ] No new secret in code; `.env.example` is updated if a variable was added.
- [ ] Docs or an ADR are updated.
- [ ] A reviewer (or you, against this list when working alone) has checked it.