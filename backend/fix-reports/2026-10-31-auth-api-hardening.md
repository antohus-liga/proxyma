# Auth API hardening — fixes for the 2026-10-31 live test findings

This document describes the changes made in response to
[`../AUTH_API_TEST_REPORT.md`](../AUTH_API_TEST_REPORT.md). It is the reference for the commit that
introduces them, and it records the root cause, the implemented behaviour, the files touched and
the evidence for each finding.

| | |
|---|---|
| **Date** | 2026-10-31 |
| **Scope** | Backend auth API (`backend/`), plus API docs and env example |
| **Findings fixed** | F2 concurrent refresh, F4 error envelope, F5 rate-limit headers, F7 access tokens surviving credential changes, F8 absolute refresh expiry |
| **Findings documented only** | F3 empty CORS allowlist, F6 unauthenticated logout, F9 `401` before `404`/`405` |
| **New migration** | `V2__user_token_version.sql` |

## Summary

| # | Finding | Fix |
|---|---|---|
| F2 | Parallel refreshes revoked the whole session family | Replays inside a `refresh-reuse-grace` window (default 30 s) return `409 REFRESH_IN_PROGRESS` and leave the family intact; older replays still trip reuse detection |
| F4 | MVC, security-filter and CORS errors used three different documents | One `ApiProblem` factory builds every body; CORS rejections are now `403 CORS_REJECTED` problem+json |
| F5 | `Retry-After` only appeared on `429` | Every rate-limited response carries `RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset`; `429` keeps `Retry-After` |
| F7 | Access tokens survived password changes and admin revocation | Tokens carry a `tv` (token version) claim checked on every request; credential/state changes bump `users.token_version` |
| F8 | Refresh expiry was absolute at login time | Each rotation sets the stored expiry **and** the cookie to `now + refresh TTL` (pure sliding, no cap) |

Documentation-only findings:

- **F3** — The CORS allowlist is intentionally empty by default; the dashboard is expected to use
  a same-origin dev proxy. `PROXYMA_CORS_ALLOWED_ORIGINS` is now documented in `.env.example` and
  `docs/api.md`.
- **F6** — `POST /auth/logout` is unauthenticated and idempotent by design; documented in
  `docs/api.md`.
- **F9** — Authentication runs before routing, so unknown paths and method mismatches return `401`
  before `404`/`405`. Documented in `docs/api.md`.

## F2 — Concurrent refresh no longer logs the user out

### Root cause

`RefreshTokenService.rotate` locked the token row, so two parallel refreshes serialised. The loser
then saw `revokedAt`/`replacedByTokenId` already set and entered the theft path, which revoked the
entire token family — including the valid replacement the winner had just received.

### Change

- Added `proxyma.auth.refresh-reuse-grace` (`Duration`, default `30s`; `0s` disables the carve-out)
  to `ProxymaProperties` and `application.properties`.
- `rotate` distinguishes a token rotated inside the grace window (`wasJustRotated`) from an older
  replay. The former throws `409 REFRESH_IN_PROGRESS`; the latter keeps the existing revocation
  behaviour.
- `AuthController.refresh` does **not** clear the cookie for `REFRESH_IN_PROGRESS`. Clearing it
  would wipe the winner's valid cookie, which is exactly the race this change fixes.

The loser still receives no token. Only the winner's rotation stands.

**Files:** `auth/RefreshTokenService.kt`, `auth/AuthController.kt`, `config/ProxymaProperties.kt`,
`src/main/resources/application.properties`.

## F4 — One error envelope everywhere

### Root cause

MVC exceptions were serialised from `ProblemDetail`, filter errors were assembled by hand in
`ProblemResponseWriter`, and CORS rejections used Spring's plain-text `Invalid CORS request`.
The three paths disagreed on `type`, `instance` and key order.

### Change

- New `ApiProblem` object is the single body factory. It always emits `type`, `title`, `status`,
  `detail`, `instance`, `code`, then `errors` when non-empty, in that order. `type` is always
  `about:blank` for now.
- `GlobalExceptionHandler` returns `ResponseEntity<Map<String, Any?>>` built by `ApiProblem`, so MVC
  errors now include `type` and use the shared key order.
- `ProblemResponseWriter` takes the `HttpServletRequest` and delegates to `ApiProblem`.
- `RestAuthenticationEntryPoint` and `RestAccessDeniedHandler` use the writer.
- New `ProblemCorsProcessor` overrides `processRequest`, checks the origin, and writes
  `403 CORS_REJECTED` problem+json instead of plain text.
- `SecurityConfig` exposes the `CorsFilter` as a bean named `corsFilter` (Spring Security's
  `CorsConfigurer` picks up that bean when no `configurationSource` is set) and disables its servlet
  auto-registration to avoid running CORS twice.

`ProblemDetail` was dropped because it omits the default `about:blank` `type` on serialisation.

**Files:** `common/api/ApiProblem.kt` (new), `common/api/GlobalExceptionHandler.kt`,
`common/api/ProblemResponseWriter.kt`, `security/RestAuthenticationEntryPoint.kt`,
`security/RestAccessDeniedHandler.kt`, `security/ProblemCorsProcessor.kt` (new),
`security/SecurityConfig.kt`.

## F5 — Rate-limit headers on success and rejection

### Root cause

`RateLimitService` returned only `allowed`/`retryAfterSeconds`, and `enforce` attached a header only
to the `429` exception. Clients had no way to see the remaining budget.

### Change

- `Decision` now carries `limit`, `remaining` and `retryAfterSeconds`, plus a `headers()` helper.
- `enforce` attaches `RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset` to the exception,
  keeping `Retry-After` for `429`.
- `AuthController` applies the same headers to successful register/login/refresh responses. Login
  checks two buckets (per IP and per account) and reports the tighter one.

**Files:** `security/ratelimit/RateLimitService.kt`, `auth/AuthController.kt`.

## F7 — Credential changes retire access tokens

### Root cause

Access tokens are stateless and validation only re-read the user row for status. A password change,
suspension, unsuspension or force-logout revoked refresh tokens but left already-issued access
tokens valid until `exp` (up to 15 minutes).

### Change

- Added `users.token_version` (migration `V2__user_token_version.sql`, default `0`) and
  `User.tokenVersion`.
- `JwtService` embeds the current value as the `tv` claim.
- `UserStatusFilter` compares the claim with the row on every authenticated request and rejects a
  mismatch with `401 UNAUTHORIZED` ("The access token is no longer valid.").
- `AuthService` bumps the version on password change, suspend, unsuspend and force-logout. Unsuspend
  also bumps, so tokens issued before the suspension do not come back to life.

**Files:** `user/User.kt`, `security/JwtService.kt`, `security/UserStatusFilter.kt`,
`auth/AuthService.kt`, `src/main/resources/db/migration/V2__user_token_version.sql` (new).

**Deployment note:** the migration must run before the new code serves traffic. Hibernate
`ddl-auto=validate` fails fast if the column is missing, which is the intended behaviour.

## F8 — Sliding refresh expiry

### Root cause

`rotate` copied the old token's `expiresAt` into the replacement, so an actively used session still
died seven days after login. The response also returned the old expiry, so the cookie's `Max-Age`
did not move either.

### Change

- A rotated token now stores `expiresAt = now + proxyma.auth.refresh-token-ttl`.
- `rotate` returns the replacement's expiry, so the cookie `Max-Age`/`Expires` slides with the
  stored token.
- There is no absolute cap; this is pure sliding expiry as requested.

**Files:** `auth/RefreshTokenService.kt`.

## Tests

### Updated

`backend/src/test/kotlin/org/proxyma/backend/AuthIntegrationTest.kt`

- Replaced the reuse-revokes-family test with a race test: a concurrent replay returns
  `409 REFRESH_IN_PROGRESS`, does not revoke the family, and the winner's cookie still works.
- Added a sliding-expiry test comparing the old and replacement rows.
- Password-change test now also asserts that the previously issued access token returns `401`.
- Added envelope assertions (`type`, `instance`, `status`, `title`, `code`) and an MVC-versus-filter
  consistency test.

### New

- `RefreshTokenReuseTest.kt` — sets `refresh-reuse-grace=0s`, rotates a token, then proves a replay
  still revokes the family.
- `RateLimitResponseTest.kt` — sets `login-per-minute=1` and asserts the success headers, then the
  `429` headers plus `Retry-After`.
- `CorsRejectionTest.kt` — allows `http://localhost:5173`, asserts an allowed preflight succeeds and
  a disallowed one returns `403 CORS_REJECTED` problem+json.

The integration tests use Testcontainers and are skipped when Docker is unavailable
(`@Testcontainers(disabledWithoutDocker = true)`). In this environment Docker was not available, so
`./gradlew test` ran the unit tests and skipped the integration classes. The fixes were therefore
also verified against a live instance; the raw script and output are in `/tmp/opencode/authfix/`
and the results are recorded in `AUTH_API_TEST_REPORT.md` §15.

## Documentation

- `docs/api.md` — section 1 (errors), section 3 (cookie attributes, rotation grace, sliding expiry,
  idempotent logout, password change revoking access tokens), section 11 (rate-limit headers) and
  section 12 (canonical problem document, key order, `CORS_REJECTED`, `REFRESH_IN_PROGRESS`,
  `401`-before-`404`).
- `backend/.env.example` — documented `PROXYMA_CORS_ALLOWED_ORIGINS`.
- `application.properties` — `proxyma.cors.allowed-origins` now reads that variable with an empty
  default.
- `backend/AUTH_API_TEST_REPORT.md` — findings marked resolved/documented, with a post-fix
  verification section.

## Verification

```bash
cd backend
./gradlew compileKotlin compileTestKotlin   # compiles
./gradlew test                              # unit tests; integration tests need Docker
```

Live check (backend running on `localhost:8080`):

```bash
/tmp/opencode/authfix/run.sh
```

That script covers rate-limit headers, the concurrent refresh race, sliding expiry in the database,
all three error paths, password-change token revocation and the `429` boundary.

A second instance (`--server.port=8081`, same DB, `PROXYMA_CORS_ALLOWED_ORIGINS=http://localhost:5173`)
confirmed the two changes that cannot be seen in the first run:

- a refresh three seconds after login moved the cookie `Expires` from `18:34:34` to `18:34:37` GMT
  while `Max-Age` stayed `604799`, so the cookie expiry slides with the stored token;
- an allowed preflight returned `200` and a disallowed one `403`, proving the documented env
  variable reaches `proxyma.cors.allowed-origins`.

The final tree was also packaged with `bootJar` and smoke-tested on port 8082 (register, login,
refresh, envelope, CORS rejection, password revocation), with the same results.

## Out of scope

- The CORS allowlist default remains empty (F3); operators must set
  `PROXYMA_CORS_ALLOWED_ORIGINS` for a separately served dashboard.
- Logout stays unauthenticated and idempotent (F6).
- Authentication still runs before routing (F9).
- The base64url padding behaviour of JWT signature verification is unchanged; it is not a
  signature bypass.
