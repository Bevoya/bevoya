import { useRouter, type Href } from 'expo-router';
import {
  BookOpen,
  ChevronLeft,
  ChevronRight,
  Compass,
  Hammer,
  ListChecks,
  Search,
  Target,
  X,
  type LucideIcon,
} from 'lucide-react-native';
import { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Card, useCard } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { Icon } from '../components/ui/Icon';
import { SectionHeader } from '../components/ui/SectionHeader';
import { Text } from '../components/ui/Text';
import { groupByKind, search, type SearchItem, type SearchKind } from '../features/search/engine';
import { useSearchIndex } from '../features/search/useSearchIndex';
import { minTouch } from '../theme/shape';
import { spacing } from '../theme/spacing';
import { useTheme } from '../theme/ThemeProvider';
import { MAX_FONT_SCALE, typography } from '../theme/typography';

const kindMeta: Record<SearchKind, { title: string; icon: LucideIcon }> = {
  task: { title: 'Tasks', icon: ListChecks },
  goal: { title: 'Goals', icon: Target },
  evidence: { title: 'Evidence', icon: Hammer },
  note: { title: 'Notes', icon: BookOpen },
  page: { title: 'Go to', icon: Compass },
};

function Row({ item, onOpen }: { item: SearchItem<Href>; onOpen: (target: Href) => void }) {
  const { fg } = useCard();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={item.subtitle ? `${item.title}, ${item.subtitle}` : item.title}
      onPress={() => onOpen(item.target)}
      style={styles.row}
    >
      <View style={{ flex: 1 }}>
        <Text color={fg}>{item.title}</Text>
        {item.subtitle ? (
          <Text variant="secondary" color={fg}>
            {item.subtitle}
          </Text>
        ) : null}
      </View>
      <Icon icon={ChevronRight} size={16} color={fg} />
    </Pressable>
  );
}

export default function SearchScreen() {
  const { theme } = useTheme();
  const c = theme.colors;
  const router = useRouter();
  const items = useSearchIndex();
  const [query, setQuery] = useState('');
  const results = useMemo(() => search(items, query), [items, query]);
  const groups = useMemo(() => groupByKind(results), [results]);
  const trimmed = query.trim();
  const open = (target: Href) => router.navigate(target);

  return (
    <SafeAreaView edges={['top']} style={{ flex: 1, backgroundColor: c.background }}>
      <View style={styles.bar}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Back"
          onPress={() => router.back()}
          style={styles.iconButton}
        >
          <Icon icon={ChevronLeft} size={24} color="primary" />
        </Pressable>
        <View style={[styles.field, { backgroundColor: c.surface, borderColor: c.inputBorder }]}>
          <Icon icon={Search} color="textSecondary" />
          <TextInput
            value={query}
            onChangeText={setQuery}
            autoFocus
            autoCorrect={false}
            autoCapitalize="none"
            returnKeyType="search"
            maxLength={100}
            placeholder="Search tasks, notes, goals…"
            placeholderTextColor={c.textSecondary}
            accessibilityLabel="Search Bevoya"
            maxFontSizeMultiplier={MAX_FONT_SCALE}
            style={[typography.body, styles.input, { color: c.textPrimary }]}
          />
          {query.length > 0 ? (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Clear search"
              onPress={() => setQuery('')}
              style={styles.iconButton}
              hitSlop={8}
            >
              <Icon icon={X} color="textSecondary" />
            </Pressable>
          ) : null}
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {trimmed.length === 0 ? (
          <Card variant="oat">
            <SectionHeader icon={Compass} title="Jump to" />
            {items
              .filter((i) => i.kind === 'page')
              .slice(0, 6)
              .map((i) => (
                <Row key={i.id} item={i} onOpen={open} />
              ))}
          </Card>
        ) : results.length === 0 ? (
          <EmptyState icon={Search} message={`No results for “${trimmed}”. Try a shorter word.`} />
        ) : (
          <>
            <Text variant="caption" color="textSecondary" accessibilityLiveRegion="polite">
              {results.length} {results.length === 1 ? 'result' : 'results'}
            </Text>
            {groups.map((g, index) => (
              <Card key={g.kind} variant={index % 2 === 0 ? 'default' : 'sand'}>
                <SectionHeader icon={kindMeta[g.kind].icon} title={kindMeta[g.kind].title} />
                {g.items.map((r) => (
                  <Row key={r.id} item={r} onOpen={open} />
                ))}
              </Card>
            ))}
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  bar: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, paddingHorizontal: spacing.md, paddingVertical: spacing.sm },
  iconButton: { width: minTouch, height: minTouch, alignItems: 'center', justifyContent: 'center' },
  field: {
    flex: 1,
    minHeight: minTouch,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingLeft: spacing.lg,
    borderRadius: 24,
    borderWidth: 1,
  },
  input: { flex: 1, paddingVertical: spacing.md },
  content: { padding: spacing.xl, gap: spacing.xl },
  row: { minHeight: 56, flexDirection: 'row', alignItems: 'center', gap: spacing.md, paddingVertical: spacing.sm },
});