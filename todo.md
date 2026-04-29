# TODO - Personal Trainer Workflow

## 1. Valider Supabase

- [ ] Verifier que les tables existent :
  - [ ] `daily_metric_entries`
  - [ ] `daily_summaries`
  - [ ] `weekly_reports`
  - [ ] `metric_sources`
- [ ] Lancer le backend :

```bash
npm run dev:backend
```

- [ ] Tester l'ingestion :

```bash
make test-ingest
```

- [ ] Verifier dans Supabase que `daily_metric_entries` se remplit.
- [ ] Verifier dans Supabase que `daily_summaries` se remplit.
- [ ] Recharger le dashboard et confirmer que les donnees s'affichent.

## 2. Choisir la methode Android

Health Connect ne peut pas etre lu directement par Node.js. Il faut un pont Android.

Option recommandee : creer une petite app Android custom qui fait :

- [x] Creer le squelette Android dans `android-exporter/`.
- [x] Lecture Health Connect.
- [x] Aggregation des donnees de la veille.
- [x] Envoi JSON vers `/api/ingest/health-connect`.
- [x] Execution automatique avec WorkManager.
- [ ] Installer et tester l'app sur le telephone.
- [ ] Accorder les permissions Health Connect.
- [ ] Tester une synchronisation manuelle.
- [ ] Verifier les lignes dans Supabase.
- [ ] Activer la planification quotidienne.

Donnees a recuperer en premier :

- [ ] Pas.
- [ ] Sommeil.
- [ ] Calories actives.
- [ ] Poids.
- [ ] Frequence cardiaque au repos si disponible.
- [ ] Seances sportives.
- [ ] Calories consommees.
- [ ] Proteines.
- [ ] Glucides.
- [ ] Lipides.
- [ ] Fibres.
- [ ] Eau.

## 3. Rendre le backend accessible au telephone

Le telephone ne pourra pas appeler `localhost`.

Options :

- [ ] Temporaire : Cloudflare Tunnel ou ngrok.
- [ ] Durable : Render, Railway, Fly.io, VPS ou Supabase Edge Functions.

Decision recommandee :

- [ ] Choisir un hebergement stable pour le backend.
- [ ] Configurer les variables d'environnement de production.
- [ ] Tester `/health` depuis le telephone.
- [ ] Tester `/api/ingest/health-connect` depuis le telephone.

## 4. Securiser l'ingestion

Actuellement, l'API utilise `x-api-key`. C'est suffisant pour demarrer.

Ameliorations :

- [ ] Prevoir une rotation de `INGEST_API_KEY`.
- [ ] Ajouter un rate limit.
- [ ] Ajouter des logs d'ingestion.
- [ ] Ajouter un endpoint `/api/ingest/status`.
- [ ] Creer une table `sync_runs` pour tracer les imports et erreurs.

## 5. Automatiser les jobs

Si le backend tourne 24/7 :

```env
RUN_CRON=true
DAILY_CRON=20 8 * * *
WEEKLY_REPORT_CRON=0 9 * * 0
```

- [ ] Confirmer que le cron quotidien recalcule `daily_summaries`.
- [ ] Confirmer que le cron du dimanche genere `weekly_reports`.

Si le backend est serverless ou pas toujours actif :

- [ ] Utiliser Supabase Cron avec `supabase/migrations/002_optional_supabase_cron.sql`.
- [ ] Configurer l'URL publique du backend.
- [ ] Configurer le header `x-api-key`.
- [ ] Tester le job quotidien.
- [ ] Tester le job hebdomadaire.

## 6. Ajouter les vraies cibles

Aujourd'hui, les objectifs sont partiellement codes dans le dashboard et le backend.

A faire :

- [ ] Creer une table `user_goals`.
- [ ] Stocker calories cible.
- [ ] Stocker proteines cible.
- [ ] Stocker pas cible.
- [ ] Stocker sommeil cible.
- [ ] Stocker eau cible.
- [ ] Stocker poids cible.
- [ ] Faire lire ces objectifs au backend.
- [ ] Faire lire ces objectifs au dashboard.

## 7. Fiabiliser l'analyse perte de poids

L'analyse doit se baser sur les moyennes, pas sur une seule journee.

A ajouter :

- [ ] Moyenne poids 7 jours.
- [ ] Tendance poids 14 jours.
- [ ] Deficit estime.
- [ ] Adherence calories.
- [ ] Adherence proteines.
- [ ] Adherence pas.
- [ ] Adherence sommeil.
- [ ] Correlation sommeil court -> calories plus hautes.
- [ ] Correlation pas bas -> poids stagnant.

## 8. Mettre en place la routine quotidienne

Chaque matin :

- [ ] Zepp synchronise.
- [ ] Health Connect recoit les donnees.
- [ ] L'app Android exporte la veille.
- [ ] Supabase recoit les metriques.
- [ ] Dashboard mis a jour.
- [ ] Verifier uniquement :
  - [ ] Poids moyen 7 jours.
  - [ ] Calories moyennes.
  - [ ] Proteines.
  - [ ] Pas.
  - [ ] Sommeil.

Chaque dimanche :

- [ ] Rapport automatique.
- [ ] Actions recommandees pour la semaine.
- [ ] Ajustement calories ou pas si le poids ne bouge pas.

## 9. Ajouter les alertes

Alertes utiles au quotidien :

- [ ] Notification si pas d'import depuis 24 h.
- [ ] Notification dimanche avec resume.
- [ ] Alerte si proteines trop basses 3 jours.
- [ ] Alerte si sommeil bas + calories hautes.
- [ ] Alerte si poids moyen ne baisse pas sur 14 jours.

## 10. Prochaine etape concrete

Creer l'app Android custom Health Connect exporter.

Version minimale :

- [ ] Ecran de configuration : URL backend.
- [ ] Ecran de configuration : API key.
- [ ] Bouton `Tester l'envoi`.
- [ ] Lecture manuelle de la journee precedente.
- [ ] Envoi vers `/api/ingest/health-connect`.
- [ ] WorkManager automatique tous les matins.

Une fois ce pont Android en place, le systeme devient utilisable au quotidien.
