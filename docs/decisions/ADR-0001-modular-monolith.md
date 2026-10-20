# ADR-0001: Modular monolith

**Status:** Accepted
**Date:** 20 October 2026
**Deciders:** Founder (Leo Noel Zuze AkA jayzthe_plug)

## Context
Bevoya is built by a very small team with limited weekly hours. The product needs one core backend and a Python AI service. Operating many services would cost time and money before there is a user.

## Decision
One modular Spring Boot application (package by feature), one small FastAPI AI service, one PostgreSQL database. Add Redis, RabbitMQ, Kafka, Kubernetes, a dedicated vector database or a Node.js backend only through a new ADR that names a measured trigger.

## Options Considered

### Option A: Modular monolith plus one AI service

Complexity low, cost low, easy to test, easy to split later along module boundaries.

### Option B: Microservices per feature

High complexity and operating cost; no benefit at this size.

## Consequences
- Easier: one deploy, one codebase to reason about, simple local development.
- Harder: module boundaries rely on discipline; enforce with an ArchUnit test.
- Revisit: when a module needs independent scaling or a separate team.
