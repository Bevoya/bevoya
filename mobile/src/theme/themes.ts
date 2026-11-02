import { colors } from './colors';

// Semantic tokens from COLORS.md. Dark values and the status colours are proposals until approved
// and checked on a device. Components use these names, never raw hex values.
export type ThemeColors = {
  background: string;
  surface: string;
  surfaceElevated: string;
  surfaceSelected: string;
  textPrimary: string;
  textSecondary: string;
  primary: string;
  onPrimary: string;
  secondary: string;
  onSecondary: string;
  accentMilestone: string;
  accentLearning: string;
  accentAction: string;
  onAccent: string;
  inputBorder: string;
  cardBorder: string;
  error: string;
  onError: string;
  success: string;
  warningText: string;
  info: string;
};

export type Theme = { name: 'light' | 'dark'; colors: ThemeColors };

export const lightTheme: Theme = {
  name: 'light',
  colors: {
    background: colors.ivory,
    surface: colors.white,
    surfaceElevated: colors.white,
    surfaceSelected: colors.sage,
    textPrimary: colors.charcoal,
    textSecondary: colors.slate,
    primary: colors.forest,
    onPrimary: colors.white,
    secondary: '#2E7D6B',
    onSecondary: colors.white,
    accentMilestone: colors.amber,
    accentLearning: colors.mist,
    accentAction: '#F79F4D',
    onAccent: colors.charcoal,
    inputBorder: colors.slate,
    cardBorder: 'rgba(32, 41, 35, 0.08)',
    error: '#B3261E',
    onError: colors.white,
    success: '#2E6B3F',
    warningText: '#7A4B00',
    info: '#1F5F7A',
  },
};

export const darkTheme: Theme = {
  name: 'dark',
  colors: {
    background: '#121A15',
    surface: '#1B2620',
    surfaceElevated: '#24322A',
    surfaceSelected: '#2C4034',
    textPrimary: '#F1F3EE',
    textSecondary: '#A9B5AC',
    primary: '#8FCBA4',
    onPrimary: '#0F1A14',
    secondary: '#2E7D6B',
    onSecondary: colors.white,
    accentMilestone: colors.amber,
    accentLearning: colors.mist,
    accentAction: '#F79F4D',
    onAccent: colors.charcoal,
    inputBorder: '#6F8577',
    cardBorder: 'rgba(241, 243, 238, 0.12)',
    error: '#F2A29A',
    onError: '#0F1A14',
    success: '#8FCBA4',
    warningText: colors.amber,
    info: colors.mist,
  },
};