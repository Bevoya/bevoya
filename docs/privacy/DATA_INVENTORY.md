# Bevoya data inventory

Version 1 · 20 October 2026
Sources: `bevoya_design_product_security.md` (privacy), `bevoya_final_launch_decisions.md` (retention, deletion), the architecture blueprint. Update this file whenever a field or a third party is added.

## 1. Principles

Collect only what a feature needs. Explain why before asking. Least-privilege access. Users control AI memory. Journals are separate from learning notes. Delete means delete. Export and deletion never depend on a paid plan. Never sell personal data. No training on private content without explicit consent. Minimise what goes to external AI providers.

## 2. Inventory

| Data | Sensitivity | Stored in | Sent to Gemini? | Deletion |
|---|---|---|---|---|
| Email, password hash | Personal | Supabase Auth only | No | Deleted with the auth user |
| Display name, user type, time zone, available time | Low | `profiles` | No | Cascade |
| Age confirmation (18+) and consents | Personal | `consent_records` | No | Cascade |
| Goals, milestones, tasks | Medium | Postgres | Only for roadmap and next-task features, minimal fields | Cascade, or per item |
| Sessions, evidence, learning notes | Medium | Postgres | Notes only when AI memory is on | Per item, with embeddings |
| Reflections, mood, energy | **High** | Postgres | Only on an explicit user action; retrieval only with journal consent | Immediate hard delete, with embeddings |
| Embeddings | Derived | `document_chunks` | The source text is sent once to be embedded | Deleted with the source, same transaction |
| Recommendations | Medium | `recommendations` | Output only | Cascade |
| Usage counters, audit events | Low | Postgres | No | Counters cascade; audit user id nulled |
| Push tokens | Low | `device_tokens` | No | Cascade and on logout |
| Screen-time data | High | Not in the MVP | No | n/a until built |

## 3. Third parties

| Party | Gets | Notes |
|---|---|---|
| Supabase (Mumbai) | Auth, database, storage | Region fixed at project creation |
| Render (Singapore) | Runs Java and Python; sees traffic in transit | No data at rest on Render |
| Google (Gemini API) | Minimal text per AI request | Free-tier terms differ from paid; see `AI_RULES.md` |
| Sentry | Crash and error reports | Scrubbed; no journal or note text |
| PostHog | Allow-listed events only | No free text, no session replay |
| Google Play | Purchases | Later |

## 4. Retention and deletion

- Active account: kept while the account is active.
- Deleted reflection or note: removed immediately from the primary database, with its embeddings.
- Account deletion: revoke sessions, delete the profile row (cascades to every owned table and all embeddings), delete Storage objects, delete integration tokens, record a deletion request containing only a hash, delete the auth user, confirm to the user.
- Backups: deleted data may remain in Supabase backups until they expire. Verify the backup period for your plan, then state it honestly in the privacy policy. Do not claim a period you have not verified.
- Audit logs: only as long as needed for security and legal reasons.

## 5. To verify before beta (not yet checked)

India's DPDP Act and Rules (notice, consent, grievance contact, breach handling, deletion, commencement dates); Gemini data-use terms for free versus paid use; Google Play Data safety form, account-deletion requirements and subscription disclosures; Supabase backup retention for your plan.
