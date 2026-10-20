# Bevoya product definition

Version 1 · 20 October 2026
Sources: `bevoya_design_product_security.md`, `bevoya_claude_project_brief.md`, `bevoya_final_launch_decisions.md`, `bevoya_master_prompt.md`.

## 1. What Bevoya is

An Android-first, privacy-first personal growth and life-management app. It helps people turn long-term goals into daily actions, learn effectively, record real evidence of progress, reflect honestly, understand habits and receive practical AI-guided next steps.

**Positioning:** "A private AI companion that helps you plan, learn, reflect, and grow, with your goals and your data under your control."

**Core promise:** help people make progress through daily action, honest reflection and evidence, while keeping control of their data and decisions.

**Bevoya is not** a simple to-do app, a habit tracker, a journaling app, an unrestricted chatbot, a life-decision automation system, or a medical, legal, financial or psychological advice service.

## 2. Core loop

Goal, Roadmap, Daily plan, Session, Evidence, Reflection, Review, Next action, repeat. Every feature must support this loop. If a feature does not, it waits.

## 3. Launch decisions

| Topic | Decision |
|---|---|
| First market | India, starting in Pune |
| First language | English (strings externalised from day one) |
| First platform | Android |
| Age | **18+ at launch**, self-declared and recorded in consent records. Teen mode only after legal review |
| Pricing | Freemium (Free and Pro). Billing is off in the private beta |

## 4. Users

| Group | Main needs | Example next action |
|---|---|---|
| Students | Study plans, exams, revision, quizzes, weak topics, healthy routines | "Review integration mistakes for 25 minutes, then redo five similar questions tomorrow." |
| Young professionals | Career goals, skill roadmaps, portfolio, interview prep | "Practise two behavioural interview answers tomorrow." |
| Working adults | Work and personal tasks, deep work, balance, reflection | "Schedule your personal-project session earlier tomorrow." |
| Creators and freelancers | Content planning, consistency, client work | "Turn your recent project lesson into a short tutorial." (never promise reach) |
| Parents and older adults | Simple routines, larger text, plain language, privacy | Simpler interface mode |

## 5. MVP scope (build in this order)

1. Authentication and onboarding
2. Profile and preferences
3. Goal creation
4. Roadmap generation and editing
5. Daily task planning
6. Focus-session tracking
7. Learning notes
8. Evidence recording
9. Journal and reflection
10. AI-generated quizzes
11. Daily review
12. Weekly review
13. Basic analytics
14. Push reminders
15. Privacy settings
16. Free and Pro subscription

**Not at the start:** dating or romantic coaching, autonomous life decisions, financial advice, medical advice, social-media automation, multi-agent automation, children's mode, full team collaboration.

## 6. Plans

**Free:** one active goal, daily planning, basic session tracking, manual journal, limited AI recommendations per day, basic weekly review, basic reminders.
**Pro:** unlimited goals and roadmaps, full AI coach, personal knowledge search, advanced quizzes and spaced repetition, deeper analytics, screen-time insights, calendar and task integrations, GitHub integration, custom AI modes, priority processing, export and backup.
**Education or team (later):** study groups, classroom mode, mentor dashboards, shared goals.

Rules: price, billing period, renewal date, trial terms and cancellation shown clearly; no dark patterns. Account deletion, privacy controls and data export never depend on Pro.

## 7. Exclusions (non-negotiable)

No dating or romantic features; no relationship coaching; no autonomous life decisions; no trading or investment advice; no medical or mental-health diagnosis or therapy claims; no manipulative engagement; no dark-pattern subscriptions; no guilt-based notifications; no fake urgency; no social-media posting without approval; no guarantee of grades, jobs, followers, income or success.

## 8. Success measures

- 30 to 40% of weekly active users return the following week and complete at least one planned task.
- 20 to 30% of active users use daily reflection or weekly review.
- Also track: tasks planned and completed, sessions started and completed, evidence created, AI recommendations accepted, free-to-Pro conversion, churn, notification opt-out rate.
- Warning sign from your files: if users only open the app to check tasks, the product is not yet differentiated.

## 9. Open items

1. Confirm the Free-plan AI limit (proposed 5 per day) and Pro (proposed 50 per day) after measuring real cost.
2. Legal review of the 18+ gate and of data handling before beta.
3. Trademark, domain and handle check for "Bevoya".
