import type { Component } from 'vue'
import { Building2, LayoutDashboard, ScrollText, Settings, ShieldCheck, Users } from '@lucide/vue'

export type AppNavItem = {
  labelKey: string
  to: string
  icon: Component
}

export const appNavItems: AppNavItem[] = []

export const settingsNavItems: AppNavItem[] = [
  {
    labelKey: 'nav.settings',
    to: '/settings',
    icon: Settings,
  },
]

export const adminNavItems: AppNavItem[] = [
  {
    labelKey: 'nav.admin.overview',
    to: '/admin',
    icon: LayoutDashboard,
  },
  {
    labelKey: 'nav.admin.users',
    to: '/admin/users',
    icon: Users,
  },
  {
    labelKey: 'nav.admin.orgs',
    to: '/admin/orgs',
    icon: Building2,
  },
  {
    labelKey: 'nav.admin.audit',
    to: '/admin/audit',
    icon: ScrollText,
  },
]

export const adminShieldIcon = ShieldCheck
