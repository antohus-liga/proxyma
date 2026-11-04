-- Refresh-token families now have an absolute deadline. Sliding expiry alone
-- let an actively used (or stolen) session renew forever: every rotation reset
-- the refresh TTL. `family_expires_at` is set once when the family is created
-- and copied unchanged by every rotation.
--
-- Existing families are capped at 30 days from their first token, matching the
-- default `proxyma.auth.session-ttl`. Operators with a different value still get
-- a sane one-off cap for sessions that predate this migration.
ALTER TABLE refresh_tokens ADD COLUMN family_expires_at timestamptz;

UPDATE refresh_tokens t
SET family_expires_at = families.started_at + interval '30 days'
FROM (
    SELECT family_id, min(created_at) AS started_at
    FROM refresh_tokens
    GROUP BY family_id
) AS families
WHERE t.family_id = families.family_id;

ALTER TABLE refresh_tokens ALTER COLUMN family_expires_at SET NOT NULL;
