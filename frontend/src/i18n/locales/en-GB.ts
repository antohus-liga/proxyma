export default {
  app: {
    name: 'Proxyma',
    wordmark: 'proxyma',
    homeLabel: 'Proxyma home',
  },
  a11y: {
    skipToContent: 'Skip to main content',
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
    placeholder: {
      signIn: {
        title: 'Sign in',
        body: "Sign-in isn't available yet.",
      },
      createAccount: {
        title: 'Create an account',
        body: "Account creation isn't available yet.",
      },
      back: 'Back to home',
    },
    signIn: {
      meta: {
        title: 'Sign in — Proxyma',
      },
    },
    createAccount: {
      meta: {
        title: 'Create an account — Proxyma',
      },
    },
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
