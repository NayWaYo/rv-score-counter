---
story_id: "12.1"
story_key: "12-1-exporter-une-sauvegarde"
epic: 12
status: done
created: 2026-10-23
baseline_commit: 845de69
---

# Story 12.1 : Exporter une sauvegarde

**Epic :** 12 - Sauvegarde et restauration
**Story ID :** 12.1
**Status :** done

## Story

En tant qu'utilisateur,
Je veux enregistrer toutes mes Parties et mes Joueurs dans un fichier,
Afin de ne pas les perdre si je change de téléphone.

## Acceptance Criteria

1. **Given** l'accueil **When** il s'affiche **Then** une icône « Sauvegarde » (48dp, décrite « Sauvegarde » pour TalkBack) dans l'en-tête, à côté de « Joueurs », ouvre l'écran « Sauvegarde ».

2. **And** l'écran indique ce qui sera sauvegardé : nombre de Joueurs et de Parties (en cours comprises), et propose « Exporter » ; sans aucun Joueur ni Partie, il dit qu'il n'y a rien à sauvegarder et « Exporter » est inactif.

3. **When** je touche « Exporter » **Then** le sélecteur de fichiers d'Android propose `rv-score-counter-AAAA-MM-JJ.json` (date du jour) ; annuler le sélecteur ne fait rien.

4. **And** le fichier écrit est un JSON qui contient un numéro de format (1), la date de sauvegarde, les Joueurs et toutes les Parties avec leurs Participants, Équipes, Manches et scores, et Manches de Belote ; le thème n'est pas sauvegardé.

5. **And** un message confirme l'export, ou signale l'échec d'écriture ; « Exporter » est inactif pendant l'écriture.

6. **And** la conversion entre la base et le fichier est couverte par des tests JVM : aller-retour sans perte pour une Partie de Papayoo, de Belote et de Jeu libre, valeurs facultatives absentes comprises.

## Tasks / Subtasks

- [x] **T1 - Format de fichier** (AC: 4, 6)
  - [x] `data/backup/BackupFile.kt` : modèle `@Serializable` (format, date, Joueurs, Parties imbriquant Participants, Équipes, Manches avec scores, Manches de Belote), conversion depuis et vers les lignes Room, encodage JSON
  - [x] Tests JVM d'aller-retour par Jeu
- [x] **T2 - Données** (AC: 2, 4)
  - [x] DAO : lecture de toutes les lignes de chaque table, comptes observés des Joueurs et Parties
  - [x] `BackupRepository` (domaine) et `BackupRepositoryImpl` : lecture cohérente en transaction, écriture dans le document choisi (`ContentResolver`, mode `wt`)
  - [x] Test instrumenté Room : l'export d'une base réelle relit toutes les lignes
- [x] **T3 - Interface** (AC: 1, 2, 3, 5)
  - [x] Icône Sauvegarde sur l'accueil, route `Backup`, `BackupScreen`, `BackupViewModel`, lanceur `CreateDocument`
  - [x] Tests JVM du ViewModel, tests Compose de l'écran et de l'accueil
- [x] **T4 - Validation** : `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` API 36

## Dev Notes

- Choix validés le 2026-10-23 : tout remplacer à la restauration (Story 12.2), accès par une icône de l'en-tête de l'accueil, toutes les Parties et tous les Joueurs, pas le thème.
- Le fichier reflète les tables Room, pas les modèles du domaine : `Round.toDomain` exige une couleur du 7 et échouerait sur une Manche de Jeu libre, et la restauration doit réécrire exactement les mêmes lignes. Les valeurs d'enum restent leur `name`, comme en base ; la Story 12.2 les validera à la lecture.
- Le format a son propre numéro (`format = 1`), indépendant de la version Room : il ne change que si le fichier change.
- La conversion vers le fichier et retour (`toBackupFile` / `toTables`) vit dans `data/backup` ; la Story 12.2 réutilisera `toTables` et le décodage.
- Le sélecteur de fichiers (Storage Access Framework) ne demande aucune permission ; l'URI choisie passe au repository en `String` pour garder le domaine sans Android.
- Aucune évolution du schéma Room.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 12, Story 12.1)
- `_bmad-output/project-context.md`

## Dev Agent Record

### Debug Log References

- JVM : `./gradlew testDebugUnitTest` → 495 tests, 0 échec (479 avant la story) ; nouveaux : `BackupFileTest` (8), `BackupViewModelTest` (7), `HomeViewModelTest` (+1).
- Build : `kspDebugKotlin assembleDebug compileDebugAndroidTestKotlin` OK.
- Instrumentés API 36 (`Pixel_API_36`, `ANDROID_SERIAL=emulator-5554`) : `connectedDebugAndroidTest` → 251 tests, 0 échec (241 avant) ; nouveaux : `BackupRepositoryImplTest` (3, export vers un vrai fichier via `Uri.fromFile`), `BackupScreenTest` (6), `HomeScreenTest` (+1).
- `git diff --check` propre.
- Non vérifié : l'export réel depuis le sélecteur de fichiers d'Android (Téléchargements, Google Drive) sur le téléphone ; à essayer avec le build release.

### Completion Notes List

- Le fichier reflète les tables Room (format 1) : chaque Partie imbrique ses Participants, Équipes, Manches avec leurs scores et Manches de Belote ; les Joueurs sont listés à part (un Joueur sans Partie est gardé). La clé `nameKey` n'est pas écrite, elle est recalculée à la relecture.
- La lecture se fait dans une transaction Room pour un instantané cohérent ; l'écriture ouvre le document en mode `wt` pour tronquer un fichier plus long réutilisé.
- `toTables` et `decodeBackupFile` existent déjà pour la Story 12.2, qui ajoutera la validation détaillée (enums, références) et la restauration.
- L'icône « Sauvegarde » est placée à gauche de « Joueurs » dans l'en-tête de l'accueil ; la restauration (Story 12.2) viendra sur le même écran.

### File List

- `app/src/main/java/com/rvetiveau/rvscorecount/data/backup/BackupFile.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/repository/BackupRepositoryImpl.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/dao/{Party,Player,Participant,Team,Round,RoundScore,BeloteRound}Dao.kt` (lectures complètes, comptes)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/BackupContent.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/repository/BackupRepository.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/ObserveBackupContentUseCase.kt`, `ExportBackupUseCase.kt` (nouveaux)
- `app/src/main/java/com/rvetiveau/rvscorecount/di/RepositoryModule.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/components/BackupButton.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/backup/BackupUiState.kt`, `BackupViewModel.kt`, `BackupRoute.kt`, `BackupScreen.kt` (nouveaux)
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/home/HomeScreen.kt`, `HomeRoute.kt`, `HomeViewModel.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/Routes.kt`, `NavGraph.kt`
- `app/src/main/res/values/strings.xml`, `app/src/main/res/drawable/ic_backup.xml` (nouveau)
- Tests : `app/src/test/.../data/backup/BackupFileTest.kt`, `app/src/test/.../ui/screens/backup/BackupViewModelTest.kt`, `app/src/test/.../ui/screens/home/HomeViewModelTest.kt`, `app/src/androidTest/.../data/repository/BackupRepositoryImplTest.kt`, `app/src/androidTest/.../ui/screens/backup/BackupScreenTest.kt`, `app/src/androidTest/.../ui/screens/home/HomeScreenTest.kt`
- `_bmad-output/planning-artifacts/epics.md` (Epic 12), `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-10-23 : Story 12.1 implémentée (export JSON de toute la base depuis l'écran « Sauvegarde »), testée JVM et API 36.
