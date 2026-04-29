# Data Model

## Format d'ingestion

Le backend accepte un payload long.

```json
{
  "date": "2026-04-29",
  "source": "health_connect",
  "metrics": [
    { "metric": "steps", "value": 9200, "unit": "count" },
    { "metric": "sleep_duration_min", "value": 452, "unit": "min" },
    { "metric": "weight_kg", "value": 90.8, "unit": "kg" }
  ]
}
```

## Pourquoi un format long

Un format long permet :

- d'ajouter une metrique sans migration SQL ;
- de melanger plusieurs sources ;
- de garder une ingestion idempotente ;
- de calculer ensuite des resumes journaliers.

## Metriques principales

| Metrique | Unite | Source typique |
| --- | --- | --- |
| `steps` | `count` | Health Connect / Zepp |
| `sleep_duration_min` | `min` | Health Connect / Zepp |
| `sleep_deep_min` | `min` | Export avance |
| `sleep_rem_min` | `min` | Export avance |
| `sleep_score` | `score` | Zepp si disponible |
| `active_energy_kcal` | `kcal` | Health Connect |
| `workout_duration_min` | `min` | Health Connect |
| `workout_count` | `count` | Health Connect |
| `workout_energy_kcal` | `kcal` | Health Connect si disponible |
| `heart_rate_resting_bpm` | `bpm` | Health Connect |
| `weight_kg` | `kg` | Health Connect / balance |
| `body_fat_pct` | `%` | Balance si disponible |
| `calories_intake_kcal` | `kcal` | App nutrition |
| `protein_g` | `g` | App nutrition |
| `carbs_g` | `g` | App nutrition |
| `fat_g` | `g` | App nutrition |
| `fiber_g` | `g` | App nutrition |
| `water_ml` | `ml` | App nutrition / Health Connect |

## Tables Supabase

### `daily_metric_entries`

Stocke les metriques au format long.

Colonnes importantes :

- `metric_date`
- `source`
- `metric`
- `value`
- `unit`
- `start_time`
- `end_time`
- `dedupe_key`
- `metadata`

La colonne `dedupe_key` permet d'upsert la meme mesure sans doublon.

### `daily_summaries`

Stocke un resume par jour.

- `metric_date`
- `metrics` JSONB
- `score` JSONB

### `weekly_reports`

Stocke les rapports hebdomadaires.

- `week_start`
- `week_end`
- `metrics` JSONB
- `insights` JSONB
- `markdown`

## Fallback CSV local

Le format local normalise est :

```csv
date,source,metric,value,unit,start_time,end_time,notes
2026-04-29,zepp,steps,10432,count,,,
2026-04-29,zepp,sleep_duration_min,443,min,,,
```

Scripts :

```bash
python3 scripts/normalize_daily.py --input data/raw --output data/processed/daily_metrics.csv
python3 scripts/build_daily_report.py --input data/processed/daily_metrics.csv --output-dir data/reports
```
