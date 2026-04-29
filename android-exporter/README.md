# Personal Trainer Android Exporter

Application Android native qui sert de pont entre Health Connect et le backend Node.js.

Elle fait :

- lecture Health Connect ;
- aggregation des donnees de la veille ;
- envoi JSON vers `/api/ingest/health-connect` ;
- synchronisation manuelle ;
- planification quotidienne via WorkManager.

## Donnees exportees

L'app tente d'exporter ces metriques quand les permissions et les donnees existent :

- `steps`
- `sleep_duration_min`
- `active_energy_kcal`
- `weight_kg`
- `heart_rate_resting_bpm`
- `workout_duration_min`
- `workout_count`
- `calories_intake_kcal`
- `protein_g`
- `carbs_g`
- `fat_g`
- `fiber_g`
- `water_ml`

## Pre-requis telephone

- Android 9 minimum pour Health Connect.
- Health Connect disponible :
  - Android 14+ : integre au systeme ;
  - Android 13 et moins : installer Health Connect depuis Google Play.
- Zepp doit ecrire les donnees vers Health Connect.
- Ton app nutrition doit ecrire ses donnees vers Health Connect si tu veux les macros/calories.

## Build

Ouvre le dossier `android-exporter/` dans Android Studio.

N'ouvre pas la racine `PersonnalTrainer/` dans Android Studio. Cette racine est un workspace complet Node.js + Android ; Android Studio doit ouvrir le sous-dossier Gradle `android-exporter/`.

Puis :

1. Laisse Android Studio synchroniser Gradle.
2. Branche ton telephone Android.
3. Lance la configuration `app`.

Si Android Studio demande d'installer un SDK Android ou Gradle, accepte.

Si la synchronisation echoue sur `dl.google.com` ou `repo.maven.apache.org`, verifie ta connexion, ton proxy ou les reglages reseau Android Studio. Le projet a un Gradle wrapper, donc Android Studio doit utiliser `./gradlew`.

## Configuration dans l'app

Dans l'ecran principal :

1. Saisis l'URL du backend.
2. Saisis `INGEST_API_KEY`.
3. Appuie sur `Sauvegarder`.
4. Appuie sur `Autoriser` pour accorder les permissions Health Connect.
5. Appuie sur `Synchroniser hier maintenant`.
6. Verifie dans Supabase que les tables se remplissent.

## URL backend selon le contexte

Depuis un emulateur Android :

```text
http://10.0.2.2:8787
```

Depuis un vrai telephone :

- utilise une URL publique HTTPS ;
- ou l'adresse LAN de ton ordinateur si le backend ecoute sur le reseau ;
- ou un tunnel type Cloudflare Tunnel/ngrok.

Exemple LAN :

```text
http://192.168.1.42:8787
```

Pour un usage quotidien, privilegie HTTPS.

## Planification

Le bouton `Planifier chaque matin` programme un `WorkManager` quotidien autour de 08:20.

Important : la lecture Health Connect en arriere-plan depend de la disponibilite de la fonctionnalite sur le telephone et de la permission `READ_HEALTH_DATA_IN_BACKGROUND`. Si elle n'est pas disponible, la synchronisation manuelle fonctionne quand l'app est ouverte, mais la sync automatique peut echouer.

## Debug

Si l'envoi echoue :

1. Teste le backend depuis ton ordinateur :

```bash
curl http://127.0.0.1:8787/health
```

2. Depuis le telephone, verifie que l'URL backend est joignable.
3. Verifie que `x-api-key` correspond a `INGEST_API_KEY`.
4. Verifie dans Health Connect que Zepp et ton app nutrition ecrivent bien les donnees.
5. Regarde Logcat avec le filtre `personaltrainer`.
