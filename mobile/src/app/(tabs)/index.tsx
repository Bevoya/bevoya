import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Button } from '../../components/ui/Button';
import { Text } from '../../components/ui/Text';
import { spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeProvider';

// Temporary design preview (font and colour check on a real phone). Replaced by the real Home screen.
export default function Screen() {
  const { theme, mode, setMode } = useTheme();
  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.colors.background }}>
      <ScrollView contentContainerStyle={{ padding: spacing.xl, gap: spacing.lg }}>
        <Text variant="display">Your life, moving forward.</Text>
        <Text variant="h1">Section title</Text>
        <Text variant="h2">Card title</Text>
        <Text>Body text at 16. Goals, plans, sessions, evidence and reflection.</Text>
        <Text variant="secondary" color="textSecondary">Secondary text at 14 for supporting detail.</Text>
        <Text variant="caption" color="textSecondary">Caption at 12 for labels and timestamps</Text>
        <View style={{ gap: spacing.md }}>
          <Button label="Start session" onPress={() => {}} />
          <Button label="Add task" variant="secondary" onPress={() => {}} />
          <Button label="Skip" variant="tertiary" onPress={() => {}} />
          <Button label="Delete reflection" variant="destructive" onPress={() => {}} />
          <Button label="Saving" loading onPress={() => {}} />
          <Button label="Not available" disabled onPress={() => {}} />
        </View>
        <Button
          label={`Theme: ${mode} (tap to change)`}
          variant="secondary"
          onPress={() => setMode(mode === 'system' ? 'light' : mode === 'light' ? 'dark' : 'system')}
        />
      </ScrollView>
    </SafeAreaView>
  );
}