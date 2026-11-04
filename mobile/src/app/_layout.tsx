import {
  Figtree_400Regular,
  Figtree_500Medium,
  Figtree_600SemiBold,
  Figtree_700Bold,
  useFonts,
} from '@expo-google-fonts/figtree';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import { LoadingScreen } from '../components/ui/LoadingScreen';
import { AuthProvider, useAuth } from '../lib/auth';
import { ThemeProvider } from '../theme/ThemeProvider';

void SplashScreen.preventAutoHideAsync();

function Gate({ fontsReady }: { fontsReady: boolean }) {
  const { session, loading } = useAuth();

  // Shown only while fonts and the saved session are really loading.
  if (!fontsReady || loading) {
    return <LoadingScreen />;
  }

  const signedIn = session !== null;

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Protected guard={signedIn}>
        <Stack.Screen name="(tabs)" />
      </Stack.Protected>
      <Stack.Protected guard={!signedIn}>
        <Stack.Screen name="(auth)" />
      </Stack.Protected>
    </Stack>
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
    // The loading screen matches the native splash, so hand over at once.
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