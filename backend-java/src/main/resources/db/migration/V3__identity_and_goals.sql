create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  display_name text check (char_length(display_name) between 1 and 80),
  user_type text not null default 'other'
    check (user_type in ('student','young_professional','working_adult','creator','other')),
  time_zone text not null default 'UTC',
  available_minutes_per_day integer check (available_minutes_per_day between 0 and 1440),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
select public.secure_table('public.profiles');

-- Append-only: withdrawing consent inserts a new row. bevoya_app can read and insert only.
create table public.consent_records (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  consent_type text not null
    check (consent_type in ('age_18_plus','terms','privacy_policy','ai_memory','journal_retrieval')),
  granted boolean not null,
  policy_version text not null,
  recorded_at timestamptz not null default now()
);
create index consent_records_user_type_idx
  on public.consent_records (user_id, consent_type, recorded_at desc);
select public.secure_table('public.consent_records');
revoke update, delete on table public.consent_records from bevoya_app;

create table public.goals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  title text not null check (char_length(title) between 1 and 200),
  description text check (char_length(description) <= 2000),
  status text not null default 'active'
    check (status in ('active','paused','completed','archived')),
  target_date date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, user_id)  -- target for composite foreign keys from child tables
);
create index goals_user_status_idx on public.goals (user_id, status);
select public.secure_table('public.goals');