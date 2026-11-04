import { Image, StyleSheet, View } from 'react-native';
import { spacing } from '../../theme/spacing';
import { Text } from './Text';
import { useCard } from './Card';

export function AiBadge({ label }: { label: string }) {
  const { fg } = useCard();
  return (
    <View style={styles.row}>
      <Image
        source={require('../../../assets/images/bevoyalogo.png')}
        style={styles.mark}
        resizeMode="contain"
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
      />
      <Text variant="caption" color={fg}>
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  mark: { width: 24, height: 24 },
});