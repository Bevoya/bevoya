-- Skills. The level is derived from linked evidence and notes; only the target is stored.
create table skills (
  id               uuid primary key default gen_random_uuid(),
  user_id          uuid not null references profiles (id) on delete cascade,
  name             text not null check (char_length(name) between 1 and 100),
  category         text check (category is null or char_length(category) <= 50),
  description      text check (description is null or char_length(description) <= 1000),
  target_level     text check (target_level is null or target_level in
    ('discovered','studying','explained','practiced','built','tested','applied')),
  next_revision_at timestamptz,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now(),
  unique (id, user_id)
);

create unique index skills_user_name_idx on skills (user_id, lower(name));

alter table learning_entries add column skill_id uuid;
alter table learning_entries add constraint learning_entries_skill_fk
  foreign key (skill_id, user_id) references skills (id, user_id) on delete set null (skill_id);
create index learning_entries_skill_idx on learning_entries (user_id, skill_id) where skill_id is not null;

alter table evidence add column skill_id uuid;
alter table evidence add constraint evidence_skill_fk
  foreign key (skill_id, user_id) references skills (id, user_id) on delete set null (skill_id);
create index evidence_skill_idx on evidence (user_id, skill_id) where skill_id is not null;

select secure_table('public.skills');