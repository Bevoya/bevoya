# ADR-0006: No local Docker for now

**Status:** Accepted
**Date:** 20 October 2026
**Deciders:** Founder (Leo Noel Zuze AkA jayzthe_plug)

## Context
Docker Desktop will not start on the development laptop. Windows features and BIOS virtualization were enabled and the hypervisor is present, but the vmcompute and hns services do not exist, so WSL2 cannot start. A DISM repair did not fix it.
 
## Decision
Do not depend on local Docker. Use a hosted Supabase dev project (real PostgreSQL with pgvector, Mumbai) for development and run Flyway migrations against it. Run Testcontainers-based integration tests in GitHub Actions. Keep infrastructure/docker-compose.yml in the repo for later.

## Options Considered

### Option A: Hosted dev database plus CI for Docker-dependent tests

Unblocks work now; slower feedback for integration tests.

### Option B: Keep debugging Docker, or use a cloud workspace such as GitHub Codespaces (check the organisation's plan before relying on it)

Time-consuming or plan-dependent.

## Consequences
- Harder: no offline development; integration tests give results only after a push.
- Revisit: if Docker becomes available (for example after a Windows repair or reinstall), restore the local compose workflow.
- Care: the dev database is shared by all work; use synthetic data only.
