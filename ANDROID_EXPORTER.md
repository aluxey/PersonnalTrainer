# Android Exporter

## Role

L'app Android est le pont entre Health Connect et le backend.

Elle se trouve dans :

```text
android-exporter/
```

## Pourquoi elle existe

Health Connect est une API locale Android. Le backend Node.js ne peut pas lire directement les donnees du telephone.

L'app Android doit donc :

- demander les permissions Health Connect ;
- lire les donnees de la veille ;
- envoyer un JSON au backend ;
- planifier la sync quotidienne.

## Stack

- Kotlin
- Jetpack Compose
- Health Connect SDK
- WorkManager
- HttpURLConnection pour l'envoi HTTP

## Fichiers principaux

- `android-exporter/app/src/main/java/com/personaltrainer/exporter/MainActivity.kt`
- `android-exporter/app/src/main/java/com/personaltrainer/exporter/HealthConnectExporter.kt`
- `android-exporter/app/src/main/java/com/personaltrainer/exporter/BackendClient.kt`
- `android-exporter/app/src/main/java/com/personaltrainer/exporter/DailySyncWorker.kt`
- `android-exporter/app/src/main/java/com/personaltrainer/exporter/SyncScheduler.kt`
- `android-exporter/app/src/main/AndroidManifest.xml`

## Donnees lues

L'app tente de lire :

- pas ;
- sommeil ;
- calories actives ;
- poids ;
- frequence cardiaque au repos ;
- duree des entrainements ;
- nombre d'entrainements ;
- calories nutrition ;
- proteines ;
- glucides ;
- lipides ;
- fibres ;
- eau.

Les donnees nutrition seront presentes uniquement si une app nutrition ecrit dans Health Connect.

## Installation

1. Ouvrir `android-exporter/` dans Android Studio.
2. Laisser Android Studio synchroniser Gradle.
3. Brancher le telephone.
4. Lancer l'app `app`.
5. Renseigner l'URL backend.
6. Renseigner `INGEST_API_KEY`.
7. Appuyer sur `Sauvegarder`.
8. Appuyer sur `Autoriser`.
9. Appuyer sur `Synchroniser hier maintenant`.
10. Verifier Supabase et le dashboard.

## URL backend

Emulateur Android :

```text
http://10.0.2.2:8787
```

Telephone physique :

- URL publique HTTPS recommandee ;
- ou adresse LAN de l'ordinateur ;
- ou tunnel Cloudflare/ngrok.

Exemple LAN :

```text
http://192.168.1.42:8787
```

## Planification

Le bouton `Planifier chaque matin` programme un WorkManager quotidien autour de 08:20.

La lecture en arriere-plan depend :

- de la version Android ;
- de la version Health Connect ;
- de la permission `READ_HEALTH_DATA_IN_BACKGROUND`.

Si cette fonctionnalite n'est pas disponible, la sync manuelle reste possible.

## Debug

Verifier :

- Health Connect est installe et disponible ;
- Zepp ecrit bien dans Health Connect ;
- les permissions sont accordees ;
- l'URL backend est joignable depuis le telephone ;
- l'API key est correcte ;
- Supabase recoit les lignes.

## Documentation detaillee

Voir aussi :

- [android-exporter/README.md](android-exporter/README.md)
- [docs/android-exporter.md](docs/android-exporter.md)
