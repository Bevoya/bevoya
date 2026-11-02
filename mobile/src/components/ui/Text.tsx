import { Text as NativeText, type TextProps as NativeTextProps } from 'react-native';
import { useTheme } from '../../theme/ThemeProvider';
import type { ThemeColors } from '../../theme/themes';
import { MAX_FONT_SCALE, typography, type TypographyVariant } from '../../theme/typography';

type Props = NativeTextProps & { variant?: TypographyVariant; color?: keyof ThemeColors };

export function Text({ variant = 'body', color = 'textPrimary', style, ...rest }: Props) {
  const { theme } = useTheme();
  return (
    <NativeText
      maxFontSizeMultiplier={MAX_FONT_SCALE}
      style={[typography[variant], { color: theme.colors[color] }, style]}
      {...rest}
    />
  );
}