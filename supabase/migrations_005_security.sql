-- Luna v1.5 — security hardening. Run once in Supabase → SQL Editor (safe to re-run).
-- Release the matching app version first: it calls request_pair_v2 / delete_my_account, and the old
-- request_pair is switched off below (older app versions then show an error instead of being brute-forceable).

-- 1) A partner must not read her e-mail through the profiles table. The e-mail already lives in auth.users;
--    the app now remembers its own e-mail on the device.
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, pair_code)
  values (new.id, upper(substr(md5(random()::text || new.id::text), 1, 8)));  -- 8 chars for new accounts
  return new;
end $$;
alter table public.profiles drop column if exists email;

-- 2) Users may edit their own profile, but not the fields that identify it.
create or replace function public.profiles_guard() returns trigger
language plpgsql as $$
begin
  if auth.uid() is null then return new; end if;                 -- SQL editor / service role
  if new.id is distinct from old.id then raise exception 'immutable_field'; end if;
  if new.pair_code is distinct from old.pair_code then raise exception 'immutable_field'; end if;
  if new.created_at is distinct from old.created_at then raise exception 'immutable_field'; end if;
  -- the role can still be changed while she is going through the first-run setup, but not afterwards
  if old.onboarded and old.role is not null and new.role is distinct from old.role then raise exception 'role_locked'; end if;
  return new;
end $$;
drop trigger if exists profiles_guard on public.profiles;
create trigger profiles_guard before update on public.profiles
  for each row execute function public.profiles_guard();

-- 3) Pairing codes: rate-limited and uniform answers. Failed attempts have to be *committed*, so the
--    function returns a status text instead of raising (a raised exception would roll the attempt back).
create table if not exists public.pair_attempts (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  at timestamptz not null default now()
);
create index if not exists pair_attempts_user_at on public.pair_attempts (user_id, at desc);
alter table public.pair_attempts enable row level security;   -- no policies: only the function below touches it

create or replace function public.request_pair_v2(p_code text) returns text
language plpgsql security definer set search_path = public as $$
declare her uuid;
begin
  if auth.uid() is null then return 'not_authenticated'; end if;
  delete from public.pair_attempts where user_id = auth.uid() and at < now() - interval '1 day';
  if (select count(*) from public.pair_attempts
        where user_id = auth.uid() and at > now() - interval '10 minutes') >= 8 then
    return 'too_many_attempts';
  end if;
  insert into public.pair_attempts (user_id) values (auth.uid());

  select id into her from public.profiles where pair_code = upper(trim(p_code)) and role = 'her';
  if her is null then return 'invalid_code'; end if;
  if her = auth.uid() then return 'self_code'; end if;
  insert into public.pairs (her_id, partner_id) values (her, auth.uid()) on conflict do nothing;
  return 'ok';
end $$;
revoke all on function public.request_pair_v2(text) from public, anon;
grant execute on function public.request_pair_v2(text) to authenticated;

-- the old function has no rate limit: switch it off
revoke execute on function public.request_pair(text) from public, anon, authenticated;

-- 4) Delete my account and everything attached to it (profile, logs, notes, messages, pairs cascade).
create or replace function public.delete_my_account() returns void
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  delete from auth.users where id = auth.uid();
end $$;
revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;

-- 5) Sane size limits (existing rows are not re-checked).
do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'messages_body_len') then
    alter table public.messages add constraint messages_body_len check (char_length(body) <= 1000) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'notes_body_len') then
    alter table public.notes add constraint notes_body_len check (char_length(body) <= 5000) not valid;
  end if;
end $$;
