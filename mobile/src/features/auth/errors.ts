// Maps Supabase auth errors to a small set of problems, then to plain messages.
// Codes are checked, never error text, so a message never leaks which accounts exist on sign-in.
export type AuthProblem =
  | 'invalid_credentials'
  | 'email_not_confirmed'
  | 'user_exists'
  | 'weak_password'
  | 'rate_limited'
  | 'offline'
  | 'unknown';

type ErrorLike = { code?: string; name?: string; status?: number } | null | undefined;

export function classifyAuthError(error: ErrorLike): AuthProblem {
  if (!error) return 'unknown';
  const { code, name, status } = error;
  if (code === 'invalid_credentials') return 'invalid_credentials';
  if (code === 'email_not_confirmed') return 'email_not_confirmed';
  if (code === 'user_already_exists' || code === 'email_exists') return 'user_exists';
  if (code === 'weak_password') return 'weak_password';
  if (code === 'over_request_rate_limit' || code === 'over_email_send_rate_limit' || status === 429) {
    return 'rate_limited';
  }
  if (name === 'AuthRetryableFetchError' || status === 0) return 'offline';
  return 'unknown';
}

// When confirmation is on and the email already exists, Supabase answers with a user that has no identities.
export function isExistingSignUp(data: { user?: { identities?: unknown[] | null } | null } | null | undefined): boolean {
  const identities = data?.user?.identities;
  return Array.isArray(identities) && identities.length === 0;
}

export type AuthContext = 'sign_in' | 'sign_up';

export function authMessage(problem: AuthProblem, context: AuthContext): string {
  switch (problem) {
    case 'invalid_credentials':
      return 'That email and password don’t match. Check them and try again.';
    case 'email_not_confirmed':
      return 'Confirm your email first. Check your inbox for the link.';
    case 'user_exists':
      return 'An account with this email already exists. Sign in instead.';
    case 'weak_password':
      return 'That password is too weak. Try a longer one.';
    case 'rate_limited':
      return 'Too many attempts. Please try again in a few minutes.';
    case 'offline':
      return 'We couldn’t reach the server. Check your connection and try again.';
    default:
      return context === 'sign_in'
        ? 'We couldn’t sign you in. Please try again.'
        : 'We couldn’t create your account. Please try again.';
  }
}