# Mise en place Android + Supabase

## Etape 1 : Zepp vers Health Connect

Sur Android :

1. Ouvre Zepp.
2. Va dans les permissions, integrations ou services tiers disponibles.
3. Active la synchronisation vers Health Connect si disponible.
4. Ouvre Health Connect dans les parametres Android.
5. Verifie que Zepp a le droit d'ecrire les donnees utiles : activite, sommeil, frequence cardiaque, poids.
6. Verifie que ton app nutrition a aussi le droit d'ecrire calories, macros, eau et poids si elle les gere.

Health Connect doit devenir le hub unique. Zepp garde l'experience montre, mais l'analyse part de Health Connect.

## Etape 2 : stockage quotidien dans Supabase

Dans Supabase :

1. Cree un projet.
2. Ouvre le SQL Editor.
3. Execute `supabase/migrations/001_health_metrics.sql`.
4. Recupere :
   - Project URL ;
   - anon public key ;
   - service role key.
5. Copie `.env.example` vers `.env`.
6. Renseigne les variables.

La cle `SUPABASE_SERVICE_ROLE_KEY` reste uniquement cote backend. Ne jamais la mettre dans le dashboard.

En local, garde ces valeurs :

```bash
HOST=127.0.0.1
PORT=8787
```

## Etape 3 : backend Node.js

Installation :

```bash
npm install
```

Demarrage :

```bash
npm run dev:backend
```

Test :

```bash
curl http://localhost:8787/health
```

Ingestion de test :

```bash
curl -X POST http://localhost:8787/api/ingest/health-connect \
  -H "content-type: application/json" \
  -H "x-api-key: $INGEST_API_KEY" \
  --data @examples/api/health-connect-daily.json
```

## Etape 4 : export Android quotidien

Tu as deux options.

### Option A : app d'export Health Connect

Utilise une app capable de lire Health Connect et d'envoyer un JSON vers un webhook/API custom. Configure :

- URL : `https://ton-backend/api/ingest/health-connect`
- Header : `x-api-key: ton_token`
- Methode : `POST`
- Frequence : quotidienne apres la synchro Zepp du matin

Le JSON attendu est documente dans `docs/api.md`.

### Option B : app Android custom

Si aucune app d'export ne convient, on cree une petite app Android qui :

1. demande les permissions Health Connect ;
2. lit les aggregats de la veille ou du jour ;
3. convertit les donnees en metriques longues ;
4. appelle le backend ;
5. tourne via WorkManager chaque matin.

Metriques a lire en priorite :

- `StepsRecord`
- `SleepSessionRecord`
- calories actives / depense energetique
- `ExerciseSessionRecord`
- `WeightRecord`
- nutrition si ton app alimentaire ecrit dans Health Connect

Pour les donnees cumulatives comme les pas, il faut privilegier les aggregats Health Connect pour eviter les doubles comptages.

## Etape 5 : dashboard React

Demarrage :

```bash
npm run dev:dashboard
```

Le dashboard lit :

- `daily_summaries`
- `weekly_reports`

Il utilise la cle anon Supabase et les policies de lecture definies dans la migration.

## Etape 6 : cron quotidien et rapport automatique

Par defaut, le backend lance `node-cron` si `RUN_CRON=true`.

Variables :

```bash
DAILY_CRON=20 8 * * *
WEEKLY_REPORT_CRON=0 9 * * 0
```

- `DAILY_CRON` recalcule les resumes.
- `WEEKLY_REPORT_CRON` genere le rapport hebdomadaire.

Si le backend n'est pas toujours allume, utilise plutot Supabase Cron avec `supabase/migrations/002_optional_supabase_cron.sql`.

## Etape 7 : API custom avancee

Quand le flux simple marche, les ameliorations utiles sont :

- ajouter une app Android custom pour ne plus dependre d'une app d'export ;
- ajouter des metriques fines par tranche horaire pour sommeil et activite ;
- ajouter une table `food_logs` si tu veux analyser les repas individuellement ;
- ajouter un moteur d'analyse plus pousse dans `backend/src/services/reports.js`.
