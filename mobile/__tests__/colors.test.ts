import { colors } from '../src/theme/colors';

// WCAG 2.x relative luminance and contrast ratio.
function luminance(hex: string): number {
  const h = hex.replace('#', '');
  const [r, g, b] = [0, 2, 4].map((i) => parseInt(h.slice(i, i + 2), 16) / 255);
  const lin = (c: number) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
  return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
}

function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

// Pairs that COLORS.md allows for normal text (4.5:1).
const allowedTextPairs: [string, keyof typeof colors, keyof typeof colors][] = [
  ['white on forest', 'white', 'forest'],
  ['forest on sage', 'forest', 'sage'],
  ['forest on ivory', 'forest', 'ivory'],
  ['forest on white', 'forest', 'white'],
  ['forest on amber', 'forest', 'amber'],
  ['forest on mist', 'forest', 'mist'],
  ['charcoal on ivory', 'charcoal', 'ivory'],
  ['charcoal on white', 'charcoal', 'white'],
  ['charcoal on sage', 'charcoal', 'sage'],
  ['charcoal on amber', 'charcoal', 'amber'],
  ['charcoal on mist', 'charcoal', 'mist'],
  ['slate on white', 'slate', 'white'],
  ['slate on ivory', 'slate', 'ivory'],
];

describe('brand colour contrast (COLORS.md)', () => {
  it.each(allowedTextPairs)('%s meets 4.5:1', (_label, fg, bg) => {
    expect(contrast(colors[fg], colors[bg])).toBeGreaterThanOrEqual(4.5);
  });

  // Documented failures: these pairs must never be used for normal text.
  it.each([
    ['slate on sage', 'slate', 'sage'],
    ['slate on mist', 'slate', 'mist'],
    ['charcoal on forest', 'charcoal', 'forest'],
    ['white on amber', 'white', 'amber'],
  ] as const)('%s is still a known failing pair', (_label, fg, bg) => {
    expect(contrast(colors[fg], colors[bg])).toBeLessThan(4.5);
  });
});