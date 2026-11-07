import { useRouter } from 'expo-router';
import { Moon, Sunrise } from 'lucide-react-native';
import { View } from 'react-native';
import { copy, TOTAL_STEPS } from '../../features/onboarding/copy';
import { useOnboarding } from '../../features/onboarding/store';
import { OnbButton, OnboardingShell, ToggleRow } from '../../features/onboarding/ui';
import { spacing } from '../../theme/spacing';

export default function NotificationsStep() {
  const router = useRouter();
  const answers = useOnboarding((s) => s.answers);
  const setAnswer = useOnboarding((s) => s.setAnswer);
  const c = copy.reminders;
  return (
    <OnboardingShell
      step={5}
      total={TOTAL_STEPS}
      title={c.title}
      subtitle={c.subtitle}
      footer={<OnbButton label={copy.continue} onPress={() => router.push('/privacy-choices')} />}
    >
      <View style={{ gap: spacing.md }}>
        <ToggleRow
          icon={Sunrise}
          title={c.morningTitle}
          detail={c.morningDetail}
          value={answers.morningPlan}
          onValueChange={(value) => setAnswer({ morningPlan: value })}
        />
        <ToggleRow
          icon={Moon}
          title={c.eveningTitle}
          detail={c.eveningDetail}
          value={answers.eveningReflection}
          onValueChange={(value) => setAnswer({ eveningReflection: value })}
        />
      </View>
    </OnboardingShell>
  );
}