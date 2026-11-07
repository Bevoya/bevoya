import {
  emptyAnswers,
  sanitizeAnswers,
  sanitizeStored,
  summarize,
  USER_TYPE_VALUES,
} from '../src/features/onboarding/answers';
import { privacySections, termsSections } from '../src/features/legal/content';

describe('onboarding answers', () => {
  it('keeps valid values', () => {
    const result = sanitizeAnswers({
      focus: 'career',
      userType: 'student',
      minutes: 30,
      bestTime: 'evening',
      morningPlan: true,
      eveningReflection: false,
      aiMemory: true,
    });
    expect(result).toEqual({
      focus: 'career',
      userType: 'student',
      minutes: 30,
      bestTime: 'evening',
      morningPlan: true,
      eveningReflection: false,
      aiMemory: true,
    });
  });

  it('drops values it does not know', () => {
    const result = sanitizeAnswers({ focus: 'money', userType: 'parent', minutes: 45, bestTime: 'noon' });
    expect(result.focus).toBeNull();
    expect(result.userType).toBeNull();
    expect(result.minutes).toBeNull();
    expect(result.bestTime).toBeNull();
  });

  it('only treats true as true', () => {
    const result = sanitizeAnswers({ morningPlan: 'yes', eveningReflection: 1, aiMemory: 'true' });
    expect(result.morningPlan).toBe(false);
    expect(result.eveningReflection).toBe(false);
    expect(result.aiMemory).toBe(false);
  });

  it('survives garbage', () => {
    expect(sanitizeAnswers(null)).toEqual(emptyAnswers);
    expect(sanitizeAnswers('text')).toEqual(emptyAnswers);
    expect(sanitizeStored(undefined)).toEqual({ completed: false, answers: emptyAnswers });
  });

  it('reads the completed flag strictly', () => {
    expect(sanitizeStored({ completed: true, answers: {} }).completed).toBe(true);
    expect(sanitizeStored({ completed: 'true' }).completed).toBe(false);
  });

  it('does not offer a parent type until the database allows it', () => {
    expect(USER_TYPE_VALUES).not.toContain('parent');
  });

  it('summarises only what was answered, in order', () => {
    expect(summarize(emptyAnswers)).toEqual([]);
    const rows = summarize({ ...emptyAnswers, focus: 'learning', minutes: 60 });
    expect(rows).toEqual([
      { label: 'Focus', value: 'Learning' },
      { label: 'Daily time', value: '60 minutes' },
    ]);
  });
});

describe('legal content', () => {
  it.each([
    ['terms', termsSections],
    ['privacy', privacySections],
  ])('%s has headings and text in every section', (_name, sections) => {
    expect(sections.length).toBeGreaterThan(5);
    for (const section of sections) {
      expect(section.heading.length).toBeGreaterThan(0);
      expect(section.paragraphs.length).toBeGreaterThan(0);
    }
  });
});