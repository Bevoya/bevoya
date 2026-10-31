-- Focus sessions. The timer is derived from started_at, never from a client clock.
create table sessions (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null references profiles (id) on delete cascade,
  task_id         uuid,
  activity_type   text not null
    check (activity_type in ('study','build','revise','practice','create','work','read','reflect')),
  topic           text check (topic is null or char_length(topic) <= 200),
  started_at      timestamptz not null default now(),
  ended_at        timestamptz,
  actual_minutes  int check (actual_minutes is null or actual_minutes >= 0),
  focus_score     smallint check (focus_score is null or focus_score between 1 and 5),
  output          text check (output is null or char_length(output) <= 5000),
  notes           text check (notes is null or char_length(notes) <= 10000),
  created_at      timestamptz not null default now(),
  constraint sessions_end_after_start check (ended_at is null or ended_at >= started_at),
  constraint sessions_ended_has_minutes check ((ended_at is null) = (actual_minutes is null)),
  unique (id, user_id),
  foreign key (task_id, user_id) references tasks (id, user_id) on delete set null (task_id)
);

-- At most one running session per person.
create unique index sessions_one_running_per_user on sessions (user_id) where ended_at is null;

create index sessions_user_started_idx on sessions (user_id, started_at desc);

select secure_table('public.sessions');