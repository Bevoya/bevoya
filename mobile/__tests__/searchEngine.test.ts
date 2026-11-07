import { groupByKind, normalize, search, tokenize, type SearchItem } from '../src/features/search/engine';

const items: SearchItem<string>[] = [
  { id: 't1', kind: 'task', title: 'Build a document-search endpoint', subtitle: 'Build · 60 min', target: '/plan' },
  { id: 't2', kind: 'task', title: 'Study API authentication', subtitle: 'Learn · 30 min', target: '/plan' },
  { id: 'e1', kind: 'evidence', title: 'Added vector search endpoint', subtitle: 'Built', target: '/' },
  { id: 'p1', kind: 'page', title: 'Export my data', keywords: ['download', 'backup'], target: '/privacy/export' },
  { id: 'p2', kind: 'page', title: 'Café notes', target: '/learn' },
];

const ids = (query: string) => search(items, query).map((r) => r.id);

describe('search engine', () => {
  it('returns nothing for an empty query', () => {
    expect(ids('')).toEqual([]);
    expect(ids('   ')).toEqual([]);
  });

  it('normalizes accents and case', () => {
    expect(normalize('CAFÉ')).toBe('cafe');
    expect(ids('cafe')).toEqual(['p2']);
    expect(ids('CAFÉ')).toEqual(['p2']);
  });

  it('splits words on punctuation', () => {
    expect(tokenize('Document-search, endpoint!')).toEqual(['document', 'search', 'endpoint']);
  });

  it('matches a word prefix', () => {
    expect(ids('docu')[0]).toBe('t1');
  });

  it('needs every word to match', () => {
    expect([...ids('search endpoint')].sort()).toEqual(['e1', 't1']);
    expect(ids('search authentication')).toEqual([]);
  });

  it('tolerates one typo in a longer word', () => {
    expect(ids('authenticaton')).toEqual(['t2']);
  });

  it('finds items by keyword', () => {
    expect(ids('backup')).toEqual(['p1']);
  });

  it('ranks a title match above a subtitle match', () => {
    const two: SearchItem<string>[] = [
      { id: 'a', kind: 'task', title: 'Walk', subtitle: 'Health', target: '' },
      { id: 'b', kind: 'task', title: 'Health check', target: '' },
    ];
    expect(search(two, 'health').map((r) => r.id)).toEqual(['b', 'a']);
  });

  it('respects the limit', () => {
    expect(search(items, 'e', 2).length).toBeLessThanOrEqual(2);
  });

  it('groups results in a fixed order', () => {
    const groups = groupByKind(search(items, 'search'));
    expect(groups.map((g) => g.kind)).toEqual(['task', 'evidence']);
  });
});