# Bevoya threat model (starter)

Version 1 · 20 October 2026
Sources: security sections of the design and technology files, and the architecture blueprint. Review each feature for 30 minutes before beta and add to this file.

## 1. Assets

Reflections and journals (highest), goals and evidence, account credentials, AI-provider and service keys, billing entitlements, embeddings (derived from private text).

## 2. Actors

Another signed-in user; an anonymous internet attacker; a malicious or careless insider; a compromised dependency; the AI model itself (untrusted output).

## 3. Threats and controls

| Threat | Control | Evidence |
|---|---|---|
| Reading or editing another user's data | User id only from the verified JWT; owner-scoped repositories; composite foreign keys; `404` for foreign ids | Cross-user test on every endpoint |
| Forged or misconfigured tokens | JWKS validation: signature, issuer, audience, expiry, pinned algorithm | Token security tests |
| Secrets in the APK or repo | Only public values in `EXPO_PUBLIC_*`; service-role key only in Java; `.env.local` ignored; secret scan in CI | CI scan; APK string check |
| Supabase Data API exposing tables | Data API off; RLS on every table; no policies for `anon` or `authenticated` | Schema test |
| Public AI service used as a back door | Internal token on every route, constant-time compare, rotation | Contract test |
| Prompt injection, data exfiltration | No tools or SQL for the model; delimited user text; schema-validated output; Java re-validates | AI safety fixtures |
| SSRF, malicious uploads | No server-side URL fetching or uploads in the MVP; links are stored, never fetched | Design constraint |
| Abuse and cost attacks | Per-user rate limit and daily AI quota; global monthly limit; input size caps | Quota tests |
| Private text leaking via logs, Sentry, analytics, notifications | No request bodies in logs; Sentry scrubbing; typed analytics allow-list; generic notification text | Manual log review before beta |
| Data on a lost or backed-up phone | `allowBackup` false; tokens encrypted; offline journals opt-in; logout wipes caches | Device check |
| Supply chain | Lockfiles, Dependabot, pinned CI actions, least-privilege workflow permissions | CI config |
| Brute force, spam sign-ups | Supabase Auth rate limits, email confirmation, custom SMTP with SPF and DKIM | Settings review |
| Insider access | No admin endpoints; no tool to browse reflections; audit events | Design constraint |
| Weak backups | Verified backups and one restore drill | Drill record |

## 4. Rules that must never break

Never trust a client-supplied user id. Never expose provider or service-role keys to the app. Never let AI output become business state without validation. Never log journals, prompts or tokens. Never give the AI unrestricted SQL or URL fetching.

## 5. Open items

Pre-beta restore drill; first full threat-modelling pass per feature; penetration-style review of the deletion and export flows; incident response contact (see `SECURITY.md`).
