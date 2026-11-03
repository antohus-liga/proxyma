export default {
  app: {
    name: 'Proxyma',
    wordmark: 'proxyma',
    homeLabel: 'Proxyma home',
    signedIn: {
      meta: {
        title: 'Signed in — Proxyma',
      },
      title: "You're signed in",
      description:
        "Signed in as {name} ({email}). Organisation onboarding isn't available yet.",
      signOut: 'Sign out',
    },
  },
  a11y: {
    skipToContent: 'Skip to main content',
  },
  common: {
    required: 'required',
  },
  nav: {
    account: 'Account',
    signIn: 'Sign in',
    createAccount: 'Create an account',
  },
  home: {
    meta: {
      title: 'Proxyma — Always near, always here.',
    },
    hero: {
      eyebrow: 'Managed reverse proxy',
      title: 'Always near, always here.',
      lead: "A reverse proxy you don't have to babysit.",
      body: 'Proxyma runs an isolated proxy container for your project. Add routes, set rate limits, and watch live traffic from one dashboard.',
    },
    valueProps: {
      title: 'Why Proxyma',
      proxy: {
        title: 'Your own proxy',
        body: 'An isolated container and URL for each organisation.',
      },
      traffic: {
        title: 'Traffic you can see',
        body: 'Live requests, latency, and bans without a terminal.',
      },
      changes: {
        title: 'Changes without a restart',
        body: 'Routes and limits apply as a new config version.',
      },
    },
    howItWorks: {
      title: 'How it works',
      intro: 'Three steps from an account to your first proxied request.',
      steps: {
        account: {
          title: 'Create an account',
          body: 'Start with your email and a password.',
        },
        organisation: {
          title: 'Create an organisation',
          body: 'Proxyma starts an isolated proxy container and gives you its URL.',
        },
        traffic: {
          title: 'Point traffic at your proxy',
          body: 'Add a route, set a rate limit, and send requests through it.',
        },
      },
    },
    closing: {
      title: 'No server setup. No hand-edited configuration.',
      body: 'Create an account and Proxyma starts an isolated proxy container for your project.',
    },
  },
  footer: {
    copyright: '© {year} Proxyma',
  },
  auth: {
    signIn: {
      meta: {
        title: 'Sign in — Proxyma',
      },
      title: 'Sign in to Proxyma',
      description: 'Enter your email and password to manage your proxy.',
      submit: 'Sign in',
      newHere: 'New to Proxyma?',
      createAccountLink: 'Create an account',
      sessionExpired: 'Your session has ended. Sign in again.',
    },
    createAccount: {
      meta: {
        title: 'Create an account — Proxyma',
      },
      title: 'Create your Proxyma account',
      description: 'Create an account to start using Proxyma.',
      submit: 'Create account',
      haveAccount: 'Already have an account?',
      signInLink: 'Sign in',
      passwordHelp: 'Use at least 10 characters. Avoid common passwords.',
      emailTaken: 'An account with this email already exists.',
      emailTakenLink: 'Sign in instead',
      success: {
        title: 'Account created',
        description: 'Sign in to continue.',
        signIn: 'Sign in',
      },
    },
    fields: {
      displayName: 'Display name',
      email: 'Email',
      password: 'Password',
    },
    password: {
      show: 'Show',
      hide: 'Hide',
      showLabel: 'Show password',
      hideLabel: 'Hide password',
    },
    fieldErrors: {
      displayName: {
        required: 'Enter a display name.',
        tooLong: 'Use 80 characters or fewer.',
        invalid: 'Enter a display name of 80 characters or fewer.',
      },
      email: {
        required: 'Enter your email address.',
        tooLong: 'Use 254 characters or fewer.',
        invalid: 'Enter a valid email address.',
      },
      password: {
        required: 'Enter your password.',
        tooShort: 'Use at least 10 characters.',
        tooLong: 'Use 128 characters or fewer.',
        invalid: 'Use a password between 10 and 128 characters.',
      },
    },
  },
  errors: {
    INVALID_CREDENTIALS: 'Email or password is incorrect.',
    ACCOUNT_SUSPENDED:
      'This account is suspended. Contact the operator if you think this is a mistake.',
    RATE_LIMITED: 'Too many attempts. Try again in {seconds} seconds.',
    EMAIL_TAKEN: 'An account with this email already exists. Sign in instead.',
    WEAK_PASSWORD:
      'Choose a stronger password. Use at least 10 characters and avoid common passwords.',
    VALIDATION_FAILED: 'Check the highlighted fields and try again.',
    MALFORMED_REQUEST: "We couldn't read that request. Refresh the page and try again.",
    INTERNAL_ERROR:
      'Something went wrong on our side. Try again. If it keeps happening, contact the operator.',
    NETWORK_ERROR: "We couldn't reach Proxyma. Check your connection and try again.",
    TIMEOUT: 'The request took too long. Try again.',
    CORS_REJECTED: "This request came from an origin Proxyma doesn't allow.",
    FORBIDDEN: "You don't have permission to do that.",
    UNAUTHORIZED: 'Sign in to continue.',
    INVALID_REFRESH_TOKEN: 'Your session has ended. Sign in again.',
    UNKNOWN: 'Something went wrong. Try again.',
  },
  notFound: {
    meta: {
      title: 'Page not found — Proxyma',
    },
    title: 'Page not found',
    description: "This page doesn't exist or you don't have access to it.",
    back: 'Back to home',
  },
}
