# Supabase

## Role

Supabase est la base centrale du projet.

Il stocke :

- les mesures journalieres ;
- les resumes journaliers ;
- les rapports hebdomadaires ;
- les sources de donnees.

## Migrations

Migration principale :

```text
supabase/migrations/001_health_metrics.sql
```

Migration optionnelle pour Supabase Cron :

```text
supabase/migrations/002_optional_supabase_cron.sql
```

## Installation

1. Creer un projet Supabase.
2. Ouvrir SQL Editor.
3. Executer `supabase/migrations/001_health_metrics.sql`.
4. Recuperer :
   - Project URL ;
   - anon key ;
   - service role key.
5. Renseigner `.env`.

## Variables liees

Backend :

```env
SUPABASE_URL=
SUPABASE_SERVICE_ROLE_KEY=
```

Dashboard :

```env
VITE_SUPABASE_URL=
VITE_SUPABASE_ANON_KEY=
```

La cle `SUPABASE_SERVICE_ROLE_KEY` doit rester uniquement cote backend.

## Tables

### `metric_sources`

Liste les sources disponibles :

- `zepp`
- `health_connect`
- `nutrition`
- `manual`
- `derived`

### `daily_metric_entries`

Table d'ingestion brute au format long.

### `daily_summaries`

Table lue par le dashboard pour afficher les donnees journalieres agregees.

### `weekly_reports`

Table lue par le dashboard pour afficher le dernier rapport hebdomadaire.

## RLS

La migration active RLS et ajoute des policies de lecture publiques sur les tables utiles au dashboard.

Le dashboard utilise la cle anon pour lire.

Le backend utilise la service role key pour ecrire.

## Verification

Apres un test d'ingestion :

```bash
make test-ingest
```

Verifier dans Supabase :

- `daily_metric_entries` contient plusieurs lignes ;
- `daily_summaries` contient une ligne pour la date testee ;
- le dashboard affiche les donnees.

## Cron Supabase optionnel

Si le backend n'est pas actif 24/7, Supabase Cron peut appeler :

- `/api/jobs/daily-summary`
- `/api/jobs/weekly-report`

Voir :

```text
supabase/migrations/002_optional_supabase_cron.sql
```
