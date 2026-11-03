import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { fonts, MAX_FONT_SCALE } from '../../theme/typography';

type Props = {
  variant: 'primary' | 'secondary';
  label: string;
  icon?: ReactNode;
  onPress: () => void;
};

export function AuthButton({ variant, label, icon, onPress }: Props) {
  const primary = variant === 'primary';

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      onPress={onPress}
      android_ripple={{ color: primary ? colors.sage : colors.forest, borderless: false }}
      style={({ pressed }) => [
        styles.base,
        primary ? styles.primary : styles.secondary,
        pressed && styles.pressed,
      ]}
    >
      {icon ? (
        <View
          style={styles.icon}
          accessibilityElementsHidden
          importantForAccessibility="no-hide-descendants"
        >
          {icon}
        </View>
      ) : null}
      <Text
        style={[styles.label, { color: primary ? colors.white : colors.forest }]}
        maxFontSizeMultiplier={MAX_FONT_SCALE}
      >
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: 56,
    borderRadius: 999,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.xxl,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.md,
    overflow: 'hidden',
  },
  primary: { backgroundColor: colors.forest },
  secondary: { backgroundColor: colors.white, borderWidth: 1.5, borderColor: colors.forest },
  pressed: { opacity: 0.9 },
  icon: { alignItems: 'center', justifyContent: 'center' },
  label: { fontFamily: fonts.semibold, fontSize: 16, lineHeight: 24, flexShrink: 1, textAlign: 'center' },
});