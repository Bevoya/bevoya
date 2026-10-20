# Bevoya components

Version 1 · 20 October 2026 · Format follows the design-system skill (description, variants, states, accessibility, do and don't).
Sources: `bevoya_design_product_security.md` (components, AI components), `bevoya_master_prompt.md` (states), `bevoya_brand_starter_guide.md`.
All components live in `mobile/src/components/ui/`, read colours from `COLORS.md` tokens, text styles from `TYPOGRAPHY.md`, and spacing from `DESIGN.md`. No raw hex values, no raw font sizes.

**Build order** (matches the first sprint): Button, Card, TextField, Chip, EmptyState and ErrorState, ProgressRing, AiInsightCard, then the rest.

**Standard states for interactive components:** default, pressed, focused, disabled, loading, error. Android has no hover state; use pressed and focused.

## 1. Button

| Variant | Use when | Look |
|---|---|---|
| `primary` | The one main action on a screen | Forest fill, White text, radius 14 to 16 |
| `secondary` | Supporting action (for example "Add task") | White or Sage fill, Forest text and border |
| `tertiary` | Low emphasis (for example "Skip") | No fill, Forest text |
| `destructive` | Delete account, disconnect integration, delete memory | `error` colour with icon and clear label, always behind a confirmation |

Props: `variant`, `label` (required), `onPress`, `icon?`, `loading?`, `disabled?`, `fullWidth?`.
States: pressed (slightly darker or ripple), focused (2 dp Forest outline), disabled (reduced contrast but label still readable, with `accessibilityState.disabled`), loading (spinner replaces the icon, label stays, presses ignored).
Accessibility: `accessibilityRole="button"`, label equals the visible text, 48 dp tall minimum (44 floor).
Do: one `primary` per screen. Don't: use colour alone to mean "danger"; write "Delete" with an icon and a confirmation.

## 2. Card

Container for goals, tasks, skills, sessions, reflections, AI recommendations, weekly reviews, integrations.
Look: White surface, radius 16 to 24, padding 12 to 20, subtle shadow or hairline border. A clear title, body and action hierarchy.
Variants: `default`, `selected` (Sage fill plus 2 dp Forest border and a check icon, never fill alone), `insight` (Mist, learning content), `milestone` (Amber).
Accessibility: if the whole card is tappable, it is one accessible element with a combined label; inner buttons are separate only when needed.
Do: keep to one idea per card. Don't: nest cards inside cards.

## 3. TextField

Props: `label` (always visible), `value`, `onChangeText`, `helperText?`, `errorText?`, `secureTextEntry?`, `multiline?`, `maxLength?`.
States: default, focused (Forest outline), filled, error (error colour **plus** an icon and the error text), disabled.
Rules: placeholder is never the only label; border meets 3:1 (`inputBorder`); errors say what is wrong and how to fix it; connect the error text to the field for screen readers. Journal text areas grow with content and never hide the Save button.
Validation uses React Hook Form with Zod, the same schema family as the API.

## 4. Chip

| Type | Use |
|---|---|
| `SourceChip` | Shows where an AI insight came from ("Your goal", "Recent sessions", "Quiz results", "Notes"). Tappable to see the source |
| `StatusChip` | Task or goal status. **Always icon plus text**, never colour alone |
| `FilterChip` | Plan filters: Today, Upcoming, Overdue, Completed, All |

Touch target 48 dp even if the chip looks smaller.

## 5. Progress indicators

- `ProgressRing`: goal completion. Shows the number as text inside or beside it.
- `ProgressBar`: skill mastery (Discovered, Studying, Explained, Practiced, Built, Tested, Applied).
- `Timeline`: roadmaps and milestones as a path.
- `ConsistencyIndicator`: replaces a punishing "streak".
Accessibility: every indicator has a text equivalent ("Goal 60 percent complete, 3 of 5 milestones"). Charts need a text summary.
Don't: show red or negative states for missed days. Missed days appear as neutral gaps with supportive copy.

## 6. BottomSheet

For quick add (task, evidence, reflection prompts) and confirmations. Has a visible close button and supports Android Back. Content never depends on a gesture alone. Focus moves into the sheet when it opens and back to the trigger when it closes.

## 7. EmptyState

Pattern: a small illustration or icon, one sentence of what is missing, one primary action. Example from your master prompt: "No learning evidence yet. Start a session, add a note, or complete a quiz to begin building proof of progress."
Don't: blame the user or show a blank screen.

## 8. ErrorState and OfflineBanner

- `ErrorState`: says what happened in plain words, offers **Try again**, and never shows technical details or IDs unless the user opens "More details" (request ID only).
- `OfflineBanner`: a persistent, calm strip: "You're offline. Your changes are saved on this device and will sync later." Pair it with an icon.
Accessibility: banners use a polite live region so screen readers announce them once.

## 9. AiInsightCard (central to the product)

Your design file requires every AI recommendation to show all of these:

1. A clear **"AI insight"** label with an icon (so AI content is visually distinct).
2. A short explanation of **why** ("Based on your recent sessions and quiz results…").
3. **Source chips** for every input used.
4. A suggested action with estimated time and an optional resource type.
5. **Accept, Edit, Dismiss** actions.
6. A privacy note where relevant (for example "Uses your journal because AI memory is on").

Also required by your master prompt: separate **facts from your own data** from **AI interpretation**, and show uncertainty when information is incomplete. Footer text: "AI suggestions are guidance, not professional advice."

States: `proposed`, `accepted`, `edited`, `dismissed`, `loading` ("Reviewing your recent sessions, notes, and goal…"), `unavailable` ("AI is unavailable right now. You can still plan and write as usual."), `quota` ("You've used today's AI suggestions. They reset tomorrow.").
Accessibility: read as one block in the order label, explanation, sources, action; each button has a full label ("Accept: build a retrieval test").

## 10. TaskRow and GoalCard

- `TaskRow`: checkbox (48 dp target), title, category icon and label, estimated time, linked goal, "why it matters" on expand. Completion shows icon plus text.
- `GoalCard`: title, progress ring, next milestone, status chip.
Accessibility: checkbox announces "Task, not done" or "Task, done".

## 11. Toast and inline feedback

Short, non-blocking messages ("Saved"), with an undo for deletions. Use a polite live region. Do not use toasts for errors the user must act on; use inline text or an `ErrorState`.

## 12. Patterns

| Pattern | Rule |
|---|---|
| Forms | Labels above fields, one primary action, save progress for long forms |
| Loading | Skeletons for lists, a spinner only for short actions |
| Permission request | Explain why **before** the system prompt; offer "Not now" |
| Permission denied | Say what is unavailable and how to enable it later, without nagging |
| Confirmations | Destructive actions need a named confirmation ("Delete this reflection") |
| Free-plan limit | Neutral wording, shows what is included, no fake urgency or countdowns |

## 13. Do and don't (summary)

| Do | Don't |
|---|---|
| Pair colour with icon and label | Signal status by colour alone |
| Give every AI item sources and Accept/Edit/Dismiss | Present AI output as fact or auto-apply it |
| Use calm, specific, supportive copy | Use guilt, urgency or streak-shaming |
| Design all states first | Ship only the happy path |
| Use tokens | Hard-code hex values or font sizes |
