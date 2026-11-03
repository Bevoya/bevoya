import { useEffect, useState } from 'react';
import { AccessibilityInfo, Animated, Easing, StyleSheet, View } from 'react-native';
import { colors } from '../../theme/colors';

const logo = require('../../../assets/images/bevoyalogo.png');

const BAR_TRACK = 120;
const BAR_FILL = 40;

export function LoadingScreen() {
  const [breathe] = useState(() => new Animated.Value(0));
  const [slide] = useState(() => new Animated.Value(0));
  const [reduceMotion, setReduceMotion] = useState(false);

  useEffect(() => {
    void AccessibilityInfo.isReduceMotionEnabled().then(setReduceMotion);
  }, []);

  useEffect(() => {
    if (reduceMotion) return;
    const breathing = Animated.loop(
      Animated.sequence([
        Animated.timing(breathe, {
          toValue: 1,
          duration: 1200,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(breathe, {
          toValue: 0,
          duration: 1200,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ]),
    );
    const sliding = Animated.loop(
      Animated.timing(slide, {
        toValue: 1,
        duration: 1400,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    );
    breathing.start();
    sliding.start();
    return () => {
      breathing.stop();
      sliding.stop();
    };
  }, [breathe, slide, reduceMotion]);

  const scale = breathe.interpolate({ inputRange: [0, 1], outputRange: [1, 1.06] });
  const opacity = breathe.interpolate({ inputRange: [0, 1], outputRange: [0.85, 1] });
  const translateX = slide.interpolate({ inputRange: [0, 1], outputRange: [-BAR_FILL, BAR_TRACK] });

  return (
    <View
      style={styles.root}
      accessible
      accessibilityRole="progressbar"
      accessibilityLabel="Loading Bevoya"
    >
      <Animated.Image
        source={logo}
        resizeMode="contain"
        style={[styles.logo, { opacity, transform: [{ scale }] }]}
      />
      <View style={styles.track}>
        <Animated.View style={[styles.fill, { transform: [{ translateX }] }]} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.ivory,
    gap: 32,
  },
  logo: { width: 120, height: 120 },
  track: {
    width: BAR_TRACK,
    height: 4,
    borderRadius: 2,
    backgroundColor: colors.sage,
    overflow: 'hidden',
  },
  fill: { width: BAR_FILL, height: 4, borderRadius: 2, backgroundColor: colors.forest },
});