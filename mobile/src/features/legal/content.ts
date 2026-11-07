export const LEGAL_VERSION = '0.1 (07/11/2026, under review)';
export const SUPPORT_EMAIL = 'bevoya.support@gmail.com';

export type LegalSection = { heading: string; paragraphs: string[] };

export const termsSections: LegalSection[] = [
  {
    heading: 'Who can use Bevoya',
    paragraphs: ['Bevoya is for people aged 18 and over. By creating an account you confirm that you are 18 or older.'],
  },
  {
    heading: 'Your account',
    paragraphs: [
      'Use accurate details and keep your sign-in secure. You are responsible for activity on your account. Tell us if you think someone else has access.',
    ],
  },
  {
    heading: 'What Bevoya is, and is not',
    paragraphs: [
      'Bevoya helps you plan, learn and reflect. AI suggestions are guidance, not professional advice.',
      'Bevoya does not give medical, legal, financial or mental-health advice, and it is not for emergencies. We do not promise any result, such as grades, jobs, income or followers.',
    ],
  },
  {
    heading: 'Your content',
    paragraphs: [
      'What you write and add is yours. You allow us to store and process it only to run Bevoya for you.',
      'We do not sell your personal data. We do not use your private content to train AI models without your explicit consent.',
    ],
  },
  {
    heading: 'Using Bevoya fairly',
    paragraphs: [
      'Do not break the law, attack or overload the service, try to reach other people’s data, or misuse the AI features.',
    ],
  },
  {
    heading: 'Paid plans',
    paragraphs: [
      'If paid plans are offered, they are billed through Google Play. You will see the price, the renewal date and how to cancel before you buy.',
    ],
  },
  {
    heading: 'Ending your use',
    paragraphs: [
      'You can delete your account at any time in the app. We may suspend accounts that seriously break these terms.',
    ],
  },
  {
    heading: 'Changes',
    paragraphs: ['If we change these terms in a way that matters, we will tell you in the app before it applies.'],
  },
  { heading: 'Contact', paragraphs: [`Questions: ${SUPPORT_EMAIL}`] },
];

export const privacySections: LegalSection[] = [
  {
    heading: 'What we collect',
    paragraphs: [
      'Your email address when you sign up. The setup choices you make, such as your focus and daily time.',
      'What you add: goals, tasks, sessions, evidence, notes and reflections.',
      'A small number of usage events and crash reports, which never include your text.',
    ],
  },
  {
    heading: 'Why we use it',
    paragraphs: ['To run the features you use, keep your account secure and fix problems. Nothing else.'],
  },
  {
    heading: 'AI features',
    paragraphs: [
      'When you ask for an AI result, we send only the text needed to a third-party AI provider (Google’s Gemini API). You see the result as a suggestion that you can accept, edit or dismiss.',
      'AI memory is off by default. Your journal is not used for AI memory unless you switch that on. Before real user content goes to any AI provider, we review that provider’s data terms.',
    ],
  },
  {
    heading: 'Where your data is',
    paragraphs: [
      'Your account and content are stored with Supabase in Mumbai, India. Our app servers run on Render in Singapore. AI requests go to Google and may be handled outside India.',
    ],
  },
  {
    heading: 'Who we share it with',
    paragraphs: [
      'Service providers that help us run Bevoya: hosting, database and sign-in, AI, crash reporting, product analytics and, later, payments through Google Play. We do not sell your personal data.',
    ],
  },
  {
    heading: 'Your controls',
    paragraphs: [
      'You can delete what you wrote, turn AI memory off, export your data and delete your account. These controls are in the Privacy centre in the app.',
    ],
  },
  {
    heading: 'Keeping and deleting',
    paragraphs: [
      'When you delete something, it is removed from our live database straight away. Backups may hold a copy for a limited time before they expire.',
    ],
  },
  {
    heading: 'Age',
    paragraphs: ['Bevoya is for people aged 18 and over. If we learn that someone under 18 has an account, we will delete it.'],
  },
  {
    heading: 'Security',
    paragraphs: [
      'We use encryption in transit and access controls. No service is perfectly secure, so we limit what we collect.',
    ],
  },
  { heading: 'Contact and complaints', paragraphs: [`Questions or concerns: ${SUPPORT_EMAIL}`] },
];

export const legalCopy = {
  signUpAgree: 'I agree to the Terms of Service and Privacy Policy',
  termsLink: 'Read the Terms',
  privacyLink: 'Read the Privacy Policy',
  signUpError: 'Please agree to continue.',
};