---
story_id: "3.4"
story_key: "3-4-validation-de-manche-et-persistance"
epic: 3
status: done
created: 2026-10-18
baseline_commit: 8bf2d9c
---

# Story 3.4 : Validation de Manche & persistance

**Epic :** 3 - Saisie de Manche
**Story ID :** 3.4
**Status :** done

## Story

En tant qu'utilisateur,
Je veux valider les scores d'une Manche pour qu'ils soient définitivement enregistrés,
Afin de mettre à jour le classement et de passer à la Manche suivante.

## Acceptance Criteria

1. **Given** tous les champs de score remplis, la somme égale à `totalPointsPerRound` et la Couleur du 7 tirée **When** l'utilisateur tape « Valider la Manche » **Then** un `Round` est inséré avec la Couleur du 7, un `roundNumber` égal au plus grand numéro existant + 1, et un `RoundScore` par Participant.

2. **And** `currentDealerIndex` de la Partie passe à `(currentDealerIndex + 1) % nombreDeParticipants` **And** l'insertion du Round, des RoundScores et la mise à jour de la Partie sont faites dans une seule transaction Room : en cas d'échec, rien n'est persisté.

3. **And** `ValidateRoundUseCase` existe dans `domain/usecase/`, résout le moteur via `Party.gameId` et délègue le contrôle de somme à `GameEngine.validateRound()` : un écart est renvoyé comme résultat métier (`SumMismatch(difference)`), sans écriture.

4. **And** le use case rejette sans écriture : une Partie absente ou non `IN_PROGRESS`, un moteur introuvable, un jeu de scores qui ne couvre pas exactement les Participants de la Partie, un score hors de `0..totalPointsPerRound`, un `currentDealerIndex` incohérent.

5. **And** les Scores Cumulés sont recalculés à partir des Manches lues dans Room et de la nouvelle Manche, avant l'écriture (une fois la Manche enregistrée, plus rien ne peut faire échouer la validation), et le Classement est renvoyé trié par Score Cumulé croissant (égalité départagée par `listPosition`), via une fonction pure du domaine réutilisable par la Story 4.1.

6. **Given** la validation réussie **Then** la navigation retourne vers PartyScreen **And** PartyScreen relit la Partie à son retour au premier plan, si bien que le Donneur affiché est le nouveau Donneur.

7. **Given** l'écran de saisie **Then** « Valider la Manche » n'est activé que si la Couleur du 7 est tirée, tous les champs sont remplis, aucun score n'est négatif et la somme est exacte **And** pendant l'écriture le bouton est désactivé (pas de double validation) **And** un échec d'écriture affiche un message et laisse la saisie intacte.

8. **Given** des scores saisis avant tout tirage **Then** « Lancer » reste utilisable tant qu'aucune couleur n'a été tirée (seul « Relancer » est verrouillé par la saisie), pour que la Manche puisse toujours être validée.

9. **And** une Manche validée ne peut plus être modifiée : DAO en insertion `ABORT` uniquement, aucun bouton d'édition d'une Manche passée.

10. **And** aucune dépendance, migration Room ni modification du contrat `GameEngine` n'est ajoutée; toutes les chaînes visibles viennent de `strings.xml`.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 3, 4, 5)
  - [x] `Standing` et `computeStandings()` purs dans `domain/model/`.
  - [x] `ValidateRoundUseCase` avec `ValidateRoundOutcome` (`Validated`, `SumMismatch`) et exceptions scellées `InvalidRoundException` / `RoundPersistenceException`.
- [x] **T2 - Persistance atomique** (AC: 1, 2, 9)
  - [x] `PartyRepository.saveRound(round, scores, updatedParty)` : Round + RoundScores + mise à jour de la Partie dans une transaction.
- [x] **T3 - Saisie de Manche** (AC: 6, 7, 8)
  - [x] `RoundEntryViewModel.onValidate()`, états `isSaving` / `saveError`, événement de navigation retour.
  - [x] `canValidate` exige la couleur tirée et des scores positifs ou nuls; premier tirage possible après saisie.
  - [x] `RoundEntryRoute` relaie la navigation; `NavGraph` la câble.
- [x] **T4 - Retour sur PartyScreen** (AC: 6)
  - [x] `PartyViewModel` relit la Partie au retour au premier plan.
- [x] **T5 - Tests et validations** (AC: 1-10)
  - [x] Tests JVM du use case, de `computeStandings`, des ViewModels; test instrumenté de la transaction.
  - [x] `testDebugUnitTest`, `kspDebugKotlin`, `assembleDebug`, compilation des tests instrumentés.

## Dev Notes

- Le contrôle de somme reste dans `GameEngine` (aucun `250` hors `PapayooEngine`).
- `RoundEntryViewModel` convertit les brouillons en `Int` uniquement à la validation; un brouillon pré-rempli négatif bloque la validation (AC 7) et le use case le rejette aussi (AC 4).
- La contrainte unique `(partyId, roundNumber)` protège contre une double validation concurrente : la seconde transaction échoue et ne modifie pas le Donneur.
- L'affichage du classement sur PartyScreen appartient à la Story 4.1, qui consommera `computeStandings()`.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 3.4]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — AD-2, AD-3, AD-4, FR-8, FR-9]
- [Source: `_bmad-output/project-context.md`]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- 2026-10-18 : `testDebugUnitTest` : 141 tests, 0 échec (dont 9 `ValidateRoundUseCaseTest`, 3 `StandingTest`, 5 nouveaux `RoundEntryViewModelTest`, 1 nouveau `PartyViewModelTest`).
- 2026-10-18 : `kspDebugKotlin`, `assembleDebug` et `compileDebugAndroidTestKotlin` réussis; `git diff --check` propre.
- 2026-10-18 : `connectedDebugAndroidTest` sur émulateur API 37 : `PartyRepositoryImplTest` 4/4 (dont les 2 tests de transaction `saveRound`). Les 72 tests Compose échouent tous, y compris sur des écrans non modifiés, avec `NoSuchMethodException: InputManager.getInstance` (Espresso incompatible avec l'API 37).
- 2026-10-18 : `connectedDebugAndroidTest` relancé sur un émulateur API 36 (`Pixel_API_36`) : 76 tests, 0 échec.

### Completion Notes List

- `ValidateRoundUseCase` : moteur résolu via `Party.gameId`, contrôle de somme délégué à `GameEngine.validateRound()`, rejets explicites (Partie, moteur, Participants, bornes de score, index du Donneur), classement calculé avant l'écriture.
- `PartyRepository.saveRound` écrit Round, RoundScores et rotation du Donneur dans une seule transaction; la contrainte unique `(partyId, roundNumber)` bloque une double validation.
- `computeStandings()` pur dans le domaine, prêt pour la Story 4.1.
- Saisie : validation exigeant couleur tirée et scores positifs ou nuls, état d'enregistrement, message d'échec, retour vers PartyScreen; premier tirage toujours possible après saisie.
- PartyScreen relit la Partie au retour au premier plan et affiche `Manche N — Donneur`.

### File List

- CLAUDE.md
- _bmad-output/implementation-artifacts/3-4-validation-de-manche-et-persistance.md
- _bmad-output/implementation-artifacts/sprint-status.yaml
- app/src/main/java/com/rvetiveau/rvscorecount/domain/model/Standing.kt
- app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/ValidateRoundUseCase.kt
- app/src/main/java/com/rvetiveau/rvscorecount/domain/repository/PartyRepository.kt
- app/src/main/java/com/rvetiveau/rvscorecount/data/repository/PartyRepositoryImpl.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/NavGraph.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyScreen.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyUiState.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModel.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/round/RoundEntryRoute.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/round/RoundEntryScreen.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/round/RoundEntryUiState.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/round/RoundEntryViewModel.kt
- app/src/main/res/values/strings.xml
- app/src/androidTest/java/com/rvetiveau/rvscorecount/data/repository/PartyRepositoryImplTest.kt
- app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/round/RoundEntryScreenTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/domain/model/StandingTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/domain/usecase/ValidateRoundUseCaseTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModelTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/round/RoundEntryViewModelTest.kt
- Faux repositories mis à la nouvelle signature de `saveRound` : `AddParticipantUseCaseTest`, `CreatePartyUseCaseTest`, `ResumePartyUseCaseTest`, `RootViewModelTest`, `SetupViewModelTest`

### Change Log

- 2026-10-18 : Story créée et passée `in-progress`.
- 2026-10-18 : Validation de Manche, persistance atomique, classement et retour PartyScreen implémentés; story passée `review` en attente des tests Compose sur un émulateur API 36.
- 2026-10-18 : Tests instrumentés validés sur API 36; story passée `done`.
