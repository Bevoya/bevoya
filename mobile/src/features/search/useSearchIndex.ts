import type { Href } from 'expo-router';
import { useMemo } from 'react';
import { sampleEvidence, sampleGoals, sampleNotes } from '../content/sample';
import { useSampleTasks } from '../tasks/sample';
import type { SearchItem } from './engine';

const pages: SearchItem<Href>[] = [
  { id: 'p-home', kind: 'page', title: 'Home', keywords: ['today', 'dashboard'], target: '/' },
  { id: 'p-plan', kind: 'page', title: 'Plan', keywords: ['tasks', 'todo', 'daily'], target: '/plan' },
  { id: 'p-learn', kind: 'page', title: 'Learn', keywords: ['notes', 'quiz', 'skills'], target: '/learn' },
  { id: 'p-journal', kind: 'page', title: 'Journal', keywords: ['reflection', 'diary', 'mood'], target: '/journal' },
  { id: 'p-profile', kind: 'page', title: 'Profile', keywords: ['settings', 'account', 'sign out'], target: '/profile' },
  { id: 'p-privacy', kind: 'page', title: 'Privacy centre', keywords: ['data', 'consent'], target: '/privacy' },
  { id: 'p-ai', kind: 'page', title: 'AI memory', keywords: ['ai', 'memory', 'journal retrieval'], target: '/privacy/ai-memory' },
  { id: 'p-export', kind: 'page', title: 'Export my data', keywords: ['download', 'backup'], target: '/privacy/export' },
  { id: 'p-delete', kind: 'page', title: 'Delete account', keywords: ['remove', 'erase'], target: '/privacy/delete-account' },
  { id: 'p-weekly', kind: 'page', title: 'Weekly review', keywords: ['progress', 'review'], target: '/progress/weekly' },
  { id: 'p-progress', kind: 'page', title: 'Progress', keywords: ['analytics', 'stats'], target: '/progress' },
];

const label = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);

// Everything searchable lives on the device. Queries are never logged or sent anywhere.
export function useSearchIndex(): SearchItem<Href>[] {
  const { tasks } = useSampleTasks();
  return useMemo(
    () => [
      ...tasks.map(
        (t): SearchItem<Href> => ({
          id: `task-${t.id}`,
          kind: 'task',
          title: t.title,
          subtitle: `${label(t.category)} · ${t.minutes} min${t.done ? ' · Done' : ''}`,
          target: '/plan',
        }),
      ),
      ...sampleGoals.map(
        (g): SearchItem<Href> => ({
          id: `goal-${g.id}`,
          kind: 'goal',
          title: g.title,
          subtitle: g.detail,
          target: { pathname: '/goals/[id]', params: { id: g.id } },
        }),
      ),
      ...sampleEvidence.map(
        (e): SearchItem<Href> => ({ id: `ev-${e.id}`, kind: 'evidence', title: e.title, subtitle: e.level, target: '/' }),
      ),
      ...sampleNotes.map(
        (n): SearchItem<Href> => ({ id: `note-${n.id}`, kind: 'note', title: n.title, subtitle: n.detail, target: '/learn' }),
      ),
      ...pages,
    ],
    [tasks],
  );
}