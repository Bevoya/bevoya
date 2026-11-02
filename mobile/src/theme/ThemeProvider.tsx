import { createContext, useContext, useState, type ReactNode } from 'react';
import { useColorScheme } from 'react-native';
import { darkTheme, lightTheme, type Theme } from './themes';

export type ThemeMode = 'system' | 'light' | 'dark';

type ThemeContextValue = { theme: Theme; mode: ThemeMode; setMode: (mode: ThemeMode) => void };

const ThemeContext = createContext<ThemeContextValue | null>(null);

// Follows the system setting by default; the manual override is not saved yet (Settings comes later).
export function ThemeProvider({ children }: { children: ReactNode }) {
  const system = useColorScheme();
  const [mode, setMode] = useState<ThemeMode>('system');
  const dark = mode === 'dark' || (mode === 'system' && system === 'dark');
  return (
    <ThemeContext.Provider value={{ theme: dark ? darkTheme : lightTheme, mode, setMode }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextValue {
  const value = useContext(ThemeContext);
  if (!value) {
    throw new Error('useTheme must be used inside ThemeProvider');
  }
  return value;
}