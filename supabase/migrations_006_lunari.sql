-- Lunari redesign: moods, health data. Safe to run more than once, also after the first (older) version of this file.
--
-- Who can read what:
--   daily_logs   (flow, symptoms, moods)        her + her connected partner  (existing policy)
--   health_logs  (weight, temperature, water)    her + her connected partner  (while sharing is on)
--   health_profile (height, weight, water goal)  her + her connected partner  (while sharing is on)
--   private_logs (medicines, note)               only her
--   private_profile (birth date, gender, phone)  only her

-- ---------------------------------------------------------------- shared with her partner
create table if not exists public.health_profile (
  user_id uuid primary key references auth.users on delete cascade,
  height_cm numeric(5,1) check (height_cm is null or height_cm between 80 and 250),
  weight_kg numeric(5,1) check (weight_kg is null or weight_kg between 20 and 400),
  water_goal_ml int not null default 2400 check (water_goal_ml between 500 and 8000),
  cup_ml int not null default 200 check (cup_ml between 50 and 1000),
  updated_at timestamptz not null default now()
);
alter table public.health_profile enable row level security;
drop policy if exists hp_owner on public.health_profile;
create policy hp_owner on public.health_profile for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists hp_partner on public.health_profile;
create policy hp_partner on public.health_profile for select to authenticated using (
  exists (select 1 from public.pairs p join public.profiles h on h.id = p.her_id
          where p.her_id = health_profile.user_id and p.partner_id = auth.uid() and p.status = 'active' and h.share_enabled)
);

create table if not exists public.health_logs (
  user_id uuid not null references auth.users on delete cascade,
  day date not null,
  weight_kg numeric(5,1) check (weight_kg is null or weight_kg between 20 and 400),
  temperature_c numeric(4,1) check (temperature_c is null or temperature_c between 30 and 45),
  water_ml int check (water_ml is null or water_ml between 0 and 20000),
  primary key (user_id, day)
);
alter table public.health_logs enable row level security;
drop policy if exists hl_owner on public.health_logs;
create policy hl_owner on public.health_logs for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists hl_partner on public.health_logs;
create policy hl_partner on public.health_logs for select to authenticated using (
  exists (select 1 from public.pairs p join public.profiles h on h.id = p.her_id
          where p.her_id = health_logs.user_id and p.partner_id = auth.uid() and p.status = 'active' and h.share_enabled)
);

-- ---------------------------------------------------------------- only her
create table if not exists public.private_profile (
  user_id uuid primary key references auth.users on delete cascade,
  birth_date date,
  gender text not null default 'female',
  phone text check (phone is null or char_length(phone) <= 32),
  updated_at timestamptz not null default now()
);
alter table public.private_profile enable row level security;
drop policy if exists pp_owner on public.private_profile;
create policy pp_owner on public.private_profile for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

create table if not exists public.private_logs (
  user_id uuid not null references auth.users on delete cascade,
  day date not null,
  medicines text[] not null default '{}',
  note text check (note is null or char_length(note) <= 2000),
  primary key (user_id, day)
);
alter table public.private_logs enable row level security;
drop policy if exists pl_owner on public.private_logs;
create policy pl_owner on public.private_logs for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ---------------------------------------------------------------- moods on the daily log (shared like symptoms)
alter table public.daily_logs add column if not exists moods text[] not null default '{}';

-- ---------------------------------------------------------------- move data from the first version of this migration (if it was run)
do $mig$
begin
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'private_profile' and column_name = 'height_cm') then
    insert into public.health_profile (user_id, height_cm, weight_kg, water_goal_ml, cup_ml)
      select user_id, height_cm, weight_kg, water_goal_ml, cup_ml from public.private_profile
      on conflict (user_id) do nothing;
    alter table public.private_profile drop column height_cm, drop column weight_kg, drop column water_goal_ml, drop column cup_ml;
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'health_logs' and column_name = 'medicines') then
    insert into public.private_logs (user_id, day, medicines, note)
      select user_id, day, medicines, note from public.health_logs
      where coalesce(array_length(medicines, 1), 0) > 0 or note is not null
      on conflict (user_id, day) do nothing;
    alter table public.health_logs drop column medicines, drop column note;
  end if;
end
$mig$;

-- ---------------------------------------------------------------- removed features
drop table if exists public.articles;
drop table if exists public.support_tickets;
