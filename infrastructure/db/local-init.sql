-- Local Postgres only. On Supabase you create this role by hand with a real password.
create schema if not exists extensions;
create role bevoya_app login password 'dev_only_change_me';
alter role bevoya_app set search_path = public, extensions;
