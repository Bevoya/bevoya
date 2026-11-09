export const verifyCopy = {
  title: 'Check your email',
  body: (email: string) => `We sent a code to ${email}. Enter it here to finish creating your account.`,
  codeLabel: 'Code',
  confirm: 'Confirm',
  resend: 'Send a new code',
  resendWait: (seconds: number) => `Send a new code in ${seconds} s`,
  sent: 'We sent a new code. It can take a minute to arrive.',
  spamHint: 'Can’t find it? Check your spam folder.',
  differentEmail: 'Use a different email',
  missingEmail: 'We lost track of your email. Please start again.',
  startAgain: 'Back to sign up',
};