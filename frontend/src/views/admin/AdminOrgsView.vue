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
import {
  ADMIN_ORG_STATES,
  adminApi,
  type AdminOrgState,
  type AdminOrgSummary,
} from '@/lib/admin-api'
import { isNotFoundError } from '@/lib/api-errors'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { formatDate } = useFormatters()

function queryString(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function queryState(value: unknown): AdminOrgState | '' {
  return ADMIN_ORG_STATES.includes(value as AdminOrgState) ? (value as AdminOrgState) : ''
}

const searchInput = ref(queryString(route.query.q))
const stateInput = ref<AdminOrgState | ''>(queryState(route.query.state))

const filters = computed(() => ({
  q: queryString(route.query.q),
  state: queryState(route.query.state),
}))

const filtersKey = computed(() => `${filters.value.q}|${filters.value.state}`)

const {
  items,
  error,
  loadMoreError,
  isLoading,
  isLoadingMore,
  hasMore,
  load,
  loadMore,
} = useCursorList<AdminOrgSummary>((cursor) =>
  adminApi.listOrgs({
    q: filters.value.q || undefined,
    state: filters.value.state || undefined,
    cursor,
  }),
)

const notFound = computed(() => error.value !== null && isNotFoundError(error.value))

function syncInputsFromRoute() {
  searchInput.value = filters.value.q
  stateInput.value = filters.value.state
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

  if (stateInput.value) {
    query.state = stateInput.value
  }

  await router.replace({ query })
}

async function clearFilters() {
  searchInput.value = ''
  stateInput.value = ''
  await router.replace({ query: {} })
}
</script>

<template>
  <PxPageHeader v-if="!notFound" :title="t('admin.orgs.title')" :description="t('admin.orgs.description')">
    <template #actions>
      <PxButton variant="secondary" :loading="isLoading" @click="load()">
        {{ t('admin.refresh') }}
      </PxButton>
    </template>
  </PxPageHeader>

  <NotFoundContent v-if="notFound" back-to="/settings/account" :back-label="t('nav.backToSettings')" />

  <AdminErrorState v-else-if="error" :error="error" @retry="load()" />

  <div v-else class="px-page-stack">
    <form class="px-filter-bar" @submit.prevent="applyFilters">
      <div class="px-filter-bar__field">
        <label class="admin-filter__label" for="admin-orgs-q">{{ t('admin.orgs.search') }}</label>
        <PxInput
          id="admin-orgs-q"
          v-model="searchInput"
          type="search"
          :placeholder="t('admin.orgs.searchPlaceholder')"
          autocomplete="off"
        />
      </div>

      <div class="px-filter-bar__field admin-filter__select">
        <label class="admin-filter__label" for="admin-orgs-state">
          {{ t('admin.orgs.state') }}
        </label>
        <PxSelect id="admin-orgs-state" v-model="stateInput">
          <option value="">{{ t('admin.orgs.allStates') }}</option>
          <option v-for="state in ADMIN_ORG_STATES" :key="state" :value="state">
            {{ t(`admin.status.org.${state}`) }}
          </option>
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
      <PxTable :caption="t('admin.orgs.tableCaption')">
        <thead>
          <tr>
            <th scope="col">{{ t('admin.orgs.columns.org') }}</th>
            <th scope="col">{{ t('admin.orgs.columns.state') }}</th>
            <th scope="col">{{ t('admin.orgs.columns.owner') }}</th>
            <th scope="col" class="px-table__numeric">{{ t('admin.orgs.columns.members') }}</th>
            <th scope="col">{{ t('admin.orgs.columns.created') }}</th>
            <th scope="col" class="px-table__actions">{{ t('admin.orgs.columns.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <template v-if="isLoading && items.length === 0">
            <tr v-for="index in 5" :key="`skeleton-${index}`">
              <td class="px-table__cell--primary"><PxSkeleton width="180px" /></td>
              <td :data-label="t('admin.orgs.columns.state')"><PxSkeleton width="80px" /></td>
              <td :data-label="t('admin.orgs.columns.owner')"><PxSkeleton width="120px" /></td>
              <td :data-label="t('admin.orgs.columns.members')"><PxSkeleton width="32px" /></td>
              <td :data-label="t('admin.orgs.columns.created')"><PxSkeleton width="100px" /></td>
              <td class="px-table__actions"><PxSkeleton width="48px" /></td>
            </tr>
          </template>

          <template v-else>
            <tr v-for="org in items" :key="org.id">
              <td class="px-table__cell--primary">
                <RouterLink class="admin-cell__link" :to="`/admin/orgs/${org.id}`">
                  {{ org.name }}
                </RouterLink>
                <span class="admin-cell__secondary admin-mono">{{ org.id }}</span>
              </td>
              <td :data-label="t('admin.orgs.columns.state')">
                <AdminStatusBadge kind="org" :value="org.state" />
              </td>
              <td :data-label="t('admin.orgs.columns.owner')">
                <RouterLink
                  v-if="org.owner"
                  :to="`/admin/users/${org.owner.user_id}`"
                  class="admin-cell__link"
                >
                  {{ org.owner.display_name }}
                </RouterLink>
                <span v-else class="admin-cell__secondary">{{ t('admin.orgs.noOwner') }}</span>
              </td>
              <td :data-label="t('admin.orgs.columns.members')" class="px-table__numeric">
                {{ org.member_count }}
              </td>
              <td :data-label="t('admin.orgs.columns.created')">{{ formatDate(org.created_at) }}</td>
              <td class="px-table__actions">
                <PxButton variant="ghost" size="sm" :to="`/admin/orgs/${org.id}`">
                  {{ t('admin.orgs.view') }}
                </PxButton>
              </td>
            </tr>

            <tr v-if="!isLoading && items.length === 0">
              <td colspan="6">
                <PxEmptyState
                  :title="t('admin.orgs.empty.title')"
                  :description="t('admin.orgs.empty.description')"
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
          {{ t('admin.orgs.loaded', { count: items.length }) }}
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
