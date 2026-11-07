import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import { CircleAlert } from 'lucide-react-native';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { AuthButton } from '../../components/ui/AuthButton';
import { FormScreen } from '../../components/ui/FormScreen';
import { TextField } from '../../components/ui/TextField';
import { authMessage, classifyAuthError } from '../../features/auth/errors';
import { signInSchema, type SignInValues } from '../../features/auth/schemas';
import en from '../../localization/en.json';
import { supabase } from '../../lib/supabase';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { statusColors } from '../../theme/status';
import { MAX_FONT_SCALE, typography } from '../../theme/typography';

export default function EmailSignIn() {
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignInValues>({
    resolver: zodResolver(signInSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);

    const { error } = await supabase.auth.signInWithPassword({
      email: values.email,
      password: values.password,
    });

    if (!error) return; // The session change moves the app to the tabs.

    setFormError(authMessage(classifyAuthError(error), 'sign_in'));
  });

  return (
    <FormScreen title={en.auth.sign_in_title} subtitle={en.auth.sign_in_subtitle}>
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
              autoComplete="current-password"
              textContentType="password"
              returnKeyType="done"
              onSubmitEditing={() => void onSubmit()}
            />
          )}
        />
      </View>

      {formError ? (
        <View style={styles.formError} accessibilityLiveRegion="polite">
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
          label={isSubmitting ? '…' : en.auth.sign_in_action}
          onPress={() => {
            if (!isSubmitting) void onSubmit();
          }}
        />

        <Pressable
          accessibilityRole="button"
          accessibilityLabel={en.auth.need_account}
          onPress={() => router.replace('/sign-up')}
          style={styles.link}
        >
          <Text
            style={[typography.body, { color: colors.forest }]}
            maxFontSizeMultiplier={MAX_FONT_SCALE}
          >
            {en.auth.need_account}
          </Text>
        </Pressable>
      </View>
    </FormScreen>
  );
}

const styles = StyleSheet.create({
  fields: {
    gap: spacing.xl,
  },
  formError: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  actions: {
    gap: spacing.md,
  },
  link: {
    minHeight: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
});