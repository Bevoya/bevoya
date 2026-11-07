export const TOTAL_STEPS = 6;

export const copy = {
  continue: 'Continue',
  pickFirst: 'Choose one option to continue',
  welcome: {
    tagline: 'Your life, moving forward.',
    start: 'Get started',
    haveAccount: 'I already have an account',
    slides: [
      { title: 'One clear next step', body: 'Turn a big goal into what to do today.' },
      { title: 'Proof, not just time', body: 'Log what you built, practised and learned.' },
      { title: 'Reflect in private', body: 'Your journal stays yours.' },
    ],
  },
  focus: { title: 'What brings you here?', subtitle: 'Pick what matters most right now.' },
  userType: { title: 'Which fits you best?', subtitle: 'We tailor your tips to it.' },
  time: { title: 'How much time most days?', subtitle: 'Small is fine. You can change it later.' },
  bestTime: { title: 'When do you work best?', subtitle: 'We time your reminders around it.' },
  reminders: {
    title: 'Gentle reminders?',
    subtitle: 'Optional. Your phone asks for permission later.',
    morningTitle: 'Morning plan',
    morningDetail: 'A calm nudge to choose today’s tasks.',
    eveningTitle: 'Evening reflection',
    eveningDetail: 'A quiet prompt to look back on the day.',
  },
  plan: {
    title: 'Here’s your starting point',
    subtitle: 'Create an account to save it.',
    aiTitle: 'AI memory',
    aiDetail: 'Let Bevoya use your learning notes to personalise tips. Off by default. Journals stay private.',
    privateLine: 'Private by default. We never sell your data.',
    terms: 'Terms of Service',
    privacy: 'Privacy Policy',
    create: 'Create my account',
  },
} as const;