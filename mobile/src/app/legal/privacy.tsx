import { LegalScreen } from '../../features/legal/LegalScreen';
import { privacySections } from '../../features/legal/content';

export default function Privacy() {
  return <LegalScreen title="Privacy Policy" sections={privacySections} />;
}