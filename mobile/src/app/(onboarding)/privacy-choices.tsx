import { useRouter } from 'expo-router';
import { NotebookPen } from 'lucide-react-native';
import { StyleSheet, View } from 'react-native';
import { BrandText } from '../../components/ui/BrandText';
import { summarize } from '../../features/onboarding/answers';
import { copy, TOTAL_STEPS } from '../../features/onboarding/copy';
import { useOnboarding } from '../../features/onboarding/store';
import { OnbButton, OnboardingShell, ToggleRow } from '../../features/onboarding/ui';
import { withAlpha } from '../../theme/color';
import { colors } from '../../theme/colors';
import { fonts } from '../../theme/typography';
import { radius } from '../../theme/shape';
import { spacing } from '../../theme/spacing';

export default function PlanStep() {
  const router = useRouter();
  const answers = useOnboarding((s) => s.answers);
  const setAnswer = useOnboarding((s) => s.setAnswer);
  const markCompleted = useOnboarding((s) => s.markCompleted);
  const rows = summarize(answers);
  const c = copy.plan;

const create = () => {
  markCompleted();
  router.replace('/sign-up');
};
  return (
    <OnboardingShell
      step={6}
      total={TOTAL_STEPS}
      title={c.title}
      subtitle={c.subtitle}
      footer={
        <>
          <OnbButton label={c.create} onPress={create} />
          <View style={styles.links}>
            <OnbButton variant="link" label={c.terms} onPress={() => router.push('/legal/terms')} />
            <OnbButton variant="link" label={c.privacy} onPress={() => router.push('/legal/privacy')} />
          </View>
        </>
      }
    >
      {rows.length > 0 ? (
        <View style={styles.summary}>
          {rows.map((row) => (
            <View key={row.label} style={styles.row}>
              <BrandText variant="secondary" color={colors.slate}>
                {row.label}
              </BrandText>
              <BrandText style={{ fontFamily: fonts.semibold, flexShrink: 1, textAlign: 'right' }}>{row.value}</BrandText>
            </View>
          ))}
        </View>
      ) : null}

      <ToggleRow
        icon={NotebookPen}
        title={c.aiTitle}
        detail={c.aiDetail}
        value={answers.aiMemory}
        onValueChange={(value) => setAnswer({ aiMemory: value })}
      />

      <BrandText variant="secondary">{c.privateLine}</BrandText>
    </OnboardingShell>
  );
}

const styles = StyleSheet.create({
  summary: {
    backgroundColor: colors.white,
    borderRadius: radius.card,
    borderWidth: 1,
    borderColor: withAlpha(colors.charcoal, 0.1),
    padding: spacing.lg,
    gap: spacing.md,
  },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: spacing.md },
  links: { flexDirection: 'row', justifyContent: 'center', flexWrap: 'wrap' },
});