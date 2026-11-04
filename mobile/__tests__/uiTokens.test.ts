import { contrastRatio, withAlpha } from '../src/theme/color';
import { clamp01, ringOffset } from '../src/components/ui/ringMath';
import { darkTheme, lightTheme } from '../src/theme/themes';

// Every card fill against the text colour Card uses on it.
const pairs = (t: typeof lightTheme) => [
  ['sand', t.colors.accentWarm, t.colors.textPrimary],
  ['oat', t.colors.accentOat, t.colors.textPrimary],
  ['focus', t.colors.surfaceSelected, t.colors.textPrimary],
  ['insight', t.colors.accentLearning, t.colors.onAccent],
  ['milestone', t.colors.accentMilestone, t.colors.onAccent],
  ['default', t.colors.surface, t.colors.textPrimary],
] as const;

describe.each([lightTheme, darkTheme])('card contrast, $name theme', (theme) => {
  it.each(pairs(theme))('%s fill reaches 4.5:1', (_name, fill, text) => {
    expect(contrastRatio(fill, text)).toBeGreaterThanOrEqual(4.5);
  });
});

describe('helpers', () => {
  it('builds a translucent colour', () => {
    expect(withAlpha('#FFFFFF', 0.25)).toBe('rgba(255, 255, 255, 0.25)');
  });
  it('clamps ring values', () => {
    expect(clamp01(2)).toBe(1);
    expect(clamp01(-1)).toBe(0);
    expect(clamp01(NaN)).toBe(0);
  });
  it('computes the ring offset', () => {
    expect(ringOffset(100, 0.25)).toBe(75);
  });
});