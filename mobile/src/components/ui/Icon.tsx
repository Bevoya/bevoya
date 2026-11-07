import type { LucideIcon } from 'lucide-react-native';
import { useTheme } from '../../theme/ThemeProvider';
import type { ThemeColors } from '../../theme/themes';

type Props = { icon: LucideIcon; size?: 16 | 20 | 24; color?: keyof ThemeColors };

// One stroke width everywhere. Decorative: the text next to it carries the meaning.
export function Icon({ icon: Glyph, size = 20, color = 'textPrimary' }: Props) {
  const { theme } = useTheme();
  return (
    <Glyph
      size={size}
      color={theme.colors[color]}
      strokeWidth={1.75}
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
    />
  );
}