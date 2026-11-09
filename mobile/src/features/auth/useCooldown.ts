import { useCallback, useEffect, useState } from 'react';

// Counts down whole seconds. Used to space out "send a new code" requests.
export function useCooldown(initialSeconds = 0) {
  const [remaining, setRemaining] = useState(initialSeconds);

  useEffect(() => {
    if (remaining <= 0) return;
    const id = setTimeout(() => setRemaining((seconds) => seconds - 1), 1000);
    return () => clearTimeout(id);
  }, [remaining]);

  const start = useCallback((seconds: number) => setRemaining(seconds), []);
  return { remaining, start };
}