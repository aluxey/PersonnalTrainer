# Backend

## Role

Le backend Node.js recoit les donnees, les stocke dans Supabase et genere les analyses.

Il se trouve dans :

```text
backend/
```

## Commandes

Lancer en dev :

```bash
npm run dev:backend
```

Verifier la syntaxe :

```bash
npm run check --workspace backend
```

Tester la sante :

```bash
curl http://127.0.0.1:8787/health
```

## Variables

```env
PORT=8787
HOST=127.0.0.1
SUPABASE_URL=
SUPABASE_SERVICE_ROLE_KEY=
INGEST_API_KEY=
RUN_CRON=true
DAILY_CRON=20 8 * * *
WEEKLY_REPORT_CRON=0 9 * * 0
```

## Routes

### `GET /health`

Verifie que le backend tourne.

### `POST /api/ingest/health-connect`

Recoit les donnees de l'app Android.

Headers :

```http
content-type: application/json
x-api-key: <INGEST_API_KEY>
```

Payload :

```json
{
  "date": "2026-04-29",
  "source": "health_connect",
  "metrics": [
    { "metric": "steps", "value": 9200, "unit": "count" }
  ]
}
```

### `POST /api/jobs/daily-summary`

Recalcule les resumes quotidiens.

Payload optionnel :

```json
{
  "from": "2026-04-01",
  "to": "2026-04-29"
}
```

### `POST /api/jobs/weekly-report`

Genere un rapport hebdomadaire.

Payload optionnel :

```json
{
  "weekStart": "2026-04-20",
  "weekEnd": "2026-04-26"
}
```

## Jobs

Le scheduler est dans :

```text
backend/src/jobs/scheduler.js
```

Si `RUN_CRON=true` :

- `DAILY_CRON` recalcule `daily_summaries` ;
- `WEEKLY_REPORT_CRON` genere `weekly_reports`.

## Services

- `backend/src/services/metrics.js` : ingestion, normalisation, resume journalier.
- `backend/src/services/reports.js` : rapport hebdomadaire.
- `backend/src/lib/supabase.js` : client Supabase service role.
- `backend/src/lib/http.js` : API key, async handler, error handler.

## Test d'ingestion

Le script `scripts/test_ingest.sh` lit automatiquement `.env`.

```bash
make test-ingest
```

## Notes importantes

- Le telephone ne peut pas appeler `localhost`.
- En production, le backend doit etre accessible depuis le telephone.
- `INGEST_API_KEY` protege les routes d'ecriture.
- La service role key ne doit jamais etre exposee au dashboard.
