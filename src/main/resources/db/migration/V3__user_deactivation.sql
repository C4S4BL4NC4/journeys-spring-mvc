-- Deactivating a user (admin delete or deleteMe) disables them in Keycloak and stamps this
-- column. Profiles with a value are hidden from the API, and tokens issued before it are
-- rejected; a token issued after it means Keycloak re-enabled the account, which clears it.
alter table users add column deactivated_at timestamp(6) with time zone;
