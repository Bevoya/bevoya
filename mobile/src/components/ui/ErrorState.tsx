import { CircleAlert } from 'lucide-react-native';
import { StyleSheet, View } from 'react-native';
import { spacing } from '../../theme/spacing';
import { Button } from './Button';
import { Icon } from './Icon';
import { Text } from './Text';

type Props = { message: string; retryLabel: string; onRetry: () => void };

export function ErrorState({ message, retryLabel, onRetry }: Props) {
  return (
    <View style={styles.wrap} accessibilityLiveRegion="polite">
      <Icon icon={CircleAlert} size={24} color="error" />
      <Text color="error" style={styles.message}>
        {message}
      </Text>
      <Button label={retryLabel} variant="secondary" onPress={onRetry} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { alignItems: 'center', gap: spacing.lg, padding: spacing.xxl },
  message: { textAlign: 'center' },
});