# Refresh-token theft hardening — 2026-11-04

This records three follow-up fixes to the auth/session design after a manual
"copy the refresh cookie" test. It documents the root cause, the implemented
behaviour, the files touched and the verification evidence.

| | |
|---|---|
| **Date** | 2026-11-04 |
| **Scope** | Backend refresh-token handling, frontend session bootstrap, API/security docs |
| **Fixes** | Absolute session lifetime, access-token retirement on reuse detection, bootstrap handling of `409 REFRESH_IN_PROGRESS` |
| **New migration** | `V4__refresh_token_family_expiry.sql` |

## Why

The refresh flow already rotated tokens and revoked a whole family when a
rotated token was replayed outside the reuse-grace window. A manual test
showed three gaps:

1. **Sliding expiry had no ceiling.** Every rotation reset the refresh token to
   `now + 7 days`, so an attacker who copied the current cookie could renew the
   session forever, provided the legitimate client never replayed the old copy
   after the grace window.
2. **Reuse detection only revoked refresh tokens.** The thief's already-issued
   access token stayed valid for up to its 15-minute lifetime, giving it a
   window to keep acting after detection.
3. **A `409` during bootstrap looked like a logout.** The store retried
   `REFRESH_IN_PROGRESS` only when a session was already authenticated. On a
   fresh page load there is no session yet, so a lost race (or a stale copied
   cookie) immediately sent the user to the sign-in page. That also meant the
   stale copy never reached the point where reuse detection would revoke the
   family.

## 1. Absolute session lifetime

### Root cause

`RefreshTokenService.issue` accepted a `familyExpiresAt` parameter, but no
caller passed it and rotation always used `now + refresh-token-ttl`. There was
no upper bound on how long a family could live.

### Change

- Added `proxyma.auth.session-ttl` (`Duration`, default `30d`) to
  `ProxymaProperties` and `application.properties`.
- Added `refresh_tokens.family_expires_at` (migration `V4`). It is set once
  when the family is created and copied unchanged by every rotation.
- `issue` stores `family_expires_at` and caps the token's own `expires_at` at
  `min(now + refresh-token-ttl, family_expires_at)`.
- `rotate` rejects a family whose absolute deadline has passed, revoking the
  family as it does for an idle expiry. A replacement's `expires_at` is again
  `min(now + refresh-token-ttl, family_expires_at)`.
- The response reports the replacement's effective expiry, so the cookie's
  `Max-Age` is capped by the family deadline too.

Existing families are backfilled at 30 days from their first token, matching the
default. An operator who has configured a different `session-ttl` gets a sane
one-off cap for sessions that predate the migration.

**Files:** `auth/RefreshToken.kt`, `auth/RefreshTokenService.kt`,
`config/ProxymaProperties.kt`, `src/main/resources/application.properties`,
`src/main/resources/db/migration/V4__refresh_token_family_expiry.sql` (new).

## 2. Reuse detection retires access tokens immediately

### Root cause

`rotate` called `revokeFamily` and threw `INVALID_REFRESH_TOKEN`. Access tokens
are stateless and are checked against `users.token_version` on each request, but
nothing bumped that version, so a detected thief kept using its current access
token until natural expiry (up to 15 minutes).

### Change

- Added `UserRepository.bumpTokenVersion`, a bulk `token_version + 1` update.
- When a rotated token is replayed outside the grace window, `rotate` revokes
  the family **and** bumps the account's token version before throwing.
- The bump only happens when the replayed token was actually rotated
  (`replaced_by_token_id` set) **and** revoking the family retired at least one
  live token. Replaying a cookie from an already-dead family, for example after
  logout, does not keep bumping the version.
- Other devices are not logged out: their refresh tokens remain valid and their
  next refresh issues an access token with the new version.

**Files:** `user/UserRepository.kt`, `auth/RefreshTokenService.kt`.

## 3. Bootstrap treats `409` as a retryable race

### Root cause

`doRefresh` captured `hadSession = isAuthenticated.value` before the first
attempt. During bootstrap that is always false, and
`handleRefreshFailure` treated `REFRESH_IN_PROGRESS` as an immediate failure
when there was no session. A legitimate multi-tab race or a copied cookie
therefore produced a sign-in redirect instead of a retry, and the stale copy was
never replayed after the grace window.

### Change

- `doRefresh` now retries `REFRESH_IN_PROGRESS` regardless of `hadSession`.
- Retries use exponential backoff from 500 ms up to 8 s, for slightly longer
  than the backend's default 30-second reuse-grace window
  (`REFRESH_CONFLICT_WINDOW_MS = 32_000`).
- In the same browser the winner's `Set-Cookie` is shared, so the next attempt
  normally succeeds within a second. A stale copy from another client never
  succeeds; when the window passes, its next request trips reuse detection,
  which revokes the family and retires the access tokens (fix 2).
- After the window the existing behaviour applies: an authenticated session is
  deferred and retried later; a bootstrap attempt resolves as anonymous. A
  persistent conflict is not marked as an expired session, because the cookie
  has not been proven invalid.
- Removed the now-unused random retry-delay helper.

**Trade-off:** a persistent conflict keeps bootstrap waiting for up to about
32 seconds before the sign-in page appears. That is deliberate: the wait is what
gives a stale copied token the chance to trigger server-side reuse detection.
Normal races resolve on the first retry.

**Files:** `frontend/src/stores/auth.ts`,
`frontend/src/stores/__tests__/auth.spec.ts`.

## Tests

### Backend

New in `AuthIntegrationTest`:

- `refresh expiry is capped by the absolute session deadline` — sets a
  one-hour family deadline, refreshes, and proves both the replacement row and
  its expiry stop at the deadline rather than seven days out.
- `refresh is rejected after the absolute session deadline` — an expired family
  returns `401 INVALID_REFRESH_TOKEN` and every token in it is revoked.

Updated `RefreshTokenReuseTest`:

- After a replay outside the grace window, the access token issued at login is
  rejected with `401 UNAUTHORIZED`, and `users.token_version` is `1`.

### Frontend

Updated `src/stores/__tests__/auth.spec.ts`:

- The existing `409` retry test still passes.
- A bootstrap conflict retries twice and then restores the session instead of
  signing the user out.
- A conflict that never clears resolves as anonymous without setting
  `sessionExpired`.

## Verification

```sh
# Backend
cd backend
./gradlew test --console=plain
# 85 tests, 0 failures, 0 errors, 0 skipped (including the new tests)

# Frontend
cd frontend
bun run test:unit --run     # 72 passed
bun run type-check          # clean
bun run lint:check          # clean
bun run build-only          # built

# Full browser suite (all three projects, run in the Playwright Ubuntu image)
podman run --rm --init --ipc=host --security-opt label=disable --userns=keep-id \
  -v "$PWD:/work" -w /work -e CI=1 -e PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
  mcr.microsoft.com/playwright:v1.64.1-noble \
  node_modules/.bin/playwright test
# 114 passed
```

## Documentation

- `docs/api.md` — refresh expiry now has an idle TTL and an absolute session
  deadline; reuse detection bumps `token_version`, so access tokens are retired
  immediately.
- `docs/proxyma-security.md` — updated the token row and added a
  "Refresh tokens" subsection covering rotation, both expiries, reuse detection
  and the residual limit of bearer tokens.
- `docs/design-decisions.md` — decision 15 records the absolute session cap and
  its consequences.

## Residual risk and out of scope

- The 30-second grace is still a blind spot by design, and a copied bearer token
  used before the legitimate client cannot be distinguished from the real
  client. The absolute deadline bounds the exposure to at most
  `session-ttl` (30 days by default); it does not eliminate it.
- No client binding (user-agent/IP), session-management UI, or
  sender-constrained tokens (DPoP-style) were added. Those were discussed as
  follow-ups, not part of this change.
- Detection logs out the family and bumps `token_version`, which also retires
  other devices' access tokens until their next refresh. Their refresh tokens
  are untouched.
- Refreshing a family exactly at its deadline is a normal `401`; clients sign in
  again.
