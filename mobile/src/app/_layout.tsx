import {
  Figtree_400Regular,
  Figtree_500Medium,
  Figtree_600SemiBold,
  Figtree_700Bold,
  useFonts,
} from '@expo-google-fonts/figtree';
import { Stack, useRootNavigationState, useRouter, useSegments } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useCallback, useEffect, useState } from 'react';
import { View } from 'react-native';
import { BrandSplash } from '../components/ui/BrandSplash';
import { useOnboarding } from '../features/onboarding/store';
import { useReduceMotion } from '../hooks/useReduceMotion';
import { AuthProvider, useAuth } from '../lib/auth';
import { colors } from '../theme/colors';
import { ThemeProvider } from '../theme/ThemeProvider';

void SplashScreen.preventAutoHideAsync();

// The start screen stays at least this long so the animation is seen. Set to 0 to remove the delay.
const MIN_SPLASH_MS = 2500;

function Gate({ fontsReady }: { fontsReady: boolean }) {
  const { session, loading } = useAuth();
  const reduceMotion = useReduceMotion();
  const router = useRouter();
  const segments = useSegments();
  const navState = useRootNavigationState();
  const hydrate = useOnboarding((s) => s.hydrate);
  const hydrated = useOnboarding((s) => s.hydrated);
  const completed = useOnboarding((s) => s.completed);
  const markCompleted = useOnboarding((s) => s.markCompleted);
  const [minElapsed, setMinElapsed] = useState(false);
  const [splashGone, setSplashGone] = useState(false);
  const handleGone = useCallback(() => setSplashGone(true), []);

  const ready = fontsReady && !loading && hydrated;
  const signedIn = session !== null;
  const group = String(segments[0] ?? '');

  useEffect(() => {
    void hydrate();
    const timer = setTimeout(() => setMinElapsed(true), MIN_SPLASH_MS);
    return () => clearTimeout(timer);
  }, [hydrate]);

  // Anyone with a session has used the app: never show them onboarding or sign-up again after sign-out.
  useEffect(() => {
    if (session && hydrated && !completed) markCompleted();
  }, [session, hydrated, completed, markCompleted]);

  // A brand-new, signed-out person always starts at Welcome, whatever screen the stack opened first.
  useEffect(() => {
    if (!ready || !navState?.key) return;
    if (__DEV__) console.log('[gate]', { signedIn, completed, group });
    if (!signedIn && !completed && group !== '(onboarding)' && group !== 'legal') {
      router.replace('/welcome');
    }
  }, [ready, navState?.key, signedIn, completed, group, router]);

  return (
    <View style={{ flex: 1, backgroundColor: colors.ivory }}>
      {ready ? (
        <Stack screenOptions={{ headerShown: false }}>
          <Stack.Protected guard={signedIn}>
            <Stack.Screen name="(tabs)" />
            <Stack.Screen name="search" options={{ animation: 'fade' }} />
          </Stack.Protected>
          <Stack.Protected guard={!signedIn}>
            <Stack.Screen name="(auth)" />
            <Stack.Screen name="(onboarding)" />
          </Stack.Protected>
          <Stack.Screen name="legal/terms" />
          <Stack.Screen name="legal/privacy" />
        </Stack>
      ) : null}
      {splashGone ? null : (
        <BrandSplash
          fontsReady={fontsReady}
          leaving={ready && (minElapsed || reduceMotion)}
          onGone={handleGone}
        />
      )}
    </View>
  );
}

export default function RootLayout() {
  const [fontsLoaded, fontError] = useFonts({
    Figtree_400Regular,
    Figtree_500Medium,
    Figtree_600SemiBold,
    Figtree_700Bold,
  });

  useEffect(() => {
    // The branded start screen takes over from the native splash at once.
    void SplashScreen.hideAsync();
  }, []);

  return (
    <AuthProvider>
      <ThemeProvider>
        <Gate fontsReady={fontsLoaded || !!fontError} />
      </ThemeProvider>
    </AuthProvider>
  );
}