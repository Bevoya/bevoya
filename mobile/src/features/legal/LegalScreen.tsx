import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { BackButton } from '../../components/ui/BackButton';
import { BrandText } from '../../components/ui/BrandText';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { LEGAL_VERSION, type LegalSection } from './content';

export function LegalScreen({ title, sections }: { title: string; sections: LegalSection[] }) {
  const router = useRouter();
  return (
    <SafeAreaView style={styles.root}>
      <View style={styles.top}>
        <BackButton onPress={() => router.back()} />
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <BrandText variant="h1" color={colors.forest} accessibilityRole="header">
          {title}
        </BrandText>
        <BrandText variant="caption" color={colors.slate}>
          Version {LEGAL_VERSION}
        </BrandText>
        {sections.map((s) => (
          <View key={s.heading} style={{ gap: spacing.sm }}>
            <BrandText variant="h2" color={colors.forest} accessibilityRole="header">
              {s.heading}
            </BrandText>
            {s.paragraphs.map((p) => (
              <BrandText key={p}>{p}</BrandText>
            ))}
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.ivory },
  top: { paddingHorizontal: spacing.md },
  content: { padding: spacing.xl, gap: spacing.xl },
});