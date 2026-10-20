# Bevoya AI rules

Version 1 · 20 October 2026
Sources: `bevoya_design_product_security.md` (AI system rules), `bevoya_service_portability_llm_abstraction.md`, `bevoya_technology_stack.md`, `bevoya_final_launch_decisions.md`. Model names verified against Google's Gemini API pages on 20 October 2026.

## 1. What AI may do

Convert goals into roadmaps; turn journal entries into structured reflections; recommend daily tasks and learning topics; generate quizzes; summarise weekly progress; detect repeated blockers; suggest learning resources and career or creator actions; help compare decisions.

## 2. What AI must never do

Make life decisions; diagnose medical or mental-health conditions; give investment, legal or therapy advice; guarantee exam results, jobs, followers, income or success; send messages without approval; access data without permission; train on private user content without explicit consent.

## 3. Recommendation format (every recommendation)

Title, reason, evidence sources (source chips), suggested action, estimated time, optional resource type, and Accept, Edit, Dismiss. Labelled "AI insight", with a privacy note where relevant. Facts from the user's own data are shown separately from AI interpretation and from general or external suggestions.

## 4. Resource recommendations

Suggest resource **types or platforms** (YouTube, Coursera, Khan Academy, official documentation, GitHub, MDN, freeCodeCamp, university notes, past papers) with the reason. Prefer free or accessible resources. Never enrol anyone in a paid course, never invent a course, never claim a resource guarantees a result, never scrape restricted sites. Let users save, dismiss or replace suggestions. Label unverified suggestions as unverified.

## 5. Models

Verified model IDs (Gemini API, 20 October 2026):

| Task | Tier | Model ID |
|---|---|---|
| Reflection extraction, next-task suggestion, tagging | Fast | `gemini-3.5-flash-lite` |
| Quizzes, roadmaps, weekly review, career-gap analysis | Standard and strong | `gemini-3.8-flash` |

Notes from Google's pages: 3.8 Flash is the newest Flash model; Google recommends 3.5 Flash-Lite or 3.8 Flash for new projects; 3.5 Flash-Lite is listed at $0.30 per million input tokens and $2.50 per million output tokens (check the pricing page for 3.8 Flash and for any change). Gemini Flash models from 3.6 onward do not support assistant prefill. Google also warns that newer model generations may reject some request parameters, so the Gemini adapter must not hard-code sampling parameters; read the migration notes before adding any.

Model IDs live in environment variables (`GEMINI_FAST_MODEL`, `GEMINI_TEXT_MODEL`, `GEMINI_STRONG_MODEL`), never in business code, behind the `LlmGateway` interface so providers can be swapped.

**Embeddings:** your launch file chose `gemini-embedding-001 ` `moved already n using 2`. Google now lists `gemini-embedding-2` as stable (April 2026), with adjustable output size (128 to 3072, 768 recommended). Decide **before storing any vectors**, because vectors from different models cannot be mixed. See ADR-0004. Whichever you pick: request 768 dimensions, normalise vectors yourself unless the docs for that model say they are normalised, and store model name and dimensions on every row.

## 6. Cost and abuse controls

- Per-user daily quota enforced in Java (proposed: Free 5, Pro 50). Global monthly token limit as a kill-switch. Per-request ceilings on input and output tokens, bounded retrieval, one schema retry only.
- Python returns token usage on every response so Java can meter cost.
- Fast model for simple tasks; stronger model only where needed. Cache weekly reviews unless inputs change.

## 7. Safety and privacy controls

- Gemini free-tier privacy: as understood from Google's terms, free-tier content may be used to improve Google's products. **Use only synthetic or your own data until billing is enabled on the Gemini project**, and re-check Google's current terms before any real user data.
- Send the minimum context. No emails, names or ids that are not needed. Never log prompts or responses.
- User text goes in a delimited data block, never in system instructions. **The model has no tools** in the MVP: no SQL, no URL fetching.
- Validate every output against a schema in Python, then business rules in Java. Treat model output as untrusted.
- AI memory and journal retrieval default to **off**. Each requires an explicit consent record. Retrieval filters by user id and consent scope every time. Deleting a source deletes its embeddings.
- Moderation and safety checks for sensitive or public-facing content. Distress signals trigger supportive, non-diagnostic copy (see `VOICE_AND_TONE.md`).

## 8. Quality

Keep a synthetic evaluation set. Before changing a prompt or model, run the eval and compare. Required test cases: malformed output, provider timeout, prompt-injection text, irrelevant retrieval, unsafe advice requests, and "no evidence available" (the AI must say so).
