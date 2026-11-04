import type { Component } from 'vue'
import { createMemoryHistory, createRouter, type RouteRecordRaw } from 'vue-router'

const StubView = { template: '<div />' }

/**
 * Mirrors the named route paths from `@/router`.
 *
 * The catch-all route is intentionally omitted so a `RouterLink` pointing at a
 * path missing from this list logs `VUE_ROUTER_R0004` instead of silently
 * matching the not-found route. Keep this in sync when adding a route.
 */
const routes = [
  { path: '/', name: 'home' },
  { path: '/sign-in', name: 'sign-in' },
  { path: '/create-account', name: 'create-account' },
  { path: '/app', name: 'app' },
  { path: '/app/password', name: 'account-password' },
  { path: '/admin', name: 'admin' },
  { path: '/admin/users', name: 'admin-users' },
  { path: '/admin/users/:userId', name: 'admin-user-detail' },
  { path: '/admin/orgs', name: 'admin-orgs' },
  { path: '/admin/orgs/:orgId', name: 'admin-org-detail' },
  { path: '/admin/audit', name: 'admin-audit' },
] as const

type TestRouteName = (typeof routes)[number]['name']

export function createTestRouter(components: Partial<Record<TestRouteName, Component>> = {}) {
  const testRoutes: RouteRecordRaw[] = routes.map((route) => ({
    path: route.path,
    name: route.name,
    component: components[route.name] ?? StubView,
  }))

  return createRouter({
    history: createMemoryHistory(),
    routes: testRoutes,
  })
}
