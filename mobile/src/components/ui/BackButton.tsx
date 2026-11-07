import { ChevronLeft } from 'lucide-react-native';
import { Pressable, StyleSheet } from 'react-native';
import { colors } from '../../theme/colors';
import { minTouch } from '../../theme/shape';

export function BackButton({ onPress, label = 'Back' }: { onPress: () => void; label?: string }) {
  return (
    <Pressable accessibilityRole="button" accessibilityLabel={label} onPress={onPress} hitSlop={8} style={styles.btn}>
      <ChevronLeft size={28} color={colors.forest} strokeWidth={1.75} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  btn: { width: minTouch, height: minTouch, alignItems: 'center', justifyContent: 'center' },
});