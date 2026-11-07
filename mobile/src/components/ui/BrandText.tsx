import { Text, type TextProps } from 'react-native';
import { colors } from '../../theme/colors';
import { MAX_FONT_SCALE, typography, type TypographyVariant } from '../../theme/typography';

type Props = TextProps & { variant?: TypographyVariant; color?: string };

// Text in the fixed brand colours, for the cream screens (welcome, onboarding, legal).
// The small horizontal padding stops Android from clipping the last glyph of a line
// when the box is measured a pixel or two too narrow for a custom font.
export function BrandText({ variant = 'body', color = colors.charcoal, style, ...rest }: Props) {
  return (
    <Text
      maxFontSizeMultiplier={MAX_FONT_SCALE}
      style={[typography[variant], { color, paddingHorizontal: 2 }, style]}
      {...rest}
    />
  );
}