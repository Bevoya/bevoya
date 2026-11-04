import type { LucideIcon } from 'lucide-react-native';
import { StyleSheet, View } from 'react-native';
import { spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeProvider';
import { Button } from './Button';
import { Icon } from './Icon';
import { Text } from './Text';

type Props = { icon: LucideIcon; message: string; actionLabel?: string; onAction?: () => void };

export function EmptyState({ icon, message, actionLabel, onAction }: Props) {
  const { theme } = useTheme();
  return (
    <View style={styles.wrap}>
      <View style={[styles.badge, { backgroundColor: theme.colors.surfaceSelected }]}>
        <Icon icon={icon} size={24} color="primary" />
      </View>
      <Text style={styles.message}>{message}</Text>
      {actionLabel && onAction ? <Button label={actionLabel} variant="secondary" onPress={onAction} /> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { alignItems: 'center', gap: spacing.lg, padding: spacing.xxl },
  badge: { width: 56, height: 56, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  message: { textAlign: 'center' },
});