import { describe, expect, it, vi } from 'vitest'
import { useCursorList } from '../useCursorList'

type Item = { id: string }
type Page = { items: Item[]; next_cursor: string | null }
type Loader = (cursor: string | null) => Promise<Page>

function deferred<T>() {
  let resolve: (value: T) => void = () => {}
  let reject: (reason: unknown) => void = () => {}
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })

  return { promise, resolve, reject }
}

describe('useCursorList', () => {
  it('loads the first page and exposes the next cursor', async () => {
    const loader = vi.fn<Loader>().mockResolvedValue({
      items: [{ id: 'a' }],
      next_cursor: 'cursor-1',
    })
    const list = useCursorList<Item>(loader)

    const loading = list.load()

    expect(list.isLoading.value).toBe(true)
    await loading

    expect(loader).toHaveBeenCalledWith(null)
    expect(list.items.value).toEqual([{ id: 'a' }])
    expect(list.nextCursor.value).toBe('cursor-1')
    expect(list.hasMore.value).toBe(true)
    expect(list.status.value).toBe('ready')
    expect(list.isLoading.value).toBe(false)
  })

  it('appends a page and drops duplicate ids', async () => {
    const loader = vi
      .fn<Loader>()
      .mockResolvedValueOnce({ items: [{ id: 'a' }, { id: 'b' }], next_cursor: 'cursor-1' })
      .mockResolvedValueOnce({ items: [{ id: 'b' }, { id: 'c' }], next_cursor: null })
    const list = useCursorList<Item>(loader)

    await list.load()
    await list.loadMore()

    expect(loader).toHaveBeenLastCalledWith('cursor-1')
    expect(list.items.value).toEqual([{ id: 'a' }, { id: 'b' }, { id: 'c' }])
    expect(list.hasMore.value).toBe(false)
  })

  it('tracks load-more progress and leaves existing rows on failure', async () => {
    const pending = deferred<Page>()
    const loader = vi
      .fn<Loader>()
      .mockResolvedValueOnce({ items: [{ id: 'a' }], next_cursor: 'cursor-1' })
      .mockReturnValueOnce(pending.promise)
    const list = useCursorList<Item>(loader)

    await list.load()
    const more = list.loadMore()

    expect(list.isLoadingMore.value).toBe(true)

    pending.reject(new Error('offline'))
    await more

    expect(list.isLoadingMore.value).toBe(false)
    expect(list.items.value).toEqual([{ id: 'a' }])
    expect(list.loadMoreError.value?.code).toBe('UNKNOWN')
    expect(list.status.value).toBe('ready')
  })

  it('resets the cursor and rows when a filter reloads the list', async () => {
    const loader = vi
      .fn<Loader>()
      .mockResolvedValueOnce({ items: [{ id: 'a' }], next_cursor: 'cursor-1' })
      .mockResolvedValueOnce({ items: [{ id: 'b' }], next_cursor: null })
    const list = useCursorList<Item>(loader)

    await list.load()
    await list.load()

    expect(loader).toHaveBeenLastCalledWith(null)
    expect(list.items.value).toEqual([{ id: 'b' }])
    expect(list.nextCursor.value).toBeNull()
  })

  it('does not load more without a cursor', async () => {
    const loader = vi.fn<Loader>().mockResolvedValue({ items: [], next_cursor: null })
    const list = useCursorList<Item>(loader)

    await list.load()
    await list.loadMore()

    expect(loader).toHaveBeenCalledTimes(1)
  })
})
