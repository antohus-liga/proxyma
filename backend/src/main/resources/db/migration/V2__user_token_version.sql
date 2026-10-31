-- Access tokens are stateless, so a password change or an admin force-logout
-- could not retire them before their 15-minute expiry. Every token now carries
-- a `tv` claim which is compared against this column on each request; bumping
-- the column invalidates all tokens issued before the bump.
ALTER TABLE users ADD COLUMN token_version integer NOT NULL DEFAULT 0;
