import type { ApiError } from '@/lib/api'
import { errorMessageKey, hasProblemPath } from '@/lib/api-errors'

export type ProfileField = 'displayName'
export type PasswordField = 'currentPassword' | 'newPassword' | 'confirmPassword'

export type ProfileFieldErrors = Partial<Record<ProfileField, string>>
export type PasswordFieldErrors = Partial<Record<PasswordField, string>>

export type ProfileErrorMap = {
  fieldErrors: ProfileFieldErrors
  formError?: string
}

export type PasswordErrorMap = {
  fieldErrors: PasswordFieldErrors
  formError?: string
  retryAfterSeconds?: number
}

export function mapProfileError(error: ApiError): ProfileErrorMap {
  if (hasProblemPath(error, '/display_name')) {
    return {
      fieldErrors: {
        displayName: 'account.profile.fieldErrors.invalid',
      },
    }
  }

  return {
    fieldErrors: {},
    formError: errorMessageKey(error),
  }
}

export function mapPasswordError(error: ApiError): PasswordErrorMap {
  if (error.code === 'REAUTH_FAILED') {
    return {
      fieldErrors: {
        currentPassword: 'errors.REAUTH_FAILED',
      },
    }
  }

  if (error.code === 'WEAK_PASSWORD') {
    return {
      fieldErrors: {
        newPassword: 'errors.WEAK_PASSWORD',
      },
    }
  }

  if (hasProblemPath(error, '/current_password')) {
    return {
      fieldErrors: {
        currentPassword: 'account.password.fieldErrors.currentInvalid',
      },
    }
  }

  if (hasProblemPath(error, '/new_password')) {
    return {
      fieldErrors: {
        newPassword: 'account.password.fieldErrors.newInvalid',
      },
    }
  }

  return {
    fieldErrors: {},
    formError: errorMessageKey(error),
    ...(error.retryAfterSeconds === null ? {} : { retryAfterSeconds: error.retryAfterSeconds }),
  }
}
