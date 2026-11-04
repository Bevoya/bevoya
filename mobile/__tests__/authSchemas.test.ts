import { signInSchema, signUpSchema } from '../src/features/auth/schemas';

describe('signInSchema', () => {
  it('accepts a valid email and a password', () => {
    expect(signInSchema.safeParse({ email: 'a@example.com', password: 'x' }).success).toBe(true);
  });

  it('trims the email', () => {
    const result = signInSchema.safeParse({ email: '  a@example.com ', password: 'x' });
    expect(result.success && result.data.email).toBe('a@example.com');
  });

  it('rejects an empty or malformed email and an empty password', () => {
    expect(signInSchema.safeParse({ email: '', password: 'x' }).success).toBe(false);
    expect(signInSchema.safeParse({ email: 'nope', password: 'x' }).success).toBe(false);
    expect(signInSchema.safeParse({ email: 'a@example.com', password: '' }).success).toBe(false);
  });
});

describe('signUpSchema', () => {
  const valid = { email: 'a@example.com', password: 'longenough', confirmAge: true };

  it('accepts valid values', () => {
    expect(signUpSchema.safeParse(valid).success).toBe(true);
  });

  it('rejects a short password', () => {
    expect(signUpSchema.safeParse({ ...valid, password: 'short' }).success).toBe(false);
  });

  it('requires the 18 or over confirmation', () => {
    expect(signUpSchema.safeParse({ ...valid, confirmAge: false }).success).toBe(false);
  });
});