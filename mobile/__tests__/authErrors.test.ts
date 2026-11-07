import { authMessage, classifyAuthError, isExistingSignUp } from '../src/features/auth/errors';

describe('classifyAuthError', () => {
  it.each([
    [{ code: 'invalid_credentials' }, 'invalid_credentials'],
    [{ code: 'email_not_confirmed' }, 'email_not_confirmed'],
    [{ code: 'user_already_exists' }, 'user_exists'],
    [{ code: 'email_exists' }, 'user_exists'],
    [{ code: 'weak_password' }, 'weak_password'],
    [{ code: 'over_request_rate_limit' }, 'rate_limited'],
    [{ code: 'over_email_send_rate_limit' }, 'rate_limited'],
    [{ status: 429 }, 'rate_limited'],
    [{ name: 'AuthRetryableFetchError' }, 'offline'],
    [{ code: 'something_new' }, 'unknown'],
    [null, 'unknown'],
  ])('%j becomes %s', (error, expected) => {
    expect(classifyAuthError(error)).toBe(expected);
  });
});

describe('isExistingSignUp', () => {
  it('is true when the user has no identities', () => {
    expect(isExistingSignUp({ user: { identities: [] } })).toBe(true);
  });
  it('is false for a new user or missing data', () => {
    expect(isExistingSignUp({ user: { identities: [{}] } })).toBe(false);
    expect(isExistingSignUp({ user: null })).toBe(false);
    expect(isExistingSignUp(null)).toBe(false);
  });
});

describe('authMessage', () => {
  it('does not reveal whether an account exists on sign-in', () => {
    const text = authMessage('invalid_credentials', 'sign_in').toLowerCase();
    expect(text).not.toContain('exist');
    expect(text).not.toContain('no account');
  });
  it('tells the person to sign in when the account exists', () => {
    expect(authMessage('user_exists', 'sign_up')).toContain('Sign in');
  });
  it('uses a context message for unknown errors', () => {
    expect(authMessage('unknown', 'sign_in')).toContain('sign you in');
    expect(authMessage('unknown', 'sign_up')).toContain('create your account');
  });
});