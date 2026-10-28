-- Runs once, when the postgres container initialises an empty data volume.
-- Keycloak gets its own database and login, separate from the API's "journeys" database
-- (created from POSTGRES_DB / POSTGRES_USER in docker-compose.yml), so neither can touch
-- the other's tables. Dev-only credentials.
create role keycloak with login password 'keycloak';
create database keycloak owner keycloak;
