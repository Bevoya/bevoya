import { useRouter, type Href } from 'expo-router';
import { Check, type LucideIcon } from 'lucide-react-native';
import type { ReactNode } from 'react';
import { Pressable, ScrollView, StyleSheet, Switch, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { BackButton } from '../../components/ui/BackButton';
import { BrandText } from '../../components/ui/BrandText';
import { withAlpha } from '../../theme/color';
import { colors } from '../../theme/colors';
import { minTouch, radius } from '../../theme/shape';
import { spacing } from '../../theme/spacing';
import { fonts } from '../../theme/typography';
import { copy } from './copy';
import type { Option } from './options';

const hairline = withAlpha(colors.charcoal, 0.15);

// Same job as AuthButton and Button; merge the three later (tracker task).
export function OnbButton({
  label,
  onPress,
  variant = 'primary',
  disabled = false,
  hint,
}: {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'link';
  disabled?: boolean;
  hint?: string;
}) {
  const primary = variant === 'primary';
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityHint={disabled ? hint : undefined}
      accessibilityState={{ disabled }}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        primary && styles.buttonPrimary,
        { opacity: disabled ? 0.45 : pressed ? 0.85 : 1 },
      ]}
    >
      <BrandText color={primary ? colors.white : colors.forest} style={{ fontFamily: fonts.semibold }}>
        {label}
      </BrandText>
    </Pressable>
  );
}

// Selected state is a 2 dp Forest border plus a check icon, never fill alone (COLORS.md rule 4).
export function OptionCard<T extends string | number>({
  option,
  selected,
  onPress,
}: {
  option: Option<T>;
  selected: boolean;
  onPress: () => void;
}) {
  const Glyph = option.icon;
  return (
    <Pressable
      accessibilityRole="radio"
      accessibilityState={{ checked: selected }}
      accessibilityLabel={option.detail ? `${option.title}, ${option.detail}` : option.title}
      onPress={onPress}
      style={[styles.card, selected && styles.cardSelected]}
    >
      <View style={[styles.chip, { backgroundColor: selected ? colors.white : colors.sage }]}>
        <Glyph size={22} color={colors.forest} strokeWidth={1.75} />
      </View>
      <View style={{ flex: 1 }}>
        <BrandText style={{ fontFamily: fonts.semibold }}>{option.title}</BrandText>
        {option.detail ? <BrandText variant="secondary">{option.detail}</BrandText> : null}
      </View>
      {selected ? <Check size={22} color={colors.forest} strokeWidth={2.5} /> : <View style={styles.ring} />}
    </Pressable>
  );
}

export function ToggleRow({
  icon: Glyph,
  title,
  detail,
  value,
  onValueChange,
}: {
  icon: LucideIcon;
  title: string;
  detail: string;
  value: boolean;
  onValueChange: (next: boolean) => void;
}) {
  return (
    <Pressable
      accessibilityRole="switch"
      accessibilityState={{ checked: value }}
      accessibilityLabel={title}
      accessibilityHint={detail}
      onPress={() => onValueChange(!value)}
      style={styles.card}
    >
      <View style={[styles.chip, { backgroundColor: colors.sage }]}>
        <Glyph size={22} color={colors.forest} strokeWidth={1.75} />
      </View>
      <View style={{ flex: 1 }}>
        <BrandText style={{ fontFamily: fonts.semibold }}>{title}</BrandText>
        <BrandText variant="secondary">{detail}</BrandText>
      </View>
      <View pointerEvents="none" importantForAccessibility="no-hide-descendants">
        <Switch value={value} trackColor={{ false: colors.slate, true: colors.forest }} thumbColor={colors.white} />
      </View>
    </Pressable>
  );
}

export function OnboardingShell({
  step,
  total,
  title,
  subtitle,
  children,
  footer,
}: {
  step: number;
  total: number;
  title: string;
  subtitle?: string;
  children: ReactNode;
  footer: ReactNode;
}) {
  const router = useRouter();
  const percent = `${Math.round((step / total) * 100)}%` as `${number}%`;
  return (
    <SafeAreaView style={styles.root}>
      <View style={styles.top}>
        <BackButton onPress={() => (router.canGoBack() ? router.back() : router.replace('/welcome'))} />
        <View
          style={styles.track}
          accessible
          accessibilityRole="progressbar"
          accessibilityLabel={`Step ${step} of ${total}`}
          accessibilityValue={{ min: 0, max: total, now: step }}
        >
          <View style={[styles.fill, { width: percent }]} />
        </View>
        <View style={{ width: minTouch }} />
      </View>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <View style={{ gap: spacing.sm }}>
          <BrandText variant="h1" color={colors.forest} accessibilityRole="header">
            {title}
          </BrandText>
          {subtitle ? <BrandText>{subtitle}</BrandText> : null}
        </View>
        {children}
      </ScrollView>
      <View style={styles.footer}>{footer}</View>
    </SafeAreaView>
  );
}

export function SingleChoiceStep<T extends string | number>({
  step,
  total,
  title,
  subtitle,
  options,
  selected,
  onSelect,
  next,
}: {
  step: number;
  total: number;
  title: string;
  subtitle: string;
  options: Option<T>[];
  selected: T | null;
  onSelect: (value: T) => void;
  next: Href;
}) {
  const router = useRouter();
  return (
    <OnboardingShell
      step={step}
      total={total}
      title={title}
      subtitle={subtitle}
      footer={
        <OnbButton
          label={copy.continue}
          disabled={selected === null}
          hint={copy.pickFirst}
          onPress={() => router.push(next)}
        />
      }
    >
      <View accessibilityRole="radiogroup" style={{ gap: spacing.md }}>
        {options.map((option) => (
          <OptionCard
            key={String(option.value)}
            option={option}
            selected={selected === option.value}
            onPress={() => onSelect(option.value)}
          />
        ))}
      </View>
    </OnboardingShell>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.ivory },
  top: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: spacing.md, gap: spacing.sm },
  track: { flex: 1, height: 6, borderRadius: 3, backgroundColor: withAlpha(colors.charcoal, 0.12), overflow: 'hidden' },
  fill: { height: 6, borderRadius: 3, backgroundColor: colors.forest },
  content: { padding: spacing.xl, gap: spacing.xl },
  footer: { paddingHorizontal: spacing.xl, paddingBottom: spacing.lg, paddingTop: spacing.sm, gap: spacing.sm },
  button: {
    minHeight: minTouch,
    paddingHorizontal: spacing.xl,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonPrimary: { backgroundColor: colors.forest },
  card: {
    minHeight: 64,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    padding: spacing.md,
    borderRadius: radius.card,
    borderWidth: 2,
    borderColor: hairline,
    backgroundColor: colors.white,
  },
  cardSelected: { backgroundColor: colors.sage, borderColor: colors.forest },
  chip: { width: 44, height: 44, borderRadius: 14, alignItems: 'center', justifyContent: 'center' },
  ring: { width: 22, height: 22, borderRadius: 11, borderWidth: 2, borderColor: colors.slate },
});