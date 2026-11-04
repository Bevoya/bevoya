import { Tabs } from 'expo-router';
import { GraduationCap, House, ListChecks, NotebookPen, User, type LucideIcon } from 'lucide-react-native';
import { useTheme } from '../../theme/ThemeProvider';
import { fonts } from '../../theme/typography';

type TabColors = { active: string; inactive: string };

function tab(title: string, Glyph: LucideIcon, colors: TabColors) {
  return {
    title,
    // The selected tab also gets a heavier stroke, so selection is not colour alone.
    tabBarIcon: ({ focused }: { focused: boolean }) => (
      <Glyph
        size={24}
        color={focused ? colors.active : colors.inactive}
        strokeWidth={focused ? 2.5 : 1.75}
      />
    ),
  };
}

export default function TabsLayout() {
  const { theme } = useTheme();
  const c = theme.colors;
  const colors: TabColors = { active: c.primary, inactive: c.textSecondary };
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: c.primary,
        tabBarInactiveTintColor: c.textSecondary,
        tabBarStyle: { backgroundColor: c.surface, borderTopColor: c.cardBorder },
        tabBarLabelStyle: { fontFamily: fonts.medium, fontSize: 12 },
      }}
    >
      <Tabs.Screen name="index" options={tab('Home', House, colors)} />
      <Tabs.Screen name="plan" options={tab('Plan', ListChecks, colors)} />
      <Tabs.Screen name="learn" options={tab('Learn', GraduationCap, colors)} />
      <Tabs.Screen name="journal" options={tab('Journal', NotebookPen, colors)} />
      <Tabs.Screen name="profile" options={tab('Profile', User, colors)} />
    </Tabs>
  );
}