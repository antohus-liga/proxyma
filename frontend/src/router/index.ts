import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { i18n } from '@/i18n'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    titleKey?: string
    requiresAuth?: boolean
    guestOnly?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { titleKey: 'home.meta.title' },
    },
    {
      path: '/sign-in',
      name: 'sign-in',
      component: () => import('../views/SignInView.vue'),
      meta: { titleKey: 'auth.signIn.meta.title', guestOnly: true },
    },
    {
      path: '/create-account',
      name: 'create-account',
      component: () => import('../views/CreateAccountView.vue'),
      meta: { titleKey: 'auth.createAccount.meta.title', guestOnly: true },
    },
    {
      path: '/app',
      name: 'app',
      component: () => import('../views/AppPlaceholderView.vue'),
      meta: { titleKey: 'app.signedIn.meta.title', requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
      meta: { titleKey: 'notFound.meta.title' },
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if ((to.meta.requiresAuth || to.meta.guestOnly) && auth.status === 'idle') {
    await auth.bootstrap()
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    const query: Record<string, string> = { redirect: to.fullPath }

    if (auth.suspended) {
      query.reason = 'suspended'
    } else if (auth.sessionExpired) {
      query.reason = 'session-expired'
    }

    return { name: 'sign-in', query }
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'app' }
  }

  return true
})

router.afterEach((to) => {
  if (to.meta.titleKey) {
    document.title = i18n.global.t(to.meta.titleKey)
  }

  window.requestAnimationFrame(() => {
    const heading = document.querySelector<HTMLElement>('#main-content h1')

    if (!heading) {
      return
    }

    heading.tabIndex = -1
    heading.focus({ preventScroll: true })
  })
})

export default router
