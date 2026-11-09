import { z } from 'zod';

export const emailSchema = z
  .string()
  .trim()
  .min(1, 'Enter your email address.')
  .email('Enter a valid email address.');

export const signInSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Enter your password.'),
});

export const signUpSchema = z.object({
  email: emailSchema,
  password: z
    .string()
    .min(8, 'Use at least 8 characters.')
    .max(72, 'Use 72 characters or fewer.'),
  confirmAge: z.boolean().refine((value) => value === true, {
    message: 'Please confirm you are 18 or over.',
  }),
});

// Supabase email codes are 6 to 8 digits depending on the project setting.
// Once the real length is known, narrow these two numbers and the pattern below.
export const CODE_MIN_LENGTH = 6;
export const CODE_MAX_LENGTH = 8;

export const codeSchema = z.object({
  code: z
    .string()
    .trim()
    .regex(/^\d{6,8}$/, 'Enter the code from your email.'),
});

export type SignInValues = z.infer<typeof signInSchema>;
export type SignUpValues = z.infer<typeof signUpSchema>;
export type CodeValues = z.infer<typeof codeSchema>;