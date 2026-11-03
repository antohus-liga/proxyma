import type { ApiError } from '@/lib/api'

export type AuthField = 'displayName' | 'email' | 'password'

export type AuthFieldErrors = Partial<Record<AuthField, string>>

export type AuthErrorMap = {
  formError?: string
  fieldErrors: AuthFieldErrors
  emailTaken?: boolean
  suspended?: boolean
  retryAfterSeconds?: number
}

const FIELD_BY_PATH: Record<string, AuthField> = {
  '/display_name': 'displayName',
  '/email': 'email',
  '/password': 'password',
}

const FIELD_ERROR_BY_PATH: Record<string, string> = {
  '/display_name': 'auth.fieldErrors.displayName.invalid',
  '/email': 'auth.fieldErrors.email.invalid',
  '/password': 'auth.fieldErrors.password.invalid',
}

const FORM_ERROR_BY_CODE: Record<string, string> = {
  INVALID_CREDENTIALS: 'errors.INVALID_CREDENTIALS',
  RATE_LIMITED: 'errors.RATE_LIMITED',
  EMAIL_TAKEN: 'errors.EMAIL_TAKEN',
  WEAK_PASSWORD: 'errors.WEAK_PASSWORD',
  VALIDATION_FAILED: 'errors.VALIDATION_FAILED',
  MALFORMED_REQUEST: 'errors.MALFORMED_REQUEST',
  INTERNAL_ERROR: 'errors.INTERNAL_ERROR',
  NETWORK_ERROR: 'errors.NETWORK_ERROR',
  TIMEOUT: 'errors.TIMEOUT',
  CORS_REJECTED: 'errors.CORS_REJECTED',
  FORBIDDEN: 'errors.FORBIDDEN',
  UNAUTHORIZED: 'errors.UNAUTHORIZED',
  INVALID_REFRESH_TOKEN: 'errors.INVALID_REFRESH_TOKEN',
  REFRESH_IN_PROGRESS: 'errors.UNKNOWN',
  UNKNOWN: 'errors.UNKNOWN',
}

export function mapAuthError(error: ApiError): AuthErrorMap {
  const result: AuthErrorMap = { fieldErrors: {} }

  if (error.code === 'ACCOUNT_SUSPENDED') {
    result.suspended = true
    return result
  }

  if (error.code === 'EMAIL_TAKEN') {
    result.fieldErrors.email = 'errors.EMAIL_TAKEN'
    result.emailTaken = true
    return result
  }

  if (error.retryAfterSeconds !== null) {
    result.retryAfterSeconds = error.retryAfterSeconds
  }

  if (error.problem?.errors?.length) {
    for (const fieldError of error.problem.errors) {
      const field = FIELD_BY_PATH[fieldError.path]
      const message = FIELD_ERROR_BY_PATH[fieldError.path]

      if (field && message) {
        result.fieldErrors[field] = message
      }
    }

    if (error.code === 'WEAK_PASSWORD') {
      result.fieldErrors.password = 'errors.WEAK_PASSWORD'
    }

    if (Object.keys(result.fieldErrors).length > 0) {
      return result
    }
  }

  if (error.code === 'WEAK_PASSWORD') {
    result.fieldErrors.password = 'errors.WEAK_PASSWORD'
    return result
  }

  result.formError = FORM_ERROR_BY_CODE[error.code] ?? 'errors.UNKNOWN'
  return result
}
