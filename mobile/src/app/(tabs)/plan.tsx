import { format } from 'date-fns';
import { ListChecks } from 'lucide-react-native';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AppHeader } from '../../components/ui/AppHeader';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { EmptyState } from '../../components/ui/EmptyState';
import { ProgressRing } from '../../components/ui/ProgressRing';
import { SectionHeader } from '../../components/ui/SectionHeader';
import { Text } from '../../components/ui/Text';
import { TaskRow } from '../../features/tasks/TaskRow';
import { useSampleTasks } from '../../features/tasks/sample';
import { spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeProvider';

export default function Plan() {
  const { theme } = useTheme();
  const c = theme.colors;
  const { tasks, toggle } = useSampleTasks();
  const todo = tasks.filter((t) => !t.done);
  const done = tasks.filter((t) => t.done);
  const minutesLeft = todo.reduce((sum, t) => sum + t.minutes, 0);
  const share = tasks.length === 0 ? 0 : done.length / tasks.length;

  return (
    <SafeAreaView edges={['top']} style={{ flex: 1, backgroundColor: c.background }}>
      <AppHeader />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={{ gap: 2 }}>
          <Text variant="h1" accessibilityRole="header">Plan</Text>
          <Text variant="secondary" color="textSecondary">{format(new Date(), 'EEEE d MMMM')}</Text>
        </View>

        <Card variant="sand">
          <View style={styles.summary}>
            <ProgressRing
              value={share}
              label={`${done.length}/${tasks.length}`}
              a11yLabel={`${done.length} of ${tasks.length} tasks done`}
              color={c.primary}
              trackColor={c.surface}
              size={64}
              strokeWidth={7}
            />
            <View style={{ flex: 1 }}>
              <Text variant="h2">{done.length} of {tasks.length} done</Text>
              <Text variant="secondary">{minutesLeft} minutes planned</Text>
            </View>
          </View>
        </Card>

        <Card variant="oat">
          <SectionHeader icon={ListChecks} title="To do" />
          {todo.length === 0 ? (
            <EmptyState icon={ListChecks} message="Nothing left for today. Add a task or rest." />
          ) : (
            todo.map((t) => <TaskRow key={t.id} task={t} onToggle={() => toggle(t.id)} />)
          )}
        </Card>

        {done.length > 0 ? (
          <Card variant="sand">
            <SectionHeader icon={ListChecks} title="Done" />
            {done.map((t) => (
              <TaskRow key={t.id} task={t} onToggle={() => toggle(t.id)} />
            ))}
          </Card>
        ) : null}

        <Button
          label="Add task"
          onPress={() => Alert.alert('Add task', 'Saving tasks arrives with the API client.')}
        />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.xl, gap: spacing.xl },
  summary: { flexDirection: 'row', alignItems: 'center', gap: spacing.lg },
});