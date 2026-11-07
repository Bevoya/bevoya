import { copy, TOTAL_STEPS } from '../../features/onboarding/copy';
import { timeOptions } from '../../features/onboarding/options';
import { useOnboarding } from '../../features/onboarding/store';
import { SingleChoiceStep } from '../../features/onboarding/ui';

export default function TimeStep() {
  const minutes = useOnboarding((s) => s.answers.minutes);
  const setAnswer = useOnboarding((s) => s.setAnswer);
  return (
    <SingleChoiceStep
      step={3}
      total={TOTAL_STEPS}
      title={copy.time.title}
      subtitle={copy.time.subtitle}
      options={timeOptions}
      selected={minutes}
      onSelect={(value) => setAnswer({ minutes: value })}
      next="/best-time"
    />
  );
}