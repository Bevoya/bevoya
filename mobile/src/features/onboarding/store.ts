import AsyncStorage from '@react-native-async-storage/async-storage';
import { create } from 'zustand';
import { emptyAnswers, sanitizeStored, type Answers } from './answers';

const KEY = 'bevoya.onboarding.v1';

type State = {
  hydrated: boolean;
  completed: boolean; // true once the person has signed in or finished onboarding
  answers: Answers;
  hydrate: () => Promise<void>;
  setAnswer: (patch: Partial<Answers>) => void;
  markCompleted: () => void;
};

function save(state: Pick<State, 'completed' | 'answers'>) {
  AsyncStorage.setItem(KEY, JSON.stringify({ completed: state.completed, answers: state.answers })).catch(() => {});
}

// Setup choices only (no journal text), kept on this phone until the API client sends them after sign-up.
export const useOnboarding = create<State>((set, get) => ({
  hydrated: false,
  completed: false,
  answers: emptyAnswers,
  hydrate: async () => {
    if (get().hydrated) return;
    try {
      const raw = await AsyncStorage.getItem(KEY);
      if (raw) {
        const stored = sanitizeStored(JSON.parse(raw));
        set({ completed: stored.completed, answers: stored.answers });
      }
    } catch {
      // Unreadable or missing data: start fresh.
    }
    set({ hydrated: true });
  },
  setAnswer: (patch) => {
    set({ answers: { ...get().answers, ...patch } });
    save(get());
  },
  markCompleted: () => {
    if (get().completed) return;
    set({ completed: true });
    save(get());
  },
}));