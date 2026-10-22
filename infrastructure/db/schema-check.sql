-- 1. RLS on every public table (flyway_schema_history included, no exclusions).
do $$
declare bad text;
begin
  select string_agg(c.relname, ', ') into bad
  from pg_class c join pg_namespace n on n.oid = c.relnamespace
  where n.nspname = 'public' and c.relkind in ('r','p') and not c.relrowsecurity;
  if bad is not null then raise exception 'Tables without RLS: %', bad; end if;
end $$;

-- 2. API roles hold no privileges on any existing public relation.
do $$
declare bad text;
begin
  select string_agg(format('%s on %s', r.rolname, c.relname), ', ') into bad
  from pg_class c
  join pg_namespace n on n.oid = c.relnamespace
  cross join pg_roles r
  where n.nspname = 'public' and c.relkind in ('r','p','v','m')
    and r.rolname in ('anon','authenticated','service_role')
    and has_table_privilege(r.rolname, c.oid,
        'select,insert,update,delete,truncate,references,trigger');
  if bad is not null then raise exception 'API role grants found: %', bad; end if;
end $$;

-- 3. Default privileges: a brand-new table and function are closed to API roles.
do $$
declare r text;
begin
  create table public._acl_probe_t (id int);
  create function public._acl_probe_f() returns int language sql as 'select 1';
  foreach r in array array['anon','authenticated','service_role'] loop
    if has_table_privilege(r, 'public._acl_probe_t', 'select,insert,update,delete') then
      raise exception 'default table privileges leak to %', r;
    end if;
    if has_function_privilege(r, 'public._acl_probe_f()', 'execute') then
      raise exception 'default function privileges leak to %', r;
    end if;
  end loop;
  drop table public._acl_probe_t;
  drop function public._acl_probe_f();
end $$;

-- 4. secure_table() is not callable by API roles.
do $$
begin
  if has_function_privilege('anon', 'public.secure_table(regclass)', 'execute') then
    raise exception 'anon can execute secure_table';
  end if;
end $$;

-- 5. Runtime role attributes.
do $$
begin
  if exists (select 1 from pg_roles
             where rolname = 'bevoya_app' and (rolsuper or rolbypassrls)) then
    raise exception 'bevoya_app is superuser or bypasses RLS';
  end if;
end $$;

-- 6. Behaviour: consent is append-only, but deleting a profile still cascades.
do $$
declare n int;
begin
  insert into auth.users (id) values ('00000000-0000-0000-0000-0000000000a1');
  insert into public.profiles (id) values ('00000000-0000-0000-0000-0000000000a1');
  insert into public.consent_records (user_id, consent_type, granted, policy_version)
    values ('00000000-0000-0000-0000-0000000000a1', 'age_18_plus', true, 'test');

  set local role bevoya_app;
  begin
    update public.consent_records set granted = false;
    raise exception 'bevoya_app could update consent_records';
  exception when insufficient_privilege then null;
  end;
  delete from public.profiles where id = '00000000-0000-0000-0000-0000000000a1';
  reset role;

  select count(*) into n from public.consent_records;
  if n <> 0 then raise exception 'consent_records did not cascade on profile delete'; end if;
  delete from auth.users;
end $$;