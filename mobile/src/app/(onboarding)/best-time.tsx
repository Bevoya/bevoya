import { copy, TOTAL_STEPS } from '../../features/onboarding/copy';
import { bestTimeOptions } from '../../features/onboarding/options';
import { useOnboarding } from '../../features/onboarding/store';
import { SingleChoiceStep } from '../../features/onboarding/ui';

export default function BestTimeStep() {
  const bestTime = useOnboarding((s) => s.answers.bestTime);
  const setAnswer = useOnboarding((s) => s.setAnswer);
  return (
    <SingleChoiceStep
      step={4}
      total={TOTAL_STEPS}
      title={copy.bestTime.title}
      subtitle={copy.bestTime.subtitle}
      options={bestTimeOptions}
      selected={bestTime}
      onSelect={(value) => setAnswer({ bestTime: value })}
      next="/notifications"
    />
  );
}