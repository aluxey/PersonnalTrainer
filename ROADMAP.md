# Roadmap

## Court terme

- Tester l'app Android sur telephone.
- Valider la sync manuelle Health Connect -> backend.
- Rendre le backend accessible depuis le telephone.
- Activer la sync quotidienne WorkManager.
- Verifier les donnees pendant 7 jours.

## Moyen terme

- Ajouter une table `sync_runs` pour tracer les imports.
- Ajouter un endpoint `/api/ingest/status`.
- Ajouter un rate limit sur l'ingestion.
- Centraliser les objectifs dans une table `user_goals`.
- Faire lire les objectifs au backend et au dashboard.
- Ajouter des alertes si pas d'import depuis 24 h.

## Analyse perte de poids

- Moyenne poids 7 jours.
- Tendance poids 14 jours.
- Adherence calories.
- Adherence proteines.
- Adherence pas.
- Adherence sommeil.
- Detection des jours qui tirent la moyenne calorique.
- Correlation sommeil court -> calories plus hautes.
- Correlation pas bas -> poids stagnant.

## Dashboard

- Filtres 7 jours, 30 jours, 90 jours.
- Page nutrition detaillee.
- Page sommeil detaillee.
- Page activite detaillee.
- Page rapports hebdomadaires.
- Etats vides plus propres.
- Export CSV depuis Supabase.

## Android

- Ecran historique des dernieres syncs.
- Notification apres sync reussie ou echouee.
- Choix manuel de la date a exporter.
- Diagnostic des permissions Health Connect.
- Diagnostic des donnees disponibles par type.
- Support d'une URL backend de production.

## Backend

- Logs structures.
- Table `sync_runs`.
- Validation plus stricte du payload.
- Rotation de `INGEST_API_KEY`.
- Tests automatises.
- Deploiement production.

## Production

- Choisir hebergement backend.
- Configurer HTTPS.
- Configurer variables de production.
- Configurer monitoring.
- Configurer cron stable.
- Sauvegarder la base Supabase.
