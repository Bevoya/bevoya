// Text styles from TYPOGRAPHY.md. Android takes weights from the loaded font name, not fontWeight.
export const fonts = {
  regular: 'Figtree_400Regular',
  medium: 'Figtree_500Medium',
  semibold: 'Figtree_600SemiBold',
  bold: 'Figtree_700Bold',
} as const;

export const typography = {
  display: { fontFamily: fonts.bold, fontSize: 30, lineHeight: 38 },
  h1: { fontFamily: fonts.bold, fontSize: 24, lineHeight: 32 },
  h2: { fontFamily: fonts.semibold, fontSize: 20, lineHeight: 28 },
  body: { fontFamily: fonts.regular, fontSize: 16, lineHeight: 24 },
  secondary: { fontFamily: fonts.regular, fontSize: 14, lineHeight: 20 },
  caption: { fontFamily: fonts.medium, fontSize: 12, lineHeight: 16 },
} as const;

export type TypographyVariant = keyof typeof typography;

// Usable up to 200% font scale (ACCESSIBILITY.md).
export const MAX_FONT_SCALE = 2;