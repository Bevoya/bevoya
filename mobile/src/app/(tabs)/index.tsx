import { format } from 'date-fns';
import { useRouter } from 'expo-router';
import { BookOpen, Hammer, ListChecks, Timer, type LucideIcon } from 'lucide-react-native';
import { ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AiBadge } from '../../components/ui/AiBadge';
import { AppHeader } from '../../components/ui/AppHeader';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ProgressRing } from '../../components/ui/ProgressRing';
import { SectionHeader } from '../../components/ui/SectionHeader';
import { StatusChip } from '../../components/ui/StatusChip';
import { Text } from '../../components/ui/Text';
import { sampleEvidence } from '../../features/content/sample';
import { TaskRow } from '../../features/tasks/TaskRow';
import { useSampleTasks } from '../../features/tasks/sample';
import { withAlpha } from '../../theme/color';
import { spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeProvider';

const levelIcon: Record<string, LucideIcon> = { Built: Hammer, Read: BookOpen };

export default function Home() {
  const { theme } = useTheme();
  const c = theme.colors;
  const router = useRouter();
  const { tasks, toggle } = useSampleTasks();
  const weekValue = 0.4;

  return (
    <SafeAreaView edges={['top']} style={{ flex: 1, backgroundColor: c.background }}>
      <AppHeader />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={{ gap: 2 }}>
          <Text variant="h1" accessibilityRole="header">Today</Text>
          <Text variant="secondary" color="textSecondary">{format(new Date(), 'EEEE d MMMM')}</Text>
        </View>

        <Card variant="sand">
          <View style={styles.heroRow}>
            <View style={styles.heroText}>
              <Text variant="caption" color="primary">Main goal</Text>
              <Text variant="h2">Become an AI fintech developer</Text>
              <Text variant="secondary">2 of 5 milestones</Text>
            </View>
            <ProgressRing
              value={weekValue}
              label="40%"
              a11yLabel="Weekly progress, 40 percent"
              color={c.primary}
              trackColor={withAlpha(c.textPrimary, 0.12)}
            />
          </View>
        </Card>

        <Card variant="oat">
          <SectionHeader icon={ListChecks} title="Today's plan" actionLabel="See all" onAction={() => router.navigate('/plan')} />
          <View>
            {tasks.slice(0, 3).map((t) => (
              <TaskRow key={t.id} task={t} onToggle={() => toggle(t.id)} />
            ))}
          </View>
        </Card>

        <Card variant="focus">
          <SectionHeader icon={Timer} title="Focus session" />
          <Text color="textPrimary">25 minutes on your next task. One thing at a time.</Text>
          <Button label="Start session" onPress={() => router.push('/session/active')} />
        </Card>

        <Card variant="insight">
          <AiBadge label="AI insight" />
          <Text color="onAccent">
            You built a lot this week and read less. A short note on what you built would lock it in.
          </Text>
          <Text variant="caption" color="onAccent">Based on: recent sessions, evidence</Text>
          <View style={styles.actions}>
            <Button label="Accept" variant="secondary" onPress={() => {}} />
            <Button label="Edit" variant="secondary" onPress={() => {}} />
            <Button label="Dismiss" variant="secondary" onPress={() => {}} />
          </View>
          <Text variant="caption" color="onAccent">AI suggestions are guidance, not professional advice.</Text>
        </Card>

        <Card variant="sand">
          <SectionHeader icon={Hammer} title="Recent evidence" />
          <View style={{ gap: spacing.md }}>
            {sampleEvidence.map((e) => (
              <View key={e.id} style={styles.evidenceRow}>
                <Text style={{ flex: 1 }}>{e.title}</Text>
                <StatusChip icon={levelIcon[e.level] ?? BookOpen} label={e.level} />
              </View>
            ))}
          </View>
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.xl, gap: spacing.xl },
  heroRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.lg },
  heroText: { flex: 1, gap: spacing.xs },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  evidenceRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.md },
});