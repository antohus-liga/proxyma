# Proxyma frontend–backend API contract

This document is the frontend-facing source of truth for the **currently implemented**
HTTP API. It is written so a frontend engineer can integrate without reading the
Kotlin backend.

> **Important:** [`docs/api.md`](../../docs/api.md) describes the full intended
> product API (organisations, config, bans, logs, audit, SSE). Most of those
> endpoints are **design-stage and not implemented yet**. This file lists only
> what the backend actually serves today. If the two disagree about an
> implemented endpoint, this file and the backend tests win.

---

## 1. At a glance

| Topic | Contract |
|---|---|
| Base path | `/api/v1` |
| Default dev URL | `http://localhost:8080` (Spring Boot default; override with `VITE_BACKEND_URL`) |
| Request body | JSON, UTF-8, `Content-Type: application/json` |
| Response body | JSON, `snake_case` field names |
| Error body | `application/problem+json` with a stable `code` |
| IDs | UUID strings |
| Timestamps | ISO-8601 UTC instants, e.g. `2026-10-18T14:30:00Z` |
| Protected calls | `Authorization: Bearer <access_token>` |
| Refresh token | `HttpOnly` cookie named `proxyma_refresh`, never in JSON |
| Cookie path | `/api/v1/auth` (browser only sends it to auth endpoints) |
| Cookie flags | `HttpOnly`, `SameSite=Strict`, `Secure` unless disabled for local HTTP |
| Pagination | `items` + `next_cursor`; cursor is opaque; `null` means last page |
| List order | Newest first |
| Admin availability | Off by default; the routes return `404` unless enabled |

### Implemented endpoint inventory

| Method | Path | Auth | Exists today |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Yes |
| `POST` | `/api/v1/auth/login` | Public | Yes |
| `POST` | `/api/v1/auth/refresh` | Refresh cookie | Yes |
| `POST` | `/api/v1/auth/logout` | Refresh cookie (optional) | Yes |
| `GET` | `/api/v1/users/me` | Access token | Yes |
| `PATCH` | `/api/v1/users/me` | Access token | Yes |
| `POST` | `/api/v1/users/me/password` | Access token | Yes |
| `GET` | `/api/v1/admin/stats` | Platform admin | Yes |
| `GET` | `/api/v1/admin/users` | Platform admin | Yes |
| `GET` | `/api/v1/admin/users/{user_id}` | Platform admin | Yes |
| `POST` | `/api/v1/admin/users/{user_id}/suspend` | Platform admin + step-up | Yes |
| `POST` | `/api/v1/admin/users/{user_id}/unsuspend` | Platform admin | Yes |
| `POST` | `/api/v1/admin/users/{user_id}/force-logout` | Platform admin + step-up | Yes |
| `GET` | `/api/v1/admin/orgs` | Platform admin | Yes |
| `GET` | `/api/v1/admin/orgs/{org_id}` | Platform admin | Yes |
| `GET` | `/api/v1/admin/audit` | Platform admin | Yes |
| `GET` | `/actuator/health` | Public | Yes (ops only) |

**Not implemented yet:** `/api/v1/orgs`, members, invitations, config, bans,
logs, org audit, stream tickets, SSE. Do not build against those paths from
`docs/api.md` yet.

---

## 2. Integration setup

### 2.1 Local development and CORS

The backend allows **no browser origins by default**. Pick one of these:

1. **Recommended:** serve the frontend through a same-origin dev proxy (Vite
   `server.proxy`) to `http://localhost:8080` by default. Set `VITE_BACKEND_URL`
   in `frontend/.env` to proxy to a different host. No CORS configuration needed.
2. Set `PROXYMA_CORS_ALLOWED_ORIGINS=http://localhost:5173` on the backend.

CORS rules:

- Credentials are allowed (`allowCredentials = true`).
- The origin allowlist is exact; wildcards are not allowed with credentials.
- Allowed methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- Allowed request headers: `*`.
- Preflight cache: 1 hour.
- A disallowed `Origin` gets `403 CORS_REJECTED` before authentication runs.

### 2.2 Cookies and `fetch`

The refresh token is an `HttpOnly` cookie scoped to `/api/v1/auth`. JavaScript
cannot read it, and it is **not** sent to `/users/me` or `/admin/...`.

For `/auth/login`, `/auth/refresh` and `/auth/logout`, send credentials:

```ts
fetch('/api/v1/auth/refresh', {
  method: 'POST',
  credentials: 'include', // required when frontend and backend are cross-origin
})
```

With a same-origin Vite proxy, `same-origin` is enough, but `include` is safe.

Because the cookie is `SameSite=Strict`, a frontend served from a **different
site** cannot use refresh. Prefer same-site deployment (different ports on
`localhost` are still same-site).

### 2.3 Access tokens

- Successful login/refresh returns an access token in the JSON body.
- Send it as `Authorization: Bearer <token>` on protected calls.
- Access token lifetime:
  - normal user: **900 seconds** (15 minutes),
  - platform admin: **300 seconds** (5 minutes).
- `expires_in` in the response is the authoritative number of seconds.
- The token carries no roles. The backend checks the account, status,
  platform-admin flag and token version from the database on every request.
- A password change, admin force-logout, suspension, or operator
  grant/revoke invalidates existing access tokens immediately. The next request
  returns `401 UNAUTHORIZED`.

Do not derive permissions from JWT claims. Use `user.is_platform_admin` from
login/`GET /users/me` for UI hints, but always handle server responses.

---

## 3. Error contract

Every error path — controllers, security filters and CORS — returns the same
document shape and `Content-Type: application/problem+json`.

```json
{
  "type": "about:blank",
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "The request has 2 problems.",
  "instance": "/api/v1/auth/register",
  "code": "VALIDATION_FAILED",
  "errors": [
    { "path": "/email", "message": "must be a valid email address" },
    { "path": "/password", "message": "is too common" }
  ]
}
```

Rules:

- Key order is fixed: `type`, `title`, `status`, `detail`, `instance`, `code`,
  then `errors` only when non-empty.
- `type` is always `"about:blank"` today.
- `title` is the HTTP reason phrase, e.g. `"Unauthorized"`, `"Forbidden"`.
- `instance` is the request path.
- `errors[].path` is a JSON Pointer into the submitted document, using
  `snake_case` segments, e.g. `/display_name`, `/new_password`.
- `code` is the stable machine-readable value. Branch on this, not on `detail`.
- Responses never include stack traces or internal details.

### 3.1 Status and code reference

| HTTP | `code` | Meaning / when |
|---|---|---|
| `400` | `MALFORMED_REQUEST` | Body is not readable JSON |
| `400` | `INVALID_PARAMETER` | Bad query/path parameter, unknown `status`/`state`, invalid date, `from > to` |
| `400` | `INVALID_CURSOR` | Admin list cursor is malformed or not a Proxyma cursor |
| `401` | `UNAUTHORIZED` | Missing, malformed, expired or retired access token |
| `401` | `INVALID_CREDENTIALS` | Login email/password wrong (generic for unknown account too) |
| `401` | `INVALID_REFRESH_TOKEN` | Refresh cookie missing, expired, revoked or replayed after the grace window |
| `403` | `ACCOUNT_SUSPENDED` | Suspended account tries to log in, refresh, or use a valid access token on a normal route |
| `403` | `FORBIDDEN` | Authenticated but not allowed |
| `403` | `REAUTH_FAILED` | Step-up password is wrong (admin suspend/force-logout, password change) |
| `403` | `ADMIN_CANNOT_TARGET_ADMIN` | Admin tried to suspend another platform admin |
| `403` | `CORS_REJECTED` | Request `Origin` is not in the allowlist |
| `404` | `NOT_FOUND` | Unknown user/org, or hidden admin namespace |
| `409` | `EMAIL_TAKEN` | Registration email already exists |
| `409` | `REFRESH_IN_PROGRESS` | Refresh token was just rotated by a concurrent request; retry with the current cookie |
| `409` | `CANNOT_SUSPEND_SELF` | Admin tried to suspend their own account |
| `409` | `CONFLICT` | Generic database constraint conflict |
| `422` | `VALIDATION_FAILED` | Bean validation failed; inspect `errors` |
| `422` | `WEAK_PASSWORD` | Password policy failed; inspect `errors` |
| `429` | `RATE_LIMITED` | Rate limit exhausted; honour `Retry-After` |
| `500` | `INTERNAL_ERROR` | Unexpected server failure |

### 3.2 Validation vs. malformed input

- Invalid field values → `422 VALIDATION_FAILED` (or `422 WEAK_PASSWORD`).
- Invalid query parameter types → `400 INVALID_PARAMETER`.
- Invalid JSON syntax → `400 MALFORMED_REQUEST`.
- Missing bodies on endpoints that require one → `400 MALFORMED_REQUEST`.

### 3.3 Routing is authenticated before it is resolved

An unknown path under the protected API returns `401 UNAUTHORIZED` when no valid
access token is present, rather than `404`. With a valid token it returns `404`.
This prevents anonymous route enumeration, so do not interpret every `401` as
"the route exists but my session expired" and do not interpret a `404` as proof
that the frontend called the wrong URL. CORS rejection still happens first, so a
bad `Origin` wins over `401`.

---

## 4. Authentication and user API

### 4.1 `POST /api/v1/auth/register`

Public. Creates the account but **does not log in**: no access token and no
refresh cookie are returned. Call login afterwards.

Request:

```json
{
  "email": "ana@example.com",
  "display_name": "Ana",
  "password": "correct horse battery staple"
}
```

Constraints:

- `email`: non-blank, valid email, max 254 characters. Trimmed and lowercased.
- `display_name`: non-blank, 1–80 characters. Trimmed.
- `password`: non-blank; at least **10** and at most **128** characters by
  default; must not be a common password.

`201 Created`:

```json
{
  "id": "5f0c9d8e-1a2b-4c3d-8e9f-0123456789ab",
  "email": "ana@example.com",
  "display_name": "Ana",
  "is_platform_admin": false
}
```

Errors: `409 EMAIL_TAKEN`, `422 VALIDATION_FAILED`, `422 WEAK_PASSWORD`,
`400 MALFORMED_REQUEST`, `429 RATE_LIMITED`.

### 4.2 `POST /api/v1/auth/login`

Public. On success, sets the refresh cookie and returns an access token.

Request:

```json
{ "email": "ana@example.com", "password": "correct horse battery staple" }
```

`200 OK`:

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 900,
  "user": {
    "id": "5f0c9d8e-1a2b-4c3d-8e9f-0123456789ab",
    "email": "ana@example.com",
    "display_name": "Ana",
    "is_platform_admin": false
  }
}
```

Response header:

```http
Set-Cookie: proxyma_refresh=...; Path=/api/v1/auth; Max-Age=604800; HttpOnly; SameSite=Strict; Secure
```

Errors:

- `401 INVALID_CREDENTIALS` — same response for unknown email and wrong password.
- `403 ACCOUNT_SUSPENDED` — correct credentials but the account is suspended.
- `422 VALIDATION_FAILED` — missing/overlong fields.
- `429 RATE_LIMITED` — see rate limits below.

### 4.3 `POST /api/v1/auth/refresh`

Requires the refresh cookie; no request body. Returns the same shape as login
and **rotates** the refresh cookie (a new cookie value and a sliding expiry).
The browser must accept the new `Set-Cookie`.

`200 OK` — same `LoginResponse` body as login.

Failure behaviour:

| Situation | Response | Cookie |
|---|---|---|
| No cookie | `401 INVALID_REFRESH_TOKEN` | cleared |
| Unknown, expired, revoked | `401 INVALID_REFRESH_TOKEN` | cleared |
| Replay after the grace window (suspected theft) | `401 INVALID_REFRESH_TOKEN` | cleared; token family revoked |
| Replay within the 30 s grace window (concurrent refresh race) | `409 REFRESH_IN_PROGRESS` | **not** cleared |
| Suspended account | `403 ACCOUNT_SUSPENDED` | cleared; all user refresh tokens revoked |

The backend uses a **sliding** refresh expiry (default 7 days). Every successful
refresh renews the cookie.

Frontend requirement: make refresh **single-flight**. If two tabs or two calls
refresh simultaneously, one can receive `409 REFRESH_IN_PROGRESS`. That is not a
logout: keep the session, wait briefly, and retry with the cookie the winner
already set.

### 4.4 `POST /api/v1/auth/logout`

Requires no access token. Revokes the refresh token family if a cookie is
present, clears the cookie and always returns `204 No Content`.

- Idempotent: missing or already-revoked cookies still return `204`.
- The access token is not explicitly revoked by logout (it expires or is
  invalidated by another action), so drop it client-side immediately.

### 4.5 `GET /api/v1/users/me`

Requires an access token.

`200 OK`:

```json
{
  "id": "5f0c9d8e-1a2b-4c3d-8e9f-0123456789ab",
  "email": "ana@example.com",
  "display_name": "Ana",
  "is_platform_admin": false
}
```

Errors: `401 UNAUTHORIZED`, `403 ACCOUNT_SUSPENDED`.

### 4.6 `PATCH /api/v1/users/me`

Requires an access token. Only the display name can be changed.

Request:

```json
{ "display_name": "New Name" }
```

`200 OK` — updated `UserResponse`.

Constraints: non-blank, 1–80 characters, trimmed.

Errors: `401`, `403 ACCOUNT_SUSPENDED`, `422 VALIDATION_FAILED`.

### 4.7 `POST /api/v1/users/me/password`

Requires an access token.

Request:

```json
{
  "current_password": "correct horse battery staple",
  "new_password": "another very long passphrase"
}
```

`204 No Content`.

Effects:

- Replaces the password.
- Revokes **all** refresh tokens for the user.
- Bumps the token version, so the current access token is rejected with `401`
  on its next use.
- The old refresh cookie is not cleared in this response, but it is revoked;
  the next refresh returns `401 INVALID_REFRESH_TOKEN` and clears it.

Errors: a wrong current password returns `403 REAUTH_FAILED`. A weak new
password returns `422 WEAK_PASSWORD` with `errors[].path = "/new_password"`.
Validation errors return `422 VALIDATION_FAILED`.

Frontend should treat a successful password change as a logout: forget the
access token and send the user back to login.

---

## 5. Admin API

The admin API is **off by default**. Availability and authorization are checked
before the controller runs.

### 5.1 Visibility and access rules

`PROXYMA_ADMIN_ENABLED` controls the whole namespace. `PROXYMA_ADMIN_ALLOWED_CIDRS`
optionally restricts source networks (comma-separated IPv4/IPv6 CIDRs; empty
means no network restriction).

| Situation | Response |
|---|---|
| Panel disabled | `404 NOT_FOUND`, even with a valid admin token |
| Source IP outside the allowlist | `404 NOT_FOUND`, even with a valid admin token |
| Missing/malformed/expired access token | `401 UNAUTHORIZED` |
| Valid token for a normal user | `404 NOT_FOUND` |
| Valid token for a deleted account | `404 NOT_FOUND` |
| Valid token for a suspended admin | `404 NOT_FOUND` (not `403`) |
| Admin flag revoked after login | `404 NOT_FOUND` immediately |
| Token version stale | `401 UNAUTHORIZED` |
| Valid platform admin | Normal response |

The platform-admin flag and account status come from the database on every
request. Do not cache the admin namespace as available: a `404` is also the
normal "not available" response.

### 5.2 Admin rate limits

| Budget | Default | Applies to |
|---|---|---|
| General | 120 requests/minute per admin | Every request that authenticates as a platform admin under `/api/v1/admin/**` |
| Risky | 10 requests/minute per admin | `suspend` and `force-logout` only |

Environment overrides:

- `PROXYMA_ADMIN_RATE_LIMIT_REQUESTS_PER_MINUTE`
- `PROXYMA_ADMIN_RATE_LIMIT_RISKY_PER_MINUTE`

A risky action consumes **both** budgets. Failed step-up attempts consume the
budget too. `unsuspend` is not risky and only consumes the general budget.
Because both checks run, a risky action's response headers describe the
**risky** budget (it is the tighter one).

Successful admin responses include:

```http
RateLimit-Limit: 120
RateLimit-Remaining: 119
RateLimit-Reset: 42
```

`RateLimit-Reset` is seconds until the window resets. A rejected request returns
`429 RATE_LIMITED` with those headers plus `Retry-After` seconds.

### 5.3 Pagination (users, orgs, audit)

All admin lists accept:

| Param | Meaning |
|---|---|
| `limit` | 1–200, default 50. Out of range → `422 VALIDATION_FAILED` |
| `cursor` | Opaque value copied from the previous `next_cursor`. Malformed → `400 INVALID_CURSOR` |

Response shape:

```json
{
  "items": [],
  "next_cursor": "eyJjcmVhdGVkX2F0Ijoi..." 
}
```

- `next_cursor` is `null` on the last page.
- Treat it as opaque; never parse or construct it.
- Reset the cursor whenever a filter changes.
- Ordering is `created_at DESC, id DESC`; pages do not overlap.

### 5.4 `GET /api/v1/admin/stats`

No query parameters. Not audited.

```json
{
  "users": { "total": 42, "suspended": 1 },
  "orgs": { "total": 31, "online": 28, "offline": 3 }
}
```

`orgs.offline` is `total - online`; every org not currently `ONLINE`
(`CREATING`, `STARTING`, `OFFLINE`, `DELETING`) counts as offline.

### 5.5 `GET /api/v1/admin/users`

Not audited.

Query parameters:

| Param | Meaning |
|---|---|
| `q` | Case-insensitive substring match on email or display name |
| `status` | `active` or `suspended`; anything else → `400 INVALID_PARAMETER` |
| `limit`, `cursor` | Pagination |

Response:

```json
{
  "items": [
    {
      "id": "5f0c9d8e-1a2b-4c3d-8e9f-0123456789ab",
      "email": "ana@example.com",
      "display_name": "Ana",
      "status": "active",
      "created_at": "2026-09-01T10:00:00Z",
      "last_login_at": "2026-10-18T09:12:00Z",
      "owns_org": true,
      "membership_count": 2
    }
  ],
  "next_cursor": null
}
```

- `status` is `active` or `suspended`.
- `last_login_at` may be `null` (never logged in).
- `owns_org` is true if the user has an owner membership (at most one).
- `membership_count` counts all org memberships.

### 5.6 `GET /api/v1/admin/users/{user_id}`

Audited as `user.view` in the platform audit trail.

Response:

```json
{
  "id": "5f0c9d8e-1a2b-4c3d-8e9f-0123456789ab",
  "email": "ana@example.com",
  "display_name": "Ana",
  "status": "active",
  "suspension": null,
  "is_platform_admin": false,
  "created_at": "2026-09-01T10:00:00Z",
  "last_login_at": "2026-10-18T09:12:00Z",
  "owned_org": { "id": "b7a1...", "name": "Acme" },
  "memberships": [
    {
      "org_id": "b7a1...",
      "org_name": "Acme",
      "role": "owner",
      "joined_at": "2026-09-01T10:05:00Z"
    }
  ],
  "active_sessions": 1
}
```

Suspension object when suspended:

```json
{
  "reason": "Repeated abuse reports",
  "suspended_at": "2026-10-18T14:35:12Z",
  "suspended_by": { "user_id": "a21e...", "display_name": "Platform Admin" }
}
```

- `suspension` is `null` when active.
- `suspended_by` can be `null` (e.g. suspended directly by an operator).
- `owned_org` is `null` when the user owns nothing.
- `memberships` includes every membership; roles are `owner`, `co_owner` or
  `member`.
- `active_sessions` counts non-revoked, non-expired refresh tokens.
- The response never includes password hashes or tokens.

Errors: `404 NOT_FOUND`.

### 5.7 `POST /api/v1/admin/users/{user_id}/suspend`

Risky: consumes the risky budget and requires the admin's **own password**.

Request:

```json
{ "reason": "Repeated abuse reports", "password": "the admin's own password" }
```

`200 OK` — the updated `AdminUserDetail` (same shape as 5.6), with
`status: "suspended"` and a populated `suspension` object.

Effects:

- Marks the account suspended.
- Revokes all refresh tokens and bumps `token_version`; existing access tokens
  stop working on the next request.
- Does **not** stop the user's org or proxy.
- Writes `user.suspend` to the platform audit and `member.suspend` to each org
  the user belongs to.

Errors:

- `403 REAUTH_FAILED` — wrong step-up password.
- `403 ADMIN_CANNOT_TARGET_ADMIN` — target is a platform admin.
- `409 CANNOT_SUSPEND_SELF` — target is the caller.
- `404 NOT_FOUND` — unknown user.
- `422 VALIDATION_FAILED` — missing/blank reason (max 500) or password.
- `429 RATE_LIMITED`.

### 5.8 `POST /api/v1/admin/users/{user_id}/unsuspend`

Not risky; no step-up password.

Request:

```json
{ "reason": "Resolved after contact" }
```

`200 OK` — updated user with `status: "active"` and `suspension: null`.

Effects: activates the account, clears suspension fields, bumps
`token_version` (pre-suspension tokens stay dead), writes `user.unsuspend` and
`member.unsuspend` audit entries.

Errors: `404 NOT_FOUND`, `422 VALIDATION_FAILED` (blank reason),
`429 RATE_LIMITED`.

### 5.9 `POST /api/v1/admin/users/{user_id}/force-logout`

Risky: consumes the risky budget and requires the admin's own password.

Request:

```json
{ "reason": "Possible account compromise", "password": "the admin's own password" }
```

`204 No Content`.

Effects: revokes all refresh tokens and bumps `token_version`, so the user's
current access token fails immediately. Writes `user.force_logout`.

Unlike suspend, this **may target another platform admin**.

Errors: `403 REAUTH_FAILED`, `404 NOT_FOUND`, `422 VALIDATION_FAILED`,
`429 RATE_LIMITED`.

### 5.10 `GET /api/v1/admin/orgs`

Not audited.

Query parameters:

| Param | Meaning |
|---|---|
| `q` | Case-insensitive substring match on org name, owner display name or owner email |
| `state` | `CREATING`, `STARTING`, `ONLINE`, `OFFLINE`, `DELETING`; anything else → `400 INVALID_PARAMETER` |
| `limit`, `cursor` | Pagination |

Response:

```json
{
  "items": [
    {
      "id": "b7a1...",
      "name": "Acme",
      "state": "ONLINE",
      "owner": { "user_id": "5f0c...", "display_name": "Ana" },
      "member_count": 3,
      "created_at": "2026-09-01T10:00:00Z"
    }
  ],
  "next_cursor": null
}
```

`owner` is `null` for an org with no owner membership.

### 5.11 `GET /api/v1/admin/orgs/{org_id}`

Audited as `org.view`.

```json
{
  "id": "b7a1...",
  "name": "Acme",
  "state": "ONLINE",
  "created_at": "2026-09-01T10:00:00Z",
  "owner": { "user_id": "5f0c...", "display_name": "Ana" },
  "members": [
    { "user_id": "5f0c...", "display_name": "Ana", "role": "owner" },
    { "user_id": "a21e...", "display_name": "Bo", "role": "member" }
  ],
  "proxy": {
    "last_heartbeat_at": "2026-10-18T14:35:12Z",
    "running_config_version": 7,
    "latest_config_version": 7,
    "config_status": "applied",
    "active_ban_count": 4
  }
}
```

Field notes:

- Members are ordered owner first, then co-owner, then the rest by display name.
- `proxy.last_heartbeat_at` and both config versions may be `null`.
- `config_status`:
  - `applied` — proxy acknowledged the latest config version,
  - `rejected` — latest version was rejected,
  - `pending` — otherwise, including an org with no config yet.
- `active_ban_count` counts bans that are not removed and not expired.
- The response never includes config contents, logs, ban IPs or org audit.

Errors: `404 NOT_FOUND`.

### 5.12 `GET /api/v1/admin/audit`

Reads the **platform** audit trail only, newest first. Not itself audited.

Query parameters:

| Param | Meaning |
|---|---|
| `from` | ISO-8601 instant, inclusive |
| `to` | ISO-8601 instant, **exclusive** |
| `admin_user_id` | Exact admin UUID |
| `action` | Exact action name |
| `limit`, `cursor` | Pagination |

`from > to` → `400 INVALID_PARAMETER`. Invalid dates → `400 INVALID_PARAMETER`.
Malformed cursor → `400 INVALID_CURSOR`.

Response:

```json
{
  "items": [
    {
      "id": "9d2b...",
      "timestamp": "2026-10-18T14:35:12Z",
      "admin": { "user_id": "a21e...", "display_name": "Platform Admin" },
      "action": "user.suspend",
      "target": { "type": "user", "id": "5f0c..." },
      "summary": "Suspended ana@example.com: Repeated abuse reports",
      "source_ip": "203.0.113.10"
    }
  ],
  "next_cursor": null
}
```

Semantics:

- `target` is `null` only when both target type and id are absent; otherwise it
  is an object and either field can be `null`.
- `admin` is `null` for server-side operator commands (`grant-admin` /
  `revoke-admin`); `source_ip` is also `null` for those entries.
- `source_ip` is a plain address string (`198.51.100.23`, `2001:db8::1`), not CIDR.
- Org audit entries never appear here.

Action names currently emitted:

`user.view`, `user.suspend`, `user.unsuspend`, `user.force_logout`, `org.view`,
`platform_admin.grant`, `platform_admin.revoke`.

---

## 6. Authentication state and refresh strategy

Recommended client state machine:

1. **Login** → keep `access_token` and `user` in memory (or your chosen store),
   never the refresh cookie (it is `HttpOnly`).
2. **Any protected request returns `401`** → run one shared refresh.
   - Refresh succeeded → retry the original request **once**.
   - Refresh failed with `401`/`403` → clear local auth state and show login.
   - Refresh failed with `409 REFRESH_IN_PROGRESS` → do **not** log out; wait a
     short randomised delay and retry refresh.
   - Refresh failed with `429` → wait `Retry-After`, then retry; keep the session.
3. **Any request returns `403 ACCOUNT_SUSPENDED`** → clear local auth state and
   show the suspended-account message.
4. **Admin request returns `404`** → treat the admin panel as unavailable; do
   not retry or show a crash.
5. **Proactively refresh** before `expires_in` elapses, especially for admins
   (5 minutes).

Never send the refresh token manually; the browser handles the cookie.

### 6.1 Suggested error helper

```ts
type ApiProblem = {
  type: string
  title: string
  status: number
  detail: string
  instance: string
  code: string
  errors?: { path: string; message: string }[]
}

async function readProblem(response: Response): Promise<ApiProblem | null> {
  if (response.headers.get('content-type')?.includes('application/problem+json')) {
    return response.json()
  }
  return null
}
```

Display `errors[].message` next to the matching field using `errors[].path`;
fall back to `detail` for non-field errors.

---

## 7. Rate limits for auth routes

Only these routes are rate limited today (plus `/admin/**` above).

| Route | Scope | Default |
|---|---|---|
| `POST /auth/register` | per client IP | 10/min |
| `POST /auth/login` | per client IP **and** per normalised email | 10/min each |
| `POST /auth/refresh` | per client IP | 30/min |

Every rate-limited successful response advertises the **tightest** applicable
budget:

```http
RateLimit-Limit: 10
RateLimit-Remaining: 9
RateLimit-Reset: 47
```

On `429`, the same headers are present plus `Retry-After`. `RateLimit-Reset` and
`Retry-After` are seconds.

`GET /users/me`, `PATCH /users/me`, `POST /users/me/password` and logout are not
rate limited by the backend today.

---

## 8. Field and enum reference

### User

| Field | Type | Notes |
|---|---|---|
| `id` | UUID string | |
| `email` | string | Stored lowercase; registration/login normalise case |
| `display_name` | string | 1–80 chars |
| `is_platform_admin` | boolean | UI hint only; server rechecks |

### Admin user summary

`id`, `email`, `display_name`, `status`, `created_at`, `last_login_at`,
`owns_org`, `membership_count`.

### Admin user detail

`id`, `email`, `display_name`, `status`, `suspension`, `is_platform_admin`,
`created_at`, `last_login_at`, `owned_org`, `memberships`, `active_sessions`.

### Admin org summary/detail

`id`, `name`, `state`, `owner`, `member_count`, `created_at`; detail adds
`members` and `proxy`.

### Enums

| Enum | Values |
|---|---|
| User status | `active`, `suspended` |
| Org state | `CREATING`, `STARTING`, `ONLINE`, `OFFLINE`, `DELETING` |
| Org role | `owner`, `co_owner`, `member` |
| Config status (admin) | `applied`, `pending`, `rejected` |
| Audit target type | `user`, `org` |

---

## 9. Frontend checklist

- [ ] Prefix every request with `/api/v1`.
- [ ] Use `snake_case` request and response fields exactly.
- [ ] Send `credentials: 'include'` to `/auth/login`, `/auth/refresh`,
      `/auth/logout`.
- [ ] Send `Authorization: Bearer <access_token>` to every other non-public
      endpoint.
- [ ] Keep refresh single-flight and retry a `401` once after refresh.
- [ ] Treat `409 REFRESH_IN_PROGRESS` as a retry, not a logout.
- [ ] Treat `403 ACCOUNT_SUSPENDED` as a logout plus a clear message.
- [ ] Treat admin `404` as "panel unavailable", not as a generic failure.
- [ ] Ask for the admin's password again for suspend and force-logout.
- [ ] Reset pagination cursors when filters change.
- [ ] Honour `Retry-After` on `429`.
- [ ] Render `errors[].path` / `errors[].message` for `422`.
- [ ] Do not parse JWTs for permissions.
- [ ] Do not assume the org/config/bans/logs/SSE endpoints exist yet.

---

## 10. Where this contract comes from

Backend implementation and tests that define the behaviour above:

- `backend/src/main/kotlin/org/proxyma/backend/auth/`
- `backend/src/main/kotlin/org/proxyma/backend/user/`
- `backend/src/main/kotlin/org/proxyma/backend/admin/`
- `backend/src/main/kotlin/org/proxyma/backend/security/`
- `backend/src/main/kotlin/org/proxyma/backend/common/api/`
- `backend/src/main/resources/application.properties`
- `backend/src/test/kotlin/org/proxyma/backend/AuthIntegrationTest.kt`
- `backend/src/test/kotlin/org/proxyma/backend/Admin*IntegrationTest.kt`
- `backend/src/test/kotlin/org/proxyma/backend/RateLimitResponseTest.kt`
- `backend/src/test/kotlin/org/proxyma/backend/CorsRejectionTest.kt`

Broader product design (including endpoints not yet implemented):
[`docs/api.md`](../../docs/api.md) and
[`docs/proxyma-security.md`](../../docs/proxyma-security.md).
