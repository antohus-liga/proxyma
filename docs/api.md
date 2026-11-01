# API reference

> Status: design stage. This is the intended contract between the dashboard and the backend. Field names and endpoints may change while the project is being built, and this file should change with them.

## 1. Conventions

| Topic | Convention |
|---|---|
| Base path | `/api/v1` |
| Format | JSON, UTF-8, `Content-Type: application/json` |
| Errors | `application/problem+json`, one shape from every layer (see [section 12](#12-errors)) |
| Naming | `snake_case` everywhere, including the proxy config, so documents are identical end to end |
| IDs | UUIDs |
| Timestamps | ISO 8601 in UTC, for example `2026-10-18T14:30:00Z` |
| Authentication | `Authorization: Bearer <access_token>`, except for the public auth endpoints. See [`proxyma-security.md`](proxyma-security.md#3-authentication). |
| Org scoping | Org endpoints live under `/orgs/{org_id}/...`. The backend checks the caller's **membership** first, then their role. |
| Not a member | `404 Not Found`, so the API doesn't reveal which orgs exist |
| Member without permission | `403 Forbidden` |
| Pagination | Cursor-based: `?limit=50&cursor=<opaque>`. Responses return `items` and `next_cursor` (`null` on the last page). Default `limit` is 50, maximum 200 (logs: 1000). |
| Async changes | Anything that has to reach the proxy returns `202 Accepted`. Its progress is reported through status fields and the live stream. |

## 2. Endpoint overview

**Roles**: **Public** (no login), **User** (any logged-in user), **Member**, **Co-owner**, **Owner**. A higher role includes everything below it.

| Method | Path | Min. role | Purpose |
|---|---|---|---|
| POST | `/auth/register` | Public | Create an account |
| POST | `/auth/login` | Public | Log in |
| POST | `/auth/refresh` | Public (refresh cookie) | Get a new access token |
| POST | `/auth/logout` | User | Revoke the refresh token |
| GET | `/users/me` | User | Current user |
| PATCH | `/users/me` | User | Update display name |
| POST | `/users/me/password` | User | Change password |
| POST | `/orgs` | User | Create an org (one owned org per user) |
| GET | `/orgs` | User | Orgs I belong to |
| GET | `/orgs/{org_id}` | Member | Org details and proxy status |
| PATCH | `/orgs/{org_id}` | Co-owner | Rename org |
| DELETE | `/orgs/{org_id}` | Owner | Delete org |
| POST | `/orgs/{org_id}/leave` | Member (not owner) | Leave an org |
| GET | `/orgs/{org_id}/members` | Member | List members |
| PATCH | `/orgs/{org_id}/members/{user_id}` | Co-owner | Change a member's role |
| DELETE | `/orgs/{org_id}/members/{user_id}` | Co-owner | Remove a member |
| GET | `/orgs/{org_id}/invitations` | Co-owner | Pending invitations |
| POST | `/orgs/{org_id}/invitations` | Co-owner | Invite someone |
| DELETE | `/orgs/{org_id}/invitations/{invitation_id}` | Co-owner | Revoke an invitation |
| POST | `/invitations/accept` | User | Accept an invitation with its token |
| GET | `/orgs/{org_id}/config` | Member | Current config and its status |
| PUT | `/orgs/{org_id}/config` | Member | Save a new config |
| POST | `/orgs/{org_id}/config/validate` | Member | Dry-run validation, nothing is saved |
| GET | `/orgs/{org_id}/config/versions` | Member | Config history |
| GET | `/orgs/{org_id}/config/versions/{version}` | Member | One historical config |
| POST | `/orgs/{org_id}/config/versions/{version}/restore` | Member | Save an old config as a new version |
| GET | `/orgs/{org_id}/bans` | Member | List bans |
| POST | `/orgs/{org_id}/bans` | Member | Ban an IP |
| DELETE | `/orgs/{org_id}/bans/{ban_id}` | Member | Lift a ban |
| GET | `/orgs/{org_id}/logs` | Member | Search log history |
| GET | `/orgs/{org_id}/logs/stats` | Member | Aggregated counts over time |
| GET | `/orgs/{org_id}/audit` | Member | Audit trail |
| POST | `/orgs/{org_id}/stream/ticket` | Member | Get a one-time ticket for the live stream |
| GET | `/orgs/{org_id}/stream` | Member (ticket) | Live stream (SSE) |

## 3. Authentication

### `POST /auth/register`

```json
{ "email": "ana@example.com", "display_name": "Ana", "password": "correct horse battery" }
```

`201 Created`

```json
{ "id": "5f0c…", "email": "ana@example.com", "display_name": "Ana", "is_platform_admin": false }
```

Errors: `409` (email taken, with a generic message that doesn't confirm it to attackers where possible), `422` (weak password or invalid email).

### `POST /auth/login`

```json
{ "email": "ana@example.com", "password": "correct horse battery" }
```

`200 OK`. The refresh token is set as an `HttpOnly`, `SameSite=Strict` cookie (`Secure` is included unless `proxyma.auth.cookie.secure=false` for plain-HTTP development). It is never in the body.

```json
{
  "access_token": "eyJ…",
  "token_type": "Bearer",
  "expires_in": 900,
  "user": { "id": "5f0c…", "email": "ana@example.com", "display_name": "Ana", "is_platform_admin": false }
}
```

Errors: `401` with the same generic message for an unknown email and a wrong password.

### `POST /auth/refresh`

Needs the refresh cookie and no body. Returns a new access token (same shape as login) and **rotates** the refresh cookie. Expiry slides: each rotation renews the stored token and the cookie for a full refresh TTL.

Clients must still de-duplicate refreshes, but a lost race is no longer fatal. A just-rotated token replayed within `proxyma.auth.refresh-reuse-grace` (default 30 s) returns `409 REFRESH_IN_PROGRESS`; the token family is left intact and the cookie is not cleared, so the winner's response remains valid. A replay after that window is treated as theft: the whole family is revoked and the response is `401 INVALID_REFRESH_TOKEN` with a cookie-clearing `Set-Cookie`.

### `POST /auth/logout`

Revokes the refresh token and clears the cookie. `204 No Content`. It is unauthenticated and idempotent: a missing or already-revoked cookie still returns `204` and still clears the cookie.

### Users

- `GET /users/me` returns `{ id, email, display_name, is_platform_admin }`.
- `PATCH /users/me` accepts `{ "display_name": "…" }`.
- `POST /users/me/password` accepts `{ "current_password": "…", "new_password": "…" }`, revokes all refresh tokens and returns `204`. It also invalidates already-issued access tokens, which are rejected with `401` on their next request.

## 4. Organisations

### `POST /orgs`

```json
{ "name": "Acme" }
```

`201 Created`. The proxy container is started asynchronously, so the org begins in `STARTING`.

```json
{
  "id": "b7a1…",
  "name": "Acme",
  "role": "owner",
  "state": "STARTING",
  "created_at": "2026-10-18T14:30:00Z"
}
```

Errors: `409 ORG_ALREADY_OWNED` if the user already owns an org, `422` for an invalid name (1 to 64 characters).

### `GET /orgs`

Every org the caller belongs to, with their role in each. This feeds the dashboard's org switcher.

```json
{
  "items": [
    { "id": "b7a1…", "name": "Acme", "role": "owner", "state": "ONLINE" },
    { "id": "c93d…", "name": "Side project", "role": "member", "state": "OFFLINE" }
  ]
}
```

### `GET /orgs/{org_id}`

```json
{
  "id": "b7a1…",
  "name": "Acme",
  "role": "owner",
  "state": "ONLINE",
  "proxy": {
    "url": "http://127.0.0.1:20017",
    "last_heartbeat_at": "2026-10-18T14:35:12Z",
    "running_config_version": 7
  },
  "created_at": "2026-10-18T14:30:00Z"
}
```

`state` is one of `CREATING`, `STARTING`, `ONLINE`, `OFFLINE`, `DELETING` (see [`architecture.md`](architecture.md#7-flow-proxy-lifecycle)). `proxy.url` is where the org's traffic should be sent.

### `PATCH /orgs/{org_id}`

`{ "name": "New name" }`. Returns the updated org.

### `DELETE /orgs/{org_id}`

Owner only. `202 Accepted`. The org moves to `DELETING` and cleanup continues in the background. A deleted org's endpoints return `404`.

### `POST /orgs/{org_id}/leave`

Removes the caller's own membership. `204`. The owner can't leave and must delete the org instead (`409 OWNER_CANNOT_LEAVE`).

## 5. Members and invitations

### Members

`GET /orgs/{org_id}/members`

```json
{
  "items": [
    { "user_id": "5f0c…", "display_name": "Ana", "email": "ana@example.com", "role": "owner", "joined_at": "…" },
    { "user_id": "a21e…", "display_name": "Bo", "email": "bo@example.com", "role": "member", "joined_at": "…" }
  ]
}
```

`PATCH /orgs/{org_id}/members/{user_id}` with `{ "role": "co_owner" }` or `{ "role": "member" }`.
`DELETE /orgs/{org_id}/members/{user_id}` returns `204`.

Rules, each returning `403` with a specific `code`:

| Rule | Code |
|---|---|
| Nobody can change or remove the owner | `OWNER_PROTECTED` |
| Only the owner can change or remove a co-owner | `CO_OWNER_PROTECTED` |
| You can't change your own role | `SELF_ROLE_CHANGE` |

### Invitations

`POST /orgs/{org_id}/invitations`

```json
{ "email": "bo@example.com", "role": "member" }
```

`role` is `member` (default) or `co_owner`. `201 Created`:

```json
{
  "id": "e4b2…",
  "email": "bo@example.com",
  "role": "member",
  "expires_at": "2026-10-25T14:30:00Z",
  "invite_link": "http://localhost:5173/invite?token=…"
}
```

The `invite_link` (and its token) is returned **only in this response** and stored hashed, so the inviter shares it with the invitee. Sending email is not part of the MVP.

`GET /orgs/{org_id}/invitations` lists pending invitations (without tokens). `DELETE /orgs/{org_id}/invitations/{invitation_id}` revokes one.

`POST /invitations/accept`

```json
{ "token": "…" }
```

Returns the joined org (same shape as an item in `GET /orgs`). The token is single-use and expires after 7 days. The caller must be logged in as the invited email address.

Errors: `410 INVITATION_EXPIRED`, `404` for an unknown or already-used token, `403 INVITATION_EMAIL_MISMATCH`, `409 ALREADY_MEMBER`.

## 6. Proxy config

The config document is described in [`proxy-config.md`](proxy-config.md). The API wraps it with versions and status.

### `GET /orgs/{org_id}/config`

```json
{
  "version": 7,
  "published_version": 7,
  "applied_version": 6,
  "status": "pending",
  "updated_at": "2026-10-18T14:40:00Z",
  "updated_by": { "user_id": "a21e…", "display_name": "Bo" },
  "rejection": null,
  "config": { "routes": [], "rate_limit": { "enabled": true, "requests": 100, "per": "minute" } }
}
```

The response also sets `ETag: "7"`.

| `status` | Meaning |
|---|---|
| `applied` | The proxy confirmed the latest version |
| `pending` | Saved, waiting for the proxy to apply (or the proxy is offline) |
| `rejected` | The proxy refused the latest version. `rejection` holds its errors, and the proxy is still running the previous version. |

### `PUT /orgs/{org_id}/config`

```http
PUT /api/v1/orgs/b7a1…/config
If-Match: "7"
```

```json
{ "config": { "…": "…" } }
```

`If-Match` is **required**. It carries the version the editor started from. This protects against two members overwriting each other's changes.

| Result | When |
|---|---|
| `202 Accepted` | Saved as a new version. Body: `{ "version": 8, "status": "pending" }` |
| `412 Precondition Failed` | Someone saved a newer version first. The response includes the current version so the dashboard can show a diff. |
| `428 Precondition Required` | `If-Match` missing |
| `422 Unprocessable Entity` | Config failed validation, with `errors` (see [section 12](#12-errors)) |

Saving a config that is identical to the current one creates no new version and returns `200 OK`.

The proxy later confirms or rejects the version, which the dashboard sees as a `config_status` event on the live stream (or through `GET /orgs/{org_id}/config`).

### `POST /orgs/{org_id}/config/validate`

Same body as `PUT`, no `If-Match`. Runs the same validation without saving or publishing anything.

```json
{ "valid": false, "errors": [ { "path": "/routes/1/upstream/url", "message": "must be a valid http or https URL" } ] }
```

The dashboard uses this for live feedback while the user is typing.

### History

- `GET /orgs/{org_id}/config/versions` lists versions: `{ version, created_at, created_by, status }`, newest first, paginated.
- `GET /orgs/{org_id}/config/versions/{version}` returns the stored config for that version.
- `POST /orgs/{org_id}/config/versions/{version}/restore` saves the old config as a **new** version (it never rewrites history) and returns `202` like `PUT`. It requires `If-Match` too.

## 7. Bans

The concepts are in [`architecture.md`](architecture.md) and the ban record shape is the same as in the `BANS` bucket.

### `GET /orgs/{org_id}/bans`

Query: `active` (`true` by default), `ip`, `source` (`auto` or `manual`), `limit`, `cursor`.

```json
{
  "items": [
    {
      "id": "7c1e…",
      "ip": "198.51.100.23",
      "source": "auto",
      "reason": "10 rate-limit violations in 60s",
      "rule": "rate_limit_exceeded",
      "created_by": null,
      "created_at": "2026-10-18T12:00:00Z",
      "expires_at": "2026-10-18T12:15:00Z",
      "removed_at": null
    }
  ],
  "next_cursor": null
}
```

### `POST /orgs/{org_id}/bans`

```json
{ "ip": "198.51.100.23", "reason": "Scanning /admin", "duration_seconds": 3600 }
```

`duration_seconds` is optional. `null` or omitted means permanent. The maximum is 2,592,000 (30 days). Returns `202 Accepted` with the ban. Delivery to the proxy is asynchronous.

Errors: `422` (invalid IP), `409 ALREADY_BANNED`, `409 BAN_LIMIT_REACHED` (10,000 active bans per org).

### `DELETE /orgs/{org_id}/bans/{ban_id}`

Lifts a ban early. `202 Accepted`. The ban row is kept as history with `removed_at` and `removed_by` set.

## 8. Logs

Logs come from ClickHouse, with every query scoped to the org from the verified membership.

### `GET /orgs/{org_id}/logs`

| Param | Meaning |
|---|---|
| `from`, `to` | Time range. Defaults to the last hour. The maximum span is 30 days (the retention period). |
| `ip` | Exact client IP |
| `action` | `allowed`, `rate_limited`, `blocked` or `malicious`. May be repeated. |
| `status` | HTTP status code, or a class such as `5xx` |
| `method` | HTTP method |
| `path_prefix` | Path starts with |
| `rule` | Rule or route name |
| `limit`, `cursor` | Pagination, newest first |

```json
{
  "items": [
    {
      "id": "01JA…",
      "timestamp": "2026-10-18T14:35:12.482Z",
      "client_ip": "198.51.100.23",
      "method": "GET",
      "path": "/api/users",
      "status": 200,
      "upstream": "users-api",
      "latency_ms": 18,
      "action": "allowed",
      "rule": "",
      "config_version": 7
    }
  ],
  "next_cursor": "…"
}
```

Entries are ordered by `(timestamp, id)` and the cursor is a keyset position, so paging stays fast and stable while new logs arrive.

### `GET /orgs/{org_id}/logs/stats`

Query: `from`, `to`, `interval` (`1m`, `5m`, `1h`, `1d`). The backend limits the number of buckets returned.

```json
{
  "interval": "5m",
  "buckets": [
    { "t": "2026-10-18T14:30:00Z", "total": 412, "allowed": 398, "rate_limited": 9, "blocked": 3, "malicious": 2 }
  ]
}
```

This feeds the dashboard's charts without sending raw rows to the browser.

## 9. Audit trail

### `GET /orgs/{org_id}/audit`

Query: `from`, `to`, `actor` (user ID), `action`, `limit`, `cursor`. Newest first, read-only.

```json
{
  "items": [
    {
      "id": "9a3b…",
      "timestamp": "2026-10-18T14:40:00Z",
      "actor": { "type": "user", "user_id": "a21e…", "display_name": "Bo" },
      "action": "config.update",
      "target": { "type": "config", "id": "8" },
      "summary": "Config v7 to v8: added route users-api",
      "source_ip": "203.0.113.10"
    }
  ],
  "next_cursor": null
}
```

Actions use the form `<area>.<verb>`, for example `org.create`, `org.delete`, `member.invite`, `member.role_change`, `member.remove`, `config.update`, `config.restore`, `ban.create`, `ban.remove` and `permission.denied`. Automatic actions have `actor.type = "system"` and the rule that caused them.

## 10. Live stream (Server-Sent Events)

One stream per dashboard tab carries everything the dashboard shows live, so a browser doesn't need several connections.

### Getting access

1. `POST /orgs/{org_id}/stream/ticket` (normal authentication) returns:

```json
{ "ticket": "c2f9…", "expires_in": 30 }
```

2. `GET /orgs/{org_id}/stream?ticket=c2f9…` opens the stream.

The ticket is single-use, tied to the user and org, and valid for about 30 seconds. The stream closes when the user's session is revoked or their membership ends. See [`proxyma-security.md`](proxyma-security.md#sse-and-authentication).

Optional filters, applied server-side: `action` (repeatable), `ip`, `status`.

### Event types

| Event | Data |
|---|---|
| `log` | One log entry, same shape as in `GET /logs`. The SSE `id` is the log entry's `id`. |
| `config_status` | `{ "version": 8, "status": "applied" }`, or `"rejected"` with `errors` |
| `proxy_status` | `{ "state": "OFFLINE" }` or `{ "state": "ONLINE", "running_config_version": 8 }` |
| `ban` | `{ "ban_id": "7c1e…", "ip": "…", "change": "created" | "removed" }` |
| `heartbeat` | Keeps the connection open through proxies and load balancers (comment line every 15 seconds) |

```text
id: 01JA…
event: log
data: {"timestamp":"2026-10-18T14:35:12.482Z","client_ip":"198.51.100.23","method":"GET","path":"/api/users","status":200,"action":"allowed"}

event: config_status
data: {"version":8,"status":"applied"}
```

### Reconnecting

The live view is not replayable. After a reconnect, the dashboard sends the last `id` it saw (`Last-Event-ID`) and backfills the gap with `GET /logs?from=<last timestamp>`, then continues from the stream. It must de-duplicate by `id`, because the two sources can overlap. `config_status`, `proxy_status` and `ban` events are not replayed either, so the dashboard re-reads `GET /config`, `GET /orgs/{org_id}` and `GET /bans` after reconnecting.

## 11. Rate limits on the API

The backend rate limits itself, separately from anything the proxy does. Every response from a rate-limited endpoint advertises the current budget:

| Header | Meaning |
|---|---|
| `RateLimit-Limit` | Requests allowed in the current window |
| `RateLimit-Remaining` | Requests still allowed in this window |
| `RateLimit-Reset` | Seconds until the window resets |

Login checks two buckets (per IP and per account) and reports the tighter one. Exceeding a limit returns `429 Too Many Requests` with the same three headers plus `Retry-After`.

| Endpoints | Limit (default) |
|---|---|
| `/auth/login`, `/auth/register` | 10 per minute per IP, plus a per-account limit on login |
| `/auth/refresh` | 30 per minute per IP |
| `/invitations/accept` | 10 per minute per user |
| `PUT .../config`, `.../restore` | 30 per minute per org |
| `.../logs`, `.../logs/stats` | 60 per minute per user |
| `.../stream/ticket` | 30 per minute per user |
| `/admin/...` | 120 per minute per admin, and 10 per minute for suspend and force-logout |
| Everything else | 300 per minute per user |

## 12. Errors

Errors use the standard `application/problem+json` format, with a stable machine-readable `code`:

```json
{
  "type": "about:blank",
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "The request has 2 problems.",
  "instance": "/api/v1/orgs/5f0c…/config",
  "code": "VALIDATION_FAILED",
  "errors": [
    { "path": "/routes/1/upstream/url", "message": "must be a valid http or https URL" },
    { "path": "/detection/custom_sequences/0/value", "message": "hex value must have an even number of digits" }
  ]
}
```

Every error path — controllers, security filters, CORS rejections — returns this same document in the same key order: `type`, `title`, `status`, `detail`, `instance`, `code`, then `errors` when present. `type` is always `about:blank` for now, so clients can rely on it existing; `instance` is the request path; `title` is the HTTP reason phrase.

`errors[].path` is a JSON Pointer into the submitted document, the same format the proxy uses when it rejects a config. Responses never contain stack traces or internal details.

| Status | Used for |
|---|---|
| `400` | Malformed JSON or parameters |
| `401` | Missing, expired or invalid access token (`UNAUTHORIZED`), including tokens retired by a password change or force-logout |
| `403` | The caller is a member but lacks the role (`code` explains which rule), also `ACCOUNT_SUSPENDED` (403 returned on login and refresh), `REAUTH_FAILED`, `ADMIN_CANNOT_TARGET_ADMIN`, and `CORS_REJECTED` when the request origin is not in the allowlist |
| `404` | Unknown resource, **or an org the caller doesn't belong to** |
| `409` | State conflict, such as `ORG_ALREADY_OWNED`, `ALREADY_MEMBER`, `ALREADY_BANNED`, `BAN_LIMIT_REACHED`, `OWNER_CANNOT_LEAVE`, `CANNOT_SUSPEND_SELF`, `REFRESH_IN_PROGRESS` |
| `410` | `INVITATION_EXPIRED` |
| `412` / `428` | Config `If-Match` conflict or missing |
| `422` | Validation failed |
| `429` | Rate limited |
| `500` | Unexpected error (logged on the server, generic body) |

Unmatched routes are authenticated before they are routed: an unknown path without a valid access token returns `401`, not `404`, and a method mismatch on a protected path does the same. This avoids anonymous route enumeration. CORS rejections happen before authentication, so a disallowed `Origin` wins over `401`.

## 13. Admin API

Available only when `PROXYMA_ADMIN_ENABLED=true`, only to platform admins, and
only from the networks in `PROXYMA_ADMIN_ALLOWED_CIDRS` (when that allowlist is
set). The namespace is deny-by-default:

| Situation | Response |
|---|---|
| Panel disabled, or the source address is outside the allowed networks | `404 NOT_FOUND`, checked **before** the token is examined |
| Missing, malformed or expired access token | `401 UNAUTHORIZED` |
| Valid token for a normal user, a missing account, or a suspended account | `404 NOT_FOUND` |

A non-admin therefore cannot tell an enabled panel from a route that does not
exist. The platform-admin flag and the account status are read from the
database on every request; neither is stored in the JWT. Admin access tokens
have a shorter lifetime (5 minutes by default) than normal access tokens.

Responses contain **metadata only**: no config contents, logs, ban IPs, org
audit entries, password hashes or tokens. See
[`proxyma-security.md`](proxyma-security.md#14-platform-administration).

| Method | Path | Purpose | Audited |
|---|---|---|---|
| GET | `/admin/stats` | Platform totals | No |
| GET | `/admin/users` | Search users | No |
| GET | `/admin/users/{user_id}` | One user's details | Yes (view) |
| POST | `/admin/users/{user_id}/suspend` | Suspend an account | Yes |
| POST | `/admin/users/{user_id}/unsuspend` | Restore an account | Yes |
| POST | `/admin/users/{user_id}/force-logout` | Revoke all of a user's sessions | Yes |
| GET | `/admin/orgs` | Search orgs | No |
| GET | `/admin/orgs/{org_id}` | One org's details | Yes (view) |
| GET | `/admin/audit` | The platform audit trail | No |

Lists are ordered newest first. They accept `limit` (1–200, default 50) and an
opaque `cursor` from the previous response's `next_cursor`. A malformed cursor
returns `400 INVALID_CURSOR`.

The admin API has two per-admin rate limits: 120 requests per minute for every
admin route, and 10 per minute for the risky actions (`suspend` and
`force-logout`). Successful responses carry `RateLimit-Limit`,
`RateLimit-Remaining` and `RateLimit-Reset`; a rejected request returns
`429 RATE_LIMITED` with `Retry-After`. Both limits are configurable.

### `GET /admin/stats`

```json
{
  "users": { "total": 42, "suspended": 1 },
  "orgs": { "total": 31, "online": 28, "offline": 3 }
}
```

`orgs.offline` is `total - online`, so every org that is not currently `ONLINE`
(including `CREATING`, `STARTING` and `DELETING`) is counted as offline.

### `GET /admin/users`

Query: `q` (case-insensitive match on email or display name), `status` (`active`
or `suspended`), `limit`, `cursor`. An unknown `status` returns
`400 INVALID_PARAMETER`.

```json
{
  "items": [
    {
      "id": "5f0c…",
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

### `GET /admin/users/{user_id}`

```json
{
  "id": "5f0c…",
  "email": "ana@example.com",
  "display_name": "Ana",
  "status": "active",
  "suspension": null,
  "is_platform_admin": false,
  "created_at": "2026-09-01T10:00:00Z",
  "last_login_at": "2026-10-18T09:12:00Z",
  "owned_org": { "id": "b7a1…", "name": "Acme" },
  "memberships": [
    { "org_id": "b7a1…", "org_name": "Acme", "role": "owner", "joined_at": "2026-09-01T10:05:00Z" },
    { "org_id": "c93d…", "org_name": "Side project", "role": "member", "joined_at": "2026-09-20T16:00:00Z" }
  ],
  "active_sessions": 1
}
```

When the account is suspended, `suspension` is an object instead of `null`:

```json
{
  "reason": "Repeated abuse reports",
  "suspended_at": "2026-10-18T14:35:12Z",
  "suspended_by": { "user_id": "a21e…", "display_name": "Platform Admin" }
}
```

`suspended_by` is `null` when the account was suspended by an operator rather
than through the admin API. Viewing this endpoint is recorded as `user.view` in
the platform audit trail.

### `POST /admin/users/{user_id}/suspend`

```json
{ "reason": "Repeated abuse reports", "password": "<admin's own password>" }
```

`200 OK` with the updated user (including the new `suspension` object). The
account is marked suspended, its refresh tokens are revoked and its
`token_version` is bumped, so current access tokens stop working on the next
request. Orgs keep running, and each org the user belongs to gets a
`member.suspend` entry in its own audit trail with the actor shown as a platform
administrator.

Errors: `403 REAUTH_FAILED` (wrong step-up password), `403
ADMIN_CANNOT_TARGET_ADMIN`, `409 CANNOT_SUSPEND_SELF`, `422` for a missing or
blank reason.

### `POST /admin/users/{user_id}/unsuspend`

`{ "reason": "Resolved after contact" }`. Returns the updated user with
`suspension: null`. The account becomes active again, its `token_version` is
bumped so pre-suspension tokens stay dead, and each affected org gets a
`member.unsuspend` audit entry. A reason is required (`422` otherwise).

### `POST /admin/users/{user_id}/force-logout`

`{ "reason": "Possible account compromise", "password": "…" }`. Revokes all
refresh tokens and bumps `token_version`, so the user's current access token
stops working immediately. Returns `204`. This action may target another
platform admin. `403 REAUTH_FAILED` for a wrong password and `422` for a missing
reason.

### `GET /admin/orgs`

Query: `q` (case-insensitive match on org name, owner display name or owner
email), `state` (`CREATING`, `STARTING`, `ONLINE`, `OFFLINE` or `DELETING`),
`limit`, `cursor`. An unknown `state` returns `400 INVALID_PARAMETER`.

```json
{
  "items": [
    {
      "id": "b7a1…",
      "name": "Acme",
      "state": "ONLINE",
      "owner": { "user_id": "5f0c…", "display_name": "Ana" },
      "member_count": 3,
      "created_at": "2026-09-01T10:00:00Z"
    }
  ],
  "next_cursor": null
}
```

`owner` is `null` for an org without an owner membership.

### `GET /admin/orgs/{org_id}`

```json
{
  "id": "b7a1…",
  "name": "Acme",
  "state": "ONLINE",
  "created_at": "2026-09-01T10:00:00Z",
  "owner": { "user_id": "5f0c…", "display_name": "Ana" },
  "members": [
    { "user_id": "5f0c…", "display_name": "Ana", "role": "owner" },
    { "user_id": "a21e…", "display_name": "Bo", "role": "member" }
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

`config_status` is `applied` when the proxy has acknowledged the latest config
version, `rejected` when the latest version was rejected, and `pending`
otherwise (including an org that has no config yet). `active_ban_count` counts
bans that are not removed and not expired. The response has **no** config
contents, logs, ban IPs or org audit entries. Viewing it is recorded as
`org.view` in the platform audit trail.

### `GET /admin/audit`

Query: `from`, `to` (ISO-8601 instants; `from` is inclusive and `to` is
exclusive), `admin_user_id`, `action`, `limit`, `cursor`. Returns platform audit
entries newest first. `from` after `to` returns `400 INVALID_PARAMETER`.

```json
{
  "items": [
    {
      "id": "9d2b…",
      "timestamp": "2026-10-18T14:35:12Z",
      "admin": { "user_id": "a21e…", "display_name": "Platform Admin" },
      "action": "user.suspend",
      "target": { "type": "user", "id": "5f0c…" },
      "summary": "Suspended ana@example.com: Repeated abuse reports",
      "source_ip": "203.0.113.10"
    }
  ],
  "next_cursor": null
}
```

`target` is `null` when an action has no target. `admin` is `null` for entries
written by the server-side `grant-admin` / `revoke-admin` commands, where the
actor is the host operator rather than an authenticated admin; `source_ip` is
also `null` for those entries.

The action names are `user.view`, `user.suspend`, `user.unsuspend`,
`user.force_logout`, `org.view`, `platform_admin.grant` and
`platform_admin.revoke`. This endpoint reads only the platform trail; an org's
own audit entries are never exposed through it, and reading the audit trail is
not itself audited.

### Operator commands

Platform-admin status can only be changed on the server, never through the API:

```bash
cd backend
./gradlew bootRun --args='grant-admin ana@example.com'
./gradlew bootRun --args='revoke-admin ana@example.com'
```

The command starts the application without a web server, applies migrations,
changes the flag and writes a `platform_admin.grant` or `platform_admin.revoke`
entry to `platform_audit`. The command is idempotent: when the account is
already in the requested state it changes nothing and writes no audit entry. A
change bumps the user's `token_version`, so an existing token cannot silently
acquire or keep admin powers.

## 14. Not in the MVP

- Email delivery for invitations (links are shared by hand)
- Email verification and password reset
- Account deletion
- Ownership transfer
- API keys or machine access (the API is for the dashboard only)
- Webhooks or notifications
- Exporting logs
