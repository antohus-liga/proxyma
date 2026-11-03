const API_BASE = '/api/v1'
const DEFAULT_TIMEOUT_MS = 15_000

export type User = {
  id: string
  email: string
  display_name: string
  is_platform_admin: boolean
}

export type LoginResponse = {
  access_token: string
  token_type: string
  expires_in: number
  user: User
}

export type ApiProblem = {
  type: string
  title: string
  status: number
  detail: string
  instance: string
  code: string
  errors?: { path: string; message: string }[]
}

export type RegisterInput = {
  email: string
  display_name: string
  password: string
}

export type LoginInput = {
  email: string
  password: string
}

type RequestOptions = {
  method?: string
  body?: unknown
  timeoutMs?: number
}

/**
 * A rejected request. `code` is either a problem+json code or one of the
 * client-side codes `NETWORK_ERROR` and `TIMEOUT`.
 */
export class ApiError extends Error {
  readonly code: string
  readonly status: number
  readonly problem: ApiProblem | null
  readonly retryAfterSeconds: number | null

  constructor(options: {
    code: string
    status?: number
    problem?: ApiProblem | null
    retryAfterSeconds?: number | null
  }) {
    super(options.problem?.detail || options.code)
    this.name = 'ApiError'
    this.code = options.code
    this.status = options.status ?? options.problem?.status ?? 0
    this.problem = options.problem ?? null
    this.retryAfterSeconds = options.retryAfterSeconds ?? null
  }
}

export function asApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }

  return new ApiError({ code: 'UNKNOWN' })
}

async function readProblem(response: Response): Promise<ApiProblem | null> {
  const contentType = response.headers.get('content-type')

  if (!contentType?.includes('application/problem+json')) {
    return null
  }

  try {
    return (await response.json()) as ApiProblem
  } catch {
    return null
  }
}

function readRetryAfter(response: Response): number | null {
  const header = response.headers.get('retry-after') ?? response.headers.get('ratelimit-reset')

  if (!header) {
    return null
  }

  const seconds = Number.parseInt(header, 10)
  return Number.isFinite(seconds) && seconds >= 0 ? seconds : null
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), options.timeoutMs ?? DEFAULT_TIMEOUT_MS)

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      method: options.method ?? 'GET',
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        ...(options.body === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: controller.signal,
    })

    if (!response.ok) {
      const problem = await readProblem(response)

      throw new ApiError({
        code: problem?.code ?? 'UNKNOWN',
        status: response.status,
        problem,
        retryAfterSeconds: readRetryAfter(response),
      })
    }

    if (response.status === 204) {
      return undefined as T
    }

    return (await response.json()) as T
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }

    if (error instanceof Error && error.name === 'AbortError') {
      throw new ApiError({ code: 'TIMEOUT' })
    }

    throw new ApiError({ code: 'NETWORK_ERROR' })
  } finally {
    clearTimeout(timer)
  }
}

export const api = {
  register(input: RegisterInput): Promise<User> {
    return request<User>('/auth/register', { method: 'POST', body: input })
  },

  login(input: LoginInput): Promise<LoginResponse> {
    return request<LoginResponse>('/auth/login', { method: 'POST', body: input })
  },

  refresh(): Promise<LoginResponse> {
    return request<LoginResponse>('/auth/refresh', { method: 'POST' })
  },

  logout(): Promise<void> {
    return request<void>('/auth/logout', { method: 'POST' })
  },
}
