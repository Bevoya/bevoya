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
import { authMessage, classifyAuthError, isExistingSignUp, type AuthProblem } from '../../features/auth/errors';
import { legalCopy } from '../../features/legal/content';
import en from '../../localization/en.json';
import { supabase } from '../../lib/supabase';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { statusColors } from '../../theme/status';
import { MAX_FONT_SCALE, typography } from '../../theme/typography';

export default function SignUp() {
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const [problem, setProblem] = useState<AuthProblem | null>(null);
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [termsError, setTermsError] = useState(false);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignUpValues>({
    resolver: zodResolver(signUpSchema),
    defaultValues: { email: '', password: '', confirmAge: false },
  });

  const fail = (next: AuthProblem) => {
    setProblem(next);
    setFormError(authMessage(next, 'sign_up'));
  };

  const onSubmit = handleSubmit(async (values) => {
    if (!termsAccepted) {
      setTermsError(true);
      return;
    }
    setFormError(null);
    setProblem(null);
    const { data, error } = await supabase.auth.signUp({
      email: values.email,
      password: values.password,
    });

    if (error) {
      fail(classifyAuthError(error));
      return;
    }
    if (isExistingSignUp(data)) {
      fail('user_exists');
      return;
    }
    // With email confirmation off, Supabase returns a session and the app moves on by itself.
    if (data.session) return;
    setSentTo(values.email);
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

        <View style={styles.ageWrap}>
          <Pressable
            accessibilityRole="checkbox"
            accessibilityState={{ checked: termsAccepted }}
            accessibilityLabel={legalCopy.signUpAgree}
            onPress={() => {
              setTermsAccepted(!termsAccepted);
              setTermsError(false);
            }}
            style={styles.ageRow}
          >
            <View style={[styles.box, termsAccepted && styles.boxChecked]}>
              {termsAccepted ? <Check size={18} color={colors.white} /> : null}
            </View>
            <Text
              style={[typography.body, { color: colors.charcoal, flexShrink: 1 }]}
              maxFontSizeMultiplier={MAX_FONT_SCALE}
            >
              {legalCopy.signUpAgree}
            </Text>
          </Pressable>
          <View style={styles.legalLinks}>
            <Pressable
              accessibilityRole="link"
              accessibilityLabel={legalCopy.termsLink}
              onPress={() => router.push('/legal/terms')}
              style={styles.legalLink}
            >
              <Text style={[typography.secondary, styles.linkText]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
                {legalCopy.termsLink}
              </Text>
            </Pressable>
            <Pressable
              accessibilityRole="link"
              accessibilityLabel={legalCopy.privacyLink}
              onPress={() => router.push('/legal/privacy')}
              style={styles.legalLink}
            >
              <Text style={[typography.secondary, styles.linkText]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
                {legalCopy.privacyLink}
              </Text>
            </Pressable>
          </View>
          {termsError ? (
            <View style={styles.errorRow} accessibilityLiveRegion="polite">
              <CircleAlert size={16} color={statusColors.error} />
              <Text
                style={[typography.secondary, { color: statusColors.error, flexShrink: 1 }]}
                maxFontSizeMultiplier={MAX_FONT_SCALE}
              >
                {legalCopy.signUpError}
              </Text>
            </View>
          ) : null}
        </View>
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
        {problem === 'user_exists' ? (
          <AuthButton
            variant="primary"
            label="Sign in instead"
            onPress={() => router.replace('/email-sign-in')}
          />
        ) : (
          <AuthButton
            variant="primary"
            label={isSubmitting ? '…' : en.auth.sign_up_action}
            onPress={() => {
              if (isSubmitting) return;
              if (!termsAccepted) setTermsError(true);
              void onSubmit();
            }}
          />
        )}
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
  legalLinks: { flexDirection: 'row', flexWrap: 'wrap', columnGap: spacing.xl, paddingLeft: 28 + spacing.md },
  legalLink: { minHeight: 48, justifyContent: 'center' },
  linkText: { color: colors.forest, textDecorationLine: 'underline' },
  actions: { gap: spacing.md },
  link: { minHeight: 48, alignItems: 'center', justifyContent: 'center' },
});