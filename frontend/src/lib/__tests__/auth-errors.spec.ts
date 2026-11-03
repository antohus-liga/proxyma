import { describe, expect, it } from 'vitest'
import { ApiError, type ApiProblem } from '@/lib/api'
import { mapAuthError } from '@/lib/auth-errors'

function problem(code: string, errors?: ApiProblem['errors']): ApiProblem {
  return {
    type: 'about:blank',
    title: 'Error',
    status: 422,
    detail: 'Request failed',
    instance: '/api/v1/auth/login',
    code,
    errors,
  }
}

describe('mapAuthError', () => {
  it('maps invalid credentials to a single form error', () => {
    const mapped = mapAuthError(
      new ApiError({ code: 'INVALID_CREDENTIALS', problem: problem('INVALID_CREDENTIALS') }),
    )

    expect(mapped.formError).toBe('errors.INVALID_CREDENTIALS')
    expect(mapped.fieldErrors).toEqual({})
  })

  it('maps a taken email to the email field with a sign-in hint', () => {
    const mapped = mapAuthError(new ApiError({ code: 'EMAIL_TAKEN', problem: problem('EMAIL_TAKEN') }))

    expect(mapped.fieldErrors.email).toBe('errors.EMAIL_TAKEN')
    expect(mapped.emailTaken).toBe(true)
  })

  it('maps weak password errors to the password field', () => {
    const mapped = mapAuthError(
      new ApiError({ code: 'WEAK_PASSWORD', problem: problem('WEAK_PASSWORD') }),
    )

    expect(mapped.fieldErrors.password).toBe('errors.WEAK_PASSWORD')
    expect(mapped.formError).toBeUndefined()
  })

  it('maps validation paths to fields', () => {
    const mapped = mapAuthError(
      new ApiError({
        code: 'VALIDATION_FAILED',
        problem: problem('VALIDATION_FAILED', [
          { path: '/display_name', message: 'must not be blank' },
          { path: '/email', message: 'must be a valid email address' },
          { path: '/password', message: 'size must be between 10 and 128' },
        ]),
      }),
    )

    expect(mapped.fieldErrors).toEqual({
      displayName: 'auth.fieldErrors.displayName.invalid',
      email: 'auth.fieldErrors.email.invalid',
      password: 'auth.fieldErrors.password.invalid',
    })
    expect(mapped.formError).toBeUndefined()
  })

  it('uses a form summary when validation paths are unknown', () => {
    const mapped = mapAuthError(
      new ApiError({
        code: 'VALIDATION_FAILED',
        problem: problem('VALIDATION_FAILED', [{ path: '/unknown', message: 'bad' }]),
      }),
    )

    expect(mapped.formError).toBe('errors.VALIDATION_FAILED')
  })

  it('preserves retry-after seconds for rate limiting', () => {
    const mapped = mapAuthError(
      new ApiError({
        code: 'RATE_LIMITED',
        problem: problem('RATE_LIMITED'),
        retryAfterSeconds: 42,
      }),
    )

    expect(mapped.formError).toBe('errors.RATE_LIMITED')
    expect(mapped.retryAfterSeconds).toBe(42)
  })

  it('marks a suspended account', () => {
    const mapped = mapAuthError(
      new ApiError({ code: 'ACCOUNT_SUSPENDED', problem: problem('ACCOUNT_SUSPENDED') }),
    )

    expect(mapped.suspended).toBe(true)
    expect(mapped.formError).toBeUndefined()
  })

  it('falls back to the unknown message', () => {
    const mapped = mapAuthError(new ApiError({ code: 'SOMETHING_NEW', status: 500 }))

    expect(mapped.formError).toBe('errors.UNKNOWN')
  })
})
