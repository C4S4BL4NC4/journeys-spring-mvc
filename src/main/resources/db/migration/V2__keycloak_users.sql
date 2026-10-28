-- Accounts moved to Keycloak: it now owns passwords, password resets and disabling accounts.
-- The users table only mirrors each user's profile, keyed by their Keycloak user id (the
-- token's "sub"), so these columns are no longer used.
drop index ix_users_password_reset_token;
alter table users drop column password;
alter table users drop column password_changed_at;
alter table users drop column password_reset_token;
alter table users drop column password_reset_expires;
alter table users drop column active;
