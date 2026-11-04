import { StyleSheet, Text, View } from 'react-native';
import { AuthButton } from '../../components/ui/AuthButton';
import en from '../../localization/en.json';
import { supabase } from '../../lib/supabase';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { MAX_FONT_SCALE, typography } from '../../theme/typography';

export default function Profile() {
  return (
    <View style={styles.root}>
      <Text accessibilityRole="header" style={[typography.h1, styles.title]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        Profile
      </Text>
      <AuthButton
        variant="secondary"
        label={en.auth.sign_out}
        onPress={() => void supabase.auth.signOut()}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: colors.ivory,
    justifyContent: 'center',
    padding: spacing.xxl,
    gap: spacing.xxl,
  },
  title: { color: colors.forest, textAlign: 'center' },
});