import { useEffect, useState } from 'react';
import { Animated, Easing, Image, StyleSheet, View } from 'react-native';
import Svg, { Circle, Defs, RadialGradient, Stop } from 'react-native-svg';
import { useReduceMotion } from '../../hooks/useReduceMotion';
import { withAlpha } from '../../theme/color';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { BrandText } from './BrandText';

const GLOW = 260;
const TILE = 120;

type Props = { fontsReady: boolean; leaving: boolean; onGone: () => void };

// Start screen: a glow that dims and brightens, a light sweep across the leaf tile, then the wordmark.
export function BrandSplash({ fontsReady, leaving, onGone }: Props) {
  const reduce = useReduceMotion();
  const [intro] = useState(() => new Animated.Value(0));
  const [pulse] = useState(() => new Animated.Value(0));
  const [shine] = useState(() => new Animated.Value(0));
  const [text] = useState(() => new Animated.Value(0));
  const [exit] = useState(() => new Animated.Value(1));

  useEffect(() => {
    if (reduce) {
      intro.setValue(1);
      pulse.setValue(1);
      return;
    }
    const introAnim = Animated.timing(intro, {
      toValue: 1,
      duration: 700,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: true,
    });
    const pulseLoop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, { toValue: 1, duration: 1300, easing: Easing.inOut(Easing.sin), useNativeDriver: true }),
        Animated.timing(pulse, { toValue: 0, duration: 1300, easing: Easing.inOut(Easing.sin), useNativeDriver: true }),
      ]),
    );
    const shineLoop = Animated.loop(
      Animated.sequence([
        Animated.delay(700),
        Animated.timing(shine, { toValue: 1, duration: 900, easing: Easing.inOut(Easing.quad), useNativeDriver: true }),
        Animated.delay(900),
        Animated.timing(shine, { toValue: 0, duration: 0, useNativeDriver: true }),
      ]),
    );
    introAnim.start();
    pulseLoop.start();
    shineLoop.start();
    return () => {
      introAnim.stop();
      pulseLoop.stop();
      shineLoop.stop();
    };
  }, [reduce, intro, pulse, shine]);

  useEffect(() => {
    if (!fontsReady) return;
    if (reduce) {
      text.setValue(1);
      return;
    }
    const anim = Animated.timing(text, { toValue: 1, duration: 600, delay: 350, useNativeDriver: true });
    anim.start();
    return () => anim.stop();
  }, [fontsReady, reduce, text]);

  useEffect(() => {
    if (!leaving) return;
    const anim = Animated.timing(exit, { toValue: 0, duration: reduce ? 0 : 350, useNativeDriver: true });
    anim.start(({ finished }) => {
      if (finished) onGone();
    });
    return () => anim.stop();
  }, [leaving, reduce, exit, onGone]);

  return (
    <Animated.View
      style={[styles.root, { opacity: exit }]}
      pointerEvents={leaving ? 'none' : 'auto'}
      accessible
      accessibilityLabel="Bevoya"
    >
      <View style={styles.stage}>
        <Animated.View
          style={[styles.glow, { opacity: pulse.interpolate({ inputRange: [0, 1], outputRange: [0.35, 1] }) }]}
        >
          <Svg width={GLOW} height={GLOW}>
            <Defs>
              <RadialGradient id="glow" cx="50%" cy="50%" r="50%">
                <Stop offset="0%" stopColor={colors.sage} stopOpacity={0.95} />
                <Stop offset="100%" stopColor={colors.sage} stopOpacity={0} />
              </RadialGradient>
            </Defs>
            <Circle cx={GLOW / 2} cy={GLOW / 2} r={GLOW / 2} fill="url(#glow)" />
          </Svg>
        </Animated.View>

        <Animated.View
          style={[
            styles.tile,
            { opacity: intro, transform: [{ scale: intro.interpolate({ inputRange: [0, 1], outputRange: [0.88, 1] }) }] },
          ]}
        >
          <Image
            source={require('../../../assets/images/bevoyalogo.png')}
            style={styles.logo}
            resizeMode="contain"
            accessibilityElementsHidden
            importantForAccessibility="no-hide-descendants"
          />
          <Animated.View
            style={[
              styles.shine,
              {
                transform: [
                  { translateX: shine.interpolate({ inputRange: [0, 1], outputRange: [-60, 160] }) },
                  { rotate: '20deg' },
                ],
              },
            ]}
          />
        </Animated.View>
      </View>

      <Animated.View
        style={[
          styles.textWrap,
          { opacity: text, transform: [{ translateY: text.interpolate({ inputRange: [0, 1], outputRange: [10, 0] }) }] },
        ]}
      >
        <BrandText variant="wordmark" color={colors.forest} style={styles.line}>
          Bevoya
        </BrandText>
        <BrandText style={styles.line}>Your life, moving forward.</BrandText>
      </Animated.View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  root: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: colors.ivory,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stage: { width: GLOW, height: GLOW, alignItems: 'center', justifyContent: 'center' },
  glow: { position: 'absolute', top: 0, left: 0 },
  tile: {
    width: TILE,
    height: TILE,
    borderRadius: 32,
    backgroundColor: colors.sage,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  logo: { width: 72, height: 72 },
  shine: {
    position: 'absolute',
    top: -40,
    width: 36,
    height: TILE + 80,
    backgroundColor: withAlpha(colors.white, 0.55),
  },
  // Full width, so the text is never measured to its exact width and clipped at the end.
  textWrap: { alignSelf: 'stretch', alignItems: 'stretch', gap: spacing.xs, marginTop: spacing.md, paddingHorizontal: spacing.xl },
  line: { textAlign: 'center' },
});