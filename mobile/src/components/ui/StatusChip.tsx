import type { LucideIcon } from 'lucide-react-native';
import { StyleSheet, View } from 'react-native';
import { radius } from '../../theme/shape';
import { spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeProvider';
import type { ThemeColors } from '../../theme/themes';
import { useCard } from './Card';
import { Icon } from './Icon';
import { Text } from './Text';

type Tone = 'neutral' | 'success' | 'info' | 'warning';
const toneColor: Record<Exclude<Tone, 'neutral'>, keyof ThemeColors> = {
  success: 'success',
  info: 'info',
  warning: 'warningText',
};

// Always an icon plus a word, never colour alone. Neutral follows the card's own text colour,
// because Muted Slate fails contrast on the cream cards.
export function StatusChip({ icon, label, tone = 'neutral' }: { icon: LucideIcon; label: string; tone?: Tone }) {
  const { theme } = useTheme();
  const { fg } = useCard();
  const color = tone === 'neutral' ? fg : toneColor[tone];
  return (
    <View style={[styles.chip, { borderColor: theme.colors[color] }]}>
      <Icon icon={icon} size={16} color={color} />
      <Text variant="caption" color={color}>
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: spacing.xs,
    minHeight: 28,
    paddingHorizontal: spacing.md,
    borderRadius: radius.chip,
    borderWidth: 1,
  },
});