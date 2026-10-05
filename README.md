# WiFiBoost Pro (v0.1, socle fonctionnel)
Outil de **diagnostic** Wi-Fi Android. Il n'augmente PAS la puissance radio. Aucune donnée fictive : valeur indisponible = « Information non disponible ».

## Compiler
1. Android Studio (Koala+), JDK 17 : *Open* ce dossier (le wrapper Gradle est généré à l'ouverture).
2. `./gradlew assembleDebug` -> `app/build/outputs/apk/debug/app-debug.apk`
3. Tests : `./gradlew test`

## Implémenté
Dashboard réel (SSID*, RSSI, bande, canal, lien, état Internet), classification RSSI, stats min/max/moy, monitoring START/STOP (2 s, sans scan permanent), ping TCP (Internet/passerelle) avec jitter et perte, moteur de diagnostic, tests unitaires.
*SSID : permission localisation requise (restriction Android).

## Aussi implémenté (v0.2)
Scan réseaux + canaux/interférences, test de débit configurable, Room/historique (filtre, suppression, comparaison), heatmap tactile, Gaming/Streaming, optimisation guidée, rapport partageable, mode Démo, Confidentialité, build GitHub Actions.

## Compiler sans PC
Pousser sur GitHub : l'onglet Actions construit l'APK (artifact `WiFiBoostPro-debug-apk`).

## Non fait
Export PDF (partage texte seulement), notification/widget, test UI instrumentés, test Room, ping dans le rapport par cible.
