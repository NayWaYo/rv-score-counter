---
story_id: "9.1"
story_key: "9-1-scanner-les-atouts-du-papayoo"
epic: 9
status: done
created: 2026-10-22
baseline_commit: 65f04ac
---

# Story 9.1 : Scanner les atouts du Papayoo

**Epic :** 9 - Comptage par la caméra
**Story ID :** 9.1
**Status :** done

## Story

En tant que joueur de Papayoo,
Je veux photographier mes atouts tenus en éventail,
Afin que l'application compte mes points à ma place.

## Acceptance Criteria

1. **Given** la saisie ou la correction d'une Manche d'un Jeu dont les cartes comptent leur numéro (Papayoo : les Payoo 1 à 20, joués avec les atouts 1 à 20 d'un jeu de Tarot) **When** le formulaire des scores s'affiche **Then** chaque Participant a, à côté de son champ, un bouton caméra de 48dp décrit « Scanner les atouts de <prénom> » ; les Jeux sans de telles cartes (Belote) n'en ont pas.

2. **When** je touche ce bouton **Then** l'appareil photo du téléphone s'ouvre par une intention système, sans que l'application ne demande de permission (NFR-4 reste vrai) ; revenir sans photo ne change rien.

3. **Given** une photo prise **When** l'application la lit **Then** un état « Lecture des atouts… » s'affiche, puis une fenêtre de confirmation présente les 20 numéros en pastilles à cocher, ceux reconnus déjà cochés ; toucher une pastille la coche ou la décoche ; la somme des atouts cochés est affichée ; si rien n'est reconnu, un message invite à cocher les atouts à la main.

4. **And** un atout déjà confirmé pour un autre Participant de la même Manche est marqué « chez <prénom> » ; tant qu'il est coché, la confirmation est bloquée avec un message nommant ce Participant.

5. **And** confirmer écrit dans le champ du Participant la somme de ses atouts, plus les points du 7 (40) s'il est le détenteur choisi dans « Qui a eu le 40 ? » ; changer ensuite le détenteur recalcule les scores issus d'un scan ; modifier le champ à la main oublie les atouts scannés de ce Participant ; « Reprendre la photo » relance l'appareil photo et « Annuler » ne change rien.

6. **And** la lecture se fait sur le téléphone, hors ligne (ML Kit, modèle embarqué) : plusieurs rotations de la photo, découpage des numéros fusionnés, rejet des numéros lus à l'envers et vote par position ; seuls les nombres de 1 à 20 sont retenus, chacun une fois. La photo est supprimée une fois lue.

7. **And** les 11 photos de référence (éventails de 1 à 13 atouts, prises par l'utilisateur le 2026-10-22) donnent exactement leurs atouts dans un test instrumenté.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 1, 3-6)
  - [x] `GameEngine.faceValueCards` (Papayoo : 1..20, null par défaut)
  - [x] Lecture d'atouts : symboles → numéros, orientation, vote (`domain/scan/`), interface `TrumpReader`
  - [x] Tests JVM
- [x] **T2 - Lecture ML Kit** (AC: 6, 7)
  - [x] `implementation` ML Kit text-recognition (modèle embarqué) ; `MlKitTrumpReader` dans `data/scan/`, liaison Hilt
  - [x] Test instrumenté sur les 11 photos de référence
- [x] **T3 - Prise de photo** (AC: 2)
  - [x] `FileProvider` sur un dossier de cache, `ActivityResultContracts.TakePicture` dans la Route
- [x] **T4 - Saisie et confirmation** (AC: 1, 3-5)
  - [x] État de scan dans `RoundEntryUiState`, ViewModel (atouts par Participant, conflits, 40)
  - [x] Bouton caméra dans `ScoreField`, fenêtre de confirmation
  - [x] Tests JVM du ViewModel, tests Compose
- [x] **T5 - Validation** : `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, tests instrumentés API 36

## Dev Notes

- Faisabilité mesurée avant la story (test jetable `CardScanSpikeTest`) : lecture brute à plusieurs angles = toutes les cartes mais des faux positifs (6 lu 9, 18 lu 8, « 01 ») ; l'algorithme final donne 66/66 atouts sans faux positif sur les 11 photos. Seuils réglés sur ces mêmes photos : risque de sur-ajustement, compensé par la confirmation obligatoire.
- Les numéros des atouts sont grands, noirs, encadrés, en haut à gauche de chaque carte : c'est la partie visible d'un éventail. Le 7 de la couleur tirée n'est pas lu : « Qui a eu le 40 ? » existe déjà.
- Prise de photo par l'application caméra du système (`TakePicture`) plutôt que CameraX : aucune permission, moins de code ; un aperçu en direct pourra venir plus tard.
- `ImageDecoder` applique l'orientation EXIF ; la photo est réduite à 2000 px de grand côté, la taille des photos de référence.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 9)
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/round/`

## Dev Agent Record

### Debug Log References

- `testDebugUnitTest` : 380 tests, 0 échec (dont 8 `TrumpReadingTest`, 1 `PapayooEngineTest`, 9 `RoundEntryViewModelTest` du scan).
- `connectedDebugAndroidTest` sur `Pixel_API_36` : 197 tests, 0 échec, dont `MlKitTrumpReaderTest` (les 11 photos de référence lues exactement) et 5 tests Compose du scan. Après le retrait de `INTERNET`, `MlKitTrumpReaderTest` a été relancé seul (2/2) ; la suite complète ne l'a pas été.
- Sans `INTERNET`, l'envoi de statistiques de ML Kit forcé par `cmd jobscheduler run -f` échoue dans son propre exécuteur (`SecurityException` journalisée), sans faire tomber le processus.
- `assembleRelease` : APK de 41,6 Mo (25,8 Mo avant la story ; 71,6 Mo avec les 4 ABI) ; permissions : `ACCESS_NETWORK_STATE` et la permission interne d'AndroidX, aucune permission demandée à l'utilisateur.
- Non vérifié : la prise de photo par l'application caméra du téléphone, le `FileProvider` et la fenêtre de confirmation sur un appareil réel ; la durée de lecture sur le Pixel (2 à 5 s par photo sur l'émulateur).

### Completion Notes List

- Le test jetable `CardScanSpikeTest` est devenu `MlKitTrumpReaderTest` ; l'algorithme réglé pendant la faisabilité est dans le domaine (`TrumpReading`), testé en JVM sur des symboles synthétiques.
- La règle « somme des atouts + 40 du 7 » est dans le moteur (`GameEngine.scoreOfCards`, `faceValueCards`), pas dans l'écran.
- Choix hors critères : l'APK release ne contient plus que l'ABI `arm64-v8a` (le debug garde toutes les ABI pour l'émulateur) ; la permission `INTERNET` ajoutée par ML Kit est retirée du manifeste.
- Les atouts scannés ne survivent pas à la mort du processus (les scores, eux, restent dans le `SavedStateHandle`).

### File List

- `app/build.gradle.kts`, `gradle/libs.versions.toml` (ML Kit text-recognition 16.0.1, ABI release)
- `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/scan_paths.xml`, `app/src/main/res/drawable/ic_photo_camera.xml`, `app/src/main/res/values/strings.xml`
- `domain/engine/GameEngine.kt`, `game/papayoo/PapayooEngine.kt`
- `domain/scan/TrumpReader.kt`, `domain/scan/TrumpReading.kt`
- `data/scan/MlKitTrumpReader.kt`, `di/ScanModule.kt`
- `ui/components/ScoreField.kt`
- `ui/screens/round/RoundEntryRoute.kt`, `RoundEntryScreen.kt`, `RoundEntryUiState.kt`, `RoundEntryViewModel.kt`, `TrumpScanDialog.kt`
- Tests : `domain/scan/TrumpReadingTest.kt`, `game/papayoo/PapayooEngineTest.kt`, `ui/screens/round/RoundEntryViewModelTest.kt`, `androidTest/.../data/scan/MlKitTrumpReaderTest.kt`, `androidTest/.../ui/screens/round/RoundEntryScreenTest.kt`, `androidTest/assets/scan/*.webp` (11 photos)
- `_bmad-output/planning-artifacts/epics.md`, `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-10-22 : story créée et implémentée (scan des atouts du Papayoo).
