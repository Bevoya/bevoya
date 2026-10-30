create table public.milestones (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  goal_id uuid not null,
  title text not null check (char_length(title) between 1 and 200),
  description text check (char_length(description) <= 2000),
  status text not null default 'not_started'
    check (status in ('not_started','in_progress','completed')),
  target_date date,
  position integer not null check (position >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, user_id),  -- target for composite foreign keys from child tables (tasks later)
  -- A milestone can never point at another user's goal, even if application code has a bug.
  foreign key (goal_id, user_id) references public.goals (id, user_id) on delete cascade
);
create index milestones_goal_position_idx
  on public.milestones (goal_id, position, created_at, id);
select public.secure_table('public.milestones');