import { useRouter } from 'expo-router';
import { Search, User } from 'lucide-react-native';
import { Image, Pressable, StyleSheet, View } from 'react-native';
import { minTouch } from '../../theme/shape';
import { spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeProvider';
import { fonts } from '../../theme/typography';
import { Icon } from './Icon';
import { Text } from './Text';

export function AppHeader() {
  const { theme } = useTheme();
  const c = theme.colors;
  const router = useRouter();

  return (
    <View
      style={[
        styles.wrap,
        { backgroundColor: c.background, borderBottomColor: c.cardBorder },
      ]}
    >
      <View style={styles.topRow}>
        <View style={styles.brand}>
          <Image
            source={require('../../../assets/images/bevoyalogo.png')}
            style={styles.mark}
            resizeMode="contain"
            accessibilityElementsHidden
            importantForAccessibility="no-hide-descendants"
          />
          <Text
            variant="h2"
            color="primary"
            style={{ fontFamily: fonts.bold }}
            accessibilityRole="header"
          >
            Bevoya
          </Text>
        </View>

        <Pressable
          accessibilityRole="search"
          accessibilityLabel="Search"
          accessibilityHint="Opens search for tasks, notes, goals and settings"
          onPress={() => router.push('/search')}
          style={[
            styles.search,
            { backgroundColor: c.surface, borderColor: c.inputBorder },
          ]}
        >
          <Icon icon={Search} color="textSecondary" />
          <Text color="textSecondary" numberOfLines={1} style={{ flex: 1 }}>
            Search tasks, notes, goals…
          </Text>
        </Pressable>

        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Profile"
          onPress={() => router.navigate('/profile')}
          style={[styles.avatar, { backgroundColor: c.surfaceSelected }]}
        >
          <Icon icon={User} color="primary" />
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    paddingHorizontal: spacing.xl,
    paddingBottom: spacing.md,
    paddingTop: spacing.md,
    borderBottomWidth: 1,
  },
  topRow: {
    minHeight: 56,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  brand: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  mark: { width: 28, height: 28 },
  avatar: {
    width: minTouch,
    height: minTouch,
    borderRadius: minTouch / 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  search: {
    flex: 1,
    minHeight: minTouch,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingHorizontal: spacing.lg,
    borderRadius: 24,
    borderWidth: 1,
  },
});