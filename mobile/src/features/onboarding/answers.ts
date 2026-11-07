export const FOCUS_VALUES = ['learning', 'career', 'productivity', 'health', 'creativity', 'growth'] as const;
export type Focus = (typeof FOCUS_VALUES)[number];
export const FOCUS_LABELS: Record<Focus, string> = {
  learning: 'Learning',
  career: 'Career',
  productivity: 'Productivity',
  health: 'Health and routines',
  creativity: 'Creativity',
  growth: 'Personal growth',
};

// No 'parent': the database check on profiles.user_type does not allow it yet.
export const USER_TYPE_VALUES = ['student', 'young_professional', 'working_adult', 'creator', 'other'] as const;
export type UserType = (typeof USER_TYPE_VALUES)[number];
export const USER_TYPE_LABELS: Record<UserType, string> = {
  student: 'Student',
  young_professional: 'Young professional',
  working_adult: 'Working adult',
  creator: 'Creator or freelancer',
  other: 'Something else',
};

export const MINUTES_VALUES = [15, 30, 60, 120] as const;
export type Minutes = (typeof MINUTES_VALUES)[number];

export const BEST_TIME_VALUES = ['morning', 'afternoon', 'evening', 'varies'] as const;
export type BestTime = (typeof BEST_TIME_VALUES)[number];
export const BEST_TIME_LABELS: Record<BestTime, string> = {
  morning: 'Morning',
  afternoon: 'Afternoon',
  evening: 'Evening',
  varies: 'It varies',
};

export type Answers = {
  focus: Focus | null;
  userType: UserType | null;
  minutes: Minutes | null;
  bestTime: BestTime | null;
  morningPlan: boolean;
  eveningReflection: boolean;
  aiMemory: boolean; // off by default; consent is recorded properly after sign-up
};

export const emptyAnswers: Answers = {
  focus: null,
  userType: null,
  minutes: null,
  bestTime: null,
  morningPlan: false,
  eveningReflection: false,
  aiMemory: false,
};

function pick<T extends string | number>(values: readonly T[], value: unknown): T | null {
  return values.find((v) => v === value) ?? null;
}

// Stored data is untrusted: keep only values we know.
export function sanitizeAnswers(raw: unknown): Answers {
  if (typeof raw !== 'object' || raw === null) return { ...emptyAnswers };
  const r = raw as Record<string, unknown>;
  return {
    focus: pick(FOCUS_VALUES, r.focus),
    userType: pick(USER_TYPE_VALUES, r.userType),
    minutes: pick(MINUTES_VALUES, r.minutes),
    bestTime: pick(BEST_TIME_VALUES, r.bestTime),
    morningPlan: r.morningPlan === true,
    eveningReflection: r.eveningReflection === true,
    aiMemory: r.aiMemory === true,
  };
}

export function sanitizeStored(raw: unknown): { completed: boolean; answers: Answers } {
  if (typeof raw !== 'object' || raw === null) return { completed: false, answers: { ...emptyAnswers } };
  const r = raw as Record<string, unknown>;
  return { completed: r.completed === true, answers: sanitizeAnswers(r.answers) };
}

export function summarize(a: Answers): { label: string; value: string }[] {
  const rows: { label: string; value: string }[] = [];
  if (a.focus) rows.push({ label: 'Focus', value: FOCUS_LABELS[a.focus] });
  if (a.userType) rows.push({ label: 'You are', value: USER_TYPE_LABELS[a.userType] });
  if (a.minutes) rows.push({ label: 'Daily time', value: `${a.minutes} minutes` });
  if (a.bestTime) rows.push({ label: 'Best time', value: BEST_TIME_LABELS[a.bestTime] });
  return rows;
}