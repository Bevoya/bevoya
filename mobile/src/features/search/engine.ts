export type SearchKind = 'task' | 'goal' | 'evidence' | 'note' | 'page';

export type SearchItem<R = unknown> = {
  id: string;
  kind: SearchKind;
  title: string;
  subtitle?: string;
  keywords?: string[];
  target: R;
};

export type SearchResult<R = unknown> = SearchItem<R> & { score: number };

// Lower case, accents removed.
export function normalize(text: string): string {
  return text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

export function tokenize(text: string): string[] {
  return normalize(text)
    .split(/[\s\-_.,;:!?()"'/\\·]+/)
    .filter(Boolean);
}

// True when a and b differ by at most one insertion, deletion or substitution.
function withinOneEdit(a: string, b: string): boolean {
  if (a === b) return true;
  const la = a.length;
  const lb = b.length;
  if (Math.abs(la - lb) > 1) return false;
  let i = 0;
  let j = 0;
  let edits = 0;
  while (i < la && j < lb) {
    if (a[i] === b[j]) {
      i++;
      j++;
      continue;
    }
    if (++edits > 1) return false;
    if (la > lb) i++;
    else if (lb > la) j++;
    else {
      i++;
      j++;
    }
  }
  return edits + (la - i) + (lb - j) <= 1;
}

function tokenScore(token: string, words: string[], text: string): number {
  let best = 0;
  for (const w of words) {
    if (w === token) return 100;
    if (w.startsWith(token)) best = Math.max(best, 80);
    else if (token.length >= 4 && w.length >= 4 && withinOneEdit(token, w)) best = Math.max(best, 25);
  }
  if (best < 50 && text.includes(token)) best = Math.max(best, 50);
  return best;
}

// Every query word must match somewhere; title matches count most.
function scoreItem(item: SearchItem, tokens: string[], query: string): number {
  const fields = [
    { text: normalize(item.title), weight: 1 },
    { text: normalize((item.keywords ?? []).join(' ')), weight: 0.5 },
    { text: normalize(item.subtitle ?? ''), weight: 0.3 },
  ]
    .filter((f) => f.text.length > 0)
    .map((f) => ({ ...f, words: tokenize(f.text) }));

  let total = 0;
  for (const token of tokens) {
    let best = 0;
    for (const f of fields) {
      best = Math.max(best, tokenScore(token, f.words, f.text) * f.weight);
    }
    if (best === 0) return 0;
    total += best;
  }
  if (normalize(item.title).startsWith(query)) total += 20;
  return total;
}

export function search<R>(items: SearchItem<R>[], query: string, limit = 30): SearchResult<R>[] {
  const tokens = tokenize(query);
  if (tokens.length === 0) return [];
  const normalized = normalize(query).trim();
  const results: SearchResult<R>[] = [];
  for (const item of items) {
    const score = scoreItem(item, tokens, normalized);
    if (score > 0) results.push({ ...item, score });
  }
  results.sort((a, b) => b.score - a.score || a.title.localeCompare(b.title));
  return results.slice(0, limit);
}

const order: SearchKind[] = ['task', 'goal', 'evidence', 'note', 'page'];

export function groupByKind<R>(results: SearchResult<R>[]) {
  return order
    .map((kind) => ({ kind, items: results.filter((r) => r.kind === kind) }))
    .filter((g) => g.items.length > 0);
}