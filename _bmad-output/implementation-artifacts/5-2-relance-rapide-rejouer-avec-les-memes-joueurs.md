---
story_id: "5.2"
story_key: "5-2-relance-rapide-rejouer-avec-les-memes-joueurs"
epic: 5
status: done
created: 2026-10-18
baseline_commit: 98b0e02
---

# Story 5.2 : Relance rapide — rejouer avec les mêmes joueurs

**Epic :** 5 - Clôture & Relance
**Story ID :** 5.2
**Status :** done

## Story

En tant qu'utilisateur,
Je veux démarrer une revanche avec les mêmes joueurs dans le même ordre en un seul tap,
Afin de relancer une Partie sans ressaisir tous les prénoms.

## Acceptance Criteria

1. **Given** l'écran Résultats finaux **When** l'utilisateur tape « Rejouer » (activé à partir de cette story) **Then** une nouvelle Partie `IN_PROGRESS` est créée avec le même Jeu et les mêmes Participants (mêmes prénoms) dans le même ordre (`listPosition`), le premier étant Donneur de la Manche 1.

2. **And** la nouvelle Partie est distincte de la précédente : nouvel identifiant de Partie, nouveaux identifiants de Participants, aucune Manche; la Partie de référence reste `FINISHED` et inchangée.

3. **And** la création passe par `CreatePartyUseCase.fromParty(referencePartyId)` (Architecture FR-14), qui réutilise la validation et l'écriture atomique de la création normale **And** seule une Partie `FINISHED` peut servir de référence; une Partie absente, en cours ou sans Participant est rejetée sans écriture.

4. **And** la navigation remplace toute la pile par PartyScreen de la nouvelle Partie **And** pendant la création « Rejouer » est désactivé (pas de double Partie); un échec affiche un message sur les Résultats.

5. **And** le parcours tient en un tap et une seule transaction Room (objectif FR-14 : moins de 5 secondes) **And** le point d'entrée du Détail de Partie (Story 6.2) réutilisera le même use case.

6. **And** aucune migration ni dépendance; toutes les chaînes visibles viennent de `strings.xml`.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 1-3, 5)
  - [x] `CreatePartyUseCase.fromParty()`.
- [x] **T2 - Résultats** (AC: 4)
  - [x] `ResultsViewModel.onReplay()`, état de création et d'erreur; bouton « Rejouer » actif.
- [x] **T3 - Tests et validations** (AC: 1-6)
  - [x] JVM : `CreatePartyUseCaseTest`, `ResultsViewModelTest`.
  - [x] Compose : `ResultsScreenTest`.
  - [x] `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 5.2]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — FR-14]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md` — Flow 4]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- 2026-10-18 : `testDebugUnitTest` : 173 tests, 0 échec (2 nouveaux `CreatePartyUseCaseTest`, 2 nouveaux `ResultsViewModelTest`).
- 2026-10-18 : `assembleDebug` et `compileDebugAndroidTestKotlin` réussis; `git diff --check` propre.
- 2026-10-18 : `connectedDebugAndroidTest` sur `Pixel_API_36` : 97 tests, 0 échec (`ResultsScreenTest` : 5).
- Non vérifié de bout en bout dans l'application : le passage Résultats -> nouvelle Partie n'est couvert qu'au niveau de l'événement de navigation émis.

### Completion Notes List

- `CreatePartyUseCase.fromParty()` relit une Partie FINISHED et délègue à la création normale (validation, Donneur 0, transaction unique) avec les prénoms dans l'ordre de la table et de nouveaux identifiants.
- ResultsScreen : « Rejouer » actif, désactivé pendant la création, message en cas d'échec; succès -> `ReplaceAll(Party(nouvelle partie))`.

### File List

- _bmad-output/implementation-artifacts/5-2-relance-rapide-rejouer-avec-les-memes-joueurs.md
- _bmad-output/implementation-artifacts/sprint-status.yaml
- app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/CreatePartyUseCase.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsRoute.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsScreen.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsUiState.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsViewModel.kt
- app/src/main/res/values/strings.xml
- app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsScreenTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/domain/usecase/CreatePartyUseCaseTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsViewModelTest.kt

### Change Log

- 2026-10-18 : Story créée et passée `in-progress`.
- 2026-10-18 : Relance rapide depuis les Résultats implémentée et testée (JVM + API 36); story passée `done`.
