import { zodResolver } from '@hookform/resolvers/zod';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { CircleAlert, CircleCheck } from 'lucide-react-native';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { AuthButton } from '../../components/ui/AuthButton';
import { FormScreen } from '../../components/ui/FormScreen';
import { TextField } from '../../components/ui/TextField';
import { verifyCopy } from '../../features/auth/copy';
import { authMessage, classifyAuthError } from '../../features/auth/errors';
import { CODE_MAX_LENGTH, codeSchema, type CodeValues } from '../../features/auth/schemas';
import { useCooldown } from '../../features/auth/useCooldown';
import { supabase } from '../../lib/supabase';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { statusColors } from '../../theme/status';
import { MAX_FONT_SCALE, typography } from '../../theme/typography';

// Supabase limits how often one address can be sent a code, so wait before offering another.
const RESEND_SECONDS = 60;

export default function VerifyEmail() {
  const router = useRouter();
  const { email: emailParam } = useLocalSearchParams<{ email?: string }>();
  const email = typeof emailParam === 'string' ? emailParam : '';

  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [resending, setResending] = useState(false);
  const { remaining, start } = useCooldown(RESEND_SECONDS);

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CodeValues>({
    resolver: zodResolver(codeSchema),
    defaultValues: { code: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);

    const { error } = await supabase.auth.verifyOtp({
      email,
      token: values.code,
      type: 'signup',
    });

    if (!error) return; // The new session moves the app on by itself.
    setFormError(authMessage(classifyAuthError(error), 'verify'));
  });

  const resend = async () => {
    if (remaining > 0 || resending) return;
    setResending(true);
    setFormError(null);
    setNotice(null);

    const { error } = await supabase.auth.resend({ type: 'signup', email });
    setResending(false);

    if (error) {
      const problem = classifyAuthError(error);
      setFormError(authMessage(problem, 'resend'));
      if (problem === 'rate_limited') start(RESEND_SECONDS);
      return;
    }
    setNotice(verifyCopy.sent);
    start(RESEND_SECONDS);
  };

  if (!email) {
    return (
      <FormScreen title={verifyCopy.title} subtitle={verifyCopy.missingEmail}>
        <AuthButton variant="primary" label={verifyCopy.startAgain} onPress={() => router.replace('/sign-up')} />
      </FormScreen>
    );
  }

  return (
    <FormScreen title={verifyCopy.title} subtitle={verifyCopy.body(email)}>
      <View style={styles.fields}>
        <Controller
          control={control}
          name="code"
          render={({ field }) => (
            <TextField
              label={verifyCopy.codeLabel}
              value={field.value}
              onChangeText={(text) => field.onChange(text.replace(/\D/g, '').slice(0, CODE_MAX_LENGTH))}
              onBlur={field.onBlur}
              errorText={errors.code?.message}
              keyboardType="number-pad"
              autoComplete="sms-otp"
              textContentType="oneTimeCode"
              autoCapitalize="none"
              autoCorrect={false}
              returnKeyType="done"
              onSubmitEditing={() => void onSubmit()}
            />
          )}
        />
        <Text style={[typography.secondary, { color: colors.slate }]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {verifyCopy.spamHint}
        </Text>
      </View>

      {formError ? (
        <View style={styles.messageRow} accessibilityLiveRegion="polite">
          <CircleAlert size={18} color={statusColors.error} />
          <Text
            style={[typography.secondary, { color: statusColors.error, flexShrink: 1 }]}
            maxFontSizeMultiplier={MAX_FONT_SCALE}
          >
            {formError}
          </Text>
        </View>
      ) : null}

      {notice ? (
        <View style={styles.messageRow} accessibilityLiveRegion="polite">
          <CircleCheck size={18} color={colors.forest} />
          <Text
            style={[typography.secondary, { color: colors.forest, flexShrink: 1 }]}
            maxFontSizeMultiplier={MAX_FONT_SCALE}
          >
            {notice}
          </Text>
        </View>
      ) : null}

      <View style={styles.actions}>
        <AuthButton
          variant="primary"
          label={isSubmitting ? '…' : verifyCopy.confirm}
          onPress={() => {
            if (!isSubmitting) void onSubmit();
          }}
        />

        <Pressable
          accessibilityRole="button"
          accessibilityLabel={remaining > 0 ? verifyCopy.resendWait(remaining) : verifyCopy.resend}
          accessibilityState={{ disabled: remaining > 0 || resending }}
          onPress={() => void resend()}
          style={styles.link}
        >
          <Text
            style={[typography.body, { color: remaining > 0 || resending ? colors.slate : colors.forest }]}
            maxFontSizeMultiplier={MAX_FONT_SCALE}
          >
            {remaining > 0 ? verifyCopy.resendWait(remaining) : verifyCopy.resend}
          </Text>
        </Pressable>

        <Pressable
          accessibilityRole="button"
          accessibilityLabel={verifyCopy.differentEmail}
          onPress={() => router.replace('/sign-up')}
          style={styles.link}
        >
          <Text style={[typography.body, { color: colors.forest }]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {verifyCopy.differentEmail}
          </Text>
        </Pressable>
      </View>
    </FormScreen>
  );
}

const styles = StyleSheet.create({
  fields: { gap: spacing.md },
  messageRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  actions: { gap: spacing.sm },
  link: { minHeight: 48, alignItems: 'center', justifyContent: 'center' },
});