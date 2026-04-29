# Dashboard

## Role

Le dashboard React affiche les donnees stockees dans Supabase.

Il se trouve dans :

```text
dashboard/
```

## Stack

- React
- Vite
- Supabase JS client
- Recharts
- Lucide icons

## Commandes

Lancer :

```bash
npm run dev:dashboard
```

Builder :

```bash
npm run check --workspace dashboard
```

URL locale habituelle :

```text
http://localhost:5173
```

## Variables

Le dashboard lit les variables depuis le `.env` racine grace a `envDir: '..'` dans `dashboard/vite.config.js`.

```env
VITE_SUPABASE_URL=
VITE_SUPABASE_ANON_KEY=
```

## Donnees lues

Le dashboard lit :

- `daily_summaries`
- `weekly_reports`

Il ne lit pas directement `daily_metric_entries` pour rester rapide.

## Ecrans et blocs

Le dashboard contient :

- navigation laterale ;
- cartes KPI ;
- tendances 30 jours ;
- objectifs du jour ;
- repartition nutrition ;
- sommeil ;
- activite recente ;
- analyse intelligente ;
- dernier rapport hebdomadaire.

## Fichiers principaux

- `dashboard/src/App.jsx` : page principale.
- `dashboard/src/components/MetricTile.jsx` : carte KPI.
- `dashboard/src/styles/app.css` : design complet.
- `dashboard/src/lib/supabase.js` : client Supabase.

## Etats a surveiller

- Si aucune donnee n'apparait, verifier `daily_summaries`.
- Si le dashboard indique une erreur de variables, verifier `VITE_SUPABASE_URL` et `VITE_SUPABASE_ANON_KEY`.
- Si le build echoue, lancer `npm install` puis `npm run check --workspace dashboard`.

## Evolutions possibles

- Ajouter des filtres periode.
- Ajouter une page detail sommeil.
- Ajouter une page detail nutrition.
- Ajouter une page rapport hebdomadaire complet.
- Ajouter des skeleton loaders et meilleurs empty states.
