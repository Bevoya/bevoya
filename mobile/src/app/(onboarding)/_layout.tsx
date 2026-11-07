import { Stack } from 'expo-router';
import { colors } from '../../theme/colors';

// Without this, Expo Router would open the alphabetically first screen.
export const unstable_settings = { initialRouteName: 'welcome' };

export default function OnboardingLayout() {
  return (
    <Stack
      screenOptions={{ headerShown: false, animation: 'slide_from_right', contentStyle: { backgroundColor: colors.ivory } }}
    />
  );
}