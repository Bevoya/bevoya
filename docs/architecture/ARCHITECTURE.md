# Bevoya architecture (summary)

Version 1 · 20 October 2026
This is the short version. The full blueprint (schema, API contract, CI, risk register) is the Claude Doc "Bevoya: Architecture & Build Blueprint". Decisions are recorded in `docs/decisions/`.

## 1. Shape

A modular monolith plus one small AI service and one database. Nothing else until a measured need exists.

```text
Android app (React Native, Expo, TypeScript)
   | sign-in                 | HTTPS + Bearer token
   v                         v
Supabase Auth        Java Spring Boot API (Render, Singapore)
(Mumbai)              | JDBC (role bevoya_app)      | HTTPS + internal token
                      v                             v
          Supabase PostgreSQL + pgvector     Python FastAPI AI service (Render, Singapore)
          (Mumbai, ap-south-1)                      | HTTPS + API key
                                                    v
                                              Google Gemini API
```

## 2. Ownership

| Java owns | Python owns |
|---|---|
| Authentication and authorization, canonical records, consent, quotas, schedules and jobs, integration tokens, billing entitlements, audit, deletion, all database access | LLM calls, embeddings, structured extraction, evaluation, prompt versions. **No database access** |

Python proposes. Java validates and persists. The mobile app never holds database passwords, service-role keys, AI-provider keys, webhook secrets or private encryption keys.

## 3. Trust boundaries

1. The app is untrusted. Java trusts only the subject of a verified JWT.
2. Python trusts only Java (internal token) and is never called by the app.
3. Model output is untrusted until schema-validated, then validated by Java.

## 4. An AI request

App calls Java. Java checks the JWT, ownership, consent and quota, builds minimal context, calls Python. Python builds a versioned prompt, calls Gemini with token ceilings, validates the schema and returns data plus token usage. Java validates business rules, records usage, stores a `proposed` recommendation. The app shows an "AI insight" card. Accept, Edit or Dismiss calls a normal Java endpoint.

## 5. Data

One PostgreSQL database (Supabase, Mumbai). Flyway migrations, owned by Java. UUID keys, UTC timestamps, `user_id` on every user-owned table with cascade delete, composite foreign keys so a record cannot point at another user's record, RLS enabled on every table, Supabase Data API off. pgvector at 768 dimensions in the same database. Journals and learning notes are separate; journal retrieval needs explicit consent.

## 6. Environments

| Environment | Database | Auth | Notes |
|---|---|---|---|
| Local | Hosted Supabase dev project (no local Docker for now, ADR-0006) | Supabase dev | Java and Python run natively |
| CI | Testcontainers on GitHub Actions | stubbed JWKS | Integration tests run here |
| Production | Supabase prod project (Mumbai) | Supabase prod | Render Singapore for Java and Python |

## 7. Deliberately not built yet

Redis, RabbitMQ, Kafka, Kubernetes, a dedicated vector database, a Node.js backend, multiple production databases, LangChain or LangGraph agents. Add one only through a new ADR with a measured trigger.

## 8. Known trade-offs

- Render has no Mumbai region, so every database call crosses regions. Keep round trips low.
- Free tiers sleep and have small limits; plan paid instances before the private beta.
- No local Docker means integration tests run in CI, with slower feedback.
