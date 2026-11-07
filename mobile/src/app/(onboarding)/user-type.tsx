import { copy, TOTAL_STEPS } from '../../features/onboarding/copy';
import { userTypeOptions } from '../../features/onboarding/options';
import { useOnboarding } from '../../features/onboarding/store';
import { SingleChoiceStep } from '../../features/onboarding/ui';

export default function UserTypeStep() {
  const userType = useOnboarding((s) => s.answers.userType);
  const setAnswer = useOnboarding((s) => s.setAnswer);
  return (
    <SingleChoiceStep
      step={2}
      total={TOTAL_STEPS}
      title={copy.userType.title}
      subtitle={copy.userType.subtitle}
      options={userTypeOptions}
      selected={userType}
      onSelect={(value) => setAnswer({ userType: value })}
      next="/time"
    />
  );
}