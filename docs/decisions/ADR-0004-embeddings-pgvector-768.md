# ADR-0004: Embeddings with pgvector at 768 dimensions

**Status:** Proposed
**Date:** 20 October 2026
**Deciders:** Founder (Leo Noel Zuze AkA jayzthe_plug)

## Context
The launch file chose gemini-embedding-001. Google now lists gemini-embedding-2 as stable (April 2026) with adjustable output size (128 to 3072, 768 recommended). The default output is 3072 dimensions, and pgvector's HNSW index on the vector type is limited to 2000 dimensions. Vectors from different models cannot be mixed.

## Decision
Store embeddings in PostgreSQL with pgvector at 768 dimensions. Store the model name and dimensions on every row. Do not add an HNSW index until row counts justify it (exact search filtered by user id is fine at small per-user sizes). **Open:** choose gemini-embedding-001 or gemini-embedding-2 before any vector is stored, then mark this ADR Accepted.

## Options Considered

### Option A: gemini-embedding-001 at 768

Matches the launch file and a known price ($0.15 per million input tokens in your file).

### Option B: gemini-embedding-2 at 768

Newer, stable, multimodal; check its current price before choosing.

## Consequences
- Changing model later means re-embedding every row.
- Normalise vectors yourself unless the model's documentation says they are normalised.
- Delete embeddings with their source, in the same transaction.
