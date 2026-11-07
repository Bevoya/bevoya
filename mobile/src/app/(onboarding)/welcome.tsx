import { useRouter } from 'expo-router';
import { NotebookPen, Hammer, Target, type LucideIcon } from 'lucide-react-native';
import { useEffect, useState } from 'react';
import { Animated, Image, ScrollView, StyleSheet, useWindowDimensions, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { BrandText } from '../../components/ui/BrandText';
import { copy } from '../../features/onboarding/copy';
import { useOnboarding } from '../../features/onboarding/store';
import { OnbButton } from '../../features/onboarding/ui';
import { useReduceMotion } from '../../hooks/useReduceMotion';
import { withAlpha } from '../../theme/color';
import { colors } from '../../theme/colors';
import { radius } from '../../theme/shape';
import { spacing } from '../../theme/spacing';

const slideIcons: LucideIcon[] = [Target, Hammer, NotebookPen];

export default function Welcome() {
  const router = useRouter();
  const { width } = useWindowDimensions();
  const markCompleted = useOnboarding((s) => s.markCompleted);
  const reduce = useReduceMotion();
  const [page, setPage] = useState(0);
  const [fade] = useState(() => new Animated.Value(0));

  useEffect(() => {
    if (reduce) {
      fade.setValue(1);
      return;
    }
    const anim = Animated.timing(fade, { toValue: 1, duration: 600, useNativeDriver: true });
    anim.start();
    return () => anim.stop();
  }, [reduce, fade]);

  const slides = copy.welcome.slides;

  return (
    <SafeAreaView style={styles.root}>
      <Animated.View style={[styles.body, { opacity: fade }]}>
        <View style={styles.brand}>
          <View style={styles.tile}>
            <Image
              source={require('../../../assets/images/bevoyalogo.png')}
              style={styles.logo}
              resizeMode="contain"
              accessibilityElementsHidden
              importantForAccessibility="no-hide-descendants"
            />
          </View>
          <BrandText variant="h1" color={colors.forest} accessibilityRole="header" style={styles.line}>
            Bevoya
          </BrandText>
          <BrandText style={styles.line}>{copy.welcome.tagline}</BrandText>
        </View>

        <ScrollView
          horizontal
          pagingEnabled
          showsHorizontalScrollIndicator={false}
          style={{ flexGrow: 0 }}
          onMomentumScrollEnd={(e) => setPage(Math.round(e.nativeEvent.contentOffset.x / width))}
        >
          {slides.map((slide, i) => {
            const Glyph = slideIcons[i];
            return (
              <View key={slide.title} style={[styles.slide, { width }]}>
                <View style={styles.iconTile}>
                  <Glyph size={40} color={colors.forest} strokeWidth={1.75} />
                </View>
                <BrandText variant="h2" color={colors.forest} style={styles.center}>
                  {slide.title}
                </BrandText>
                <BrandText style={styles.center}>{slide.body}</BrandText>
              </View>
            );
          })}
        </ScrollView>

        <View accessible accessibilityLabel={`Page ${page + 1} of ${slides.length}`} style={styles.dots}>
          {slides.map((slide, i) => (
            <View key={slide.title} style={[styles.dot, i === page && styles.dotActive]} />
          ))}
        </View>
      </Animated.View>

      <View style={styles.footer}>
        <OnbButton label={copy.welcome.start} onPress={() => router.push('/focus')} />
        <OnbButton
          variant="link"
          label={copy.welcome.haveAccount}
          onPress={() => {
            markCompleted();
            router.push('/sign-in');
          }}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.ivory },
  body: { flex: 1, justifyContent: 'space-evenly' },
  // Items stretch to the full width so the brand text is never measured to its exact width.
  brand: { alignItems: 'stretch', gap: spacing.sm, paddingHorizontal: spacing.xl },
  tile: {
    width: 88,
    height: 88,
    borderRadius: 26,
    backgroundColor: colors.sage,
    alignItems: 'center',
    justifyContent: 'center',
    alignSelf: 'center',
  },
  logo: { width: 52, height: 52 },
  line: { textAlign: 'center' },
  slide: { paddingHorizontal: spacing.xxxl, alignItems: 'center', gap: spacing.md },
  iconTile: {
    width: 96,
    height: 96,
    borderRadius: 28,
    backgroundColor: colors.white,
    borderWidth: 1,
    borderColor: withAlpha(colors.charcoal, 0.1),
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.sm,
  },
  center: { textAlign: 'center' },
  dots: { flexDirection: 'row', alignSelf: 'center', gap: spacing.sm },
  dot: { width: 8, height: 8, borderRadius: radius.chip, backgroundColor: withAlpha(colors.charcoal, 0.2) },
  dotActive: { width: 24, backgroundColor: colors.forest },
  footer: { paddingHorizontal: spacing.xl, paddingBottom: spacing.lg, gap: spacing.sm },
});