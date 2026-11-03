import { apiRequest } from './api'

export const ADMIN_ORG_STATES = [
  'CREATING',
  'STARTING',
  'ONLINE',
  'OFFLINE',
  'DELETING',
] as const

export const ADMIN_USER_STATUSES = ['active', 'suspended'] as const

export const ADMIN_CONFIG_STATUSES = ['applied', 'pending', 'rejected'] as const

export const ADMIN_ROLES = ['owner', 'co_owner', 'member'] as const

export const ADMIN_AUDIT_ACTIONS = [
  'user.view',
  'user.suspend',
  'user.unsuspend',
  'user.force_logout',
  'org.view',
  'platform_admin.grant',
  'platform_admin.revoke',
] as const

export type AdminOrgState = (typeof ADMIN_ORG_STATES)[number]
export type AdminUserStatus = (typeof ADMIN_USER_STATUSES)[number]
export type AdminConfigStatus = (typeof ADMIN_CONFIG_STATUSES)[number]
export type AdminRole = (typeof ADMIN_ROLES)[number]
export type AdminAuditAction = (typeof ADMIN_AUDIT_ACTIONS)[number]

export type AdminStats = {
  users: {
    total: number
    suspended: number
  }
  orgs: {
    total: number
    online: number
    offline: number
  }
}

export type AdminUserRef = {
  user_id: string
  display_name: string
}

export type AdminOrgRef = {
  id: string
  name: string
}

export type AdminSuspension = {
  reason: string | null
  suspended_at: string
  suspended_by: AdminUserRef | null
}

export type AdminUserSummary = {
  id: string
  email: string
  display_name: string
  status: AdminUserStatus
  created_at: string
  last_login_at: string | null
  owns_org: boolean
  membership_count: number
}

export type AdminMembership = {
  org_id: string
  org_name: string
  role: AdminRole
  joined_at: string
}

export type AdminUserDetail = {
  id: string
  email: string
  display_name: string
  status: AdminUserStatus
  suspension: AdminSuspension | null
  is_platform_admin: boolean
  created_at: string
  last_login_at: string | null
  owned_org: AdminOrgRef | null
  memberships: AdminMembership[]
  active_sessions: number
}

export type AdminUserListResponse = {
  items: AdminUserSummary[]
  next_cursor: string | null
}

export type AdminOrgSummary = {
  id: string
  name: string
  state: AdminOrgState
  owner: AdminUserRef | null
  member_count: number
  created_at: string
}

export type AdminOrgMember = {
  user_id: string
  display_name: string
  role: AdminRole
}

export type AdminOrgProxy = {
  last_heartbeat_at: string | null
  running_config_version: number | null
  latest_config_version: number | null
  config_status: AdminConfigStatus
  active_ban_count: number
}

export type AdminOrgDetail = {
  id: string
  name: string
  state: AdminOrgState
  created_at: string
  owner: AdminUserRef | null
  members: AdminOrgMember[]
  proxy: AdminOrgProxy
}

export type AdminOrgListResponse = {
  items: AdminOrgSummary[]
  next_cursor: string | null
}

export type AdminAuditTarget = {
  type: string | null
  id: string | null
}

export type AdminAuditEntry = {
  id: string
  timestamp: string
  admin: AdminUserRef | null
  action: string
  target: AdminAuditTarget | null
  summary: string
  source_ip: string | null
}

export type AdminAuditListResponse = {
  items: AdminAuditEntry[]
  next_cursor: string | null
}

export type AdminUserFilters = {
  q?: string
  status?: AdminUserStatus
  limit?: number
  cursor?: string | null
}

export type AdminOrgFilters = {
  q?: string
  state?: AdminOrgState
  limit?: number
  cursor?: string | null
}

export type AdminAuditFilters = {
  from?: string
  to?: string
  action?: string
  limit?: number
  cursor?: string | null
}

export type AdminSuspendInput = {
  reason: string
  password: string
}

export type AdminUnsuspendInput = {
  reason: string
}

export type AdminForceLogoutInput = {
  reason: string
  password: string
}

type QueryValue = string | number | null | undefined

function withQuery(path: string, params: Record<string, QueryValue>): string {
  const search = new URLSearchParams()

  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') {
      continue
    }

    search.set(key, String(value))
  }

  const query = search.toString()
  return query ? `${path}?${query}` : path
}

export const adminApi = {
  stats(): Promise<AdminStats> {
    return apiRequest<AdminStats>('/admin/stats')
  },

  listUsers(filters: AdminUserFilters = {}): Promise<AdminUserListResponse> {
    return apiRequest<AdminUserListResponse>(
      withQuery('/admin/users', {
        q: filters.q,
        status: filters.status,
        limit: filters.limit,
        cursor: filters.cursor,
      }),
    )
  },

  getUser(userId: string): Promise<AdminUserDetail> {
    return apiRequest<AdminUserDetail>(`/admin/users/${encodeURIComponent(userId)}`)
  },

  suspendUser(userId: string, input: AdminSuspendInput): Promise<AdminUserDetail> {
    return apiRequest<AdminUserDetail>(
      `/admin/users/${encodeURIComponent(userId)}/suspend`,
      { method: 'POST', body: input },
    )
  },

  unsuspendUser(userId: string, input: AdminUnsuspendInput): Promise<AdminUserDetail> {
    return apiRequest<AdminUserDetail>(
      `/admin/users/${encodeURIComponent(userId)}/unsuspend`,
      { method: 'POST', body: input },
    )
  },

  forceLogoutUser(userId: string, input: AdminForceLogoutInput): Promise<void> {
    return apiRequest<void>(`/admin/users/${encodeURIComponent(userId)}/force-logout`, {
      method: 'POST',
      body: input,
    })
  },

  listOrgs(filters: AdminOrgFilters = {}): Promise<AdminOrgListResponse> {
    return apiRequest<AdminOrgListResponse>(
      withQuery('/admin/orgs', {
        q: filters.q,
        state: filters.state,
        limit: filters.limit,
        cursor: filters.cursor,
      }),
    )
  },

  getOrg(orgId: string): Promise<AdminOrgDetail> {
    return apiRequest<AdminOrgDetail>(`/admin/orgs/${encodeURIComponent(orgId)}`)
  },

  listAudit(filters: AdminAuditFilters = {}): Promise<AdminAuditListResponse> {
    return apiRequest<AdminAuditListResponse>(
      withQuery('/admin/audit', {
        from: filters.from,
        to: filters.to,
        action: filters.action,
        limit: filters.limit,
        cursor: filters.cursor,
      }),
    )
  },
}
