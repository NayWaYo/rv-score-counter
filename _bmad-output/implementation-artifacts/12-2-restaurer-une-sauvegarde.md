---
story_id: "12.2"
story_key: "12-2-restaurer-une-sauvegarde"
epic: 12
status: done
created: 2026-10-23
baseline_commit: 8a50889
---

# Story 12.2 : Restaurer une sauvegarde

**Epic :** 12 - Sauvegarde et restauration
**Story ID :** 12.2
**Status :** done

## Story

En tant qu'utilisateur,
Je veux réimporter une sauvegarde,
Afin de retrouver mon historique et mon palmarès.

## Acceptance Criteria

1. **Given** l'écran « Sauvegarde » **Then** un bouton « Restaurer », actif même sans aucune donnée (cas d'un nouveau téléphone), ouvre le sélecteur de fichiers d'Android ; annuler le sélecteur ne fait rien.

2. **When** je choisis un fichier **Then** l'application le lit et le vérifie en entier avant toute question ; s'il est valide, une confirmation demande « Remplacer toutes les données actuelles par la sauvegarde du JJ/MM/AAAA (N joueurs et M parties) ? », avec « Remplacer » et « Annuler ».

3. **And** après « Remplacer », toutes les données (Joueurs, Parties, Manches…) sont remplacées par celles du fichier en une seule transaction Room ; un message confirme la restauration et l'écran affiche le nouveau contenu ; le thème n'est pas touché.

4. **And** si l'écriture échoue, rien n'est modifié et un message le dit.

5. **And** un fichier refusé ne modifie rien et affiche la raison : ce n'est pas une sauvegarde (pas du JSON, structure inconnue), elle vient d'une version plus récente de l'application (format supérieur à 1), elle est abîmée (identifiant ou valeur invalide, doublon, Manche, score ou Participant qui renvoie à une Partie, un Participant ou un Joueur absent), ou le fichier ne peut pas être lu.

6. **And** « Exporter » et « Restaurer » sont inactifs pendant une lecture, un export ou une restauration.

7. **And** la vérification est couverte par des tests JVM (chaque refus, fichier valide de chaque Jeu) et la restauration par des tests instrumentés Room (remplacement complet, retour arrière en cas d'erreur, fichier refusé sans modification).

## Tasks / Subtasks

- [x] **T1 - Vérification du fichier** (AC: 2, 5, 7)
  - [x] `data/backup` : lecture du numéro de format avant le décodage complet, puis vérification des lignes (identifiants, valeurs d'enum par les mappings Room existants, Jeux connus, doublons, références)
  - [x] Tests JVM de chaque refus et d'un fichier valide par Jeu
- [x] **T2 - Données** (AC: 3, 4, 5)
  - [x] `BackupReading` (domaine, type scellé) ; `BackupRepository.read` et `restore`
  - [x] DAO : suppression de toutes les Parties et Joueurs, insertions en lot ; remplacement en une transaction
  - [x] Tests instrumentés Room
- [x] **T3 - Interface** (AC: 1, 2, 3, 4, 5, 6)
  - [x] « Restaurer », lanceur `OpenDocument`, fenêtre de confirmation, messages ; état du ViewModel revu (une opération à la fois, un message)
  - [x] Tests JVM du ViewModel, tests Compose
- [x] **T4 - Validation** : `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` API 36

## Dev Notes

- Choix validé le 2026-10-23 : la restauration **remplace** tout, après confirmation ; pas de fusion.
- Le fichier est relu et revérifié au moment de remplacer (le domaine ne transporte pas les lignes Room) : la confirmation n'engage que le fichier tel qu'il est à cet instant.
- Le format est lu avant le décodage complet : un fichier d'un format plus récent peut avoir des champs inconnus, il doit être refusé comme « plus récent », pas comme « pas une sauvegarde ».
- Supprimer les Parties emporte Participants, Équipes, Manches, scores et Manches de Belote (CASCADE) ; les Joueurs sont supprimés ensuite (la clé étrangère des Participants n'a pas de CASCADE).
- `nameKey` est recalculée par `playerNameKey` ; deux Joueurs de même clé rendent le fichier abîmé (index unique).
- Aucune évolution du schéma Room.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 12, Story 12.2)
- Story 12.1 (`12-1-exporter-une-sauvegarde.md`)

## Dev Agent Record

### Debug Log References

- JVM : `./gradlew testDebugUnitTest` → 508 tests, 0 échec (495 avant la story) ; nouveaux : `BackupReaderTest` (7), `BackupViewModelTest` (13, réécrit : 7 → 13).
- Build : `kspDebugKotlin assembleDebug compileDebugAndroidTestKotlin` OK.
- Instrumentés API 36 (`Pixel_API_36`, `ANDROID_SERIAL=emulator-5554`) : `connectedDebugAndroidTest` → 255 tests, 0 échec (251 avant) ; `BackupRepositoryImplTest` 3 → 7 (lecture sans modification, remplacement complet, fichiers refusés sans modification, retour arrière d'une écriture en échec), `BackupScreenTest` 6 (revu).
- `git diff --check` propre.
- Non vérifié : une restauration réelle sur le téléphone depuis Téléchargements ou Google Drive.

### Completion Notes List

- `readBackupFile` lit d'abord le numéro de format (un fichier plus récent est refusé comme tel même s'il a des champs inconnus), décode ensuite strictement (champ inconnu = pas une sauvegarde), puis `checkConsistent` vérifie les lignes : identifiants UUID et valeurs d'enum par les mappings Room existants, Jeux connus, prénom non vide, doublons (identifiants, prénoms à la casse près, places, numéros de Manche, scores, Équipes) et références (Joueur des Participants et du preneur, détenteur du 7 et scores dans la même Partie). Une Manche de Papayoo exige sa couleur, une Manche de Jeu libre n'en a pas.
- Le repository relit et revérifie le fichier au moment de remplacer ; le remplacement supprime les Parties (CASCADE) puis les Joueurs et réinsère tout dans une seule transaction.
- L'écran : « Restaurer » toujours actif hors opération, confirmation datée (JJ/MM/AAAA) avec les nombres du fichier, une barre de progression pendant une opération, un seul message pour la dernière opération (vert pour un succès, rouge sinon).
- `BackupUiState.Loaded` remplace `isExporting` / `exportResult` (Story 12.1) par `operation` et `message`.

### File List

- `app/src/main/java/com/rvetiveau/rvscorecount/data/backup/BackupReader.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/repository/BackupRepositoryImpl.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/data/dao/PartyDao.kt`, `PlayerDao.kt`, `RoundDao.kt`, `BeloteRoundDao.kt` (insertions en lot, suppression de tout)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/BackupReading.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/repository/BackupRepository.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/ReadBackupUseCase.kt`, `RestoreBackupUseCase.kt` (nouveaux)
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/backup/BackupUiState.kt`, `BackupViewModel.kt`, `BackupRoute.kt`, `BackupScreen.kt`
- `app/src/main/res/values/strings.xml`
- Tests : `app/src/test/.../data/backup/BackupReaderTest.kt` (nouveau), `app/src/test/.../ui/screens/backup/BackupViewModelTest.kt`, `app/src/androidTest/.../data/repository/BackupRepositoryImplTest.kt`, `app/src/androidTest/.../ui/screens/backup/BackupScreenTest.kt`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-10-23 : Story 12.2 implémentée (vérification complète du fichier, confirmation, remplacement en une transaction), testée JVM et API 36.
