// Single source of truth for brand colours. Do not hard-code hex values elsewhere.
// Contrast: slate on ivory is about 4.5:1 (never go lighter). Slate on sage fails AA: use charcoal on sage.
export const colors = {
  forest: '#244C37',
  sage: '#DDE8D8',
  ivory: '#F8F7F2',
  white: '#FFFFFF',
  charcoal: '#202923',
  slate: '#68746B',
  amber: '#F2C98D',
  mist: '#B9DDE8',
} as const;

export type ColorName = keyof typeof colors;
