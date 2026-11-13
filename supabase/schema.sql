-- Luna / Period — Supabase schema. Run once in Supabase → SQL Editor.
-- Also: Authentication → Providers → Email → turn OFF "Confirm email".

create table if not exists public.profiles (
  id uuid primary key references auth.users on delete cascade,
  email text,
  name text,
  role text check (role in ('her','partner')),
  pair_code text unique,
  cycle_length int not null default 28,
  period_length int not null default 5,
  luteal_length int not null default 14,
  pms_days int not null default 5,
  share_enabled boolean not null default true,
  notify_enabled boolean not null default true,
  onboarded boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.period_logs (
  user_id uuid not null references auth.users on delete cascade,
  day date not null,
  primary key (user_id, day)
);

create table if not exists public.pairs (
  id uuid primary key default gen_random_uuid(),
  her_id uuid not null references public.profiles on delete cascade,
  partner_id uuid not null references public.profiles on delete cascade,
  status text not null default 'pending' check (status in ('pending','active')),
  created_at timestamptz not null default now(),
  unique (her_id, partner_id)
);

alter table public.profiles enable row level security;
alter table public.period_logs enable row level security;
alter table public.pairs enable row level security;

-- profiles
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles for select using (
  id = auth.uid()
  or exists (select 1 from public.pairs p where p.her_id = auth.uid() and p.partner_id = profiles.id)
  or exists (select 1 from public.pairs p where p.partner_id = auth.uid() and p.her_id = profiles.id and p.status = 'active')
);
drop policy if exists profiles_update on public.profiles;
create policy profiles_update on public.profiles for update using (id = auth.uid()) with check (id = auth.uid());

-- period logs
drop policy if exists logs_owner on public.period_logs;
create policy logs_owner on public.period_logs for all using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists logs_partner_read on public.period_logs;
create policy logs_partner_read on public.period_logs for select using (
  exists (
    select 1 from public.pairs p join public.profiles h on h.id = p.her_id
    where p.her_id = period_logs.user_id and p.partner_id = auth.uid()
      and p.status = 'active' and h.share_enabled
  )
);

-- pairs
drop policy if exists pairs_select on public.pairs;
create policy pairs_select on public.pairs for select using (her_id = auth.uid() or partner_id = auth.uid());
drop policy if exists pairs_approve on public.pairs;
create policy pairs_approve on public.pairs for update using (her_id = auth.uid()) with check (her_id = auth.uid());
drop policy if exists pairs_delete on public.pairs;
create policy pairs_delete on public.pairs for delete using (her_id = auth.uid() or partner_id = auth.uid());

-- profile auto-created on signup
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, email, pair_code)
  values (new.id, new.email, upper(substr(md5(random()::text || new.id::text), 1, 6)));
  return new;
end $$;
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- partner enters her code → pending request
create or replace function public.request_pair(p_code text) returns void
language plpgsql security definer set search_path = public as $$
declare her uuid;
begin
  select id into her from public.profiles where pair_code = upper(trim(p_code)) and role = 'her';
  if her is null then raise exception 'invalid_code'; end if;
  if her = auth.uid() then raise exception 'self_code'; end if;
  insert into public.pairs (her_id, partner_id) values (her, auth.uid()) on conflict do nothing;
end $$;
grant execute on function public.request_pair(text) to authenticated;
