# Bevoya Definition of Done

Version 1 · 21 October 2026
This list matches `.github/pull_request_template.md`. Tick it in every pull request. When working solo, review your own diff against it.

## Always

- [ ] Tests exist and pass, including a **cross-user test** for any endpoint that touches user data.
- [ ] No private text (journals, reflections, notes, prompts, AI responses) in logs, analytics, Sentry or notifications.
- [ ] No new secret in code. `.env.example` and `docs/engineering/ENVIRONMENT.md` are updated if a variable was added.
- [ ] Docs or an ADR are updated when behaviour or a decision changes.
- [ ] Conventional Commit messages; branch name follows `GIT_WORKFLOW.md`.

## Mobile

- [ ] Works on a real Android device, not only an emulator.
- [ ] Loading, empty, error and offline states exist.
- [ ] Uses theme tokens: no hard-coded colours, font sizes or model names.
- [ ] Touch targets are at least 44 dp (48 preferred). Every element has an accessible name and role.
- [ ] Usable at 200% font scale, with TalkBack, and with reduced motion. Status is never colour alone.
- [ ] Colour pairs come from `COLORS.md`; new pairs have a checked contrast ratio.
- [ ] Copy follows `VOICE_AND_TONE.md`: no guilt, no fake urgency, no guarantees.

## Backend

- [ ] The user id comes from `CurrentUser`, never from the request. A foreign record returns `404`.
- [ ] New tables call `secure_table()`; migrations apply cleanly to an empty database.
- [ ] OpenAPI spec regenerated and committed; mobile TypeScript types updated.
- [ ] Controllers return DTOs, not entities. No bare `findById` on user data.

## AI paths

- [ ] Per-user quota, token ceiling and schema-validated output.
- [ ] Python has no database access; Java validates business rules and persists.
- [ ] The model has no tools, no SQL and no URL fetching.
- [ ] Failure cases covered: malformed output, timeout, prompt injection, no evidence available.
- [ ] Prompt or model change: the synthetic evaluation set was run and compared.

## Releases

- [ ] Account deletion and data export still work (neither depends on a paid plan).
- [ ] `security` workflow is green; no secret in history.