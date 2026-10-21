-- V1 baseline: runtime role, default privileges and the secure_table() helper.
-- No passwords live in this file. bevoya_app has no password until you set one out of band.
-- Runs as the admin role (postgres) through Flyway only. The app never runs migrations.

-- 1. Runtime role used by the Java API. Not a superuser, cannot bypass RLS.
do $$
begin
  if not exists (select 1 from pg_roles where rolname = 'bevoya_app') then
    create role bevoya_app login nosuperuser nocreatedb nocreaterole noinherit nobypassrls;
  end if;
end
$$;

grant usage on schema public to bevoya_app;

-- 2. Anything the migration role creates later is closed to Supabase's API roles by default.
--    The Data API is off, so this is a second layer behind it.
alter default privileges for role postgres in schema public
  revoke all on tables from anon, authenticated, service_role;
alter default privileges for role postgres in schema public
  revoke all on sequences from anon, authenticated, service_role;
alter default privileges for role postgres in schema public
  revoke all on functions from anon, authenticated, service_role;
alter default privileges for role postgres in schema public
  revoke execute on functions from public;

-- 3. secure_table(): call it once for every new table, in the same migration that creates it.
--    RLS on, API roles locked out, bevoya_app granted CRUD through one explicit policy.
--    Ownership checks (user_id = CurrentUser) stay in the Java layer and the composite foreign keys.
create or replace function public.secure_table(p_table regclass)
returns void
language plpgsql
set search_path = pg_catalog
as $$
begin
  execute format('alter table %s enable row level security', p_table);
  execute format('revoke all on table %s from public, anon, authenticated, service_role', p_table);
  execute format('grant select, insert, update, delete on table %s to bevoya_app', p_table);
  execute format('drop policy if exists bevoya_app_all on %s', p_table);
  execute format(
    'create policy bevoya_app_all on %s for all to bevoya_app using (true) with check (true)',
    p_table);
end
$$;

revoke all on function public.secure_table(regclass)
  from public, anon, authenticated, service_role;