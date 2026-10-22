-- CI only: stand-ins for what Supabase provides.
create role anon nologin;
create role authenticated nologin;
create role service_role nologin;
create schema auth;
create table auth.users (id uuid primary key);
grant usage on schema public to anon, authenticated, service_role;
-- Supabase's permissive defaults, so the V1 revokes are actually exercised.
alter default privileges for role postgres in schema public
  grant all on tables to anon, authenticated, service_role;
alter default privileges for role postgres in schema public
  grant all on sequences to anon, authenticated, service_role;
alter default privileges for role postgres in schema public
  grant all on functions to anon, authenticated, service_role;