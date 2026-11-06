import { useI18n } from 'vue-i18n'
import type { ApiError } from '@/lib/api'
import { errorMessageKey } from '@/lib/api-errors'

export function useApiErrorMessage() {
  const { t } = useI18n()

  function messageFor(error: ApiError): string {
    return t(errorMessageKey(error), {
      seconds: error.retryAfterSeconds ?? 30,
    })
  }

  return { messageFor }
}
