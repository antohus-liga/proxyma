import { watch } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { i18n } from '@/i18n'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    titleKey?: string
    requiresAuth?: boolean
    requiresAdmin?: boolean
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
      path: '/settings',
      component: () => import('../views/settings/SettingsLayoutView.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: { name: 'settings-account' },
        },
        {
          path: 'account',
          name: 'settings-account',
          component: () => import('../views/settings/SettingsAccountView.vue'),
          meta: { titleKey: 'settings.account.meta.title' },
        },
        {
          path: 'general',
          name: 'settings-general',
          component: () => import('../views/settings/SettingsGeneralView.vue'),
          meta: { titleKey: 'settings.general.meta.title' },
        },
        {
          path: 'security',
          name: 'settings-security',
          component: () => import('../views/settings/SettingsSecurityView.vue'),
          meta: { titleKey: 'settings.security.meta.title' },
        },
      ],
    },
    {
      path: '/app',
      redirect: { name: 'settings-account' },
    },
    {
      path: '/app/password',
      redirect: { name: 'settings-security' },
    },
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayoutView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'admin',
          component: () => import('../views/admin/AdminOverviewView.vue'),
          meta: { titleKey: 'admin.overview.meta.title' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('../views/admin/AdminUsersView.vue'),
          meta: { titleKey: 'admin.users.meta.title' },
        },
        {
          path: 'users/:userId',
          name: 'admin-user-detail',
          component: () => import('../views/admin/AdminUserDetailView.vue'),
          meta: { titleKey: 'admin.userDetail.meta.title' },
        },
        {
          path: 'orgs',
          name: 'admin-orgs',
          component: () => import('../views/admin/AdminOrgsView.vue'),
          meta: { titleKey: 'admin.orgs.meta.title' },
        },
        {
          path: 'orgs/:orgId',
          name: 'admin-org-detail',
          component: () => import('../views/admin/AdminOrgDetailView.vue'),
          meta: { titleKey: 'admin.orgDetail.meta.title' },
        },
        {
          path: 'audit',
          name: 'admin-audit',
          component: () => import('../views/admin/AdminAuditView.vue'),
          meta: { titleKey: 'admin.audit.meta.title' },
        },
      ],
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

watch(i18n.global.locale, () => {
  const { meta } = router.currentRoute.value

  if (meta.titleKey) {
    document.title = i18n.global.t(meta.titleKey)
  }
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

  if (to.meta.requiresAdmin && !auth.user?.is_platform_admin) {
    return {
      name: 'not-found',
      params: { pathMatch: to.path.split('/').filter(Boolean) },
      query: to.query,
      hash: to.hash,
    }
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'settings-account' }
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
