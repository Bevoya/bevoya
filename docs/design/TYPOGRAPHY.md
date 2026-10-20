# Bevoya typography

Version 1 · 20 October 2026
Source: `bevoya_design_product_security.md` (type scale and rules) and `bevoya_brand_starter_guide.md` (clean, modern sans-serif; no thin lettering).
Values marked **Proposed** are my choices inside the ranges your files give.

## 1. Font

**Your direction:** a clear, friendly font that reads well on phones, close to the lettering on your brand identity board.

**What I can and cannot tell from the board.** The wordmark "Bevoya" is a heavy, rounded geometric sans-serif. The headings and the tagline look like a Google-Sans-style geometric font with an open, friendly shape. I cannot identify the exact font from an image, and the wordmark will be vector artwork anyway (see section 5). Google's own Google Sans is not a font I would assume you may ship inside an app, so I picked free, open-licence fonts that look similar.

**Proposed (decision for you):** **Figtree** for all app text.

- Free (SIL Open Font Licence) and available through Expo as `@expo-google-fonts/figtree`.
- Designed for interfaces: friendly geometric shapes, open letterforms and a generous x-height, which keeps small text clear on phone screens.
- Has true weights for 400, 500, 600 and 700, which fixes the "Roboto has no 600" problem.
- Matches the board's tone (rounded, calm, modern) better than the default Roboto.

**Two alternatives to test on your phone against Figtree:** DM Sans (very close to the board's heading style) and Plus Jakarta Sans (a bit more stylish; check small-text clarity). **Roboto** (the Android default) is the safe fallback and the one to use if you want zero extra app size.

**How to decide:** I will add a small font-test screen in the design-system branch. Look at the same paragraph, a caption and a button label in each font on your real phone, outdoors and indoors, then choose.

**Limits:** Figtree has no Devanagari (Hindi, Marathi). When those languages are added, pair it with Noto Sans Devanagari and check the weights match.

**Implementation note (important for React Native):** with a custom font, Android does not pick weights from `fontWeight`. Each weight is a separate loaded font name (for example `Figtree_400Regular`, `Figtree_500Medium`, `Figtree_600SemiBold`, `Figtree_700Bold`), so the type tokens use `fontFamily`, not `fontWeight` (see section 6).

## 2. Type scale

Text sizes are written in sp (React Native scales text with the user's font setting automatically; the numbers are the same). Layout sizes are dp.

| Token | Your range | Proposed size / line height | Weight | Use |
|---|---|---|---|---|
| `display` | 28–32 | 30 / 38 | 700 | Major screen titles |
| `h1` | 22–24 | 24 / 32 | 700 | Section titles |
| `h2` | 18–20 | 20 / 28 | 600 | Card titles |
| `body` | 15–16 | 16 / 24 | 400 | Main content |
| `secondary` | 13–14 | 14 / 20 | 400 | Supporting text |
| `caption` | 11–12 | 12 / 16 | 500 | Labels, timestamps, metadata |

Why these picks: 16 sp body is easier to read than 15 on a phone. A 12 sp caption is one step above your 11 sp floor.

## 3. Rules

1. **Never below 11 sp** (your rule). In practice, keep anything the user must read or act on at 12 sp or above.
2. Sentence case for almost all UI text. No ALL CAPS labels (poor for screen readers and dyslexic readers).
3. Bold sparingly, for hierarchy only.
4. Avoid long paragraphs on mobile. Break content into short blocks of about five lines or fewer.
5. Colour pairs must come from `COLORS.md`. Muted Slate text is only for `secondary` and `caption` on Ivory or White.
6. Dates, times and numbers in lists use tabular figures where the font supports it, so columns line up.
7. Never truncate dates, times, amounts or consent wording. Wrap them instead. Truncation with an ellipsis is allowed only for titles in dense lists, and the full text must be readable in the detail screen.

## 4. Font scaling and accessibility

- Leave `allowFontScaling` on everywhere. Set `maxFontSizeMultiplier` to **2.0** by default so the app is usable up to 200% (your accessibility target). Test at 200%.
- No fixed heights on anything that contains text. Use `minHeight` and padding so containers grow.
- Buttons and rows must grow with their text and keep the touch target (see `DESIGN.md`).
- In Senior-friendly mode (from your age and mode section), start from a larger base. **Proposed:** multiply the scale by 1.25 and show fewer choices per screen.
- Test layouts at widths 360 and 412 dp, at font scales 100%, 150% and 200%.

## 5. Wordmark

The board shows "Bevoya" in a heavy, rounded geometric sans-serif with the tagline below it. The wordmark and symbol are SVG artwork (not live text), to be finalised before launch. Your brand guide notes the logo is a concept, not a final vector or a verified trademark.

## 6. Implementation sketch

In `mobile/src/theme/typography.ts`:

```ts
// Install: npx expo install expo-font @expo-google-fonts/figtree
// Load the four weights once in the root layout before showing any screen.
export const fonts = {
  regular: 'Figtree_400Regular',
  medium: 'Figtree_500Medium',
  semibold: 'Figtree_600SemiBold',
  bold: 'Figtree_700Bold',
} as const;

export const typography = {
  display:   { fontFamily: fonts.bold,     fontSize: 30, lineHeight: 38 },
  h1:        { fontFamily: fonts.bold,     fontSize: 24, lineHeight: 32 },
  h2:        { fontFamily: fonts.semibold, fontSize: 20, lineHeight: 28 },
  body:      { fontFamily: fonts.regular,  fontSize: 16, lineHeight: 24 },
  secondary: { fontFamily: fonts.regular,  fontSize: 14, lineHeight: 20 },
  caption:   { fontFamily: fonts.medium,   fontSize: 12, lineHeight: 16 },
} as const;
```

A shared `<Text variant="body" />` component reads these tokens and the theme colours. No raw `fontSize` values in screens.

## 7. Open items

1. Confirm Figtree, or choose DM Sans, Plus Jakarta Sans or Roboto after the on-device font test.
2. Confirm the proposed sizes, or choose other values inside your ranges.
3. Check text clarity at 12 sp and 14 sp on a real phone.
4. Decide on a Devanagari font when Hindi or Marathi is added.
