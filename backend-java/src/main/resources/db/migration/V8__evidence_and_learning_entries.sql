-- Learning notes. Private journals live in a separate table (reflections), never here.
create table learning_entries (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references profiles (id) on delete cascade,
  title       text not null check (char_length(title) between 1 and 200),
  content     text not null check (char_length(content) between 1 and 20000),
  source_url  text check (source_url is null
                or (source_url ~ '^https://' and char_length(source_url) <= 2000)),
  confidence  smallint check (confidence is null or confidence between 1 and 5),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now(),
  unique (id, user_id)
);
create index learning_entries_user_created_idx on learning_entries (user_id, created_at desc);

-- Proof of progress. activity_level separates watching from doing.
create table evidence (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null references profiles (id) on delete cascade,
  session_id      uuid,
  type            text not null check (type in ('note','commit','snippet','project_update',
                    'quiz_result','document','link','voice_note','photo','manual')),
  activity_level  text not null check (activity_level in ('watched','read','practiced','built',
                    'tested','applied')),
  title           text not null check (char_length(title) between 1 and 200),
  content         text check (content is null or char_length(content) <= 10000),
  external_url    text check (external_url is null
                    or (external_url ~ '^https://' and char_length(external_url) <= 2000)),
  created_at      timestamptz not null default now(),
  unique (id, user_id),
  foreign key (session_id, user_id) references sessions (id, user_id) on delete set null (session_id)
);
create index evidence_user_created_idx on evidence (user_id, created_at desc);

select secure_table('public.learning_entries');
select secure_table('public.evidence');