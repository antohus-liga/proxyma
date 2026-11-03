import { computed, ref, type Ref } from 'vue'
import { asApiError, type ApiError } from '@/lib/api'

export type CursorPage<T> = {
  items: T[]
  next_cursor: string | null
}

export type CursorListLoader<T> = (cursor: string | null) => Promise<CursorPage<T>>

type CursorListStatus = 'idle' | 'loading' | 'ready' | 'error'

export function useCursorList<T extends { id: string }>(loader: CursorListLoader<T>) {
  const items = ref([]) as Ref<T[]>
  const nextCursor = ref<string | null>(null)
  const status = ref<CursorListStatus>('idle')
  const loadingMore = ref(false)
  const error = ref<ApiError | null>(null)
  const loadMoreError = ref<ApiError | null>(null)

  let requestId = 0

  const isLoading = computed(() => status.value === 'loading')
  const isLoadingMore = computed(() => loadingMore.value)
  const hasMore = computed(() => nextCursor.value !== null)

  async function load(reset = true): Promise<void> {
    const currentRequest = ++requestId

    if (reset) {
      items.value = []
      nextCursor.value = null
      error.value = null
    }

    loadMoreError.value = null
    status.value = reset ? 'loading' : 'ready'
    loadingMore.value = !reset

    try {
      const page = await loader(reset ? null : nextCursor.value)

      if (currentRequest !== requestId) {
        return
      }

      if (reset) {
        items.value = page.items
      } else {
        const existing = new Set(items.value.map((item) => item.id))
        items.value = [
          ...items.value,
          ...page.items.filter((item) => !existing.has(item.id)),
        ]
      }

      nextCursor.value = page.next_cursor
      status.value = 'ready'
    } catch (cause) {
      if (currentRequest !== requestId) {
        return
      }

      if (reset) {
        error.value = asApiError(cause)
        status.value = 'error'
      } else {
        loadMoreError.value = asApiError(cause)
        status.value = 'ready'
      }
    } finally {
      if (currentRequest === requestId) {
        loadingMore.value = false
      }
    }
  }

  function loadMore(): Promise<void> {
    if (!nextCursor.value || status.value !== 'ready') {
      return Promise.resolve()
    }

    return load(false)
  }

  function reset() {
    requestId += 1
    items.value = []
    nextCursor.value = null
    error.value = null
    loadMoreError.value = null
    status.value = 'idle'
  }

  return {
    items,
    nextCursor,
    status,
    error,
    loadMoreError,
    isLoading,
    isLoadingMore,
    hasMore,
    load,
    loadMore,
    reset,
  }
}
