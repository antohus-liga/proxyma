-- Platform administration audit trail.
--
-- This is deliberately separate from `org_audit_entries`: platform actions are
-- not part of any organisation's history, and admins must not be able to read
-- an org's own trail through the admin API.
--
-- The application exposes no update or delete operation for this table. The
-- database role should not have UPDATE/DELETE privileges on it either.
--
-- `admin_user_id` is null for actions taken with the server-side
-- `grant-admin`/`revoke-admin` commands, where the actor is the operator rather
-- than an authenticated platform admin.

CREATE TABLE platform_audit (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id uuid REFERENCES users (id) ON DELETE SET NULL,
    action        text NOT NULL,
    target_type   text,
    target_id     text,
    summary       text NOT NULL,
    source_ip     inet,
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX platform_audit_time_idx ON platform_audit (created_at DESC, id DESC);
CREATE INDEX platform_audit_admin_time_idx ON platform_audit (admin_user_id, created_at DESC);
CREATE INDEX platform_audit_action_time_idx ON platform_audit (action, created_at DESC);
