-- v1.4: role label for each connection (partner / mother / sister / friend …).
-- Optional: the app also keeps the label on her device, so it works without this,
-- but running it lets the other person see the role she gave them.
alter table public.pairs add column if not exists label text;
