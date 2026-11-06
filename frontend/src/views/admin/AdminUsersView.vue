<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { Search } from '@lucide/vue'
import AdminErrorState from '@/components/admin/AdminErrorState.vue'
import AdminStatusBadge from '@/components/admin/AdminStatusBadge.vue'
import NotFoundContent from '@/components/layout/NotFoundContent.vue'
import PxButton from '@/components/ui/PxButton.vue'
import PxEmptyState from '@/components/ui/PxEmptyState.vue'
import PxInput from '@/components/ui/PxInput.vue'
import PxNotice from '@/components/ui/PxNotice.vue'
import PxPageHeader from '@/components/ui/PxPageHeader.vue'
import PxPanel from '@/components/ui/PxPanel.vue'
import PxSelect from '@/components/ui/PxSelect.vue'
import PxSkeleton from '@/components/ui/PxSkeleton.vue'
import PxTable from '@/components/ui/PxTable.vue'
import { useCursorList } from '@/composables/useCursorList'
import { useFormatters } from '@/composables/useFormatters'
import { adminApi, type AdminUserStatus, type AdminUserSummary } from '@/lib/admin-api'
import { isNotFoundError } from '@/lib/api-errors'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { formatDate, formatRelativeOrAbsolute } = useFormatters()

function queryString(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function queryStatus(value: unknown): AdminUserStatus | '' {
  return value === 'active' || value === 'suspended' ? value : ''
}

const searchInput = ref(queryString(route.query.q))
const statusInput = ref<AdminUserStatus | ''>(queryStatus(route.query.status))

const filters = computed(() => ({
  q: queryString(route.query.q),
  status: queryStatus(route.query.status),
}))

const filtersKey = computed(() => `${filters.value.q}|${filters.value.status}`)

const {
  items,
  error,
  loadMoreError,
  isLoading,
  isLoadingMore,
  hasMore,
  load,
  loadMore,
} = useCursorList<AdminUserSummary>((cursor) =>
  adminApi.listUsers({
    q: filters.value.q || undefined,
    status: filters.value.status || undefined,
    cursor,
  }),
)

const notFound = computed(() => error.value !== null && isNotFoundError(error.value))

function syncInputsFromRoute() {
  searchInput.value = filters.value.q
  statusInput.value = filters.value.status
}

watch(
  filtersKey,
  () => {
    syncInputsFromRoute()
    void load()
  },
  { immediate: true },
)

async function applyFilters() {
  const query: Record<string, string> = {}

  if (searchInput.value.trim()) {
    query.q = searchInput.value.trim()
  }

  if (statusInput.value) {
    query.status = statusInput.value
  }

  await router.replace({ query })
}

async function clearFilters() {
  searchInput.value = ''
  statusInput.value = ''
  await router.replace({ query: {} })
}
</script>

<template>
  <PxPageHeader v-if="!notFound" :title="t('admin.users.title')" :description="t('admin.users.description')">
    <template #actions>
      <PxButton variant="secondary" :loading="isLoading" @click="load()">
        {{ t('admin.refresh') }}
      </PxButton>
    </template>
  </PxPageHeader>

  <NotFoundContent
    v-if="notFound"
    back-to="/settings/account"
    :back-label="t('nav.backToSettings')"
  />

  <AdminErrorState v-else-if="error" :error="error" @retry="load()" />

  <div v-else class="px-page-stack">
    <form class="px-filter-bar" @submit.prevent="applyFilters">
      <div class="px-filter-bar__field">
        <label class="admin-filter__label" for="admin-users-q">{{ t('admin.users.search') }}</label>
        <PxInput
          id="admin-users-q"
          v-model="searchInput"
          type="search"
          :placeholder="t('admin.users.searchPlaceholder')"
          autocomplete="off"
        />
      </div>

      <div class="px-filter-bar__field admin-filter__select">
        <label class="admin-filter__label" for="admin-users-status">
          {{ t('admin.users.status') }}
        </label>
        <PxSelect id="admin-users-status" v-model="statusInput">
          <option value="">{{ t('admin.users.allStatuses') }}</option>
          <option value="active">{{ t('admin.status.user.active') }}</option>
          <option value="suspended">{{ t('admin.status.user.suspended') }}</option>
        </PxSelect>
      </div>

      <div class="px-filter-bar__actions">
        <PxButton type="submit" variant="secondary">
          <template #icon>
            <Search :size="15" aria-hidden="true" />
          </template>
          {{ t('admin.filters.apply') }}
        </PxButton>
        <PxButton variant="ghost" @click="clearFilters">{{ t('admin.filters.clear') }}</PxButton>
      </div>
    </form>

    <PxPanel flush>
      <PxTable :caption="t('admin.users.tableCaption')">
        <thead>
          <tr>
            <th scope="col">{{ t('admin.users.columns.user') }}</th>
            <th scope="col">{{ t('admin.users.columns.status') }}</th>
            <th scope="col">{{ t('admin.users.columns.orgs') }}</th>
            <th scope="col">{{ t('admin.users.columns.lastSignIn') }}</th>
            <th scope="col">{{ t('admin.users.columns.created') }}</th>
            <th scope="col" class="px-table__actions">{{ t('admin.users.columns.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <template v-if="isLoading && items.length === 0">
            <tr v-for="index in 5" :key="`skeleton-${index}`">
              <td class="px-table__cell--primary"><PxSkeleton width="180px" /></td>
              <td :data-label="t('admin.users.columns.status')"><PxSkeleton width="72px" /></td>
              <td :data-label="t('admin.users.columns.orgs')"><PxSkeleton width="60px" /></td>
              <td :data-label="t('admin.users.columns.lastSignIn')"><PxSkeleton width="120px" /></td>
              <td :data-label="t('admin.users.columns.created')"><PxSkeleton width="100px" /></td>
              <td class="px-table__actions"><PxSkeleton width="48px" /></td>
            </tr>
          </template>

          <template v-else>
            <tr v-for="user in items" :key="user.id">
              <td class="px-table__cell--primary">
                <RouterLink class="admin-cell__link" :to="`/admin/users/${user.id}`">
                  {{ user.display_name }}
                </RouterLink>
                <span class="admin-cell__secondary">{{ user.email }}</span>
              </td>
              <td :data-label="t('admin.users.columns.status')">
                <AdminStatusBadge kind="user" :value="user.status" />
              </td>
              <td :data-label="t('admin.users.columns.orgs')">
                {{ t('admin.users.orgSummary', { count: user.membership_count }) }}
                <span v-if="user.owns_org" class="admin-cell__secondary">
                  {{ t('admin.users.ownsOrg') }}
                </span>
              </td>
              <td :data-label="t('admin.users.columns.lastSignIn')">
                {{
                  user.last_login_at
                    ? formatRelativeOrAbsolute(user.last_login_at)
                    : t('admin.users.never')
                }}
              </td>
              <td :data-label="t('admin.users.columns.created')">
                {{ formatDate(user.created_at) }}
              </td>
              <td class="px-table__actions">
                <PxButton variant="ghost" size="sm" :to="`/admin/users/${user.id}`">
                  {{ t('admin.users.view') }}
                </PxButton>
              </td>
            </tr>

            <tr v-if="!isLoading && items.length === 0">
              <td colspan="6">
                <PxEmptyState
                  :title="t('admin.users.empty.title')"
                  :description="t('admin.users.empty.description')"
                >
                  <template #action>
                    <PxButton variant="secondary" @click="clearFilters">
                      {{ t('admin.filters.clear') }}
                    </PxButton>
                  </template>
                </PxEmptyState>
              </td>
            </tr>
          </template>
        </tbody>
      </PxTable>

      <PxNotice v-if="loadMoreError" class="admin-load-error" tone="danger" role="alert">
        {{ t('admin.error.loadMore') }}
        <PxButton variant="link" size="sm" @click="loadMore">{{ t('admin.error.retry') }}</PxButton>
      </PxNotice>

      <div v-if="items.length > 0" class="px-load-more">
        <p class="px-load-more__count">
          {{ t('admin.users.loaded', { count: items.length }) }}
          {{ hasMore ? t('admin.list.moreAvailable') : t('admin.list.allLoaded') }}
        </p>
        <PxButton v-if="hasMore" variant="secondary" :loading="isLoadingMore" @click="loadMore">
          {{ t('admin.list.loadMore') }}
        </PxButton>
      </div>
    </PxPanel>
  </div>
</template>

<style scoped>
.admin-filter__label {
  display: block;
  margin-block-end: var(--px-space-1);
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-small-strong);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.admin-filter__select {
  max-width: 220px;
}

.admin-cell__link {
  display: block;
  font-weight: var(--px-font-weight-medium);
}

.admin-cell__secondary {
  display: block;
  margin-block-start: 2px;
  color: var(--px-color-text-tertiary);
  font-size: var(--px-font-size-caption);
  line-height: var(--px-line-compact);
}

.admin-load-error {
  margin: var(--px-space-4);
}

@media (max-width: 767px) {
  .admin-filter__select {
    max-width: none;
  }
}
</style>
