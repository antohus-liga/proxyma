import { computed, onScopeDispose, ref } from 'vue'

export function useRetryCountdown() {
  const seconds = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  const isBlocked = computed(() => seconds.value > 0)

  function stop() {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }

    seconds.value = 0
  }

  function start(durationSeconds: number) {
    stop()
    seconds.value = Math.max(0, Math.ceil(durationSeconds))

    if (seconds.value === 0) {
      return
    }

    timer = setInterval(() => {
      seconds.value = Math.max(0, seconds.value - 1)

      if (seconds.value === 0) {
        stop()
      }
    }, 1000)
  }

  onScopeDispose(stop)

  return {
    seconds,
    isBlocked,
    start,
    stop,
  }
}
