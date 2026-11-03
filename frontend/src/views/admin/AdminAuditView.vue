<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@lucide/vue'
import AdminErrorState from '@/components/admin/AdminErrorState.vue'
import NotFoundContent from '@/components/layout/NotFoundContent.vue'
import PxBadge from '@/components/ui/PxBadge.vue'
import PxButton from '@/components/ui/PxButton.vue'
import PxDialog from '@/components/ui/PxDialog.vue'
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
  ADMIN_AUDIT_ACTIONS,
  adminApi,
  type AdminAuditAction,
  type AdminAuditEntry,
} from '@/lib/admin-api'
import { isNotFoundError } from '@/lib/api-errors'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { timeZoneLabel, formatDateTime, formatRelativeOrAbsolute } = useFormatters()

function queryString(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function queryInstant(value: unknown): string {
  const raw = queryString(value)
  return raw && !Number.isNaN(Date.parse(raw)) ? raw : ''
}

function queryAction(value: unknown): AdminAuditAction | '' {
  return ADMIN_AUDIT_ACTIONS.includes(value as AdminAuditAction)
    ? (value as AdminAuditAction)
    : ''
}

function toLocalInput(iso: string): string {
  if (!iso) {
    return ''
  }

  const date = new Date(iso)

  if (Number.isNaN(date.getTime())) {
    return ''
  }

  const pad = (value: number) => String(value).padStart(2, '0')

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`
}

function fromLocalInput(value: string): string {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toISOString()
}

const fromInput = ref(toLocalInput(queryInstant(route.query.from)))
const toInput = ref(toLocalInput(queryInstant(route.query.to)))
const actionInput = ref<AdminAuditAction | ''>(queryAction(route.query.action))
const rangeError = ref<string | null>(null)

const filters = computed(() => ({
  from: queryInstant(route.query.from),
  to: queryInstant(route.query.to),
  action: queryAction(route.query.action),
}))

const filtersKey = computed(() => `${filters.value.from}|${filters.value.to}|${filters.value.action}`)

const {
  items,
  error,
  loadMoreError,
  isLoading,
  isLoadingMore,
  hasMore,
  load,
  loadMore,
} = useCursorList<AdminAuditEntry>((cursor) =>
  adminApi.listAudit({
    from: filters.value.from || undefined,
    to: filters.value.to || undefined,
    action: filters.value.action || undefined,
    cursor,
  }),
)

const notFound = computed(() => error.value !== null && isNotFoundError(error.value))

const selectedEntry = ref<AdminAuditEntry | null>(null)
const detailsOpen = computed({
  get: () => selectedEntry.value !== null,
  set: (value: boolean) => {
    if (!value) {
      selectedEntry.value = null
    }
  },
})

function syncInputsFromRoute() {
  fromInput.value = toLocalInput(filters.value.from)
  toInput.value = toLocalInput(filters.value.to)
  actionInput.value = filters.value.action
  rangeError.value = null
}

watch(
  filtersKey,
  () => {
    syncInputsFromRoute()
    void load()
  },
  { immediate: true },
)

function actionLabel(action: string): string {
  if (ADMIN_AUDIT_ACTIONS.includes(action as AdminAuditAction)) {
    return t(`admin.audit.actions.${action}`)
  }

  return action
}

function targetLabel(entry: AdminAuditEntry): string {
  const type = entry.target?.type
  const id = entry.target?.id

  if (!type && !id) {
    return t('admin.audit.noTarget')
  }

  return [type, id].filter(Boolean).join(' · ')
}

async function applyFilters() {
  rangeError.value = null

  const from = fromLocalInput(fromInput.value)
  const to = fromLocalInput(toInput.value)

  if (from && to && Date.parse(from) > Date.parse(to)) {
    rangeError.value = t('admin.audit.invalidRange')
    return
  }

  const query: Record<string, string> = {}

  if (from) {
    query.from = from
  }

  if (to) {
    query.to = to
  }

  if (actionInput.value) {
    query.action = actionInput.value
  }

  await router.replace({ query })
}

async function clearFilters() {
  fromInput.value = ''
  toInput.value = ''
  actionInput.value = ''
  rangeError.value = null
  await router.replace({ query: {} })
}
</script>

<template>
  <PxPageHeader v-if="!notFound" :title="t('admin.audit.title')" :description="t('admin.audit.description')">
    <template #actions>
      <PxButton variant="secondary" :loading="isLoading" @click="load()">
        {{ t('admin.refresh') }}
      </PxButton>
    </template>
  </PxPageHeader>

  <NotFoundContent v-if="notFound" back-to="/app" :back-label="t('nav.backToDashboard')" />

  <AdminErrorState v-else-if="error" :error="error" @retry="load()" />

  <div v-else class="px-page-stack">
    <form class="px-filter-bar" @submit.prevent="applyFilters">
      <div class="px-filter-bar__field">
        <label class="admin-filter__label" for="admin-audit-from">
          {{ t('admin.audit.from') }}
        </label>
        <PxInput id="admin-audit-from" v-model="fromInput" type="datetime-local" />
      </div>

      <div class="px-filter-bar__field">
        <label class="admin-filter__label" for="admin-audit-to">{{ t('admin.audit.to') }}</label>
        <PxInput id="admin-audit-to" v-model="toInput" type="datetime-local" />
      </div>

      <div class="px-filter-bar__field admin-filter__select">
        <label class="admin-filter__label" for="admin-audit-action">
          {{ t('admin.audit.action') }}
        </label>
        <PxSelect id="admin-audit-action" v-model="actionInput">
          <option value="">{{ t('admin.audit.allActions') }}</option>
          <option v-for="action in ADMIN_AUDIT_ACTIONS" :key="action" :value="action">
            {{ actionLabel(action) }}
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

      <p class="admin-filter__help">
        {{ t('admin.audit.timeHelp', { timezone: timeZoneLabel }) }}
      </p>

      <p v-if="rangeError" class="admin-filter__error" role="alert">{{ rangeError }}</p>
    </form>

    <PxPanel flush>
      <PxTable :caption="t('admin.audit.tableCaption')">
        <thead>
          <tr>
            <th scope="col">{{ t('admin.audit.columns.time') }}</th>
            <th scope="col">{{ t('admin.audit.columns.actor') }}</th>
            <th scope="col">{{ t('admin.audit.columns.action') }}</th>
            <th scope="col">{{ t('admin.audit.columns.target') }}</th>
            <th scope="col">{{ t('admin.audit.columns.summary') }}</th>
            <th scope="col">{{ t('admin.audit.columns.sourceIp') }}</th>
            <th scope="col" class="px-table__actions">{{ t('admin.audit.columns.details') }}</th>
          </tr>
        </thead>
        <tbody>
          <template v-if="isLoading && items.length === 0">
            <tr v-for="index in 6" :key="`skeleton-${index}`">
              <td class="px-table__cell--primary"><PxSkeleton width="150px" /></td>
              <td :data-label="t('admin.audit.columns.actor')"><PxSkeleton width="100px" /></td>
              <td :data-label="t('admin.audit.columns.action')"><PxSkeleton width="120px" /></td>
              <td :data-label="t('admin.audit.columns.target')"><PxSkeleton width="120px" /></td>
              <td :data-label="t('admin.audit.columns.summary')"><PxSkeleton width="220px" /></td>
              <td :data-label="t('admin.audit.columns.sourceIp')"><PxSkeleton width="90px" /></td>
              <td class="px-table__actions"><PxSkeleton width="48px" /></td>
            </tr>
          </template>

          <template v-else>
            <tr v-for="entry in items" :key="entry.id">
              <td class="px-table__cell--primary">
                <span :title="formatDateTime(entry.timestamp)">
                  {{ formatRelativeOrAbsolute(entry.timestamp) }}
                </span>
              </td>
              <td :data-label="t('admin.audit.columns.actor')">
                <span v-if="entry.admin">{{ entry.admin.display_name }}</span>
                <PxBadge v-else tone="neutral">{{ t('admin.audit.systemActor') }}</PxBadge>
              </td>
              <td :data-label="t('admin.audit.columns.action')">{{ actionLabel(entry.action) }}</td>
              <td :data-label="t('admin.audit.columns.target')" class="admin-mono admin-target">
                {{ targetLabel(entry) }}
              </td>
              <td :data-label="t('admin.audit.columns.summary')" class="admin-summary">
                {{ entry.summary }}
              </td>
              <td :data-label="t('admin.audit.columns.sourceIp')" class="admin-mono">
                {{ entry.source_ip ?? t('admin.audit.noSourceIp') }}
              </td>
              <td class="px-table__actions">
                <PxButton variant="ghost" size="sm" @click="selectedEntry = entry">
                  {{ t('admin.audit.details') }}
                </PxButton>
              </td>
            </tr>

            <tr v-if="!isLoading && items.length === 0">
              <td colspan="7">
                <PxEmptyState
                  :title="t('admin.audit.empty.title')"
                  :description="t('admin.audit.empty.description')"
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
          {{ t('admin.audit.loaded', { count: items.length }) }}
          {{ hasMore ? t('admin.list.moreAvailable') : t('admin.list.allLoaded') }}
        </p>
        <PxButton v-if="hasMore" variant="secondary" :loading="isLoadingMore" @click="loadMore">
          {{ t('admin.list.loadMore') }}
        </PxButton>
      </div>
    </PxPanel>

    <PxDialog
      v-model:open="detailsOpen"
      :title="t('admin.audit.detailsTitle')"
      :description="t('admin.audit.detailsDescription')"
      :close-label="t('common.close')"
      size="md"
    >
      <dl v-if="selectedEntry" class="px-definition-list">
        <dt>{{ t('admin.audit.detailsFields.time') }}</dt>
        <dd>{{ formatDateTime(selectedEntry.timestamp) }}</dd>

        <dt>{{ t('admin.audit.detailsFields.actor') }}</dt>
        <dd>
          {{ selectedEntry.admin?.display_name ?? t('admin.audit.systemActor') }}
        </dd>

        <dt>{{ t('admin.audit.detailsFields.action') }}</dt>
        <dd class="admin-mono">{{ selectedEntry.action }}</dd>

        <dt>{{ t('admin.audit.detailsFields.target') }}</dt>
        <dd>
          <span class="admin-mono">{{ targetLabel(selectedEntry) }}</span>
        </dd>

        <dt>{{ t('admin.audit.detailsFields.summary') }}</dt>
        <dd>{{ selectedEntry.summary }}</dd>

        <dt>{{ t('admin.audit.detailsFields.sourceIp') }}</dt>
        <dd class="admin-mono">
          {{ selectedEntry.source_ip ?? t('admin.audit.noSourceIp') }}
        </dd>
      </dl>

      <template #footer>
        <PxButton variant="secondary" @click="detailsOpen = false">
          {{ t('common.close') }}
        </PxButton>
      </template>
    </PxDialog>
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
  max-width: 240px;
}

.admin-filter__help {
  width: 100%;
  color: var(--px-color-text-tertiary);
  font-size: var(--px-font-size-caption);
  line-height: var(--px-line-compact);
}

.admin-filter__error {
  width: 100%;
  color: var(--px-color-danger-fg);
  font-size: var(--px-font-size-caption);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.admin-mono {
  font-family: var(--px-font-mono);
  font-size: var(--px-font-size-mono-sm);
}

.admin-target {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-summary {
  max-width: 360px;
}

.admin-load-error {
  margin: var(--px-space-4);
}

@media (max-width: 767px) {
  .admin-filter__select {
    max-width: none;
  }

  .admin-target {
    max-width: none;
    white-space: normal;
  }
}
</style>
