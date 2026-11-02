import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { i18n } from '@/i18n'

declare module 'vue-router' {
  interface RouteMeta {
    titleKey?: string
    authMode?: 'signIn' | 'createAccount'
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
      component: () => import('../views/AuthPlaceholderView.vue'),
      meta: { titleKey: 'auth.signIn.meta.title', authMode: 'signIn' },
    },
    {
      path: '/create-account',
      name: 'create-account',
      component: () => import('../views/AuthPlaceholderView.vue'),
      meta: {
        titleKey: 'auth.createAccount.meta.title',
        authMode: 'createAccount',
      },
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

router.afterEach((to) => {
  if (to.meta.titleKey) {
    document.title = i18n.global.t(to.meta.titleKey)
  }
})

export default router
