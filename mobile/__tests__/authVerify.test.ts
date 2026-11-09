 import { authMessage, classifyAuthError } from '../src/features/auth/errors';
import { codeSchema } from '../src/features/auth/schemas';

describe('codeSchema', () => {
  it('accepts 6 to 8 digits and trims spaces', () => {
    expect(codeSchema.safeParse({ code: '123456' }).success).toBe(true);
    expect(codeSchema.safeParse({ code: '12345678' }).success).toBe(true);
    const result = codeSchema.safeParse({ code: ' 123456 ' });
    expect(result.success && result.data.code).toBe('123456');
  });

  it('rejects empty, short, long and non-numeric codes', () => {
    expect(codeSchema.safeParse({ code: '' }).success).toBe(false);
    expect(codeSchema.safeParse({ code: '12345' }).success).toBe(false);
    expect(codeSchema.safeParse({ code: '123456789' }).success).toBe(false);
    expect(codeSchema.safeParse({ code: 'abc123' }).success).toBe(false);
  });
});

describe('code errors', () => {
  it('treats a wrong or expired code as an invalid code', () => {
    expect(classifyAuthError({ code: 'otp_expired', status: 403 })).toBe('invalid_code');
  });

  it('writes plain messages for each verification step', () => {
    expect(authMessage('invalid_code', 'verify')).toContain('code');
    expect(authMessage('unknown', 'verify')).toBe('We couldn’t check that code. Please try again.');
    expect(authMessage('unknown', 'resend')).toBe('We couldn’t send a new code. Please try again.');
    expect(authMessage('rate_limited', 'resend')).toContain('Too many attempts');
  });

  it('keeps the sign-in and sign-up fallbacks unchanged', () => {
    expect(authMessage('unknown', 'sign_in')).toBe('We couldn’t sign you in. Please try again.');
    expect(authMessage('unknown', 'sign_up')).toBe('We couldn’t create your account. Please try again.');
  });
});