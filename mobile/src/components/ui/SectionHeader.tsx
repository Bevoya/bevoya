import type { LucideIcon } from 'lucide-react-native';
import { ChevronRight } from 'lucide-react-native';
import { Pressable, StyleSheet, View } from 'react-native';
import { withAlpha } from '../../theme/color';
import { minTouch } from '../../theme/shape';
import { spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeProvider';
import { fonts } from '../../theme/typography';
import { useCard } from './Card';
import { Icon } from './Icon';
import { Text } from './Text';

type Props = { icon: LucideIcon; title: string; actionLabel?: string; onAction?: () => void };

export function SectionHeader({ icon, title, actionLabel, onAction }: Props) {
  const { theme } = useTheme();
  const { fg, tinted } = useCard();
  const chipBg = tinted ? withAlpha(theme.colors[fg], 0.14) : theme.colors.surfaceSelected;
  const iconColor = tinted ? fg : 'primary';
  const actionColor = tinted ? fg : 'primary';
  return (
    <View style={styles.row}>
      <View style={[styles.chip, { backgroundColor: chipBg }]}>
        <Icon icon={icon} color={iconColor} />
      </View>
      <Text variant="h2" color={fg} accessibilityRole="header" style={styles.title}>
        {title}
      </Text>
      {actionLabel && onAction ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={actionLabel}
          onPress={onAction}
          style={styles.action}
          hitSlop={8}
        >
          <Text variant="secondary" color={actionColor} style={{ fontFamily: fonts.semibold }}>
            {actionLabel}
          </Text>
          <Icon icon={ChevronRight} size={16} color={actionColor} />
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: spacing.md },
  chip: { width: 36, height: 36, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  title: { flex: 1 },
  action: { minHeight: minTouch, flexDirection: 'row', alignItems: 'center', gap: spacing.xs },
});