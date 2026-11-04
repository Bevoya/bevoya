import { CircleAlert, Eye, EyeOff } from 'lucide-react-native';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View, type TextInputProps } from 'react-native';
import en from '../../localization/en.json';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { statusColors } from '../../theme/status';
import { fonts, MAX_FONT_SCALE, typography } from '../../theme/typography';

type Props = {
  label: string;
  value: string;
  onChangeText: (text: string) => void;
  onBlur?: () => void;
  errorText?: string;
  secureTextEntry?: boolean;
} & Pick<
  TextInputProps,
  | 'keyboardType'
  | 'autoComplete'
  | 'textContentType'
  | 'returnKeyType'
  | 'onSubmitEditing'
  | 'autoCapitalize'
  | 'autoCorrect'
>;

export function TextField({
  label,
  value,
  onChangeText,
  onBlur,
  errorText,
  secureTextEntry,
  ...inputProps
}: Props) {
  const [focused, setFocused] = useState(false);
  const [hidden, setHidden] = useState(true);
  const isPassword = secureTextEntry === true;

  const borderColor = errorText ? statusColors.error : focused ? colors.forest : colors.slate;

  return (
    <View style={styles.wrap}>
      <Text style={[typography.secondary, styles.label]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {label}
      </Text>

      <View style={[styles.box, { borderColor, borderWidth: focused || errorText ? 2 : 1.5 }]}>
        <TextInput
          {...inputProps}
          value={value}
          onChangeText={onChangeText}
          onFocus={() => setFocused(true)}
          onBlur={() => {
            setFocused(false);
            onBlur?.();
          }}
          secureTextEntry={isPassword && hidden}
          accessibilityLabel={label}
          accessibilityHint={errorText}
          maxFontSizeMultiplier={MAX_FONT_SCALE}
          placeholderTextColor={colors.slate}
          style={styles.input}
        />
        {isPassword ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={hidden ? en.auth.show_password : en.auth.hide_password}
            hitSlop={8}
            onPress={() => setHidden((current) => !current)}
            style={styles.toggle}
          >
            {hidden ? (
              <Eye size={22} color={colors.charcoal} />
            ) : (
              <EyeOff size={22} color={colors.charcoal} />
            )}
          </Pressable>
        ) : null}
      </View>

      {errorText ? (
        <View style={styles.errorRow} accessibilityLiveRegion="polite">
          <CircleAlert size={16} color={statusColors.error} />
          <Text
            style={[typography.secondary, { color: statusColors.error, flexShrink: 1 }]}
            maxFontSizeMultiplier={MAX_FONT_SCALE}
          >
            {errorText}
          </Text>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: spacing.sm },
  label: { color: colors.charcoal },
  box: {
    minHeight: 52,
    borderRadius: 14,
    backgroundColor: colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: spacing.lg,
  },
  input: {
    flex: 1,
    minHeight: 48,
    paddingVertical: spacing.md,
    fontFamily: fonts.regular,
    fontSize: 16,
    lineHeight: 24,
    color: colors.charcoal,
  },
  toggle: {
    minWidth: 48,
    minHeight: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
  errorRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
});