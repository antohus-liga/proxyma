<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'
import AdminErrorState from '@/components/admin/AdminErrorState.vue'
import AdminStatusBadge from '@/components/admin/AdminStatusBadge.vue'
import NotFoundContent from '@/components/layout/NotFoundContent.vue'
import PxBreadcrumbs from '@/components/ui/PxBreadcrumbs.vue'
import PxButton from '@/components/ui/PxButton.vue'
import PxNotice from '@/components/ui/PxNotice.vue'
import PxPageHeader from '@/components/ui/PxPageHeader.vue'
import PxPanel from '@/components/ui/PxPanel.vue'
import PxSkeleton from '@/components/ui/PxSkeleton.vue'
import { useFormatters } from '@/composables/useFormatters'
import { adminApi, type AdminOrgDetail } from '@/lib/admin-api'
import { asApiError, type ApiError } from '@/lib/api'
import { isNotFoundError } from '@/lib/api-errors'

const { t } = useI18n()
const route = useRoute()
const { formatDateTime, formatRelativeOrAbsolute } = useFormatters()

const orgId = computed(() => String(route.params.orgId ?? ''))
const detail = ref<AdminOrgDetail | null>(null)
const error = ref<ApiError | null>(null)
const notFound = ref(false)
const loading = ref(false)

async function load() {
  loading.value = true
  error.value = null

  try {
    detail.value = await adminApi.getOrg(orgId.value)
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

onMounted(load)

watch(orgId, () => {
  void load()
})
</script>

<template>
  <PxBreadcrumbs
    v-if="!notFound"
    :label="t('admin.breadcrumbs.label')"
    :items="[
      { label: t('admin.orgs.title'), to: '/admin/orgs' },
      { label: detail?.name ?? t('admin.orgDetail.title') },
    ]"
  />

  <PxPageHeader
    v-if="!notFound"
    :title="detail?.name ?? t('admin.orgDetail.title')"
    :description="t('admin.orgDetail.description')"
  >
    <template #actions>
      <PxButton variant="secondary" to="/admin/orgs">
        {{ t('admin.orgDetail.back') }}
      </PxButton>
    </template>
  </PxPageHeader>

  <NotFoundContent v-if="notFound" back-to="/admin/orgs" :back-label="t('admin.orgs.title')" />

  <AdminErrorState v-else-if="error" :error="error" @retry="load" />

  <div v-else-if="loading && !detail" class="px-page-stack">
    <PxSkeleton height="160px" />
    <PxSkeleton height="200px" />
  </div>

  <div v-else-if="detail" class="px-page-stack">
    <PxNotice tone="info">
      {{ t('admin.orgDetail.readOnly') }}
    </PxNotice>

    <PxPanel :title="t('admin.orgDetail.summary.title')">
      <template #actions>
        <AdminStatusBadge kind="org" :value="detail.state" />
      </template>

      <dl class="px-definition-list">
        <dt>{{ t('admin.orgDetail.summary.id') }}</dt>
        <dd class="admin-mono">{{ detail.id }}</dd>

        <dt>{{ t('admin.orgDetail.summary.state') }}</dt>
        <dd>{{ t(`admin.status.org.${detail.state}`) }}</dd>

        <dt>{{ t('admin.orgDetail.summary.created') }}</dt>
        <dd>{{ formatDateTime(detail.created_at) }}</dd>

        <dt>{{ t('admin.orgDetail.summary.owner') }}</dt>
        <dd>
          <RouterLink v-if="detail.owner" :to="`/admin/users/${detail.owner.user_id}`">
            {{ detail.owner.display_name }}
          </RouterLink>
          <span v-else class="admin-muted">{{ t('admin.orgs.noOwner') }}</span>
        </dd>
      </dl>

      <div v-if="detail.owner" class="admin-owner-actions">
        <p class="admin-owner-actions__text">
          {{ t('admin.orgDetail.summary.ownerActions') }}
        </p>
        <PxButton variant="secondary" :to="`/admin/users/${detail.owner.user_id}`">
          {{ t('admin.orgDetail.summary.openOwner') }}
        </PxButton>
      </div>
    </PxPanel>

    <PxPanel :title="t('admin.orgDetail.proxy.title')" :description="t('admin.orgDetail.proxy.description')">
      <template #actions>
        <AdminStatusBadge kind="config" :value="detail.proxy.config_status" />
      </template>

      <dl class="px-definition-list">
        <dt>{{ t('admin.orgDetail.proxy.lastHeartbeat') }}</dt>
        <dd>
          {{
            detail.proxy.last_heartbeat_at
              ? formatRelativeOrAbsolute(detail.proxy.last_heartbeat_at)
              : t('admin.orgDetail.proxy.never')
          }}
        </dd>

        <dt>{{ t('admin.orgDetail.proxy.runningVersion') }}</dt>
        <dd>
          {{
            detail.proxy.running_config_version ?? t('admin.orgDetail.proxy.notAvailable')
          }}
        </dd>

        <dt>{{ t('admin.orgDetail.proxy.latestVersion') }}</dt>
        <dd>
          {{ detail.proxy.latest_config_version ?? t('admin.orgDetail.proxy.notAvailable') }}
        </dd>

        <dt>{{ t('admin.orgDetail.proxy.configStatus') }}</dt>
        <dd>{{ t(`admin.status.config.${detail.proxy.config_status}`) }}</dd>

        <dt>{{ t('admin.orgDetail.proxy.activeBans') }}</dt>
        <dd>{{ detail.proxy.active_ban_count }}</dd>
      </dl>
    </PxPanel>

    <PxPanel
      :title="t('admin.orgDetail.members.title')"
      :description="t('admin.orgDetail.members.description')"
      flush
    >
      <table class="px-table">
        <caption class="px-visually-hidden">{{ t('admin.orgDetail.members.caption') }}</caption>
        <thead>
          <tr>
            <th scope="col">{{ t('admin.orgDetail.members.name') }}</th>
            <th scope="col">{{ t('admin.orgDetail.members.role') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="member in detail.members" :key="member.user_id">
            <td class="px-table__cell--primary">
              <RouterLink :to="`/admin/users/${member.user_id}`">
                {{ member.display_name }}
              </RouterLink>
            </td>
            <td :data-label="t('admin.orgDetail.members.role')">
              {{ t(`admin.roles.${member.role}`) }}
            </td>
          </tr>
          <tr v-if="detail.members.length === 0">
            <td colspan="2">
              <p class="admin-muted admin-empty-cell">
                {{ t('admin.orgDetail.members.empty') }}
              </p>
            </td>
          </tr>
        </tbody>
      </table>
    </PxPanel>
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

.admin-owner-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--px-space-3);
  margin-block-start: var(--px-space-5);
  padding-block-start: var(--px-space-4);
  border-block-start: var(--px-border-width) solid var(--px-color-border-subtle);
}

.admin-owner-actions__text {
  max-width: 560px;
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  line-height: var(--px-line-body);
}
</style>
