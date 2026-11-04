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

export type SignInValues = z.infer<typeof signInSchema>;
export type SignUpValues = z.infer<typeof signUpSchema>;