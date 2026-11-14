-- Luna v1.3 — run once in Supabase → SQL Editor (safe to re-run).
alter table public.profiles add column if not exists regularity int not null default 0 check (regularity between 0 and 2);
alter table public.profiles add column if not exists hormonal boolean not null default false;
