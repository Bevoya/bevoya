# Bevoya accessibility

Version 1 · 20 October 2026
Source: `bevoya_final_launch_decisions.md` (minimum standards), `bevoya_design_product_security.md`, `bevoya_master_prompt.md`.

## 1. Standard

- Target **WCAG 2.2 AA** "where practical" (your launch decision). Treat it as the default, and record any exception with a reason in the PR.
- Primary flows must work with **TalkBack** (Android's screen reader), at **200% font size**, and with **remove animations** on.

## 2. Requirements

| Area | Requirement |
|---|---|
| Contrast | Text 4.5:1, large text and meaningful UI 3:1. Use only the verified pairs in `COLORS.md` |
| Touch targets | 44 × 44 dp minimum, 48 dp preferred, with `hitSlop` for small icons |
| Font scaling | Support Android font scaling up to 200%; no fixed heights around text; nothing clipped |
| Status | Never colour alone. Always icon plus text, or a text state |
| Labels | Every interactive element has an accessible name and role. Icon-only buttons have `accessibilityLabel` |
| Focus order | Logical, top to bottom, matching the visual order. Sheets trap focus and return it on close |
| Forms | Visible labels (not placeholder only); errors are text, linked to the field, and announced |
| Motion | Respect the system setting; no flashing; no essential information in animation alone |
| Timer | Do not announce every second. Announce start, pause, end, and on request. A running timer survives backgrounding (it is derived from a stored start time) |
| Charts and rings | Provide a text summary ("Weekly focus: 4 hours across 5 days") |
| Notifications | Plain, calm text; no private content on the lock screen |
| Time and language | Use plain language; avoid jargon; short sentences |
| Back navigation | Android Back works everywhere and never traps the user |
| Zoom and orientation | No locking that prevents users from scaling text; portrait is the design baseline |

## 3. Senior-friendly mode

Larger base text (proposed ×1.25), fewer choices per screen, plain language, clearer reminders, simpler navigation, and strong privacy controls. It is a calmer, clearer interface mode, not a reduced-capability one.

## 4. Testing

1. **Per PR (developer):** run the screen with TalkBack and swipe through every element; set font scale to 200% and check nothing is clipped; check contrast of any new colour pair.
2. **Automated:** React Native Testing Library queries by role and label (a missing label makes the test fail); the lint rule for hard-coded colours.
3. **Per release:** a TalkBack pass through onboarding, create goal, plan a task, run a session, write a reflection, change privacy settings, delete a reflection.
4. **Before private beta:** test with at least two people who use accessibility settings, if available, and at the smallest supported width (360 dp).

## 5. Per-feature checklist (copy into the PR)

- [ ] Every element has a name, role and state, and TalkBack reads it sensibly.
- [ ] 200% font scale: no clipping, buttons grow, primary action still reachable.
- [ ] Contrast checked for new pairs; status uses icon plus text.
- [ ] Touch targets at least 44 dp (48 preferred).
- [ ] Loading, empty, error and offline states are announced or visible.
- [ ] Forms: labels, error text and focus handling work.
- [ ] Reduce-motion respected.
- [ ] No information conveyed only by colour, sound or animation.

## 6. Known gaps and decisions

1. Hindi and Marathi are out of scope until after the English MVP; plan font and text-direction checks then.
2. Voice input (journal) needs captions or a text alternative and clear permission copy.
3. This file is a standard, not proof of compliance. Real assistive-technology testing is still required.
