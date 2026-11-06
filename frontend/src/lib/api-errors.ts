import type { ApiError } from './api'

const MESSAGE_KEYS: Record<string, string> = {
  INVALID_CREDENTIALS: 'errors.INVALID_CREDENTIALS',
  ACCOUNT_SUSPENDED: 'errors.ACCOUNT_SUSPENDED',
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
  SESSION_REFRESH_DELAYED: 'errors.SESSION_REFRESH_DELAYED',
  REFRESH_IN_PROGRESS: 'errors.SESSION_REFRESH_DELAYED',
  NOT_FOUND: 'errors.NOT_FOUND',
  INVALID_CURSOR: 'errors.INVALID_CURSOR',
  INVALID_PARAMETER: 'errors.INVALID_PARAMETER',
  CONFLICT: 'errors.CONFLICT',
  REAUTH_FAILED: 'errors.REAUTH_FAILED',
  ADMIN_CANNOT_TARGET_ADMIN: 'errors.ADMIN_CANNOT_TARGET_ADMIN',
  CANNOT_SUSPEND_SELF: 'errors.CANNOT_SUSPEND_SELF',
  UNKNOWN: 'errors.UNKNOWN',
}

export function errorMessageKey(error: ApiError): string {
  return MESSAGE_KEYS[error.code] ?? 'errors.UNKNOWN'
}

export function isNotFoundError(error: ApiError): boolean {
  return error.status === 404 || error.code === 'NOT_FOUND'
}

export function hasProblemPath(error: ApiError, path: string): boolean {
  return error.problem?.errors?.some((item) => item.path === path) ?? false
}
