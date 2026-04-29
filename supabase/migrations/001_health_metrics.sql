create extension if not exists pgcrypto;

create table if not exists public.metric_sources (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  kind text not null check (kind in ('zepp', 'health_connect', 'nutrition', 'manual', 'derived')),
  created_at timestamptz not null default now()
);

create table if not exists public.daily_metric_entries (
  id uuid primary key default gen_random_uuid(),
  metric_date date not null,
  source text not null,
  metric text not null,
  value numeric not null,
  unit text not null,
  start_time timestamptz,
  end_time timestamptz,
  dedupe_key text not null,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (dedupe_key)
);

create index if not exists daily_metric_entries_metric_date_idx
  on public.daily_metric_entries (metric_date desc);

create index if not exists daily_metric_entries_metric_idx
  on public.daily_metric_entries (metric);

create table if not exists public.daily_summaries (
  metric_date date primary key,
  metrics jsonb not null,
  score jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.weekly_reports (
  week_start date primary key,
  week_end date not null,
  metrics jsonb not null,
  insights jsonb not null,
  markdown text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists daily_metric_entries_touch_updated_at on public.daily_metric_entries;
create trigger daily_metric_entries_touch_updated_at
before update on public.daily_metric_entries
for each row execute function public.touch_updated_at();

drop trigger if exists daily_summaries_touch_updated_at on public.daily_summaries;
create trigger daily_summaries_touch_updated_at
before update on public.daily_summaries
for each row execute function public.touch_updated_at();

drop trigger if exists weekly_reports_touch_updated_at on public.weekly_reports;
create trigger weekly_reports_touch_updated_at
before update on public.weekly_reports
for each row execute function public.touch_updated_at();

alter table public.metric_sources enable row level security;
alter table public.daily_metric_entries enable row level security;
alter table public.daily_summaries enable row level security;
alter table public.weekly_reports enable row level security;

drop policy if exists "Read metric sources" on public.metric_sources;
create policy "Read metric sources"
on public.metric_sources for select
using (true);

drop policy if exists "Read daily metric entries" on public.daily_metric_entries;
create policy "Read daily metric entries"
on public.daily_metric_entries for select
using (true);

drop policy if exists "Read daily summaries" on public.daily_summaries;
create policy "Read daily summaries"
on public.daily_summaries for select
using (true);

drop policy if exists "Read weekly reports" on public.weekly_reports;
create policy "Read weekly reports"
on public.weekly_reports for select
using (true);

insert into public.metric_sources (name, kind)
values
  ('zepp', 'zepp'),
  ('health_connect', 'health_connect'),
  ('nutrition', 'nutrition'),
  ('manual', 'manual'),
  ('derived', 'derived')
on conflict (name) do nothing;
