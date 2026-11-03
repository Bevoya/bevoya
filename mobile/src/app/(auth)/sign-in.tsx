import { useRouter } from 'expo-router';
import { Mail } from 'lucide-react-native';
import { Alert, Image, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { AuthButton } from '../../components/ui/AuthButton';
import { LandscapeBackground } from '../../components/ui/LandscapeBackground';
import en from '../../localization/en.json';
import { colors } from '../../theme/colors';
import { GoogleG } from '../../theme/googleG';
import { spacing } from '../../theme/spacing';
import { MAX_FONT_SCALE, typography } from '../../theme/typography';

const logo = require('../../../assets/images/bevoyalogo.png');

// Distance of the buttons above the bottom edge (bigger = higher on the screen).
const BOTTOM_OFFSET = 40;

export default function SignIn() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.root}>
      <LandscapeBackground />

      <View style={[styles.hero, { paddingTop: insets.top + spacing.huge }]}>
        <Image
          source={logo}
          style={styles.logo}
          resizeMode="contain"
          accessible={false}
          importantForAccessibility="no"
        />
        <Text
          accessibilityRole="header"
          style={[typography.wordmark, styles.wordmark]}
          adjustsFontSizeToFit
          numberOfLines={1}
          maxFontSizeMultiplier={1.3}
        >
          Bevoya
        </Text>
        <Text style={[typography.body, styles.tagline]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {en.auth.tagline}
        </Text>
      </View>

      <View style={[styles.actions, { paddingBottom: insets.bottom + BOTTOM_OFFSET }]}>
        <AuthButton
          variant="primary"
          label={en.auth.continue_email}
          icon={<Mail size={20} color={colors.white} />}
          onPress={() => router.push('/sign-up')}
        />
        <AuthButton
          variant="secondary"
          label={en.auth.continue_google}
          icon={<GoogleG size={20} />}
          onPress={() => Alert.alert('Google sign-in', 'Coming in a later step.')}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.ivory },
  hero: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: spacing.xl },
  logo: { width: 112, height: 112, marginBottom: spacing.lg },
  wordmark: { color: colors.forest, textAlign: 'center' },
  tagline: { color: colors.forest, textAlign: 'center', marginTop: spacing.sm },
  actions: { paddingHorizontal: spacing.xxl, gap: spacing.md },
});