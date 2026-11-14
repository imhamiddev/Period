-- Luna v1.2 — run once in Supabase → SQL Editor (safe to re-run).

-- notification preferences
alter table public.profiles add column if not exists notify_hour int not null default 9;
alter table public.profiles add column if not exists notify_pms boolean not null default true;
alter table public.profiles add column if not exists notify_period boolean not null default true;
alter table public.profiles add column if not exists notify_ovulation boolean not null default false;
alter table public.profiles add column if not exists notify_late boolean not null default true;

-- mood / energy / symptoms
create table if not exists public.daily_logs (
  user_id uuid not null references auth.users on delete cascade,
  day date not null,
  mood int check (mood between 1 and 5),
  energy int check (energy between 1 and 3),
  symptoms text[] not null default '{}',
  primary key (user_id, day)
);

-- daily notes (partner only sees the ones marked shared)
create table if not exists public.notes (
  user_id uuid not null references auth.users on delete cascade,
  day date not null,
  body text not null,
  shared boolean not null default false,
  primary key (user_id, day)
);

-- quick messages between the two
create table if not exists public.messages (
  id uuid primary key default gen_random_uuid(),
  pair_id uuid not null references public.pairs on delete cascade,
  from_id uuid not null references auth.users on delete cascade,
  to_id uuid not null references auth.users on delete cascade,
  kind text not null default 'quick',
  body text not null,
  created_at timestamptz not null default now(),
  read_at timestamptz
);

alter table public.daily_logs enable row level security;
alter table public.notes enable row level security;
alter table public.messages enable row level security;

drop policy if exists dl_owner on public.daily_logs;
create policy dl_owner on public.daily_logs for all using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists dl_partner on public.daily_logs;
create policy dl_partner on public.daily_logs for select using (
  exists (select 1 from public.pairs p join public.profiles h on h.id = p.her_id
          where p.her_id = daily_logs.user_id and p.partner_id = auth.uid() and p.status = 'active' and h.share_enabled)
);

drop policy if exists notes_owner on public.notes;
create policy notes_owner on public.notes for all using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists notes_partner on public.notes;
create policy notes_partner on public.notes for select using (
  shared and exists (select 1 from public.pairs p join public.profiles h on h.id = p.her_id
          where p.her_id = notes.user_id and p.partner_id = auth.uid() and p.status = 'active' and h.share_enabled)
);

drop policy if exists msg_select on public.messages;
create policy msg_select on public.messages for select using (from_id = auth.uid() or to_id = auth.uid());
drop policy if exists msg_insert on public.messages;
create policy msg_insert on public.messages for insert with check (
  from_id = auth.uid() and exists (
    select 1 from public.pairs p where p.id = pair_id and p.status = 'active'
      and ((p.her_id = auth.uid() and p.partner_id = to_id) or (p.partner_id = auth.uid() and p.her_id = to_id)))
);
drop policy if exists msg_update on public.messages;
create policy msg_update on public.messages for update using (to_id = auth.uid()) with check (to_id = auth.uid());
