import { LegalScreen } from '../../features/legal/LegalScreen';
import { termsSections } from '../../features/legal/content';

export default function Terms() {
  return <LegalScreen title="Terms of Service" sections={termsSections} />;
}