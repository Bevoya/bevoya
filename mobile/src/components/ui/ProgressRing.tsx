import { StyleSheet, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import { fonts } from '../../theme/typography';
import type { ThemeColors } from '../../theme/themes';
import { clamp01, ringOffset } from './ringMath';
import { Text } from './Text';

type Props = {
  value: number; // 0 to 1
  label: string; // text in the middle, for example "40%"
  a11yLabel: string; // full sentence for screen readers
  color: string;
  trackColor: string;
  textColor?: keyof ThemeColors;
  size?: number;
  strokeWidth?: number;
};

export function ProgressRing({
  value,
  label,
  a11yLabel,
  color,
  trackColor,
  textColor = 'textPrimary',
  size = 72,
  strokeWidth = 8,
}: Props) {
  const r = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * r;
  const mid = size / 2;
  return (
    <View
      accessible
      accessibilityRole="progressbar"
      accessibilityLabel={a11yLabel}
      accessibilityValue={{ min: 0, max: 100, now: Math.round(clamp01(value) * 100) }}
      style={{ width: size, height: size }}
    >
      <Svg width={size} height={size}>
        <Circle cx={mid} cy={mid} r={r} stroke={trackColor} strokeWidth={strokeWidth} fill="none" />
        <Circle
          cx={mid}
          cy={mid}
          r={r}
          stroke={color}
          strokeWidth={strokeWidth}
          fill="none"
          strokeLinecap="round"
          strokeDasharray={`${circumference} ${circumference}`}
          strokeDashoffset={ringOffset(circumference, value)}
          rotation={-90}
          origin={`${mid}, ${mid}`}
        />
      </Svg>
      <View style={styles.center} pointerEvents="none">
        <Text variant="secondary" color={textColor} maxFontSizeMultiplier={1.3} style={{ fontFamily: fonts.semibold }}>
          {label}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  center: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center',
  },
});