import { Activity, BookOpen, Check, Hammer, RotateCcw, type LucideIcon } from 'lucide-react-native';
import { Pressable, StyleSheet, View } from 'react-native';
import { useCard } from '../../components/ui/Card';
import { Icon } from '../../components/ui/Icon';
import { Text } from '../../components/ui/Text';
import { spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeProvider';
import type { SampleTask, TaskCategory } from './sample';

export const categoryIcon: Record<TaskCategory, LucideIcon> = {
  learn: BookOpen,
  build: Hammer,
  revise: RotateCcw,
  health: Activity,
};
export const categoryName: Record<TaskCategory, string> = {
  learn: 'Learn',
  build: 'Build',
  revise: 'Revise',
  health: 'Health',
};

export function TaskRow({ task, onToggle }: { task: SampleTask; onToggle: () => void }) {
  const { theme } = useTheme();
  const { fg } = useCard();
  const c = theme.colors;
  return (
    <Pressable
      accessibilityRole="checkbox"
      accessibilityState={{ checked: task.done }}
      accessibilityLabel={`${task.title}, ${categoryName[task.category]}, ${task.minutes} minutes`}
      onPress={onToggle}
      style={styles.row}
    >
      <View
        style={[
          styles.circle,
          { borderColor: task.done ? c.primary : c.inputBorder, backgroundColor: task.done ? c.primary : 'transparent' },
        ]}
      >
        {task.done ? <Icon icon={Check} size={16} color="onPrimary" /> : null}
      </View>
      <View style={styles.body}>
        <Text color={fg} style={task.done ? { textDecorationLine: 'line-through' } : undefined}>
          {task.title}
        </Text>
        <View style={styles.meta}>
          <Icon icon={categoryIcon[task.category]} size={16} color={fg} />
          <Text variant="secondary" color={fg}>
            {categoryName[task.category]} · {task.minutes} min{task.done ? ' · Done' : ''}
          </Text>
        </View>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { minHeight: 56, flexDirection: 'row', alignItems: 'center', gap: spacing.md, paddingVertical: spacing.sm },
  circle: { width: 28, height: 28, borderRadius: 14, borderWidth: 2, alignItems: 'center', justifyContent: 'center' },
  body: { flex: 1, gap: 2 },
  meta: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs },
});