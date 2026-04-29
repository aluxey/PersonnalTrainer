# Alternatives sans Android Studio

Si Android Studio ne parvient pas a ouvrir le projet, tu peux quand meme installer l'app Android sur ton telephone.

## Option A - GitHub Actions, recommandee

C'est l'option la plus simple si tu peux pousser le projet sur GitHub.

### Etapes

1. Pousse le repo sur GitHub.
2. Va dans l'onglet `Actions`.
3. Lance le workflow `Build Android Exporter APK`.
4. Attends la fin du job.
5. Telecharge l'artifact :

```text
personal-trainer-exporter-debug-apk
```

6. Recupere le fichier :

```text
app-debug.apk
```

7. Envoie-le sur ton telephone.
8. Ouvre le fichier APK sur le telephone.
9. Autorise l'installation depuis cette source si Android le demande.
10. Lance `Personal Trainer Exporter`.

Le workflow est dans :

```text
.github/workflows/android-exporter-apk.yml
```

## Option B - Build en ligne de commande

Cette option marche si tu as le SDK Android installe localement.

### Pre-requis

- Java 17.
- Android SDK.
- Android Platform Tools si tu veux installer via USB.
- `ANDROID_HOME` ou `ANDROID_SDK_ROOT` configure.

### Build

```bash
cd /home/aluxey/Workspace/PersonnalTrainer/android-exporter
./gradlew :app:assembleDebug
```

APK genere :

```text
android-exporter/app/build/outputs/apk/debug/app-debug.apk
```

### Installation via USB

Active sur le telephone :

1. Options developpeur.
2. Debogage USB.

Puis :

```bash
cd /home/aluxey/Workspace/PersonnalTrainer
sh scripts/install_android_exporter.sh
```

Ou directement :

```bash
adb install -r android-exporter/app/build/outputs/apk/debug/app-debug.apk
```

## Option C - Copier l'APK manuellement

Si tu as un APK construit par GitHub Actions ou par une autre machine :

1. Copie `app-debug.apk` sur ton telephone.
2. Ouvre-le depuis le gestionnaire de fichiers.
3. Autorise l'installation d'apps inconnues pour cette source.
4. Installe l'app.

## Option D - Utiliser une autre machine pour build

Tu peux ouvrir `android-exporter/` sur un autre ordinateur avec Android Studio, build l'APK, puis copier l'APK sur ton telephone.

Le backend, Supabase et le dashboard peuvent rester sur ta machine actuelle.

## Apres installation

Dans l'app Android :

1. Renseigne l'URL backend.
2. Renseigne `INGEST_API_KEY`.
3. Appuie sur `Sauvegarder`.
4. Appuie sur `Autoriser`.
5. Appuie sur `Synchroniser hier maintenant`.

## URL backend depuis le telephone

Ne mets pas `localhost`.

Utilise :

- une URL publique HTTPS ;
- une adresse LAN, par exemple `http://192.168.1.42:8787` ;
- un tunnel Cloudflare/ngrok.

Teste d'abord dans le navigateur du telephone :

```text
http://IP_DE_TON_PC:8787/health
```

Tant que cette URL ne repond pas depuis le telephone, l'app ne pourra pas envoyer les donnees.
