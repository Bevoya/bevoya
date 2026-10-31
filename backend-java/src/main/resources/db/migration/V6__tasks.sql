-- V6: tasks (planning module). Copy to backend-java/src/main/resources/db/migration/
-- Composite foreign keys mean a task can never point at another user's goal or milestone.
-- Needs unique (id, user_id) on goals and milestones (milestones V5 has it; goals is referenced by milestones, so it has it).

create table tasks (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references profiles(id) on delete cascade,
  goal_id uuid,
  milestone_id uuid,
  title text not null check (char_length(title) between 1 and 200),
  description text check (char_length(description) <= 2000),
  category text not null check (category in
    ('learn','build','revise','practice','create','work','admin','health','career','personal')),
  priority smallint not null default 2 check (priority between 1 and 3),
  estimated_minutes int check (estimated_minutes between 1 and 1440),
  planned_for date,
  due_at timestamptz,
  status text not null default 'todo' check (status in ('todo','doing','done','skipped')),
  why_it_matters text check (char_length(why_it_matters) <= 500),
  completed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, user_id),
  foreign key (goal_id, user_id) references goals (id, user_id) on delete set null (goal_id),
  foreign key (milestone_id, user_id) references milestones (id, user_id) on delete set null (milestone_id)
);

create index tasks_user_planned_idx on tasks (user_id, planned_for, status);
create index tasks_user_goal_idx on tasks (user_id, goal_id);

select secure_table('public.tasks');