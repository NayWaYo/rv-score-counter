---
story_id: "10.1"
story_key: "10-1-joueurs-enregistres"
epic: 10
status: done
created: 2026-10-22
baseline_commit: 0c0886c
---

# Story 10.1 : Joueurs enregistrés

**Epic :** 10 - Profils de joueurs et palmarès
**Story ID :** 10.1
**Status :** done

## Story

En tant qu'utilisateur,
Je veux que l'application reconnaisse un joueur d'une Partie à l'autre,
Afin que son historique et ses statistiques le suivent.

## Acceptance Criteria

1. **Given** le schéma Room en version 4 **When** l'application passe en version 5 **Then** une table `players` (identifiant UUID, prénom, clé de prénom unique) apparaît, et `participants.playerId`, non nul, rattache chaque Participant à un Joueur par une clé étrangère.

2. **And** une migration 4 → 5 écrite à la main crée un Joueur par prénom distinct des Parties existantes, tous Jeux confondus (casse et espaces autour ignorés, orthographe de la Partie la plus récente), et y rattache tous les Participants ; Parties, Manches, scores et Équipes sont conservés ; testée sur un vrai fichier de version 4.

3. **And** créer une Partie de Papayoo ou de Belote, ou ajouter un Participant en cours de Partie, rattache chaque prénom au Joueur de même clé, ou crée ce Joueur, dans la même transaction que la Partie ou l'ajout.

4. **And** deux Participants d'une même Partie ne peuvent pas porter le même prénom (casse et espaces ignorés) : le domaine refuse la création ou l'ajout ; la création du Papayoo affiche « Ce prénom est déjà dans la partie », celle de la Belote bloque « Commencer » avec ce message, et l'ajout en cours de Partie affiche le même message.

5. **And** supprimer une Partie ne supprime aucun Joueur : les prénoms suggérés à la création viennent des Joueurs, du plus récemment joué au plus ancien ; un Joueur sans aucune Partie restante reste suggéré, en fin de liste, par ordre alphabétique (choix de l'utilisateur après essai sur le téléphone, 2026-10-22).

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 3, 4)
  - [x] `playerNameKey` public ; refus des prénoms en double dans `CreatePartyUseCase`, `CreateBelotePartyUseCase`, `AddParticipantUseCase`
  - [x] Tests JVM
- [x] **T2 - Données** (AC: 1-3, 5)
  - [x] `PlayerEntity`, `PlayerDao`, `participants.playerId`, version 5, `MIGRATION_4_5`, schéma 5 exporté
  - [x] Rattachement des Participants dans `PartyRepositoryImpl` et `BeloteRepositoryImpl`
  - [x] Prénoms suggérés lus dans `players`
- [x] **T3 - Interface** (AC: 4)
  - [x] Messages de prénom en double (création Papayoo, création Belote, ajout en cours de Partie)
- [x] **T4 - Tests instrumentés** : migration 4 → 5, repositories (rattachement, réutilisation, suppression de Partie)
- [x] **T5 - Validation** : `testDebugUnitTest`, `kspDebugKotlin`, `assembleDebug`, `connectedDebugAndroidTest` API 36

## Dev Notes

- Choix validés par l'utilisateur (epics.md, Epic 10) : même Joueur quand seuls la casse et les espaces diffèrent ; renommer partout (Story 10.2) ; palmarès sur les Parties terminées (Story 10.3).
- La clé de prénom (`playerNameKey` : espaces retirés, minuscules) vient du domaine ; elle est stockée dans `players.nameKey` sous un index unique. SQLite `lower()` ne connaît que l'ASCII : la migration calcule la clé en Kotlin.
- La migration recrée `participants` (colonne non nulle avec clé étrangère) : Room n'active `PRAGMA foreign_keys` qu'à l'ouverture, après les migrations, donc `round_scores` (qui référence `participants` en cascade) n'est pas touché.
- Pas de date de création du Joueur : la dernière Partie se déduit des Parties.
- Des Parties déjà enregistrées peuvent contenir deux fois le même prénom : leurs deux Participants sont rattachés au même Joueur ; la règle de l'AC 4 ne vaut que pour les nouvelles Parties.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 10, Story 10.1)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/`

## Dev Agent Record

### Debug Log References

- `testDebugUnitTest` : 384 tests, 0 échec. `kspDebugKotlin`, `assembleDebug` : OK ; schéma `5.json` exporté.
- `connectedDebugAndroidTest` sur `Pixel_API_36` : 201 tests, 0 échec, dont `openingAVersion4DatabaseCreatesOneJoueurPerNameAndKeepsEverything` (vrai fichier v4 : deux Joueurs pour « alice »/« Alice  » et « Hélène »/« HÉLÈNE », Manches, scores, détenteur du 7, preneur de Belote et Équipes conservés, cascade depuis `parties` toujours active) et les 3 nouveaux tests de `PartyRepositoryImplTest`.
- Non vérifié : la migration sur la vraie base du téléphone (version 4, release non débogable : impossible d'en tirer une copie) ; aucun parcours manuel dans l'application.

### Completion Notes List

- Pas de `createdAt` sur le Joueur (écart avec la première rédaction de l'epic) : sa dernière Partie se déduit des Parties.
- Le test `accepts duplicate names` de la story 2.2 est remplacé par le refus des prénoms en double (AC 4), conséquence du choix « un prénom = un Joueur » validé par l'utilisateur ; le glossaire du PRD (Participant) est mis à jour.
- `Participant` (domaine) ne porte pas `playerId` : le rattachement est fait par les repositories, dans la transaction d'écriture, avec `playerNameKey` du domaine. La Story 10.2 l'exposera si l'interface en a besoin.
- Les Parties existantes qui contiennent deux fois le même prénom gardent leurs deux Participants, rattachés au même Joueur.

### File List

- `domain/model/KnownPlayer.kt` (`playerNameKey`, `duplicatePlayerName`), `domain/usecase/CreatePartyUseCase.kt`, `CreateBelotePartyUseCase.kt`, `AddParticipantUseCase.kt`
- `data/entity/PlayerEntity.kt`, `data/entity/ParticipantEntity.kt`, `data/dao/PlayerDao.kt`, `data/dao/ParticipantDao.kt`, `data/db/AppDatabase.kt`, `data/db/Migrations.kt`, `di/DatabaseModule.kt`
- `data/repository/PartyRepositoryImpl.kt`, `BeloteRepositoryImpl.kt`, `PlayerRepositoryImpl.kt`
- `ui/screens/setup/SetupViewModel.kt`, `ui/screens/belote/BeloteSetupUiState.kt`, `BeloteSetupScreen.kt`, `ui/screens/party/PartyViewModel.kt`, `res/values/strings.xml`
- `app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/5.json`
- Tests : `KnownPlayerTest`, `CreatePartyUseCaseTest`, `AddParticipantUseCaseTest`, `BeloteUseCasesTest`, `SetupViewModelTest`, `BeloteViewModelsTest`, `PartyViewModelTest`, `BeloteEntityMappingTest`, `AppDatabaseMigrationTest`, `PartyRepositoryImplTest`, `BeloteRepositoryImplTest`, `PlayerRepositoryImplTest`
- `_bmad-output/planning-artifacts/prds/prd-rv-score-counter-2026-08-09/prd.md` (glossaire)

### Change Log

- 2026-10-22 : story créée et implémentée (Joueurs enregistrés, schéma v5).
- 2026-10-22 : après essai sur le téléphone (migration réussie sur ses données v4, aucune erreur), un Joueur sans Partie restante reste suggéré en fin de liste, à la demande de l'utilisateur (AC 5).
