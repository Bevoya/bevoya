# Bevoya design principles and layout

Version 1 · 20 October 2026
Source: `bevoya_design_product_security.md` (primary), `bevoya_master_prompt.md`, `bevoya_brand_starter_guide.md`, `bevoya_final_launch_decisions.md`.
Companion files: `COLORS.md`, `TYPOGRAPHY.md`, `COMPONENTS.md`, `ACCESSIBILITY.md`, `VOICE_AND_TONE.md`.

## 1. What the product should feel like

A calm, supportive, premium personal growth companion, not a noisy productivity dashboard. It never shames users for missed days. Brand personality: supportive, calm, clear, motivating, private, practical, evidence-aware.

Design principles, in priority order:

1. **The user stays in control.** AI proposes, the user accepts, edits or dismisses.
2. **Evidence over time spent.** Show what the user did (built, practised, tested), not only how long.
3. **Calm over clever.** Few elements, generous spacing, gentle motion.
4. **Privacy is visible.** Controls and explanations are easy to find, in plain language.
5. **Never shame.** Progress indicators encourage reflection; they do not punish missed days.
6. **Accessible by default.** Not a later pass.

## 2. Frame and layout

- Android-first. Your files use a **390 × 844** phone frame as the design artboard. Treat it as the reference frame, not a fixed width: real Android phones are mostly 360 to 412 dp wide, so layouts must be fluid. Test at 360 and 412.
- Bottom tab navigation, five tabs: **Home | Plan | Learn | Journal | Profile**.
- Progress and deep analytics are **not** a tab. They are a stack route opened from Home and Profile, as your design file requires ("deep analytics in Progress, not Home").
- Support Android edge-to-edge and system insets (status bar, gesture bar). Android Back must behave predictably: back from a tab goes to Home, back from Home exits.
- Support portrait first. Landscape is not required for the MVP.

## 3. Spacing, shape, depth

| Token | Value | Notes |
|---|---|---|
| Grid | 4 dp base | |
| Spacing scale | 4, 8, 12, 16, 20, 24, 32, 40 | `mobile/src/theme/spacing.ts` |
| Screen padding | 16 to 20 | |
| Card radius | 16 to 24 | |
| Card padding | 12 to 20 | |
| Button radius | 14 to 16 | |
| Chip radius | fully rounded | |
| Depth | A subtle shadow or a hairline border; never both heavy | |

**Touch targets.** Your minimum is **44 × 44**. Android's own guideline is **48 × 48 dp**, so use 48 where space allows and treat 44 as the absolute floor. Use `hitSlop` to enlarge small icons without enlarging their artwork.

## 4. Screen rules (from your design file)

- **One primary action per screen.**
- **At most five major content blocks per screen.**
- Progressive disclosure for advanced settings.
- Icons always come with labels (your master prompt).
- Icon set: Lucide (already in your stack list).
- Use progress rings for goal completion, bars for skill mastery, timelines for roadmaps, charts for patterns. Use them only where they aid understanding.

## 5. Home screen: resolving a conflict in your files

Your design file says "at most five blocks" and "avoid dashboard clutter". Your master prompt lists about ten things for Home (greeting, goal, ring, top three actions, timer, streak, recent evidence, AI recommendation, quick actions, tabs). Both cannot hold, so this is the proposed grouping:

1. **Header block:** greeting, current primary goal, weekly progress ring, consistency indicator (neutral wording, see `VOICE_AND_TONE.md`).
2. **Today:** the top three actions, each tappable.
3. **Focus timer card:** start or resume a session.
4. **One AI insight card.**
5. **Recent evidence:** the last two or three items.

Quick actions (add task, write reflection, take quiz, review week) live in one compact row inside block 2 or a single add button, not as a sixth block. Everything else (history, analytics, streak detail) is under Progress.

## 6. Core screens and their jobs

| Area | One-line job | Primary action |
|---|---|---|
| Home | What matters today | Start session |
| Plan | Choose and manage today's tasks | Add task |
| Learn | Turn information into ability: notes, skills, quizzes | Add note |
| Journal | Private daily reflection | Save reflection |
| Profile | Settings, privacy centre, subscription | (none; settings list) |
| Progress (stack) | Weekly and monthly patterns | Open weekly review |

## 7. Motion

- Calm and short: about 150 to 250 ms, ease-out. No bouncing, no celebratory confetti, no fake urgency.
- Respect the system "remove animations" setting.
- Milestones may use a gentle Soft Amber highlight, nothing flashing.

## 8. Mode variants

| Mode | Changes |
|---|---|
| Adult (launch) | Full feature set |
| Professional | Work, client, content and career emphasis in defaults and copy |
| Senior-friendly | Larger text, fewer choices per screen, plain language, clearer reminders |
| Teen (later, not at launch) | Simpler language, strong privacy, no adult features. Needs legal review first |

Launch is **18+ only** (see ADR-0005). Teen mode is documented here only so the design does not block it later.

## 9. States every screen must design

Empty, loading, success, error, offline, permission-request, permission-denied, AI thinking, AI unavailable, free-plan limit, privacy-disabled. Copy for these lives in `VOICE_AND_TONE.md`; component patterns in `COMPONENTS.md`.

## 10. Logo and icon

From the Brand Identity Board (20 October 2026):

- **Mark:** a small circle (the person) above two upward leaves, rounded shapes. Primary version: Deep Forest mark on a Soft Sage rounded-square tile. Reverse version: Sage or Ivory mark on a Deep Forest tile. There is no arrow in this version.
- **Wordmark:** "Bevoya" in a heavy, rounded sans-serif, with the tagline "Your life, moving forward." underneath on the full lockup.
- **Variations shown:** full horizontal lockup, symbol only, reverse (on Forest), compact lockup.
- **Small sizes:** the board shows launcher, favicon and tiny-icon options. For the Android launcher icon use the symbol on the Sage or Forest tile; test it at 48 dp.
- **Feature icons on the board** (Goals and Plans, Learning, Productivity, Reflection, Insights, AI Coach) are a starting set of illustrations; they will need to be redrawn as clean vectors, with labels (icons never stand alone).
- **Splash:** the board and the earlier mockup show the lockup over soft layered green hills with a pale sun and a winding path. It fits "your life, moving forward". Treat it as a mood reference; it is a launch-polish item, not an MVP blocker.
- The logo is a concept, not a production vector and not a trademark check. Both are launch tasks.

## 11. Open items

1. Approve the Home grouping in section 5.
2. Approve 48 dp as the preferred touch target.
3. Decide whether landscape is ever supported.
4. Produce the final vector logo, launcher icon and feature icons.
5. You said you like the phone screens in the board and the earlier mockup. Pick which screens to build first when we discuss visuals; this file covers the rules, not the individual screen layouts.
