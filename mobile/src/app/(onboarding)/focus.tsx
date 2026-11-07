import { copy, TOTAL_STEPS } from '../../features/onboarding/copy';
import { focusOptions } from '../../features/onboarding/options';
import { useOnboarding } from '../../features/onboarding/store';
import { SingleChoiceStep } from '../../features/onboarding/ui';

export default function FocusStep() {
  const focus = useOnboarding((s) => s.answers.focus);
  const setAnswer = useOnboarding((s) => s.setAnswer);
  return (
    <SingleChoiceStep
      step={1}
      total={TOTAL_STEPS}
      title={copy.focus.title}
      subtitle={copy.focus.subtitle}
      options={focusOptions}
      selected={focus}
      onSelect={(value) => setAnswer({ focus: value })}
      next="/user-type"
    />
  );
}