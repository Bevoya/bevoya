# ADR-0002: Render in Singapore, Supabase in Mumbai

**Status:** Accepted
**Date:** 20 October 2026
**Deciders:** Founder (Leo Noel Zuze AkA jayzthe_plug)

## Context
The first market is India (Pune). Supabase offers Mumbai (ap-south-1). Render has no Mumbai region; its regions are Oregon, Ohio, Virginia, Frankfurt and Singapore. Render cannot change a service's region after creation.

## Decision
Host bevoya-api and bevoya-ai on Render in Singapore, in the same region. Keep the database, Auth and Storage in Supabase Mumbai.

## Options Considered

### Option A: Render Singapore with Supabase Mumbai

Nearest Render region to India; data stays in India.

### Option B: A host with a Mumbai region (for example Cloud Run in Mumbai or Fly.io)

Lower latency to the database, but a different platform and billing setup than the launch decision.

## Consequences
- Harder: every database query crosses regions; keep round trips per request low and measure p95 latency.
- Revisit: if latency hurts, move compute to a Mumbai-capable host. Images are portable OCI containers.
