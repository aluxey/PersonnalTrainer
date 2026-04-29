# Operations

## Routine de developpement

Backend :

```bash
npm run dev:backend
```

Dashboard :

```bash
npm run dev:dashboard
```

Checks :

```bash
make check
```

Test ingestion :

```bash
make test-ingest
```

## Routine quotidienne cible

Chaque matin :

1. Zepp synchronise la montre vers le telephone.
2. Zepp ecrit dans Health Connect.
3. L'app Android lit les donnees de la veille.
4. L'app Android appelle `/api/ingest/health-connect`.
5. Le backend stocke dans Supabase.
6. Le backend recalcule les summaries.
7. Le dashboard affiche les donnees.

Chaque dimanche :

1. Le backend genere le rapport hebdomadaire.
2. Le dashboard affiche les insights.
3. Tu ajustes calories, pas ou sommeil selon les tendances.

## Verification apres ingestion

Dans Supabase :

- `daily_metric_entries` contient les metriques ;
- `daily_summaries` contient un resume pour la date ;
- `weekly_reports` contient le dernier rapport apres le job hebdo.

Dans le dashboard :

- les cartes KPI se remplissent ;
- les tendances 30 jours affichent les points ;
- le rapport hebdomadaire apparait apres generation.

## Commandes utiles

Health check backend :

```bash
curl http://127.0.0.1:8787/health
```

Test ingestion avec payload exemple :

```bash
make test-ingest
```

Generation manuelle du rapport hebdo :

```bash
curl -X POST http://127.0.0.1:8787/api/jobs/weekly-report \
  -H "content-type: application/json" \
  -H "x-api-key: $INGEST_API_KEY" \
  -d '{}'
```

## Probleme courant : telephone et localhost

Depuis un telephone physique, `localhost` pointe vers le telephone, pas vers ton ordinateur.

Solutions :

- deployer le backend ;
- utiliser une IP LAN ;
- utiliser Cloudflare Tunnel ou ngrok ;
- utiliser `10.0.2.2` uniquement depuis un emulateur Android.

## Probleme courant : dashboard vide

Verifier :

1. `VITE_SUPABASE_URL` et `VITE_SUPABASE_ANON_KEY`.
2. La table `daily_summaries`.
3. Les policies RLS de lecture.
4. La console navigateur.

## Probleme courant : ingestion refusee

Verifier :

1. Le backend tourne.
2. L'URL est correcte.
3. Le header `x-api-key` correspond a `INGEST_API_KEY`.
4. Supabase service role key est correcte.
5. Les tables existent.

## Probleme courant : donnees Android absentes

Verifier :

1. Zepp ecrit dans Health Connect.
2. Les permissions Health Connect sont accordees.
3. La date lue est la veille.
4. L'app nutrition ecrit bien les macros dans Health Connect.
5. Health Connect a des donnees visibles dans ses reglages.
