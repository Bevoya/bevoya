import { ActivityIndicator, Pressable, StyleSheet } from 'react-native';
import { minTouch, radius } from '../../theme/shape';
import { spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeProvider';
import { fonts } from '../../theme/typography';
import { Text } from './Text';

type Variant = 'primary' | 'secondary' | 'tertiary' | 'destructive';

type Props = {
  label: string;
  onPress: () => void;
  variant?: Variant;
  loading?: boolean;
  disabled?: boolean;
  fullWidth?: boolean;
  accessibilityHint?: string;
};

export function Button({ label, onPress, variant = 'primary', loading = false, disabled = false,
  fullWidth = false, accessibilityHint }: Props) {
  const { theme } = useTheme();
  const c = theme.colors;
  const inactive = disabled || loading;
  const look = {
    primary: { bg: c.primary, fg: c.onPrimary, border: c.primary },
    secondary: { bg: c.surface, fg: c.primary, border: c.primary },
    tertiary: { bg: 'transparent', fg: c.primary, border: 'transparent' },
    destructive: { bg: c.error, fg: c.onError, border: c.error },
  }[variant];

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityHint={accessibilityHint}
      accessibilityState={{ disabled: inactive, busy: loading }}
      disabled={inactive}
      onPress={onPress}
      android_ripple={{ color: c.surfaceSelected }}
      style={({ pressed }) => [
        styles.base,
        { backgroundColor: look.bg, borderColor: look.border, opacity: inactive ? 0.6 : pressed ? 0.85 : 1 },
        fullWidth && styles.full,
      ]}
    >
      {loading ? <ActivityIndicator color={look.fg} /> : null}
      <Text style={{ color: look.fg, fontFamily: fonts.semibold }}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: minTouch,
    minWidth: minTouch,
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.md,
    borderRadius: radius.button,
    borderWidth: 2,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    overflow: 'hidden',
  },
  full: { alignSelf: 'stretch' },
});