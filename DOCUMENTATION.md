# Documentation

Ce fichier est l'index racine du projet.

## Lecture recommandee

1. [ARCHITECTURE.md](ARCHITECTURE.md) pour comprendre le systeme.
2. [SUPABASE.md](SUPABASE.md) pour preparer la base.
3. [BACKEND.md](BACKEND.md) pour lancer l'API et les jobs.
4. [DASHBOARD.md](DASHBOARD.md) pour utiliser l'interface.
5. [ANDROID_EXPORTER.md](ANDROID_EXPORTER.md) pour connecter Health Connect.
6. [OPERATIONS.md](OPERATIONS.md) pour la routine quotidienne.
7. [ROADMAP.md](ROADMAP.md) pour la suite.

## Documentation racine

- [README.md](README.md) : presentation rapide.
- [ARCHITECTURE.md](ARCHITECTURE.md) : vue d'ensemble et responsabilites.
- [DATA_MODEL.md](DATA_MODEL.md) : format des donnees, metriques et payloads.
- [SUPABASE.md](SUPABASE.md) : tables, migrations, RLS, cron.
- [BACKEND.md](BACKEND.md) : API Node.js, routes, jobs, configuration.
- [DASHBOARD.md](DASHBOARD.md) : React/Vite, composants, donnees affichees.
- [ANDROID_EXPORTER.md](ANDROID_EXPORTER.md) : app Android Health Connect.
- [OPERATIONS.md](OPERATIONS.md) : commandes, debug et routine.
- [ROADMAP.md](ROADMAP.md) : prochaines etapes.
- [todo.md](todo.md) : checklist operationnelle.

## Documentation complementaire

- [docs/api.md](docs/api.md) : contrat HTTP detaille.
- [docs/android-exporter.md](docs/android-exporter.md) : notes Android detaillees.
- [docs/setup-android-supabase.md](docs/setup-android-supabase.md) : mise en place Android + Supabase.
- [docs/workflow-zepp.md](docs/workflow-zepp.md) : workflow Zepp.
- [docs/custom-android-exporter.md](docs/custom-android-exporter.md) : notes d'implementation Android.
- [docs/adr/001-architecture.md](docs/adr/001-architecture.md) : decision d'architecture initiale.

## Verification rapide

```bash
make check
```

## Commandes utiles

```bash
npm run dev:backend
npm run dev:dashboard
make test-ingest
make example
```
