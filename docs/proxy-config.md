# Proxy configuration

> Status: design stage. This is the contract between the dashboard, the backend and the proxy. If the schema changes, all three change with it.

## 1. Overview

Each organisation has exactly one proxy configuration. It is:

- **Edited** in the dashboard by any member, co-owner or owner.
- **Validated** by the backend against the schema below, then saved to PostgreSQL with a new `version`.
- **Delivered** to the proxy over NATS (see [`architecture.md`](architecture.md#4-flow-config-update)).
- **Validated again** by the proxy before applying. The proxy never trusts the backend blindly, and a config that fails validation is rejected whole, never half-applied.

The config is a single JSON document. It describes **behaviour** (routes, limits, rules). It does not describe **state**, such as which IPs are currently banned. State is covered in [section 8](#8-bans-are-state-not-config).

### Message envelope

What actually travels over `org.<id>.config`:

```json
{
  "version": 7,
  "schema_version": 1,
  "config": { }
}
```

| Field | Meaning |
|---|---|
| `version` | The org's config version. The proxy ignores any version lower than or equal to the one it already runs. |
| `schema_version` | The version of this schema. Lets the proxy refuse a format it doesn't understand instead of misreading it. |
| `config` | The document described below. |

## 2. Request pipeline

Every request goes through the same steps, in this order. The order matters because it decides which rule wins and keeps the cheap checks first.

```mermaid
flowchart TD
    A[Request arrives] --> B{Client IP<br/>banned?}
    B -- yes --> X1[403 + log: blocked]
    B -- no --> C{Route<br/>matches?}
    C -- no --> X2[404 + log]
    C -- yes --> D{Rate limit<br/>exceeded?}
    D -- yes --> X3[429 + log: rate_limited<br/>+ count towards auto-block]
    D -- no --> E{Malicious<br/>sequence found?}
    E -- yes --> X4[403 + log: malicious<br/>+ count towards auto-block]
    E -- no --> F[Forward to upstream]
    F --> G[Log: allowed]
```

1. **Ban check.** A banned IP is rejected immediately, before any other work.
2. **Route match.** No matching route means `404`.
3. **Rate limit.** Done before inspection, so a flood is rejected cheaply without scanning every request.
4. **Malicious-sequence detection.** Only requests that passed the earlier steps are inspected.
5. **Forward** to the upstream and log the result.

Trusted IPs (see [section 7](#7-trusted-ips)) skip steps 3 and 4 and are never auto-banned. Manual bans still apply to them.

## 3. Full example

```json
{
  "client_ip": {
    "source": "connection"
  },
  "trusted_ips": ["203.0.113.10", "10.0.0.0/24"],
  "routes": [
    {
      "id": "users-api",
      "name": "Users API",
      "match": {
        "path_prefix": "/api/users",
        "methods": ["GET", "POST"]
      },
      "upstream": {
        "url": "https://users.internal.example.com",
        "timeout_ms": 10000
      },
      "strip_prefix": false,
      "rate_limit": {
        "requests": 30,
        "per": "minute"
      }
    },
    {
      "id": "everything-else",
      "name": "Main app",
      "match": { "path_prefix": "/" },
      "upstream": { "url": "https://app.internal.example.com" }
    }
  ],
  "rate_limit": {
    "enabled": true,
    "requests": 100,
    "per": "minute"
  },
  "detection": {
    "enabled": true,
    "action": "block",
    "inspect": ["path", "query", "headers", "body"],
    "max_body_bytes": 8192,
    "builtin": {
      "sql_injection": true,
      "path_traversal": true,
      "xss": true,
      "command_injection": true,
      "null_bytes": true
    },
    "custom_sequences": [
      {
        "name": "Block a known scanner string",
        "type": "text",
        "value": "sqlmap",
        "case_insensitive": true
      },
      {
        "name": "Suspicious byte pattern",
        "type": "hex",
        "value": "0d0a0d0a"
      }
    ]
  },
  "auto_block": {
    "enabled": true,
    "rules": [
      {
        "on": "rate_limit_exceeded",
        "threshold": 10,
        "window_seconds": 60,
        "ban_seconds": 900
      },
      {
        "on": "malicious_request",
        "threshold": 3,
        "window_seconds": 300,
        "ban_seconds": 86400
      }
    ]
  }
}
```

## 4. Routes (API mapping)

`routes` is an ordered list. For each request the proxy picks the **first route that matches**, so put specific routes before general ones. The dashboard lets users reorder routes.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `id` | string | yes | | Unique within the config. Letters, digits, `-`, `_`. Used in logs. |
| `name` | string | no | `id` | Human-friendly label shown in the dashboard. |
| `match.path_prefix` | string | yes | | Must start with `/`. A request matches if its path starts with this. |
| `match.methods` | string[] | no | all | HTTP methods this route accepts. Other methods on this path do not match and fall through to the next route. |
| `upstream.url` | string | yes | | `http://` or `https://` URL of the service to forward to. |
| `upstream.timeout_ms` | integer | no | `30000` | How long to wait for the upstream before returning `504`. |
| `strip_prefix` | boolean | no | `false` | If `true`, `match.path_prefix` is removed before forwarding. With prefix `/api/users`, a request to `/api/users/42` reaches the upstream as `/42`. |
| `rate_limit` | object | no | org default | Overrides the org-wide rate limit for this route only. |

**Examples of how mapping works**

| `path_prefix` | `strip_prefix` | Incoming path | Sent to upstream |
|---|---|---|---|
| `/api/users` | `false` | `/api/users/42` | `/api/users/42` |
| `/api/users` | `true` | `/api/users/42` | `/42` |
| `/` | `false` | `/health` | `/health` |

If the upstream URL itself has a path (for example `https://host/v2`), the forwarded path is appended to it.

The proxy adds the standard `X-Forwarded-For`, `X-Forwarded-Host` and `X-Forwarded-Proto` headers so upstreams can see the original client.

## 5. Rate limiting

| Field | Type | Default | Description |
|---|---|---|---|
| `rate_limit.enabled` | boolean | `true` | Turns org-wide rate limiting on or off. |
| `rate_limit.requests` | integer | `100` | Requests allowed per window, per client IP. |
| `rate_limit.per` | `"second"`, `"minute"` or `"hour"` | `"minute"` | Window size. |

- Limits are tracked **per client IP**, **per route** (a route with its own `rate_limit` has a separate counter from the org-wide one).
- A route-level `rate_limit` replaces the org-wide one for that route. It does not stack with it.
- When the limit is exceeded, the proxy answers `429 Too Many Requests` with a `Retry-After` header and logs `action = rate_limited`.
- Counters live in the proxy's memory and reset when the proxy restarts.

## 6. Malicious byte-sequence detection

Basic by design: it catches the obvious attack patterns, not everything. It is not a replacement for a real web application firewall.

| Field | Type | Default | Description |
|---|---|---|---|
| `detection.enabled` | boolean | `true` | Master switch. |
| `detection.action` | `"block"` or `"log"` | `"block"` | `block` answers `403` and logs `action = malicious`. `log` forwards the request normally but still logs it, which is useful for trying rules out before enforcing them. |
| `detection.inspect` | string[] | `["path","query","headers","body"]` | Which parts of the request to scan. |
| `detection.max_body_bytes` | integer | `8192` | Only this many bytes from the start of the body are scanned. Larger bodies are still forwarded in full. |
| `detection.builtin` | object | all `true` | Toggles for the built-in pattern sets below. |
| `detection.custom_sequences` | object[] | `[]` | User-defined sequences. |

### Built-in pattern sets

| Key | Catches (at a basic level) |
|---|---|
| `sql_injection` | Common SQL injection fragments such as `' OR 1=1`, `UNION SELECT`, comment terminators |
| `path_traversal` | `../`, `..\`, and their URL-encoded forms |
| `xss` | `<script`, `javascript:`, inline event handlers like `onerror=` |
| `command_injection` | Shell metacharacter chains such as `; rm`, `&& cat`, `` `...` `` and `$(...)` |
| `null_bytes` | `%00` and raw `0x00` bytes |

The exact patterns ship with the proxy and are not editable by users. Users switch sets on or off, and add their own sequences.

### Custom sequences

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `name` | string | yes | | Shown in logs as the `rule` that triggered. |
| `type` | `"text"` or `"hex"` | yes | | How `value` is interpreted. |
| `value` | string | yes | | The sequence. For `hex`, an even number of hex digits (for example `0d0a0d0a`). |
| `case_insensitive` | boolean | no | `false` | Only applies to `text`. |

A custom sequence is a **literal match**, not a regular expression. This is deliberate: it keeps the feature easy to understand and avoids regex patterns that could slow the proxy down.

## 7. Trusted IPs

`trusted_ips` is a list of IP addresses or CIDR ranges (IPv4 or IPv6).

- Trusted clients **skip rate limiting and detection** and are **never auto-banned**.
- It exists mainly so an org can't accidentally lock out its own office, CI runner or monitoring.
- A manual ban still applies to a trusted IP, since that is a deliberate action.

### Client IP source

| Field | Values | Default | Description |
|---|---|---|---|
| `client_ip.source` | `"connection"`, `"x_forwarded_for"` | `"connection"` | Where the proxy reads the client IP from. |

Use `x_forwarded_for` only if Proxyma itself sits behind another trusted proxy or load balancer. Otherwise a client could fake the header to dodge rate limits and bans, which is why `connection` is the default.

## 8. Bans are state, not config

Which IPs are banned right now is **not** part of this document. Bans change constantly (an auto-block can happen at any moment), and they have their own lifecycle, so they are kept in the database and loaded by the proxy into memory.

What *is* config is the **policy** that creates automatic bans:

### Auto-block rules

| Field | Type | Default | Description |
|---|---|---|---|
| `auto_block.enabled` | boolean | `true` | Master switch. |
| `auto_block.rules[]` | object[] | see below | Evaluated independently of each other. |
| `rules[].on` | `"rate_limit_exceeded"` or `"malicious_request"` | | The violation this rule counts. |
| `rules[].threshold` | integer | | Violations needed to trigger the ban. |
| `rules[].window_seconds` | integer | | The violations must happen within this time. |
| `rules[].ban_seconds` | integer or `null` | | How long the ban lasts. `null` means permanent until someone removes it manually. |

**Example reading:** `{ "on": "rate_limit_exceeded", "threshold": 10, "window_seconds": 60, "ban_seconds": 900 }` means *"an IP that hits the rate limit 10 times within 60 seconds is banned for 15 minutes."*

**Defaults** if the user enables auto-block without touching the rules: the two rules from the example above.

Every ban, whether automatic or manual, records who or what caused it, when, the reason and the expiry, and shows up in the logs and (for manual bans) the audit trail.

> How the proxy learns about new, changed or removed bans while running is documented in [`architecture.md`](architecture.md), since it is part of the data flow rather than the config schema.

## 9. Defaults for a new organisation

When an org is created, it starts with a safe, working config so the proxy is usable straight away:

```json
{
  "client_ip": { "source": "connection" },
  "trusted_ips": [],
  "routes": [],
  "rate_limit": { "enabled": true, "requests": 100, "per": "minute" },
  "detection": {
    "enabled": true,
    "action": "log",
    "inspect": ["path", "query", "headers", "body"],
    "max_body_bytes": 8192,
    "builtin": {
      "sql_injection": true,
      "path_traversal": true,
      "xss": true,
      "command_injection": true,
      "null_bytes": true
    },
    "custom_sequences": []
  },
  "auto_block": { "enabled": false, "rules": [] }
}
```

- With **no routes**, every request gets `404`. The user adds their first route to start proxying.
- Detection starts in **`log`** mode and auto-block starts **off**, so a new user can watch what would be caught before anything starts blocking real traffic.

## 10. Validation rules

The backend and the proxy enforce the same rules. A config that breaks any of them is rejected.

| Rule | Limit |
|---|---|
| Max routes | 50 |
| Max custom sequences | 100 |
| Max trusted IPs | 100 |
| Max auto-block rules | 10 |
| Route `id` | Unique, 1 to 64 characters, `[A-Za-z0-9_-]` |
| `path_prefix` | Starts with `/`, max 256 characters |
| `upstream.url` | Valid `http` or `https` URL, with a host |
| `upstream.timeout_ms` | 100 to 120000 |
| `rate_limit.requests` | 1 to 1,000,000 |
| `detection.max_body_bytes` | 0 to 65536 |
| Custom sequence `value` | 1 to 256 characters (`hex` must be an even number of valid hex digits) |
| Auto-block `threshold` | 1 to 10,000 |
| Auto-block `window_seconds` | 1 to 86,400 |
| Auto-block `ban_seconds` | 1 to 2,592,000 (30 days), or `null` |
| Unknown fields | Rejected, to catch typos early |

### Rejection format

When the proxy rejects a config, its ack carries every problem it found, each pointing to the field responsible, so the dashboard can highlight it:

```json
{
  "version": 7,
  "status": "rejected",
  "errors": [
    { "path": "/routes/1/upstream/url", "message": "must be a valid http or https URL" },
    { "path": "/detection/custom_sequences/0/value", "message": "hex value must have an even number of digits" }
  ]
}
```

`path` is a [JSON Pointer](https://datatracker.ietf.org/doc/html/rfc6901) into the config document.

## 11. Evolving the schema

- Adding an **optional field with a default** does not need a new `schema_version`. Old configs stay valid.
- Renaming, removing or changing the meaning of a field **bumps** `schema_version`. The backend migrates stored configs on upgrade, and a proxy that sees a `schema_version` newer than it understands rejects the config instead of guessing.

## 12. Not in the MVP

Deliberately left out to keep the project small and the config easy to understand:

- Regex-based detection rules
- Per-route detection settings
- Header rewriting, redirects and response modification
- TLS termination and custom certificates
- Load balancing across several upstreams
- Authentication or API-key checks at the proxy
- Country or ASN based blocking
