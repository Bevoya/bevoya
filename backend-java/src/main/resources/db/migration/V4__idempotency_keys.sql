create table public.idempotency_keys (
  user_id uuid not null references public.profiles (id) on delete cascade,
  scope text not null check (char_length(scope) between 1 and 40),
  key text not null check (char_length(key) between 8 and 100),
  resource_id uuid not null,
  created_at timestamptz not null default now(),
  primary key (user_id, scope, key)
);
create index idempotency_keys_created_at_idx on public.idempotency_keys (created_at);
select public.secure_table('public.idempotency_keys');