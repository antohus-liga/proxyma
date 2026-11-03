<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'
import AdminErrorState from '@/components/admin/AdminErrorState.vue'
import AdminStatusBadge from '@/components/admin/AdminStatusBadge.vue'
import StepUpDialog from '@/components/admin/StepUpDialog.vue'
import NotFoundContent from '@/components/layout/NotFoundContent.vue'
import PxBreadcrumbs from '@/components/ui/PxBreadcrumbs.vue'
import PxButton from '@/components/ui/PxButton.vue'
import PxNotice from '@/components/ui/PxNotice.vue'
import PxPageHeader from '@/components/ui/PxPageHeader.vue'
import PxPanel from '@/components/ui/PxPanel.vue'
import PxSkeleton from '@/components/ui/PxSkeleton.vue'
import { useApiErrorMessage } from '@/composables/useApiErrorMessage'
import { useFormatters } from '@/composables/useFormatters'
import { adminApi, type AdminUserDetail } from '@/lib/admin-api'
import { asApiError, type ApiError } from '@/lib/api'
import { isNotFoundError } from '@/lib/api-errors'
import { useAuthStore } from '@/stores/auth'

type ActionKind = 'suspend' | 'unsuspend' | 'force-logout'

const { t } = useI18n()
const route = useRoute()
const auth = useAuthStore()
const { formatDateTime } = useFormatters()
const { messageFor } = useApiErrorMessage()

const userId = computed(() => String(route.params.userId ?? ''))
const detail = ref<AdminUserDetail | null>(null)
const error = ref<ApiError | null>(null)
const notFound = ref(false)
const loading = ref(false)

const actionKind = ref<ActionKind | null>(null)
const actionBusy = ref(false)
const actionError = ref<string | null>(null)
const successKey = ref<string | null>(null)

const isSelf = computed(() => detail.value?.id === auth.user?.id)
const targetIsAdmin = computed(() => detail.value?.is_platform_admin === true)
const canSuspend = computed(
  () => detail.value?.status === 'active' && !isSelf.value && !targetIsAdmin.value,
)
const canUnsuspend = computed(() => detail.value?.status === 'suspended')
const hasSessions = computed(() => (detail.value?.active_sessions ?? 0) > 0)

const dialogOpen = computed({
  get: () => actionKind.value !== null,
  set: (value: boolean) => {
    if (!value && !actionBusy.value) {
      actionKind.value = null
    }
  },
})

const dialogTitle = computed(() => {
  const name = detail.value?.display_name ?? ''

  if (actionKind.value === 'suspend') {
    return t('admin.userDetail.actions.suspendTitle', { name })
  }

  if (actionKind.value === 'unsuspend') {
    return t('admin.userDetail.actions.unsuspendTitle', { name })
  }

  return t('admin.userDetail.actions.forceLogoutTitle', { name })
})

const dialogDescription = computed(() => {
  const name = detail.value?.display_name ?? ''
  const sessions = detail.value?.active_sessions ?? 0

  if (actionKind.value === 'suspend') {
    return t('admin.userDetail.actions.suspendDescription', { name })
  }

  if (actionKind.value === 'unsuspend') {
    return t('admin.userDetail.actions.unsuspendDescription', { name })
  }

  return t('admin.userDetail.actions.forceLogoutDescription', { name, count: sessions })
})

const dialogConfirmLabel = computed(() => {
  if (actionKind.value === 'suspend') {
    return t('admin.userDetail.actions.suspendConfirm')
  }

  if (actionKind.value === 'unsuspend') {
    return t('admin.userDetail.actions.unsuspendConfirm')
  }

  return t('admin.userDetail.actions.forceLogoutConfirm')
})

const dialogRequiresPassword = computed(() => actionKind.value !== 'unsuspend')

async function load() {
  loading.value = true
  error.value = null

  try {
    detail.value = await adminApi.getUser(userId.value)
    notFound.value = false
  } catch (cause) {
    const apiError = asApiError(cause)

    if (isNotFoundError(apiError)) {
      notFound.value = true
      detail.value = null
    } else {
      error.value = apiError
    }
  } finally {
    loading.value = false
  }
}

function openAction(kind: ActionKind) {
  actionError.value = null
  successKey.value = null
  actionKind.value = kind
}

async function onConfirm(payload: { reason: string; password?: string }) {
  if (!detail.value || !actionKind.value || actionBusy.value) {
    return
  }

  actionBusy.value = true
  actionError.value = null

  try {
    if (actionKind.value === 'suspend') {
      detail.value = await adminApi.suspendUser(detail.value.id, {
        reason: payload.reason,
        password: payload.password ?? '',
      })
      successKey.value = 'admin.userDetail.success.suspend'
    } else if (actionKind.value === 'unsuspend') {
      detail.value = await adminApi.unsuspendUser(detail.value.id, {
        reason: payload.reason,
      })
      successKey.value = 'admin.userDetail.success.unsuspend'
    } else {
      await adminApi.forceLogoutUser(detail.value.id, {
        reason: payload.reason,
        password: payload.password ?? '',
      })
      detail.value = { ...detail.value, active_sessions: 0 }
      successKey.value = 'admin.userDetail.success.forceLogout'
    }

    actionKind.value = null
  } catch (cause) {
    actionError.value = messageFor(asApiError(cause))
  } finally {
    actionBusy.value = false
  }
}

onMounted(load)

watch(userId, () => {
  successKey.value = null
  actionKind.value = null
  void load()
})
</script>

<template>
  <PxBreadcrumbs
    v-if="!notFound"
    :label="t('admin.breadcrumbs.label')"
    :items="[
      { label: t('admin.users.title'), to: '/admin/users' },
      { label: detail?.display_name ?? t('admin.userDetail.title') },
    ]"
  />

  <PxPageHeader
    v-if="!notFound"
    :title="detail?.display_name ?? t('admin.userDetail.title')"
    :description="detail?.email ?? t('admin.userDetail.description')"
  >
    <template #actions>
      <PxButton variant="secondary" to="/admin/users">
        {{ t('admin.userDetail.back') }}
      </PxButton>
    </template>
  </PxPageHeader>

  <NotFoundContent v-if="notFound" back-to="/admin/users" :back-label="t('admin.users.title')" />

  <AdminErrorState v-else-if="error" :error="error" @retry="load" />

  <div v-else-if="loading && !detail" class="px-page-stack">
    <PxSkeleton height="160px" />
    <PxSkeleton height="220px" />
  </div>

  <div v-else-if="detail" class="px-page-stack">
    <PxNotice v-if="successKey" tone="success" role="status">
      {{ t(successKey) }}
    </PxNotice>

    <PxPanel :title="t('admin.userDetail.account.title')">
      <template #actions>
        <AdminStatusBadge kind="user" :value="detail.status" />
      </template>

      <dl class="px-definition-list">
        <dt>{{ t('admin.userDetail.account.id') }}</dt>
        <dd class="admin-mono">{{ detail.id }}</dd>

        <dt>{{ t('admin.userDetail.account.displayName') }}</dt>
        <dd>{{ detail.display_name }}</dd>

        <dt>{{ t('admin.userDetail.account.email') }}</dt>
        <dd>{{ detail.email }}</dd>

        <dt>{{ t('admin.userDetail.account.type') }}</dt>
        <dd>
          {{
            detail.is_platform_admin
              ? t('admin.userDetail.account.platformAdmin')
              : t('admin.userDetail.account.standard')
          }}
        </dd>

        <dt>{{ t('admin.userDetail.account.created') }}</dt>
        <dd>{{ formatDateTime(detail.created_at) }}</dd>

        <dt>{{ t('admin.userDetail.account.lastSignIn') }}</dt>
        <dd>
          {{ detail.last_login_at ? formatDateTime(detail.last_login_at) : t('admin.users.never') }}
        </dd>

        <dt>{{ t('admin.userDetail.account.sessions') }}</dt>
        <dd>{{ detail.active_sessions }}</dd>

        <dt>{{ t('admin.userDetail.account.ownedOrg') }}</dt>
        <dd>
          <RouterLink v-if="detail.owned_org" :to="`/admin/orgs/${detail.owned_org.id}`">
            {{ detail.owned_org.name }}
          </RouterLink>
          <span v-else class="admin-muted">{{ t('admin.userDetail.account.noOrg') }}</span>
        </dd>
      </dl>
    </PxPanel>

    <PxPanel
      v-if="detail.suspension"
      :title="t('admin.userDetail.suspension.title')"
      :description="t('admin.userDetail.suspension.description')"
    >
      <dl class="px-definition-list">
        <dt>{{ t('admin.userDetail.suspension.reason') }}</dt>
        <dd>{{ detail.suspension.reason ?? t('admin.userDetail.suspension.noReason') }}</dd>

        <dt>{{ t('admin.userDetail.suspension.at') }}</dt>
        <dd>{{ formatDateTime(detail.suspension.suspended_at) }}</dd>

        <dt>{{ t('admin.userDetail.suspension.by') }}</dt>
        <dd>
          {{
            detail.suspension.suspended_by?.display_name ??
            t('admin.userDetail.suspension.system')
          }}
        </dd>
      </dl>
    </PxPanel>

    <PxPanel
      :title="t('admin.userDetail.memberships.title')"
      :description="t('admin.userDetail.memberships.description')"
      flush
    >
      <table class="px-table">
        <caption class="px-visually-hidden">
          {{ t('admin.userDetail.memberships.caption') }}
        </caption>
        <thead>
          <tr>
            <th scope="col">{{ t('admin.userDetail.memberships.org') }}</th>
            <th scope="col">{{ t('admin.userDetail.memberships.role') }}</th>
            <th scope="col">{{ t('admin.userDetail.memberships.joined') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="membership in detail.memberships" :key="membership.org_id">
            <td class="px-table__cell--primary">
              <RouterLink :to="`/admin/orgs/${membership.org_id}`">
                {{ membership.org_name }}
              </RouterLink>
            </td>
            <td :data-label="t('admin.userDetail.memberships.role')">
              {{ t(`admin.roles.${membership.role}`) }}
            </td>
            <td :data-label="t('admin.userDetail.memberships.joined')">
              {{ formatDateTime(membership.joined_at) }}
            </td>
          </tr>
          <tr v-if="detail.memberships.length === 0">
            <td colspan="3">
              <p class="admin-muted admin-empty-cell">
                {{ t('admin.userDetail.memberships.empty') }}
              </p>
            </td>
          </tr>
        </tbody>
      </table>
    </PxPanel>

    <PxPanel
      :title="t('admin.userDetail.actions.title')"
      :description="t('admin.userDetail.actions.description')"
    >
      <div class="admin-actions">
        <div class="admin-actions__group">
          <h3 class="admin-actions__heading">{{ t('admin.userDetail.actions.accountHeading') }}</h3>

          <PxNotice v-if="canUnsuspend" tone="info">
            {{ t('admin.userDetail.actions.unsuspendHint') }}
          </PxNotice>

          <PxButton v-if="canUnsuspend" variant="secondary" @click="openAction('unsuspend')">
            {{ t('admin.userDetail.actions.unsuspend') }}
          </PxButton>

          <template v-else-if="canSuspend">
            <p class="admin-actions__description">
              {{ t('admin.userDetail.actions.suspendHint') }}
            </p>
            <PxButton variant="danger" @click="openAction('suspend')">
              {{ t('admin.userDetail.actions.suspend') }}
            </PxButton>
          </template>

          <PxNotice v-else-if="isSelf" tone="info">
            {{ t('errors.CANNOT_SUSPEND_SELF') }}
          </PxNotice>

          <PxNotice v-else-if="targetIsAdmin" tone="info">
            {{ t('errors.ADMIN_CANNOT_TARGET_ADMIN') }}
          </PxNotice>
        </div>

        <div class="admin-actions__group admin-actions__group--separated">
          <h3 class="admin-actions__heading">
            {{ t('admin.userDetail.actions.sessionsHeading') }}
          </h3>
          <p class="admin-actions__description">
            {{ t('admin.userDetail.actions.forceLogoutHint') }}
          </p>
          <PxButton
            variant="secondary"
            :disabled="!hasSessions"
            @click="openAction('force-logout')"
          >
            {{ t('admin.userDetail.actions.forceLogout') }}
          </PxButton>
          <p v-if="!hasSessions" class="admin-actions__note">
            {{ t('admin.userDetail.actions.noSessions') }}
          </p>
        </div>
      </div>
    </PxPanel>

    <StepUpDialog
      v-model:open="dialogOpen"
      :title="dialogTitle"
      :description="dialogDescription"
      :confirm-label="dialogConfirmLabel"
      :busy="actionBusy"
      :requires-password="dialogRequiresPassword"
      :error="actionError"
      @confirm="onConfirm"
    />
  </div>
</template>

<style scoped>
.admin-mono {
  font-family: var(--px-font-mono);
  font-size: var(--px-font-size-mono-sm);
}

.admin-muted {
  color: var(--px-color-text-tertiary);
}

.admin-empty-cell {
  padding-block: var(--px-space-6);
  text-align: center;
}

.admin-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--px-space-6);
}

.admin-actions__group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--px-space-3);
}

.admin-actions__group--separated {
  padding-inline-start: var(--px-space-6);
  border-inline-start: var(--px-border-width) solid var(--px-color-border-subtle);
}

.admin-actions__heading {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-control);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-control);
}

.admin-actions__description,
.admin-actions__note {
  max-width: 420px;
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  line-height: var(--px-line-body);
}

.admin-actions__note {
  color: var(--px-color-text-tertiary);
  font-size: var(--px-font-size-caption);
}

@media (max-width: 767px) {
  .admin-actions {
    grid-template-columns: minmax(0, 1fr);
  }

  .admin-actions__group--separated {
    margin-block-start: var(--px-space-3);
    padding-block-start: var(--px-space-5);
    padding-inline-start: 0;
    border-block-start: var(--px-border-width) solid var(--px-color-border-subtle);
    border-inline-start: 0;
  }
}
</style>
