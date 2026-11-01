-- Private journal. Hard delete only; no visibility column, everything is private.
create table reflections (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references profiles (id) on delete cascade,
  entry_date  date not null,
  raw_text    text not null check (char_length(raw_text) between 1 and 20000),
  mood        smallint check (mood is null or mood between 1 and 5),
  energy      smallint check (energy is null or energy between 1 and 5),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

create index reflections_user_date_idx on reflections (user_id, entry_date desc, created_at desc);

select secure_table('public.reflections');