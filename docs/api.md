# API backend

Base locale :

```text
http://localhost:8787
```

Toutes les routes d'ecriture demandent :

```http
x-api-key: <INGEST_API_KEY>
```

## Health check

```http
GET /health
```

## Ingestion Health Connect

```http
POST /api/ingest/health-connect
content-type: application/json
x-api-key: <INGEST_API_KEY>
```

Payload :

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

Reponse :

```json
{
  "ok": true,
  "insertedOrUpdated": 3,
  "metricDates": ["2026-04-29"]
}
```

L'ingestion est idempotente par date, source, metrique et intervalle temporel. Relancer le meme export met a jour la ligne au lieu de dupliquer.

## Recalcul du resume quotidien

```http
POST /api/jobs/daily-summary
content-type: application/json
x-api-key: <INGEST_API_KEY>
```

Payload optionnel :

```json
{
  "from": "2026-04-01",
  "to": "2026-04-29"
}
```

## Generation du rapport hebdomadaire

```http
POST /api/jobs/weekly-report
content-type: application/json
x-api-key: <INGEST_API_KEY>
```

Payload optionnel :

```json
{
  "weekStart": "2026-04-20",
  "weekEnd": "2026-04-26"
}
```

Sans payload, le backend genere un rapport sur les 7 derniers jours complets.
