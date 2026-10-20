# Bevoya colours

Version 1 · 20 October 2026
Sources: `bevoya_brand_starter_guide.md`, `bevoya_design_product_security.md`, and the **Brand Identity Board** image you uploaded on 20 October 2026 (adds Forest Teal and Warm Orange).
Anything marked **Proposed** is not in those files and needs your approval and an on-device check.

## 1. Brand palette (from the brand guide)

| Role | Name | Hex | Use |
|---|---|---|---|
| Primary | Deep Forest | `#244C37` | Primary buttons, key icons, headings, selected navigation |
| Primary light | Soft Sage | `#DDE8D8` | Icon backgrounds, selected cards, gentle highlights |
| Background | Warm Ivory | `#F8F7F2` | Main app background |
| Surface | Pure White | `#FFFFFF` | Cards, sheets, input fields |
| Text | Deep Charcoal | `#202923` | Body text, important labels |
| Secondary text | Muted Slate | `#68746B` | Supporting text and descriptions |
| Accent | Soft Amber | `#F2C98D` | Milestones, celebrations, small highlights |
| Secondary accent | Mist Blue | `#B9DDE8` | Learning insights, informational visuals |

**Usage balance** (a starting point, not a hard rule): 60% ivory and white surfaces, 25% forest and charcoal, 10% sage, 5% amber or blue. Use forest for important actions only, never for every element. Keep bright accents rare so the app feels calm.

## 2. Additions from the brand identity board (20 October 2026)

Your brand identity board shows eight swatches: the six original colours plus two new ones. I used the **printed hex values** as the source. The swatches in the image are shaded (sampled pixels differ by a few units from the printed hex, for example Deep Forest samples as `#2A503A` against the printed `#244C37`), so never pick colours from the image itself.

| Name | Hex | Board meaning | Status |
|---|---|---|---|
| Forest Teal | `#2E7D6B` | Trust, stability, growth | New. Needs the usage rules below |
| Warm Orange | `#F79F4D` | Motivation, creativity, action | New. Needs the usage rules below |

The board does not list **Pure White** (`#FFFFFF`) or **Muted Slate** (`#68746B`). They stay in the palette because the UI needs a surface colour and a secondary text colour.

**Forest Teal rules (from the contrast numbers)**

- White text on Teal passes (4.93). **Charcoal text on Teal fails (3.04)**, so Teal fills take white text only.
- Teal text on Ivory or White passes (4.59 and 4.93), but only just on Ivory. Teal text on Sage fails (3.90).
- Teal next to Deep Forest is only 1.97:1, so the two are hard to tell apart. Do not put them side by side without a gap or an outline, and never use the difference between them to carry meaning.
- Proposed roles: second chart series, the "Productivity" category colour, secondary links and informational accents. Not for the primary action (that stays Deep Forest).

**Warm Orange rules**

- Charcoal text on Orange passes (7.14) and Deep Forest text passes (4.63). **White text on Orange fails (2.10).**
- Orange as text or as an icon on Ivory or White fails (1.95). Use it as a **fill only**, with Charcoal or Forest text on it.
- It is close in hue to Soft Amber. Keep roles apart: Amber is for milestones and achievements, Orange is for motivation and action highlights. Never use both in the same component.
- Never use Orange for status, warnings or errors.
- Use it rarely (a small badge, one highlight per screen at most) so the app stays calm. The primary button stays Deep Forest.

**Board vocabulary worth keeping** (also used in `VOICE_AND_TONE.md`): Forest = Growth, Balance, Trust. Sage = Calm, Fresh, Clarity. Ivory = Clean, Simple, Peace. Amber = Progress, Energy, Success. Charcoal = Readability, Focus, Strength. Teal = Trust, Stability, Growth. Mist = Learning, Insights, Calm. Orange = Motivation, Creativity, Action.

## 3. Contrast (verified)

Method: WCAG 2.x relative-luminance formula, calculated on 20 October 2026. Thresholds: **4.5:1** for normal text, **3:1** for large text (18 sp or larger, or 14 sp bold) and for UI components and icons that carry meaning.

| Text on background | Ratio | Result |
|---|---:|---|
| White on Forest | 9.70 | Pass |
| Forest on Sage | 7.68 | Pass |
| Forest on Ivory | 9.05 | Pass |
| Forest on White | 9.70 | Pass |
| Forest on Amber | 6.25 | Pass |
| Forest on Mist | 6.73 | Pass |
| Charcoal on Ivory | 13.96 | Pass |
| Charcoal on White | 14.97 | Pass |
| Charcoal on Sage | 11.84 | Pass |
| Charcoal on Amber | 9.63 | Pass |
| Charcoal on Mist | 10.39 | Pass |
| Slate on White | 4.88 | Pass |
| Slate on Ivory | 4.55 | Pass, barely |
| Slate on Sage | 3.86 | **Fails for normal text** |
| Slate on Mist | 3.39 | **Fails for normal text** |
| Slate on Amber | 3.14 | **Fails for normal text** |
| Charcoal on Forest | 1.54 | **Fails** |
| White on Amber | 1.55 | **Fails** |
| White on Teal | 4.93 | Pass |
| Teal on White | 4.93 | Pass |
| Teal on Ivory | 4.59 | Pass, barely |
| Teal on Sage | 3.90 | **Fails for normal text** |
| Charcoal on Teal | 3.04 | **Fails** |
| Charcoal on Orange | 7.14 | Pass |
| Forest on Orange | 4.63 | Pass |
| White on Orange | 2.10 | **Fails** |
| Orange on Ivory (icon or text) | 1.95 | **Fails** |
| Teal next to Forest | 1.97 | Cannot be told apart reliably |

### Rules that follow from the numbers

1. **Muted Slate is allowed only on Ivory and White.** On Ivory it passes by a hair, so never lighten it and do not use it for text under 14 sp. On Sage, Mist or Amber use Charcoal instead. (Your brand guide lists Slate for "secondary information" without a background, so this rule closes that gap.)
2. **White text only on Forest** (or on Slate for a neutral button). Never on Amber, Sage or Mist.
3. **Charcoal text never goes on Forest.** Use White.
4. **A Sage fill cannot signal "selected" on its own.** Sage against Ivory is only 1.18:1. A selected card or tab also needs a 2 dp Forest border or a check icon, plus a text or accessibility state. This matches your rule that colour is never the only signal.
5. **Focus and selection outlines use Forest** (9.05:1 on Ivory).

## 4. Semantic tokens, light theme

Components use these names, never raw hex values.

| Token | Value | Notes |
|---|---|---|
| `background` | `#F8F7F2` | Page |
| `surface` | `#FFFFFF` | Cards, sheets, inputs |
| `surfaceSelected` | `#DDE8D8` | Always paired with a border or icon (rule 4) |
| `textPrimary` | `#202923` | |
| `textSecondary` | `#68746B` | Only on `background` and `surface` (rule 1) |
| `primary` | `#244C37` | |
| `onPrimary` | `#FFFFFF` | |
| `accentMilestone` | `#F2C98D` | Text on it: `textPrimary` |
| `accentLearning` | `#B9DDE8` | Text on it: `textPrimary` |
| `secondary` | `#2E7D6B` | **From the board.** Text on it: `onPrimary` (white) only |
| `accentAction` | `#F79F4D` | **From the board.** Fill only; text on it: `textPrimary` |
| `inputBorder` | `#68746B` | 4.88:1 on White. UI boundaries need 3:1 |
| `cardBorder` | charcoal at 8% opacity | Decorative; or use a soft shadow instead |

### Status colours (Proposed, not in your files)

Your files require status to use colour plus icon or label, but define no status colours. These are proposals, each checked for contrast. They must always appear with an icon and a text label.

| Token | Hex | On Ivory | On White |
|---|---|---:|---:|
| `error` | `#B3261E` | 6.09 | 6.54 |
| `success` | `#2E6B3F` | 5.95 | 6.38 |
| `warningText` | `#7A4B00` | 6.91 | 7.41 |
| `info` | `#1F5F7A` | 6.58 | 7.06 |

Warning fills use Soft Amber with Charcoal text (9.63:1). White on `error` is 6.54:1.

## 5. Dark theme (Proposed)

Your files give a direction only: deep charcoal or forest-black, elevated dark surfaces, softened green accents, high-contrast text, and not pure black everywhere. These values are my proposal, with contrast calculated the same way. Approve or change them, then check on a real device.

| Token | Hex | Contrast |
|---|---|---|
| `background` | `#121A15` | |
| `surface` | `#1B2620` | |
| `surfaceElevated` | `#24322A` | |
| `textPrimary` | `#F1F3EE` | 15.87 on background, 13.98 on surface, 12.01 on elevated |
| `textSecondary` | `#A9B5AC` | 8.35 on background, 7.36 on surface, 6.32 on elevated |
| `primary` | `#8FCBA4` | 9.50 on background, 8.36 on surface |
| `onPrimary` | `#0F1A14` | 9.55 on `primary` |
| `surfaceSelected` | `#2C4034` | text `#CFE8D8` gives 8.56 |
| `inputBorder` | `#6F8577` | 3.94 on surface, 4.47 on background |
| `error` | `#F2A29A` | 7.74 on surface |
| `accentMilestone` | `#F2C98D` (unchanged) | Charcoal text 9.63 |
| `accentLearning` | `#B9DDE8` (unchanged) | Charcoal text 10.39 |

Forest Teal is only 3.6:1 on the dark background, so it is not used as text in dark mode (use dark `primary`). Warm Orange is 8.46:1 on the dark background and works as a fill or accent there.

The earlier idea of a subtle `#3A4A40` border fails the 3:1 rule for input outlines (1.66 to 1.89), so inputs use `#6F8577`. Subtle borders are fine only on decorative card edges.

## 6. Implementation

- `mobile/src/theme/colors.ts` holds the eight brand constants (already in the repo).
- Add `lightTheme` and `darkTheme` objects with the semantic names above, plus a `useTheme()` hook, in the `feature/mobile-design-system` branch.
- Follow the system setting by default, with a manual override in Settings, as your master prompt specifies.
- No hard-coded hex values in screens or components. Add a lint rule or a grep check in CI for `#[0-9A-Fa-f]{6}` outside `src/theme/`.
- **Logo (from the board, replacing the earlier arrow concept):** the mark is a small circle (the person) above two upward leaves, in Deep Forest. The primary version sits on a Soft Sage rounded-square tile. The reverse version is a Sage or Ivory mark on a Deep Forest tile. There is no arrow swoosh in the board's logo, which also suits small launcher-icon sizes.
- The board offers app-icon colour options (Sage, Forest, Mist, a light beige, Orange). Use Sage or Forest for the Android icon. The others are for marketing variety only.

## 7. Open items

1. Approve or change the **Proposed** status and dark values, and the roles for Forest Teal and Warm Orange.
2. Check all pairs on a real phone, outdoors and at low brightness.
3. Run a colour-blindness simulation on progress rings and status pills.
4. Redo the contrast table whenever any hex value changes.
