// Pure, so it can be tested without rendering.
export function clamp01(value: number): number {
  return Math.min(1, Math.max(0, Number.isFinite(value) ? value : 0));
}

export function ringOffset(circumference: number, value: number): number {
  return circumference * (1 - clamp01(value));
}