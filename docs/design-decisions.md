# Design decisions

> Status: design stage. Each entry records a choice, the reasons, and what it costs, so future-us (and anyone reading the project) can see *why* things are the way they are and change them knowingly.

## How to read this

Every decision follows the same short format:

- **Context**: the problem or question
- **Decision**: what we chose
- **Alternatives**: what else we looked at, and why not
- **Consequences**: what we gain and what we accept

## Index

| # | Decision | Area |
|---|---|---|
| [1](#1-separate-control-plane-and-data-plane) | Separate control plane and data plane | Architecture |
| [2](#2-one-container-per-organisation) | One container per organisation | Architecture |
| [3](#3-nats-jetstream-as-the-only-channel-between-backend-and-proxy) | NATS JetStream as the only channel to proxies | Messaging |
| [4](#4-postgresql-for-state-clickhouse-for-logs) | PostgreSQL for state, ClickHouse for logs | Data |
| [5](#5-server-sent-events-for-the-live-log-view) | Server-Sent Events for the live log view | Logs |
| [6](#6-live-logs-from-nats-history-from-clickhouse) | Live logs from NATS, history from ClickHouse | Logs |
| [7](#7-batched-inserts-into-clickhouse) | Batched inserts into ClickHouse | Logs |
| [8](#8-postgres-first-then-nats-with-config-versions) | Postgres first, then NATS, with config versions | Config |
| [9](#9-the-proxy-acknowledges-every-config) | The proxy acknowledges every config | Config |
| [10](#10-a-starting-proxy-fetches-the-latest-config-from-jetstream) | A starting proxy fetches the latest config from JetStream | Config |
| [11](#11-per-org-subject-prefix-with-scoped-jwt-credentials) | Per-org subject prefix with scoped JWT credentials | Security |
| [12](#12-bans-live-in-postgresql-and-reach-the-proxy-through-nats) | Bans live in PostgreSQL and reach the proxy through NATS | Bans |
| [13](#13-docker-java-over-the-local-docker-socket) | docker-java over the local Docker socket | Lifecycle |
| [14](#14-reconciliation-loop-and-heartbeats) | Reconciliation loop and heartbeats | Lifecycle |
| [15](#15-jwt-with-an-in-memory-access-token-and-an-httponly-refresh-cookie) | JWT with an in-memory access token and an HttpOnly refresh cookie | Auth |
| [16](#16-owner-co-owner-member-with-one-owned-org-per-user) | Owner, co-owner, member, with one owned org per user | Auth |
| [17](#17-literal-matching-for-malicious-sequence-detection) | Literal matching for malicious-sequence detection | Proxy |
| [18](#18-local-first-upstream-policy) | Local-first upstream policy | Proxy |
| [19](#19-json-config-with-a-schema-version) | JSON config with a schema version | Config |
| [20](#20-30-day-log-retention) | 30-day log retention | Logs |
| [21](#21-monorepo-and-mit-license) | Monorepo and MIT license | Project |
| [22](#22-hot-config-reload-with-arcswap) | Hot config reload with ArcSwap | Proxy |
| [23](#23-a-metadata-only-off-by-default-admin-panel) | Metadata-only admin panel | Admin |

---

## 1. Separate control plane and data plane

**Context.** The system both manages things (users, orgs, config) and moves traffic. Mixing the two would make the proxy depend on the backend being up.

**Decision.** The backend, dashboard and databases form the **control plane**. Proxies form the **data plane**. They communicate only through NATS, and a running proxy keeps serving traffic with its last config even if everything else is down.

**Alternatives.** A proxy that calls the backend's REST API for config and to push logs. Simpler at first, but it couples the proxy's availability to the backend and needs the backend to be reachable from every proxy.

**Consequences.** Resilience and a clean story (the same shape as Envoy with its control plane or Traefik with its providers). The cost is more moving parts: NATS has to exist and the config has to be versioned and acknowledged.

## 2. One container per organisation

**Context.** Orgs must not affect each other, and users should never have to set anything up.

**Decision.** Creating an org makes the backend start a dedicated proxy container. The container is the isolation unit.

**Alternatives.** A single multi-tenant proxy process serving every org. Cheaper in resources, but a bug, a crash or a heavy org would affect everyone, and per-org resource limits would be hard.

**Consequences.** Strong, easy-to-explain isolation, plus per-org CPU and memory limits and independent restarts. The cost is resource use that grows with the number of orgs, and no horizontal scaling per org. Both are fine for the intended scale.

## 3. NATS JetStream as the only channel between backend and proxy

**Context.** Config must reach proxies, and logs and acknowledgements must come back. Proxies may restart or be briefly unreachable.

**Decision.** Use NATS with JetStream for config, acks, logs, heartbeats and ban events.

**Alternatives.**
- *REST or gRPC calls*: needs proxies to be addressable and doesn't give durable delivery.
- *Kafka*: far heavier to run, and overkill here.
- *RabbitMQ*: workable, but JetStream's last-message-per-subject and KV features fit "give a restarting proxy the latest state" directly.
- *Redis Streams or pub/sub*: workable but weaker on per-subject permissions.

**Consequences.** Durable, replayable delivery, simple per-org subject permissions, and a single small binary to run locally. The cost is one more service for users to start, which `docker-compose.yml` hides.

## 4. PostgreSQL for state, ClickHouse for logs

**Context.** The system stores two very different kinds of data: relational, transactional state (users, orgs, roles, config, audit) and an append-heavy stream of request logs.

**Decision.** PostgreSQL holds state. ClickHouse holds logs.

**Alternatives.** Everything in PostgreSQL. Simpler, but a busy proxy would produce a lot of log rows, and PostgreSQL is not built for fast aggregation over them. Logs in Elasticsearch or Loki were considered too, but ClickHouse's SQL and compression suit this well.

**Consequences.** Each store does what it's good at, and TTL-based retention is one line in ClickHouse. The cost is two databases to operate and no cross-database transactions, which is why the audit trail stays in PostgreSQL.

## 5. Server-Sent Events for the live log view

**Context.** The dashboard needs live logs. Data only flows from server to browser.

**Decision.** Use Server-Sent Events (SSE).

**Alternatives.** WebSockets. They are bidirectional, which we don't need, and they need their own handling for reconnects, heartbeats and proxies.

**Consequences.** SSE is plain HTTP, reconnects automatically, supports `Last-Event-ID`, and is cheaper to run. The cost is that browsers' `EventSource` can't send an `Authorization` header, so streams use a short-lived single-use ticket (see [`security.md`](security.md#sse-and-authentication)).

## 6. Live logs from NATS, history from ClickHouse

**Context.** The dashboard wants both an instant live view and searchable history. ClickHouse is best fed in batches, which adds delay.

**Decision.** The backend fans out live entries straight from NATS to connected browsers. ClickHouse is used only for history and search. After an SSE reconnect, the dashboard fills any gap by querying ClickHouse.

**Alternatives.** Feed the live view by polling ClickHouse. Simpler, but the view would lag behind by the batching delay and put steady query load on ClickHouse.

**Consequences.** A genuinely live view that works even if ClickHouse is down. The cost is two read paths to keep consistent.

## 7. Batched inserts into ClickHouse

**Context.** ClickHouse performs poorly with many tiny inserts.

**Decision.** The backend buffers log entries and inserts them in batches (about once a second, or every few thousand rows, whichever comes first).

**Consequences.** Good insert performance. Logs may be delivered at least once, so rare duplicates are possible, and `id` allows de-duplication when needed. If the backend crashes mid-batch, JetStream redelivers.

## 8. Postgres first, then NATS, with config versions

**Context.** Saving config means writing to PostgreSQL and publishing to NATS, and either step can fail without the other.

**Decision.** Save the config (and its audit entry) in one transaction, then publish. Each config has `version`, `published_version` and `applied_version`. A scheduled job republishes anything where `published_version < version`.

**Alternatives.** The transactional outbox pattern, which is the textbook answer. It's more machinery than this project needs, and versions give the same safety cheaply. Publishing first and saving after was rejected because a failed save would leave a proxy running a config nobody has a record of.

**Consequences.** No lost updates and a clear status in the dashboard (pending, applied, rejected). The cost is a small retry job and idempotent handling of repeated publishes, which the proxy gets by ignoring versions it already has.

## 9. The proxy acknowledges every config

**Context.** "Saved" and "applied" are different things. Without feedback, a user can't trust that their change is live.

**Decision.** After validating and applying a config, the proxy sends an ack (`applied` or `rejected` with a list of errors, each pointing to a field). A rejected config never replaces the last good one.

**Consequences.** The dashboard can show honest status and highlight the exact field that failed. The cost is an extra subject and an extra consumer in the backend.

## 10. A starting proxy fetches the latest config from JetStream

**Context.** Proxies start for the first time, restart after crashes and come back after being offline. Each must end up with the latest config.

**Decision.** The `CONFIG` stream keeps one message per subject. A starting proxy reads the last message on its own subject before serving traffic. The backend publishes an org's first config *before* starting its container.

**Consequences.** No separate "fetch config" API, no race on first boot, and a restarted proxy can never run stale or empty config. The proxy won't serve traffic until it has its config and its bans.

## 11. Per-org subject prefix with scoped JWT credentials

**Context.** One org's proxy must never read or write another org's messages.

**Decision.** Every org owns the subjects under `org.<id>.`. The backend issues each proxy a NATS user JWT with permissions limited to its own prefix, and each proxy uses its own inbox prefix.

**Alternatives.**
- *One NATS account per org*: the strongest isolation, but sharing streams and the KV bucket across accounts needs exports and imports, which is heavy for this scope.
- *Auth callout*: the backend would decide on every connection, so a proxy couldn't reconnect while the backend is down.
- *Static config files*: need a server reload for every org change.

**Consequences.** Adding and removing orgs needs no NATS restart. Isolation relies on permissions being correct, so it needs automated tests that try to cross org boundaries.

## 12. Bans live in PostgreSQL and reach the proxy through NATS

**Context.** Bans must survive a proxy restart, or an attacker can reset a ban by crashing the proxy. The proxy shouldn't connect to a database.

**Decision.** PostgreSQL is the source of truth. The backend is the only writer to a JetStream KV bucket (`BANS`), which proxies read and watch. The proxy keeps an in-memory set, rebuilt from KV on every start. Automatic bans are enforced immediately in memory and reported to the backend as events.

**Alternatives.**
- *The proxy reads PostgreSQL*: breaks the plane separation, and gives each proxy database credentials.
- *In-memory only*: the flaw this decision exists to fix.
- *The proxy writes KV itself*: closes a small timing window but creates two writers and two sources of truth.

**Consequences.** Durable bans, a proxy that never touches a database, and a reconciliation job that repairs drift (PostgreSQL always wins). One accepted gap: a proxy that restarts before the backend has processed a fresh automatic-ban event reloads without that ban until the backend catches up. See [`architecture.md`](architecture.md).

## 13. docker-java over the local Docker socket

**Context.** The backend must create, start and remove containers.

**Decision.** Use the `docker-java` library against the local Docker socket, with fixed, server-built container parameters.

**Alternatives.** A remote Docker daemon, Kubernetes, or Docker Compose files generated on the fly. All of them add setup for users, which cuts against the simplicity goal.

**Consequences.** The simplest working option. The cost is real: socket access is effectively root on the host. It is documented as an accepted risk, with rootless Docker or a socket proxy recommended for anything shared (see [`security.md`](security.md#10-the-docker-socket)).

## 14. Reconciliation loop and heartbeats

**Context.** Containers crash, the backend restarts, and database and Docker can disagree.

**Decision.** Proxies send a heartbeat every few seconds, and the backend marks an org offline when they stop. On startup and periodically, the backend compares PostgreSQL with running containers (and, for bans, with the KV bucket) and fixes mismatches. PostgreSQL wins every disagreement. Containers use a restart policy and carry labels so the loop only touches what Proxyma created.

**Consequences.** The system heals itself after crashes and half-finished operations, including interrupted org creation or deletion. The cost is that every multi-step operation has to be safe to repeat. A single backend instance is assumed, since several would need coordination.

## 15. JWT with an in-memory access token and an HttpOnly refresh cookie

**Context.** The dashboard needs authentication that is both simple and not trivially stolen by injected scripts.

**Decision.** A short-lived access token held in memory, plus a revocable refresh token in an `HttpOnly`, `Secure`, `SameSite=Strict` cookie. Roles are looked up per request, not stored in the token.

**Alternatives.** A long-lived JWT in `localStorage` is easier but is readable by any script on the page. Server-side sessions would also work, but JWT was a stated project choice.

**Consequences.** Better protection against token theft, immediate role changes, and revocation on logout. The cost is a refresh flow and a ticket mechanism for SSE.

## 16. Owner, co-owner, member, with one owned org per user

**Context.** Small teams need to share control without losing accountability.

**Decision.** A user can own at most one org but can be a member of many. Roles belong to the membership.

| | Member | Co-owner | Owner |
|---|---|---|---|
| View and edit config, view logs and audit, ban and unban IPs | ✅ | ✅ | ✅ |
| Invite, remove people, assign roles | ❌ | ✅ | ✅ |
| Delete the org | ❌ | ❌ | ✅ |

Nobody can remove or demote the owner, and only the owner can remove or demote a co-owner. Every action is recorded in an append-only audit trail.

**Consequences.** A simple model that answers "who did this?" and "who can undo it?". Ownership transfer is not in the MVP.

## 17. Literal matching for malicious-sequence detection

**Context.** The project is backend-focused, and the proxy should be small and predictable. The detection feature is deliberately basic.

**Decision.** Detection uses built-in pattern sets plus user-defined **literal** text or hex sequences. No user-supplied regular expressions.

**Alternatives.** Regex rules, as in a real WAF. More powerful, but they can be slow or even exploitable (regex denial of service) and are harder for small teams to write correctly.

**Consequences.** Predictable performance and simple rules. The cost is that detection is easy to bypass, so it is documented as a speed bump and a source of log data, not a security boundary.

## 18. Local-first upstream policy

**Context.** The main use case is proxying a service on the same machine or the same network, but a proxy that can reach "everything" could be misused to reach Proxyma's own infrastructure.

**Decision.** Private, loopback and LAN upstreams are allowed by default, and `localhost` is rewritten to mean the host machine. A fixed deny list always blocks link-local and cloud-metadata addresses and Proxyma's own services. A strict mode (`PROXYMA_ALLOW_PRIVATE_UPSTREAMS=false`) is available for shared servers. Addresses are checked again at connection time.

**Alternatives.** Denying private ranges by default is safer in the abstract but would break the product's own main use case.

**Consequences.** It works out of the box for local installs. The cost is that a user could point a route at other services on the host or LAN, which is stated as an accepted risk and mitigated by the deny list.

## 19. JSON config with a schema version

**Context.** The config is the contract between the dashboard, the backend and the proxy.

**Decision.** One JSON document per org, wrapped in an envelope with `version` and `schema_version`, validated by both the backend and the proxy. Unknown fields are rejected. Rejection errors use JSON Pointer paths.

**Alternatives.** YAML or TOML, which are nicer to hand-edit but the config is edited through a UI and sent over a message bus, where JSON is the natural fit.

**Consequences.** One format everywhere, precise error messages, and a defined way to evolve the schema. The cost is writing validation twice (Kotlin and Rust), mitigated by a shared set of test cases in the repository.

## 20. 30-day log retention

**Context.** Logs grow without bound, and they contain client IPs.

**Decision.** A ClickHouse TTL deletes logs after 30 days. Deleting an org makes its logs unreachable immediately and lets the TTL remove them.

**Consequences.** Bounded storage and a clear privacy story, with no cleanup job. The cost is no long-term history, which is appropriate for the scope.

## 21. Monorepo and MIT license

**Context.** Three components change together, and the project should be easy to adopt.

**Decision.** One repository with `proxy/`, `backend/`, `frontend/` and `docs/`. Licensed under MIT.

**Alternatives.** Separate repositories per component would add coordination overhead for a project this size. The license started out as CC0, but Creative Commons recommends against using its licenses for software, so MIT was chosen for the same "use it freely" spirit with a standard software license.

**Consequences.** One place for issues, docs and cross-component changes (such as a config schema update). The cost is that CI and tooling for three stacks live side by side.

## 22. Hot config reload with ArcSwap

**Context.** Restarting a proxy to apply a config would drop connections and
interrupt traffic every time someone edits a setting. Zero-downtime changes are
one of the product's main features.

**Decision.** The proxy keeps its active config in an `ArcSwap`. A new config is
validated and compiled first, then swapped in atomically. Each request takes one
snapshot when it starts and uses it for its whole life, so in-flight requests
finish on the old config while new requests use the new one. State that must
outlive a config (rate-limit counters, bans, connection pools) lives outside the
snapshot.

**Alternatives.**
- *Restart the proxy on every change*: simplest, but drops traffic and defeats
  the point of a managed proxy.
- *`RwLock<Config>`*: simple, but every request takes a lock, and a writer
  can stall readers while compiling or swapping.
- *Message passing to a single config-owning task*: avoids shared state, but
  puts a channel hop on the hot path of every request.

**Consequences.** Lock-free, cheap reads on the request path, no downtime, and a
clean meaning for `applied` (swapped in). The cost is that config must be
compiled into an immutable structure, long-lived state has to be kept outside
it, and old configs stay in memory until their last request finishes.

## 23. A metadata-only, off-by-default admin panel

**Context.** The operator needs to see who has accounts, which orgs exist, whether
proxies are healthy, and to be able to stop an abusive account. Without a panel,
this means writing SQL by hand. A panel, however, makes a powerful capability
reachable from the web.

**Decision.** Add an admin panel that is disabled by default, authorized by a
separate platform flag that can only be set from the server's command line,
limited to metadata (never config, logs, ban IPs or secrets), and recorded in its
own append-only audit trail. Risky actions need the admin's password again and a
reason, and admin actions that affect an org appear in that org's own audit trail.
Suspending an owner does not stop their org's proxy.

**Alternatives.**
- *No panel, use SQL*: safest, but poor for support, hard to demo, and invisible
  to anyone affected.
- *Admins can see everything*: more useful for debugging, but gives one account
  access to every org's traffic and configuration, which is the opposite of the
  isolation the rest of the design builds.
- *Admin as an org-level role*: mixes platform and tenant authority, and makes it
  easy to grant by mistake.
- *Impersonation ("log in as the user")*: convenient, but hard to audit and easy
  to abuse.
- *Stop the proxy when its owner is suspended*: simple to explain, but takes down
  an org's traffic and affects members who did nothing wrong.

**Consequences.** Operators get a useful, safe-by-default tool, and users can see
when platform staff touched their org. The cost is more code on the security
boundary: a second authorization path, a second audit table, and tests proving
that every admin endpoint rejects ordinary users. Two-factor authentication for
admins is deferred.
