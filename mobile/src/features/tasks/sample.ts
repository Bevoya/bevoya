import { create } from 'zustand';

export type TaskCategory = 'learn' | 'build' | 'revise' | 'health';
export type SampleTask = { id: string; title: string; category: TaskCategory; minutes: number; done: boolean };

// Synthetic placeholder data. Real tasks come from the API.
const initial: SampleTask[] = [
  { id: '1', title: 'Build a document-search endpoint', category: 'build', minutes: 60, done: false },
  { id: '2', title: 'Study API authentication', category: 'learn', minutes: 30, done: false },
  { id: '3', title: 'Revise integration mistakes', category: 'revise', minutes: 25, done: true },
  { id: '4', title: 'Take a 20-minute walk', category: 'health', minutes: 20, done: false },
];

type Store = { tasks: SampleTask[]; toggle: (id: string) => void };

// One store, so Home, Plan and Search all see the same tasks.
const useStore = create<Store>((set) => ({
  tasks: initial,
  toggle: (id) => set((s) => ({ tasks: s.tasks.map((t) => (t.id === id ? { ...t, done: !t.done } : t)) })),
}));

export function useSampleTasks() {
  const tasks = useStore((s) => s.tasks);
  const toggle = useStore((s) => s.toggle);
  return { tasks, toggle };
}