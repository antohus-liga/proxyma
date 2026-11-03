<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { Building2, ScrollText, Users } from '@lucide/vue'
import AdminErrorState from '@/components/admin/AdminErrorState.vue'
import NotFoundContent from '@/components/layout/NotFoundContent.vue'
import PxButton from '@/components/ui/PxButton.vue'
import PxMetricCard from '@/components/ui/PxMetricCard.vue'
import PxPageHeader from '@/components/ui/PxPageHeader.vue'
import PxPanel from '@/components/ui/PxPanel.vue'
import PxSkeleton from '@/components/ui/PxSkeleton.vue'
import { useFormatters } from '@/composables/useFormatters'
import { adminApi, type AdminStats } from '@/lib/admin-api'
import { asApiError, type ApiError } from '@/lib/api'
import { isNotFoundError } from '@/lib/api-errors'

const { t } = useI18n()
const { formatNumber } = useFormatters()

const stats = ref<AdminStats | null>(null)
const error = ref<ApiError | null>(null)
const notFound = ref(false)
const loading = ref(false)

async function load() {
  loading.value = true
  error.value = null

  try {
    stats.value = await adminApi.stats()
    notFound.value = false
  } catch (cause) {
    const apiError = asApiError(cause)

    if (isNotFoundError(apiError)) {
      notFound.value = true
      stats.value = null
    } else {
      error.value = apiError
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <PxPageHeader v-if="!notFound" :title="t('admin.overview.title')" :description="t('admin.overview.description')">
    <template #actions>
      <PxButton variant="secondary" :loading="loading" @click="load">
        {{ t('admin.refresh') }}
      </PxButton>
    </template>
  </PxPageHeader>

  <NotFoundContent
    v-if="notFound"
    back-to="/app"
    :back-label="t('nav.backToDashboard')"
  />

  <AdminErrorState v-else-if="error" :error="error" @retry="load" />

  <div v-else class="px-page-stack">
    <div v-if="loading && !stats" class="admin-metrics">
      <PxSkeleton v-for="index in 4" :key="index" height="118px" />
    </div>

    <div v-else-if="stats" class="admin-metrics">
      <PxMetricCard
        :label="t('admin.overview.metrics.users')"
        :value="formatNumber(stats.users.total)"
        :hint="t('admin.overview.metrics.usersHint')"
      />
      <PxMetricCard
        :label="t('admin.overview.metrics.suspended')"
        :value="formatNumber(stats.users.suspended)"
        :hint="t('admin.overview.metrics.suspendedHint')"
        :tone="stats.users.suspended > 0 ? 'warning' : 'default'"
      />
      <PxMetricCard
        :label="t('admin.overview.metrics.orgs')"
        :value="formatNumber(stats.orgs.total)"
        :hint="t('admin.overview.metrics.orgsHint')"
      />
      <PxMetricCard
        :label="t('admin.overview.metrics.online')"
        :value="formatNumber(stats.orgs.online)"
        :hint="t('admin.overview.metrics.onlineHint', { offline: formatNumber(stats.orgs.offline) })"
        :tone="stats.orgs.offline > 0 ? 'warning' : 'default'"
      />
    </div>

    <PxPanel :title="t('admin.overview.links.title')" :description="t('admin.overview.links.description')">
      <ul class="admin-links">
        <li>
          <RouterLink class="admin-links__item" to="/admin/users">
            <Users :size="18" aria-hidden="true" />
            <span>
              <span class="admin-links__title">{{ t('admin.overview.links.users') }}</span>
              <span class="admin-links__description">{{ t('admin.overview.links.usersDescription') }}</span>
            </span>
          </RouterLink>
        </li>
        <li>
          <RouterLink class="admin-links__item" to="/admin/orgs">
            <Building2 :size="18" aria-hidden="true" />
            <span>
              <span class="admin-links__title">{{ t('admin.overview.links.orgs') }}</span>
              <span class="admin-links__description">{{ t('admin.overview.links.orgsDescription') }}</span>
            </span>
          </RouterLink>
        </li>
        <li>
          <RouterLink class="admin-links__item" to="/admin/audit">
            <ScrollText :size="18" aria-hidden="true" />
            <span>
              <span class="admin-links__title">{{ t('admin.overview.links.audit') }}</span>
              <span class="admin-links__description">{{ t('admin.overview.links.auditDescription') }}</span>
            </span>
          </RouterLink>
        </li>
      </ul>
    </PxPanel>
  </div>
</template>

<style scoped>
.admin-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--px-space-4);
}

.admin-links {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--px-space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.admin-links__item {
  display: flex;
  min-height: 100%;
  align-items: flex-start;
  gap: var(--px-space-3);
  padding: var(--px-space-4);
  border: var(--px-border-width) solid var(--px-color-border-subtle);
  border-radius: var(--px-radius-control);
  color: var(--px-color-text-primary);
}

.admin-links__item:hover {
  border-color: var(--px-color-border-strong);
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.admin-links__title {
  display: block;
  font-size: var(--px-font-size-control);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-control);
}

.admin-links__description {
  display: block;
  margin-block-start: var(--px-space-1);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-caption);
  line-height: var(--px-line-compact);
}

@media (max-width: 1023px) {
  .admin-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-links {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .admin-metrics {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
