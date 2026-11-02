import { darkTheme, lightTheme, type ThemeColors } from '../src/theme/themes';

function luminance(hex: string): number {
  const channel = (i: number) => {
    const v = parseInt(hex.slice(1 + i * 2, 3 + i * 2), 16) / 255;
    return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
  };
  return 0.2126 * channel(0) + 0.7152 * channel(1) + 0.0722 * channel(2);
}

function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

type Pair = [keyof ThemeColors, keyof ThemeColors, number];

const lightPairs: Pair[] = [
  ['textPrimary', 'background', 4.5], ['textPrimary', 'surface', 4.5],
  ['textSecondary', 'background', 4.5], ['textSecondary', 'surface', 4.5],
  ['onPrimary', 'primary', 4.5], ['primary', 'background', 4.5],
  ['textPrimary', 'surfaceSelected', 4.5], ['onAccent', 'accentMilestone', 4.5],
  ['onAccent', 'accentLearning', 4.5], ['onAccent', 'accentAction', 4.5],
  ['onSecondary', 'secondary', 4.5], ['error', 'background', 4.5], ['error', 'surface', 4.5],
  ['onError', 'error', 4.5], ['success', 'surface', 4.5], ['warningText', 'surface', 4.5],
  ['info', 'surface', 4.5], ['inputBorder', 'surface', 3],
];

const darkPairs: Pair[] = [
  ['textPrimary', 'background', 4.5], ['textPrimary', 'surface', 4.5],
  ['textPrimary', 'surfaceElevated', 4.5], ['textSecondary', 'background', 4.5],
  ['textSecondary', 'surface', 4.5], ['textSecondary', 'surfaceElevated', 4.5],
  ['onPrimary', 'primary', 4.5], ['primary', 'background', 4.5], ['primary', 'surface', 4.5],
  ['textPrimary', 'surfaceSelected', 4.5], ['onAccent', 'accentMilestone', 4.5],
  ['onAccent', 'accentLearning', 4.5], ['onAccent', 'accentAction', 4.5],
  ['onSecondary', 'secondary', 4.5], ['error', 'surface', 4.5], ['onError', 'error', 4.5],
  ['success', 'surface', 4.5], ['warningText', 'surface', 4.5], ['info', 'surface', 4.5],
  ['inputBorder', 'surface', 3], ['inputBorder', 'background', 3],
];

describe('theme contrast (COLORS.md)', () => {
  test.each(lightPairs)('light: %s on %s is at least %s:1', (fg, bg, min) => {
    expect(contrast(lightTheme.colors[fg], lightTheme.colors[bg])).toBeGreaterThanOrEqual(min);
  });

  test.each(darkPairs)('dark: %s on %s is at least %s:1', (fg, bg, min) => {
    expect(contrast(darkTheme.colors[fg], darkTheme.colors[bg])).toBeGreaterThanOrEqual(min);
  });

  test('both themes define the same tokens', () => {
    expect(Object.keys(darkTheme.colors).sort()).toEqual(Object.keys(lightTheme.colors).sort());
  });
});