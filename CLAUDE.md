# Bevoya: rules for AI coding assistants

Bevoya is a privacy-first, Android-first personal growth app. Core loop: Goal, Roadmap, Daily plan, Session, Evidence, Reflection, Review, Next action. Read `docs/` before changing anything: `product/PRODUCT.md`, `product/AI_RULES.md`, `design/DESIGN.md`, `architecture/ARCHITECTURE.md`, `decisions/`.

## Stack (do not change without an ADR)

Mobile: React Native, TypeScript, Expo, Expo Router (routes in `mobile/src/app`), TanStack Query, React Hook Form, Zod, expo-sqlite, expo-secure-store.
Backend: Java 21, Spring Boot modular monolith (package by feature), Spring Security resource server, Spring Data JPA, Flyway.
AI service: Python, FastAPI, Pydantic, Gemini behind an `LlmGateway` interface.
Data: Supabase PostgreSQL (Mumbai) with pgvector, Supabase Auth. Hosting: Render (Singapore). No Redis, queues, vector DB or Node backend without an ADR.

## Ownership

Java owns auth, canonical records, consent, quotas, jobs, billing, audit and **all database access**. Python owns LLM calls, embeddings and extraction and has **no database access**. Python proposes; Java validates and persists.

## Never

- Trust a client-supplied user id. Use `CurrentUser` from the verified JWT. Return 404 for another user's records.
- Put secrets, service-role keys or AI keys in the app or the repo. Read the guidance in `docs/engineering/ENVIRONMENT.md`.
- Log or send to analytics, Sentry, notifications or third parties: journals, reflections, notes, prompts, AI responses, tokens, signed URLs.
- Let AI output become state without schema and business validation. Give the model tools, SQL or URL fetching.
- Add dating or romantic features, financial or medical advice, autonomous life decisions, guilt-based copy, fake urgency, or guarantees of results.
- Hard-code colours, font sizes or model names. Use theme tokens and environment variables.
- Use real people's data in tests or the Gemini free tier. Synthetic data only.

## Always

- Add a cross-user test for every user-data endpoint; enable RLS on every new table with `secure_table()`.
- Handle loading, empty, error and offline states; support 200% font scale and TalkBack; touch targets 44 dp minimum (48 preferred); status never by colour alone.
- Follow `docs/design/COLORS.md` contrast rules and `docs/design/VOICE_AND_TONE.md`.
- Keep changes small, use Conventional Commits, branch from `develop` using the names in `docs/engineering/GIT_WORKFLOW.md`, and tick `docs/engineering/DEFINITION_OF_DONE.md`.
- Say when something is unverified. Prefer the simplest solution; ask before adding a service.
