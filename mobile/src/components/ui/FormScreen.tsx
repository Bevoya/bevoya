import { useRouter } from 'expo-router';
import { ChevronLeft } from 'lucide-react-native';
import type { ReactNode } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import en from '../../localization/en.json';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { MAX_FONT_SCALE, typography } from '../../theme/typography';

type Props = { title: string; subtitle?: string; children: ReactNode };

export function FormScreen({ title, subtitle, children }: Props) {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.root}>
      <ScrollView
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={[
          styles.content,
          { paddingTop: insets.top + spacing.md, paddingBottom: insets.bottom + spacing.xxxl },
        ]}
      >
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={en.auth.back}
          onPress={() => (router.canGoBack() ? router.back() : router.replace('/sign-in'))}
          hitSlop={8}
          style={styles.back}
        >
          <ChevronLeft size={24} color={colors.forest} />
          <Text style={[typography.body, { color: colors.forest }]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {en.auth.back}
          </Text>
        </Pressable>

        <View style={styles.heading}>
          <Text
            accessibilityRole="header"
            style={[typography.h1, { color: colors.forest }]}
            maxFontSizeMultiplier={MAX_FONT_SCALE}
          >
            {title}
          </Text>
          {subtitle ? (
            <Text style={[typography.body, { color: colors.charcoal }]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
              {subtitle}
            </Text>
          ) : null}
        </View>

        {children}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.ivory },
  content: { paddingHorizontal: spacing.xxl, gap: spacing.xxl },
  back: {
    minHeight: 48,
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: spacing.xs,
  },
  heading: { gap: spacing.sm },
});