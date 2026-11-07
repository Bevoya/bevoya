import {
  BookOpen,
  Briefcase,
  Compass,
  GraduationCap,
  Heart,
  Laptop,
  Moon,
  PenTool,
  Shuffle,
  Sprout,
  Sun,
  Sunrise,
  Target,
  Timer,
  type LucideIcon,
} from 'lucide-react-native';
import {
  BEST_TIME_LABELS,
  FOCUS_LABELS,
  USER_TYPE_LABELS,
  type BestTime,
  type Focus,
  type Minutes,
  type UserType,
} from './answers';

export type Option<T extends string | number> = { value: T; title: string; detail?: string; icon: LucideIcon };

export const focusOptions: Option<Focus>[] = [
  { value: 'learning', title: FOCUS_LABELS.learning, detail: 'Skills, exams, courses', icon: BookOpen },
  { value: 'career', title: FOCUS_LABELS.career, detail: 'Roles, projects, portfolio', icon: Briefcase },
  { value: 'productivity', title: FOCUS_LABELS.productivity, detail: 'Focus and daily planning', icon: Target },
  { value: 'health', title: FOCUS_LABELS.health, detail: 'Habits and routines', icon: Heart },
  { value: 'creativity', title: FOCUS_LABELS.creativity, detail: 'Making and sharing', icon: PenTool },
  { value: 'growth', title: FOCUS_LABELS.growth, detail: 'Reflection and balance', icon: Sprout },
];

export const userTypeOptions: Option<UserType>[] = [
  { value: 'student', title: USER_TYPE_LABELS.student, icon: GraduationCap },
  { value: 'young_professional', title: USER_TYPE_LABELS.young_professional, icon: Laptop },
  { value: 'working_adult', title: USER_TYPE_LABELS.working_adult, icon: Briefcase },
  { value: 'creator', title: USER_TYPE_LABELS.creator, icon: PenTool },
  { value: 'other', title: USER_TYPE_LABELS.other, icon: Compass },
];

export const timeOptions: Option<Minutes>[] = [
  { value: 15, title: '15 minutes', detail: 'A light start', icon: Timer },
  { value: 30, title: '30 minutes', detail: 'Steady', icon: Timer },
  { value: 60, title: '1 hour', detail: 'Focused', icon: Timer },
  { value: 120, title: '2 hours', detail: 'Deep work', icon: Timer },
];

export const bestTimeOptions: Option<BestTime>[] = [
  { value: 'morning', title: BEST_TIME_LABELS.morning, detail: 'Before midday', icon: Sunrise },
  { value: 'afternoon', title: BEST_TIME_LABELS.afternoon, detail: 'Midday to evening', icon: Sun },
  { value: 'evening', title: BEST_TIME_LABELS.evening, detail: 'After work or class', icon: Moon },
  { value: 'varies', title: BEST_TIME_LABELS.varies, detail: 'It changes', icon: Shuffle },
];