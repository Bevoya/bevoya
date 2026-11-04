import { createContext, useContext, type ReactNode } from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';
import { radius } from '../../theme/shape';
import { spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeProvider';
import type { ThemeColors } from '../../theme/themes';

export type CardVariant = 'default' | 'sand' | 'oat' | 'focus' | 'insight' | 'milestone';

// Text colour that has verified contrast on each card fill, in both themes.
const foreground: Record<CardVariant, keyof ThemeColors> = {
  default: 'textPrimary',
  sand: 'textPrimary',
  oat: 'textPrimary',
  focus: 'textPrimary',
  insight: 'onAccent',
  milestone: 'onAccent',
};

type CardInfo = { fg: keyof ThemeColors; tinted: boolean };
const CardContext = createContext<CardInfo>({ fg: 'textPrimary', tinted: false });
export const useCard = () => useContext(CardContext);

type Props = { variant?: CardVariant; children: ReactNode; style?: StyleProp<ViewStyle> };

export function Card({ variant = 'default', children, style }: Props) {
  const { theme } = useTheme();
  const c = theme.colors;
  const fill = {
    default: c.surface,
    sand: c.accentWarm,
    oat: c.accentOat,
    focus: c.surfaceSelected,
    insight: c.accentLearning,
    milestone: c.accentMilestone,
  }[variant];
  const bordered = variant === 'default' || variant === 'sand' || variant === 'oat';
  return (
    <CardContext.Provider value={{ fg: foreground[variant], tinted: variant !== 'default' }}>
      <View
        style={[
          styles.card,
          { backgroundColor: fill, borderColor: bordered ? c.cardBorder : 'transparent' },
          style,
        ]}
      >
        {children}
      </View>
    </CardContext.Provider>
  );
}

// A very light shadow lifts the cream cards off the white page.
const styles = StyleSheet.create({
  card: {
    borderRadius: radius.card,
    borderWidth: 1,
    padding: spacing.lg,
    gap: spacing.md,
    shadowColor: '#000000',
    shadowOpacity: 0.06,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
    elevation: 1,
  },
});