# ADR-0003: The AI service has no database access

**Status:** Accepted
**Date:** 20 October 2026
**Deciders:** Founder (Leo Noel Zuze AkA jayzthe_plug)

## Context
The technology stack file allowed a restricted psycopg role for Python where necessary. Nothing in the MVP needs it.

## Decision
Java sends the AI service minimal context and runs all database queries, including pgvector similarity search. Python returns embeddings and structured results and holds no database credentials.

## Options Considered

### Option A: No database access for Python

One fewer credential, one fewer path for a cross-user leak, simple ownership.

### Option B: Restricted Python role for retrieval

Fewer round trips, but a second place where owner filters must be right.

## Consequences
- Easier: stronger isolation and a smaller attack surface.
- Harder: Java must pass retrieval context explicitly.
- Revisit: only if retrieval latency or complexity proves it necessary; then add a read-only role scoped by views.
