-- Initial schema for Proxyma.
--
-- PostgreSQL is the source of truth for accounts, organisations, proxy
-- configuration and its delivery state, IP bans and the audit trail.
-- Request logs live in ClickHouse and are intentionally absent here.
--
-- Conventions:
--   * snake_case, plural table names
--   * UUID primary keys (gen_random_uuid, core since PostgreSQL 13)
--   * timestamptz everywhere; the application treats them as UTC
--   * mutable enum-like columns are text + CHECK, not PostgreSQL enums,
--     so adding a value is a plain constraint change
--   * updated_at is maintained by the application, not a trigger

CREATE EXTENSION IF NOT EXISTS citext;

-- ---------------------------------------------------------------------------
-- Accounts
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 citext NOT NULL UNIQUE,
    password_hash         text NOT NULL,
    display_name          text NOT NULL CHECK (char_length(display_name) BETWEEN 1 AND 80),
    is_platform_admin     boolean NOT NULL DEFAULT false,
    status                text NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended')),
    suspended_at          timestamptz,
    suspended_by_user_id  uuid REFERENCES users (id) ON DELETE SET NULL,
    suspension_reason     text,
    last_login_at         timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now(),
    CHECK (status = 'active' OR suspended_at IS NOT NULL)
);

-- Refresh tokens are delivered to the browser as an HttpOnly cookie and
-- recorded here (hashed) so they can be rotated and revoked. The raw token
-- is never stored.
CREATE TABLE refresh_tokens (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash           text NOT NULL UNIQUE,
    family_id            uuid NOT NULL,
    replaced_by_token_id uuid REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    user_agent           text,
    ip                   inet,
    created_at           timestamptz NOT NULL DEFAULT now(),
    expires_at           timestamptz NOT NULL,
    revoked_at           timestamptz
);

CREATE INDEX refresh_tokens_user_active_idx ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
CREATE INDEX refresh_tokens_family_idx ON refresh_tokens (family_id);
CREATE INDEX refresh_tokens_expiry_idx ON refresh_tokens (expires_at);

-- ---------------------------------------------------------------------------
-- Organisations and membership
-- ---------------------------------------------------------------------------

CREATE TABLE organisations (
    id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 64),
    state                  text NOT NULL DEFAULT 'CREATING'
                             CHECK (state IN ('CREATING', 'STARTING', 'ONLINE', 'OFFLINE', 'DELETING')),
    container_id           text,
    container_name         text,
    proxy_port             integer CHECK (proxy_port BETWEEN 1 AND 65535),
    last_heartbeat_at      timestamptz,
    running_config_version integer,
    created_at             timestamptz NOT NULL DEFAULT now(),
    updated_at             timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX organisations_proxy_port_idx ON organisations (proxy_port) WHERE proxy_port IS NOT NULL;
CREATE INDEX organisations_state_idx ON organisations (state);

CREATE TABLE org_memberships (
    org_id    uuid NOT NULL REFERENCES organisations (id) ON DELETE CASCADE,
    user_id   uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role      text NOT NULL CHECK (role IN ('owner', 'co_owner', 'member')),
    joined_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (org_id, user_id)
);

-- A user can own at most one organisation. "Exactly one owner per org" is
-- enforced by the application inside the org-creation transaction.
CREATE UNIQUE INDEX org_memberships_one_owned_org_idx ON org_memberships (user_id) WHERE role = 'owner';
CREATE INDEX org_memberships_user_idx ON org_memberships (user_id);

-- ---------------------------------------------------------------------------
-- Proxy configuration
-- ---------------------------------------------------------------------------

-- Every saved config version. `document` is the inner config; the delivery
-- envelope (version, schema_version) is represented by columns.
CREATE TABLE org_configs (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id             uuid NOT NULL REFERENCES organisations (id) ON DELETE CASCADE,
    version            integer NOT NULL CHECK (version >= 1),
    schema_version     integer NOT NULL DEFAULT 1 CHECK (schema_version >= 1),
    document           jsonb NOT NULL,
    content_hash       text NOT NULL,
    source             text NOT NULL DEFAULT 'user' CHECK (source IN ('default', 'user', 'restore')),
    created_by_user_id uuid REFERENCES users (id) ON DELETE SET NULL,
    created_at         timestamptz NOT NULL DEFAULT now(),
    rejected_at        timestamptz,
    rejection          jsonb,
    UNIQUE (org_id, version)
);

CREATE INDEX org_configs_org_created_idx ON org_configs (org_id, created_at DESC);

-- Delivery state for an org's config. The three version columns drive the
-- dashboard status and the republish job:
--   latest_version    newest saved version
--   published_version newest version handed to NATS
--   applied_version   newest version the proxy acknowledged
CREATE TABLE org_config_state (
    org_id            uuid PRIMARY KEY REFERENCES organisations (id) ON DELETE CASCADE,
    latest_version    integer NOT NULL,
    published_version integer,
    applied_version   integer,
    updated_at        timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (org_id, latest_version) REFERENCES org_configs (org_id, version),
    FOREIGN KEY (org_id, published_version) REFERENCES org_configs (org_id, version),
    FOREIGN KEY (org_id, applied_version) REFERENCES org_configs (org_id, version),
    CHECK (published_version IS NULL OR published_version <= latest_version),
    CHECK (applied_version IS NULL OR (published_version IS NOT NULL AND applied_version <= published_version))
);

-- ---------------------------------------------------------------------------
-- Bans
-- ---------------------------------------------------------------------------

CREATE TABLE bans (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id             uuid NOT NULL REFERENCES organisations (id) ON DELETE CASCADE,
    ip                 inet NOT NULL,
    source             text NOT NULL CHECK (source IN ('auto', 'manual')),
    reason             text NOT NULL,
    rule               text,
    created_by_user_id uuid REFERENCES users (id) ON DELETE SET NULL,
    created_at         timestamptz NOT NULL DEFAULT now(),
    expires_at         timestamptz,
    removed_at         timestamptz,
    removed_by_user_id uuid REFERENCES users (id) ON DELETE SET NULL,
    CHECK (expires_at IS NULL OR expires_at > created_at)
);

-- One live ban per (org, ip). Rows that have expired but have not been swept
-- yet still occupy this index, so the ban path must mark expired rows as
-- removed before inserting a replacement.
CREATE UNIQUE INDEX bans_org_ip_active_idx ON bans (org_id, ip) WHERE removed_at IS NULL;
CREATE INDEX bans_org_expiry_idx ON bans (org_id, expires_at) WHERE removed_at IS NULL;
CREATE INDEX bans_org_created_idx ON bans (org_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- Audit trail
-- ---------------------------------------------------------------------------

CREATE TABLE org_audit_entries (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id        uuid NOT NULL REFERENCES organisations (id) ON DELETE CASCADE,
    actor_type    text NOT NULL CHECK (actor_type IN ('user', 'system', 'platform_admin')),
    actor_user_id uuid REFERENCES users (id) ON DELETE SET NULL,
    action        text NOT NULL,
    target_type   text,
    target_id     text,
    summary       text NOT NULL,
    changes       jsonb,
    source_ip     inet,
    created_at    timestamptz NOT NULL DEFAULT now(),
    CHECK (actor_type <> 'user' OR actor_user_id IS NOT NULL)
);

CREATE INDEX org_audit_org_time_idx ON org_audit_entries (org_id, created_at DESC, id);
