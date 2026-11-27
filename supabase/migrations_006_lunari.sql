-- Lunari redesign: richer daily log, health profile, articles, support tickets.
-- Safe to run more than once.

-- ---------------------------------------------------------------- private profile (onboarding + personal info)
-- Separate from `profiles` on purpose: her partner / family can read `profiles`, but never this table.
create table if not exists public.private_profile (
  user_id uuid primary key references auth.users on delete cascade,
  birth_date date,
  height_cm numeric(5,1) check (height_cm is null or height_cm between 80 and 250),
  weight_kg numeric(5,1) check (weight_kg is null or weight_kg between 20 and 400),
  gender text not null default 'female',
  phone text check (phone is null or char_length(phone) <= 32),
  water_goal_ml int not null default 2400 check (water_goal_ml between 500 and 8000),
  cup_ml int not null default 200 check (cup_ml between 50 and 1000),
  updated_at timestamptz not null default now()
);
alter table public.private_profile enable row level security;
drop policy if exists pp_owner on public.private_profile;
create policy pp_owner on public.private_profile for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ---------------------------------------------------------------- daily log: moods are shared with her partner like symptoms
alter table public.daily_logs add column if not exists moods text[] not null default '{}';

-- ---------------------------------------------------------------- private health log (never visible to anyone else)
create table if not exists public.health_logs (
  user_id uuid not null references auth.users on delete cascade,
  day date not null,
  medicines text[] not null default '{}',
  note text check (note is null or char_length(note) <= 2000),
  weight_kg numeric(5,1) check (weight_kg is null or weight_kg between 20 and 400),
  temperature_c numeric(4,1) check (temperature_c is null or temperature_c between 30 and 45),
  water_ml int check (water_ml is null or water_ml between 0 and 20000),
  primary key (user_id, day)
);
alter table public.health_logs enable row level security;
drop policy if exists hl_owner on public.health_logs;
create policy hl_owner on public.health_logs for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ---------------------------------------------------------------- articles (read-only for every signed-in user)
create table if not exists public.articles (
  id uuid primary key default gen_random_uuid(),
  category text not null,
  title text not null,
  summary text not null default '',
  body text not null,
  icon text not null default 'drop',
  sort int not null default 0,
  created_at timestamptz not null default now()
);
alter table public.articles enable row level security;
drop policy if exists articles_read on public.articles;
create policy articles_read on public.articles for select to authenticated using (true);

-- ---------------------------------------------------------------- support tickets (Help & Support → Contact)
create table if not exists public.support_tickets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users on delete cascade,
  subject text not null check (char_length(subject) between 1 and 120),
  message text not null check (char_length(message) between 1 and 2000),
  created_at timestamptz not null default now()
);
alter table public.support_tickets enable row level security;
drop policy if exists tickets_insert on public.support_tickets;
create policy tickets_insert on public.support_tickets for insert to authenticated with check (user_id = auth.uid());
drop policy if exists tickets_select on public.support_tickets;
create policy tickets_select on public.support_tickets for select to authenticated using (user_id = auth.uid());
