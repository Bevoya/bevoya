-- Flyway callback: runs after every successful migrate, in its own transaction.
-- Flyway's history table lives in public. Lock it down with no policy at all,
-- so only the owner (the migration role) can read or write it.
-- Kept out of the versioned migrations on purpose: altering the history table
-- inside a migration can block Flyway's own write to that table.
do $$
begin
  if to_regclass('public.flyway_schema_history') is not null then
    alter table public.flyway_schema_history enable row level security;
    revoke all on table public.flyway_schema_history
      from public, anon, authenticated, service_role, bevoya_app;
  end if;
end
$$;