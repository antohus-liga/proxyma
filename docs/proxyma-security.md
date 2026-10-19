# Security

> Status: design stage. This describes the intended security design. Nothing here is implemented yet, and items are marked **default** where the choice is a sensible starting point rather than a settled decision.

## 1. Scope and goals

Proxyma is a small-team tool, hosted locally, and **not** a production-scale or hardened product. The goal is to be honest and sensible: get the fundamentals right, isolate organisations from each other, and document the risks that remain.

**What we protect**

| Asset | Why it matters |
|---|---|
| Accounts and credentials | Access to an org means control over its traffic |
| Proxy configuration | Changing it changes how an org's traffic is routed and filtered |
| Request logs | Contain client IPs and request paths |
| Audit trail | Must be trustworthy to answer "who did this?" |
| The host machine | The backend can create containers, which is powerful |
| Admin access | Gives a view of every user and org |

**What we assume**

- The deployment is local or private, not exposed to the public internet.
- The host, Docker, PostgreSQL, ClickHouse and NATS are run by the person operating Proxyma and are trusted.
- Org members are semi-trusted: they can change config but not manage people.

**Out of scope for the MVP**

- Protection against a compromised host or Docker daemon
- DDoS resilience
- A full WAF, TLS termination, compliance (GDPR tooling, SOC 2, and so on)

## 2. Trust boundaries

```mermaid
flowchart LR
    subgraph Untrusted
        INT[Internet clients<br/>sending traffic]
        BR[User browsers]
    end

    subgraph Proxy[Per-org proxy containers]
        PX[Proxy<br/>handles hostile input]
    end

    subgraph Core[Trusted core]
        BE[Backend]
        N{{NATS}}
        PG[(PostgreSQL)]
        CH[(ClickHouse)]
        D[Docker daemon]
    end

    UP[Upstream services]

    INT -- hostile traffic --> PX
    PX -- forwards --> UP
    BR -- JWT-authenticated --> BE
    PX <-- scoped credentials --> N
    N <--> BE
    BE --> PG
    BE --> CH
    BE -- docker socket --> D
```

Two boundaries matter most:

1. **Internet to proxy.** Everything arriving here is hostile by default. The proxy is the part of the system that parses attacker-controlled bytes.
2. **Proxy to the core.** A proxy is the most exposed component, so we assume it **can be compromised** and make sure that doesn't give access to anything beyond its own org's NATS subjects.

Everything a proxy sends to the backend (logs, acks, ban events, heartbeats) is treated as **untrusted input** and validated.

## 3. Authentication

| Topic | Design |
|---|---|
| Passwords | Hashed with **Argon2id** (Spring Security supports it). Never stored or logged in plain text. |
| Minimum password rules | At least 10 characters, checked against a short list of common passwords. No composition rules. |
| Tokens | JWT. A short-lived **access token** (about 15 minutes) and a longer-lived **refresh token** (about 7 days), stored hashed server-side so it can be revoked. |
| Where tokens live in the browser | **Default:** access token in memory only, refresh token in an `HttpOnly`, `Secure`, `SameSite=Strict` cookie. This keeps tokens out of reach of injected scripts. |
| Logout | Revokes the refresh token on the server. |
| JWT signing | Strong secret or key loaded from the environment, never committed. Tokens carry only user ID and expiry. Roles are **not** put in the token, they're looked up on each request. |
| Login protection | Rate limiting on login, signup and invite endpoints, per IP and per account. Generic error messages so the response doesn't reveal whether an email exists. |

### SSE and authentication

The browser's `EventSource` can't send an `Authorization` header, and putting the main access token in a URL would leak it into logs. So:

1. The dashboard calls `POST /orgs/{id}/stream/ticket` with its normal authentication.
2. The backend checks the user's membership and returns a **single-use ticket** valid for about 30 seconds.
3. The dashboard opens `GET /orgs/{id}/stream?ticket=...`.
4. The backend consumes the ticket, binds the stream to that user and org, and ends the stream when the user's session is revoked or their membership is removed.

## 4. Authorization

Authorization is enforced **on the server, on every request**, never in the dashboard. The dashboard hides buttons for convenience only.

### Ownership and membership

- A user can **own at most one** organisation, but can be a **member of many**, by accepting invitations.
- Roles belong to the **membership**, not the account. The same person can be an owner in one org, a co-owner in another and a member in a third.
- Every request is checked against the user's membership in the org it targets, so permissions in one org never carry over to another.
- The dashboard needs an org switcher, and org IDs appear in every org-scoped URL.

### Role matrix

| Action | Member | Co-owner | Owner |
|---|---|---|---|
| View proxy config | ✅ | ✅ | ✅ |
| Edit proxy config | ✅ | ✅ | ✅ |
| View live logs and history | ✅ | ✅ | ✅ |
| View audit trail | ✅ | ✅ | ✅ |
| Ban / unban IPs manually | ✅ | ✅ | ✅ |
| Invite / remove people | ❌ | ✅ | ✅ |
| Assign roles | ❌ | ✅ | ✅ |
| Delete the organisation | ❌ | ❌ | ✅ |

**Protections for role changes:**

- Nobody can remove or demote the **owner**.
- A co-owner can manage members, but **cannot remove or demote another co-owner**. Only the owner can.
- Users can't change their own role.
- Transferring ownership is not in the MVP.

### Org scoping

The most common multi-tenant bug is letting a user touch another org's data by changing an ID in a request. To prevent it:

- Every org-scoped endpoint resolves the user's **membership for that org first** and rejects the request if none exists. Returning `404` rather than `403` for orgs the user doesn't belong to avoids revealing which IDs exist.
- Queries to PostgreSQL always include `org_id`, and `org_id` for ClickHouse queries comes from the verified membership, **never** from a client-supplied filter.
- Automated tests check that user A cannot read or modify anything belonging to org B across every endpoint.

### Invitations

- Invite links contain a random, unguessable token, stored hashed.
- Tokens are **single-use** and expire (default: 7 days).
- Only the invited email address can accept, and only while logged in as that account.
- An invitation can be revoked by an owner or co-owner at any time.

## 5. Audit trail

The audit trail exists to answer "who did this?", so it must be trustworthy.

| Property | Design |
|---|---|
| What is recorded | Config changes (with the version), role changes, invites and removals, manual bans and unbans, and failed permission checks |
| Fields | Org, acting user, action, target, before/after summary, timestamp, source IP |
| Written when | In the **same database transaction** as the change itself, so a change can't happen without its record |
| Append-only | The application exposes no update or delete operation for audit rows, and the application's database role has no `UPDATE`/`DELETE` privilege on that table |
| Retention | Kept for the life of the org |
| Never recorded | Passwords, tokens, or other secrets |

**Honest limit:** anyone with direct database admin access can still alter the table. The trail protects against org users, not against the host operator.

Automatic actions, such as auto-bans, are recorded with the actor `system (rule: ...)` so they can be told apart from human ones.

## 6. Tenant isolation

Isolation is enforced in **layers**, so no single mistake exposes another org.

| Layer | Mechanism |
|---|---|
| **Compute** | One container per org, with limits (see section 7) |
| **Network** | Proxy containers share a Docker network that can reach NATS and the outside world, but **not** PostgreSQL, ClickHouse or the backend |
| **Messaging** | Per-org NATS credentials limited to `org.<id>.*` subjects (section 8) |
| **Bans** | Proxies get read-only access to their own org's keys in the `BANS` bucket |
| **Application data** | Every query is scoped by `org_id` taken from the verified membership |
| **Logs** | ClickHouse queries always filter on `org_id`, and the table is ordered by it |

## 7. Proxy containers

### Hardening

Containers are created by the backend with these settings:

| Setting | Value |
|---|---|
| User | Non-root |
| Filesystem | Read-only root filesystem, small `tmpfs` for scratch |
| Capabilities | Drop all |
| Privilege escalation | `no-new-privileges` |
| Resources | Memory, CPU and process-count limits per container |
| Restart policy | `unless-stopped` |
| Labels | `proxyma.managed=true`, `proxyma.org=<id>`, used by reconciliation |
| Image | A fixed image name from backend configuration |

The backend **never** takes the image, command, environment, volumes or capabilities from user input. The only per-org values are the org ID and generated credentials, and a user cannot influence those. Reconciliation only touches containers carrying the `proxyma.managed` label.

### Exposure (reaching a proxy)

Each proxy needs a host port so traffic can reach it. The backend allocates one port per org from a configurable range (for example 20000 to 20999) and shows the resulting URL in the dashboard.

**Default:** the port is bound to `127.0.0.1`, so a proxy is reachable from the same machine only. To make proxies reachable from the local network, the operator sets the bind address to `0.0.0.0` or a specific LAN address. That is a deliberate operator choice, not something an org can change.

### Upstreams: reaching localhost and the local network

Proxyma is local-first, so the common case is a proxy forwarding to a service on the **same machine** (`localhost:3000`) or on the **local network** (`192.168.1.20:8080`). Both need to work.

| Upstream | How it works |
|---|---|
| **LAN or other private address** | Works through the container's normal outbound network access. |
| **`localhost`, `127.0.0.1`, `::1`** | Inside a container, `localhost` means the container itself, which is never what the user wants. The backend starts each proxy with `host.docker.internal` mapped to the host (`--add-host host.docker.internal:host-gateway`), and the proxy treats these three hosts as **"this machine"**, rewriting them to that name. The dashboard shows a hint next to localhost upstreams explaining this. |
| **Public hostnames** | Normal outbound access. |

**Caveat to document for users (Linux):** a service on the host that listens only on `127.0.0.1` is **not** reachable from a container. It has to listen on `0.0.0.0` or on the Docker bridge address. Docker Desktop on macOS and Windows doesn't have this restriction.

Because proxies can reach the host and the LAN **by design**, the guard rails are different from a strict "no private ranges" rule:

| Rule | Where it's enforced |
|---|---|
| **Always denied:** link-local addresses (`169.254.0.0/16`, `fe80::/10`, which covers cloud metadata endpoints), the unspecified address (`0.0.0.0`) and multicast | Backend at validation time, proxy at connection time |
| **Always denied:** Proxyma's own infrastructure, meaning the backend, NATS, PostgreSQL, ClickHouse and the Docker API, as `address:port` pairs. The backend builds this list from its own settings and gives it to each proxy as an environment variable at spawn time, so no org can change it | Backend at validation time, proxy at connection time |
| **Denied:** a route that points at the proxy's own listener (forwarding loop) | Backend at validation time |
| **Rejected:** upstream URLs containing credentials (`https://user:pass@host`) | Backend and proxy at validation time |
| **Optional strict mode:** the operator sets `PROXYMA_ALLOW_PRIVATE_UPSTREAMS=false` to also deny loopback, private and host addresses | Backend at validation time, proxy at connection time |

- The proxy checks the **resolved address at connection time**, not only when the config is saved, to resist DNS rebinding.
- Proxy containers sit on their own Docker network that **can't reach** the backend, PostgreSQL or ClickHouse by name. The deny list above covers the remaining path, which is through the host.
- In `docker-compose.yml`, infrastructure ports should be bound to `127.0.0.1` or not published at all. Services bound only to loopback on the host generally can't be reached from containers anyway, which is a useful second layer.
- `PROXYMA_ALLOW_PRIVATE_UPSTREAMS` defaults to `true` for local installs. Anyone running Proxyma on a shared server should set it to `false`.

## 8. NATS security

### Credentials

**Default:** NATS **decentralised JWT authentication**.

- The backend holds an account signing key and issues a **user JWT per org** on org creation, with the exact permissions listed in [`architecture.md`](architecture.md#8-nats-subject-layout).
- No NATS server restart or config edit is needed to add or remove an org.
- Revoking an org's credentials on deletion is done by adding the user to the account's revocation list.

Alternatives considered:

| Option | Why not |
|---|---|
| Static config file per org | Needs a config reload on every org change |
| Auth callout (backend decides on each connect) | A proxy couldn't reconnect while the backend is down, which defeats the point of the control/data plane split |
| One NATS account per org | Strongest isolation, but sharing streams and the KV bucket across accounts needs exports and imports, which adds a lot of complexity for this scope |

### Details that are easy to miss

- **Reply inboxes.** By default, NATS clients use a shared `_INBOX.>` prefix, which would let one proxy listen to another's replies. Each proxy is configured with a custom inbox prefix, `_INBOX.<org_id>`, and its permissions only allow that prefix.
- **JetStream API access.** A proxy is only allowed the specific JetStream API subjects it needs, for its own consumers and its own KV keys, not the whole `$JS.API.>` tree.
- **Limits.** Per-connection limits on payload size and subscription count.
- **Transport.** TLS between NATS and clients is a recommended setting for any non-local use. It's off by default for a local install.

### Delivering credentials to the container

**Recommended:** the backend creates the container, copies the credentials into it as a file on a `tmpfs` mount, then starts it. **Acceptable for the MVP:** pass them as an environment variable. Be aware that anyone with Docker access can see environment variables through `docker inspect`.

### Treating proxy messages as untrusted

If one proxy's credentials leak, the attacker can send messages **as that org's proxy**. The backend therefore:

- Validates every message's shape and size and drops anything malformed.
- Checks that a ban event contains a valid IP, respects `trusted_ips`, and stays under the per-org cap.
- Never trusts an `org_id` inside a message. The org always comes from the **subject** the message arrived on.
- Parameterises every ClickHouse insert.

Worst case for a leaked credential is that an attacker can inject fake logs and ban events for **that one org**, and read that org's config.

## 9. Logs contain attacker-controlled data

Request paths, user agents and similar fields come straight from hostile clients, so the logs will contain whatever an attacker chooses to send.

- The dashboard renders log fields as **plain text only**. It never uses `v-html` or injects log content into HTML. Vue escapes by default, and the rule is to keep it that way.
- Log fields have maximum lengths enforced by the proxy and again by the backend.
- The proxy **does not log request bodies or header values**.
- **Default:** the proxy logs the **path without the query string**, since query strings often carry tokens and keys. The `rule` field says which rule matched, so the payload doesn't need to be logged.
- IP addresses are personal data under some laws. The 30-day TTL limits how long they're kept, and the doc on operating Proxyma should mention it.

## 10. The Docker socket

The backend needs the Docker API to create and remove containers. **Access to the Docker socket is effectively root on the host.** If the backend is compromised, so is the machine. This is the biggest single risk in the design, and we state it plainly.

Mitigations, from simplest to strongest:

| Level | Measure |
|---|---|
| Always | The backend only calls a small set of operations (create, start, stop, remove, inspect, list) with fixed, server-built parameters. No user input reaches container creation. |
| Recommended | Run Docker in **rootless mode**, so the socket doesn't grant host root. |
| Recommended | Put a **socket proxy** (such as `tecnativa/docker-socket-proxy`) between the backend and Docker so only the required API endpoints are allowed. |
| Operational | Don't expose the backend to untrusted networks. |

For local development the plain socket is fine. For anything shared, use rootless Docker or a socket proxy.

## 11. The proxy itself (hostile input)

The proxy parses attacker-controlled bytes, which is why it's written in Rust and built on a mature HTTP library rather than custom parsing.

| Risk | Mitigation |
|---|---|
| Slow or oversized requests | Timeouts for headers, body and idle connections. Limits on header size, header count and request-line length. |
| Memory exhaustion by IP rotation | The rate-limit counter table and the ban set are **bounded**. Stale entries are evicted, and the active-ban cap (10,000 per org) applies. |
| Request smuggling and malformed requests | Handled by the HTTP library. Hop-by-hop headers are stripped before forwarding. Ambiguous requests are rejected. |
| Header spoofing | `X-Forwarded-For` is only trusted if the org sets `client_ip.source` to `x_forwarded_for`. The default uses the connection's address. |
| Collateral bans | Users behind a shared IP (an office or NAT) share a fate. `trusted_ips` and ban expiry reduce the damage, and the dashboard makes unbanning easy. |
| Regex denial of service | Detection uses literal matching only, so there are no user-supplied regexes to exploit. |
| Config abuse | Validation limits on routes, sequences and sizes (see [`proxy-config.md`](proxy-config.md#10-validation-rules)). |

### What detection does and doesn't do

The malicious byte-sequence detection catches obvious patterns. It **can** be bypassed by encodings it doesn't handle, by payloads split across fields, or by anything beyond the scanned body size. It should be treated as a low-cost speed bump and a source of useful log data, not as a security boundary.

## 12. Backend and dashboard hardening

- **Input validation** on every endpoint, with schema validation for the proxy config.
- **Parameterised queries only**, no string-built SQL.
- **CORS** restricted to the dashboard's origin.
- **CSRF**: because the refresh cookie is `SameSite=Strict`, and state-changing endpoints require the in-memory access token in a header, cross-site requests can't act on a user's behalf.
- **Security headers** on the dashboard: a strict Content-Security-Policy, `X-Content-Type-Options`, `Referrer-Policy` and clickjacking protection.
- **Secrets** (JWT key, NATS signing key, database passwords) come from environment variables or files and are never committed. A `.env.example` documents them without values.
- **Errors** returned to clients don't include stack traces or internal details.
- **Dependencies**: Dependabot (or similar) enabled, `cargo audit` for Rust, and a dependency check for Gradle and Bun, run in CI.

## 13. Known risks we accept

| Risk | Why we accept it |
|---|---|
| Docker socket gives host root | Needed to spawn containers. Mitigated by recommendations in section 10. |
| Proxies can reach the host and the local network | That is the product's local-first use case. Infrastructure ports are on a deny list, and a strict mode exists for shared servers. |
| Isolation relies on one NATS account with per-user permissions | Simpler than per-org accounts. Covered by tests that try to cross org boundaries. |
| A restart can lose a very recent ban before the backend persists it | Explained in [`architecture.md`](architecture.md). |
| Detection is easy to bypass | It's basic by design. |
| Audit trail can be changed by a database administrator | Protects against org users only. |
| No TLS by default between components | Local deployment. Recommended for anything else. |
| No protection against DDoS | Out of scope for the project. |
| Logs can be lost if NATS is unreachable for long | Logs are best-effort at this scope. |
| Invitations match on an unverified email address | Email verification isn't in the MVP, so someone who registers with an invited address can accept it. The invite link is also only as private as the channel it is shared through. |
| A compromised admin account exposes user and org metadata and allows suspending accounts | Metadata only, short sessions, step-up checks, optional network restriction. 2FA planned. |

## 14. Platform administration

Proxyma has an optional admin panel for the person operating the installation.
It exists for support and abuse handling: seeing who has accounts, which orgs
exist and whether their proxies are healthy, and suspending an account that is
misbehaving.

The operator can already read the database, so the panel doesn't create new
power. What it creates is a **web-reachable path** to that power, which is why
it is narrow, off by default, and fully recorded.

### Rules

| Rule | Design |
|---|---|
| **Off by default** | `PROXYMA_ADMIN_ENABLED=false` unless the operator sets it. While disabled, every admin route returns `404`. |
| **Optional network restriction** | `PROXYMA_ADMIN_ALLOWED_CIDRS` limits admin routes to given networks, for example `127.0.0.1/32`. |
| **Separate authorization** | A platform flag (`is_platform_admin`) on the user, checked in the database on every request, not stored in the JWT. It is independent of org roles, and admins are not automatically members of any org. |
| **No way to become admin through the product** | The flag is set and cleared only with an operator command on the server (`grant-admin <email>` and `revoke-admin <email>`). There is no API endpoint for it, and it is **not** assigned based on an email given at signup. Email isn't verified in the MVP, so granting admin by registering a particular address would be unsafe. |
| **Own namespace, deny by default** | Everything lives under `/api/v1/admin/...` behind its own authorization filter. Non-admins get `404`, so the panel's existence isn't revealed. |
| **Metadata only** | Admins see accounts and org structure. They never see password hashes, tokens, config contents, request logs, ban IPs or an org's audit trail. |
| **Everything is recorded** | Every admin action, and every view of an individual user or org, is written to the `platform_audit` table in the same transaction. |
| **Visible to the people affected** | When an admin action affects an org (for example suspending one of its members), an entry appears in that org's own audit trail with the actor shown as "Platform administrator". |
| **Step-up for risky actions** | Suspending a user or forcing a logout needs the admin's **password again** plus a written **reason**. Unsuspending needs a reason. |
| **Shorter sessions** | Admin accounts get a shorter access-token lifetime (for example 5 minutes) than normal users. |
| **Guard rails** | An admin can't suspend themselves, and can't suspend another admin. An admin's privileges have to be revoked with the operator command first. |
| **No impersonation** | There is no "log in as this user" feature. |

### What an admin can see

| Visible | Never visible |
|---|---|
| Email, display name, creation date, last login, status | Password hashes, access or refresh tokens |
| Which org a user owns and which orgs they belong to, with roles | Config contents and config history |
| Org name, owner, members and roles, state, running config version | Request logs and log statistics |
| Active ban **count** per org | Banned IP addresses |
| Whether a proxy is online and when it last reported | An org's own audit trail |
| Platform totals | NATS credentials, internal addresses of other components |

### What suspending does

- The user can't log in or refresh, and their current access token stops working on the next request, because status is checked on every request.
- Their open live streams are closed and their refresh tokens revoked.
- **Orgs keep running.** A suspended owner's org and proxy are not stopped, and its other members and co-owners continue as normal. Owner-only actions, such as deleting the org, wait until the account is unsuspended.
- Each affected org gets an audit entry saying the member was suspended by a platform administrator.

### Request flow

~~~mermaid
flowchart TD
    A[Request to /api/v1/admin/...] --> B{Admin panel<br/>enabled?}
    B -- no --> X[404]
    B -- yes --> C{Source IP in<br/>allowed networks?}
    C -- no --> X
    C -- yes --> D{Valid JWT?}
    D -- no --> Y[401]
    D -- yes --> E{User exists, not suspended,<br/>is_platform_admin?}
    E -- no --> X
    E -- yes --> F{Risky action?}
    F -- yes --> G{Password + reason<br/>correct?}
    G -- no --> Z[403 REAUTH_FAILED]
    G -- yes --> H[Do the action]
    F -- no --> H
    H --> I[Write platform_audit<br/>and org audit entries<br/>in the same transaction]
    I --> J[Respond]
~~~

### Residual risk

A stolen admin account would expose user and org **metadata** and let the attacker suspend ordinary accounts. It would not expose passwords, config, logs or traffic. Mitigations: short sessions, step-up password checks, the network restriction, and the audit trail. Two-factor authentication for admins is a planned improvement.

## 15. Before exposing Proxyma beyond a local machine

If anyone ever wants to run this on a shared or public server, at a minimum:

- [ ] Put TLS in front of the dashboard and backend, and enable TLS on NATS
- [ ] Use rootless Docker or a Docker socket proxy
- [ ] Set `PROXYMA_ALLOW_PRIVATE_UPSTREAMS=false` unless orgs genuinely need to reach internal services
- [ ] Keep the proxy bind address, database ports and NATS port off public interfaces
- [ ] Rotate all default secrets and keep them out of the environment of other containers
- [ ] Set resource limits on the host and enable log rotation
- [ ] Run `cargo audit` and dependency checks, and update base images
- [ ] Review the "Known risks" table above and decide which are still acceptable
- [ ] Keep `PROXYMA_ADMIN_ENABLED=false` unless needed, and restrict it with `PROXYMA_ADMIN_ALLOWED_CIDRS`
