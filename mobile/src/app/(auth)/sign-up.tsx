import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import { Check, CircleAlert } from 'lucide-react-native';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { AuthButton } from '../../components/ui/AuthButton';
import { FormScreen } from '../../components/ui/FormScreen';
import { TextField } from '../../components/ui/TextField';
import { signUpSchema, type SignUpValues } from '../../features/auth/schemas';
import en from '../../localization/en.json';
import { supabase } from '../../lib/supabase';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { statusColors } from '../../theme/status';
import { MAX_FONT_SCALE, typography } from '../../theme/typography';

export default function SignUp() {
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const [sentTo, setSentTo] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignUpValues>({
    resolver: zodResolver(signUpSchema),
    defaultValues: { email: '', password: '', confirmAge: false },
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    const { error } = await supabase.auth.signUp({
      email: values.email,
      password: values.password,
    });

    if (!error) {
      setSentTo(values.email);
      return;
    }
    if (error.code === 'weak_password') {
      setFormError(en.auth.weak_password);
    } else if (error.code === 'over_email_send_rate_limit') {
      setFormError(en.auth.rate_limited);
    } else if (error.name === 'AuthRetryableFetchError') {
      setFormError(en.auth.offline);
    } else {
      setFormError(en.auth.sign_up_failed);
    }
  });

  if (sentTo) {
    return (
      <FormScreen
        title={en.auth.check_email_title}
        subtitle={en.auth.check_email_body.replace('{{email}}', sentTo)}
      >
        <AuthButton
          variant="primary"
          label={en.auth.go_to_sign_in}
          onPress={() => router.replace('/email-sign-in')}
        />
      </FormScreen>
    );
  }

  return (
    <FormScreen title={en.auth.sign_up_title} subtitle={en.auth.sign_up_subtitle}>
      <View style={styles.fields}>
        <Controller
          control={control}
          name="email"
          render={({ field }) => (
            <TextField
              label={en.auth.email_label}
              value={field.value}
              onChangeText={field.onChange}
              onBlur={field.onBlur}
              errorText={errors.email?.message}
              keyboardType="email-address"
              autoCapitalize="none"
              autoCorrect={false}
              autoComplete="email"
              textContentType="emailAddress"
              returnKeyType="next"
            />
          )}
        />
        <Controller
          control={control}
          name="password"
          render={({ field }) => (
            <TextField
              label={en.auth.password_label}
              value={field.value}
              onChangeText={field.onChange}
              onBlur={field.onBlur}
              errorText={errors.password?.message}
              secureTextEntry
              autoCapitalize="none"
              autoCorrect={false}
              autoComplete="new-password"
              textContentType="newPassword"
              returnKeyType="done"
            />
          )}
        />

        <Controller
          control={control}
          name="confirmAge"
          render={({ field }) => (
            <View style={styles.ageWrap}>
              <Pressable
                accessibilityRole="checkbox"
                accessibilityState={{ checked: field.value }}
                accessibilityLabel={en.auth.age_confirm}
                onPress={() => field.onChange(!field.value)}
                style={styles.ageRow}
              >
                <View style={[styles.box, field.value && styles.boxChecked]}>
                  {field.value ? <Check size={18} color={colors.white} /> : null}
                </View>
                <Text
                  style={[typography.body, { color: colors.charcoal, flexShrink: 1 }]}
                  maxFontSizeMultiplier={MAX_FONT_SCALE}
                >
                  {en.auth.age_confirm}
                </Text>
              </Pressable>
              {errors.confirmAge?.message ? (
                <View style={styles.errorRow} accessibilityLiveRegion="polite">
                  <CircleAlert size={16} color={statusColors.error} />
                  <Text
                    style={[typography.secondary, { color: statusColors.error, flexShrink: 1 }]}
                    maxFontSizeMultiplier={MAX_FONT_SCALE}
                  >
                    {errors.confirmAge.message}
                  </Text>
                </View>
              ) : null}
            </View>
          )}
        />
      </View>

      {formError ? (
        <View style={styles.errorRow} accessibilityLiveRegion="polite">
          <CircleAlert size={18} color={statusColors.error} />
          <Text
            style={[typography.secondary, { color: statusColors.error, flexShrink: 1 }]}
            maxFontSizeMultiplier={MAX_FONT_SCALE}
          >
            {formError}
          </Text>
        </View>
      ) : null}

      <View style={styles.actions}>
        <AuthButton
          variant="primary"
          label={isSubmitting ? '…' : en.auth.sign_up_action}
          onPress={() => {
            if (!isSubmitting) void onSubmit();
          }}
        />
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={en.auth.have_account}
          onPress={() => router.replace('/email-sign-in')}
          style={styles.link}
        >
          <Text style={[typography.body, { color: colors.forest }]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {en.auth.have_account}
          </Text>
        </Pressable>
      </View>
    </FormScreen>
  );
}

const styles = StyleSheet.create({
  fields: { gap: spacing.xl },
  ageWrap: { gap: spacing.sm },
  ageRow: { minHeight: 48, flexDirection: 'row', alignItems: 'center', gap: spacing.md },
  box: {
    width: 28,
    height: 28,
    borderRadius: 8,
    borderWidth: 2,
    borderColor: colors.slate,
    backgroundColor: colors.white,
    alignItems: 'center',
    justifyContent: 'center',
  },
  boxChecked: { backgroundColor: colors.forest, borderColor: colors.forest },
  errorRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  actions: { gap: spacing.md },
  link: { minHeight: 48, alignItems: 'center', justifyContent: 'center' },
});