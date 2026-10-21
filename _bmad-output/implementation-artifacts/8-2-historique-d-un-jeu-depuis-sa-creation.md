---
story_id: "8.2"
story_key: "8-2-historique-d-un-jeu-depuis-sa-creation"
epic: 8
status: done
created: 2026-10-21
baseline_commit: 367dc7b
---

# Story 8.2 : Historique d'un Jeu depuis sa création de Partie

**Epic :** 8 - Historique par Jeu et preneur de Belote
**Story ID :** 8.2
**Status :** done

## Story

En tant qu'utilisateur,
Je veux ouvrir l'historique du Jeu que je m'apprête à lancer,
Afin de retrouver ses Parties sans celles des autres Jeux.

## Acceptance Criteria

1. **Given** l'écran de création d'une Partie de Papayoo ou de Belote **When** il s'affiche **Then** une icône Historique (la même horloge que sur l'Accueil, décrite « Historique ») est visible dans l'en-tête, à côté du bouton de thème.

2. **And** la toucher ouvre l'Historique limité à ce Jeu, titré « Historique · <Jeu> », avec la corbeille (Story 8.1) et l'ouverture du détail.

3. **And** un Historique de Jeu vide affiche « Aucune partie de <Jeu> terminée pour l'instant. » ; l'Historique de l'Accueil reste celui de tous les Jeux, inchangé.

4. **And** la liste « Parties précédentes » en bas de la création du Papayoo est retirée.

5. **And** la saisie en cours sur l'écran de création est conservée au retour de l'Historique.

## Tasks / Subtasks

- [x] **T1 - Historique d'un Jeu** (AC: 2, 3)
  - [x] Route `GameHistory(gameId)` vers le même écran ; `HistoryViewModel` lit le Jeu dans son `SavedStateHandle` (nul pour l'Accueil) ; titre et message vide selon le Jeu.
- [x] **T2 - Écrans de création** (AC: 1, 4, 5)
  - [x] Icône Historique dans l'en-tête de la création du Papayoo et de la Belote ; événement de navigation dans leurs ViewModels.
  - [x] Liste « Parties précédentes » retirée de la création du Papayoo (état, ViewModel, écran).
- [x] **T3 - Tests et validations** (AC: 1-5)
  - [x] JVM : `HistoryViewModelTest`, `SetupViewModelTest`, `BeloteViewModelsTest`. Compose : `HistoryScreenTest`, `SetupScreenTest`, `BeloteScreensTest`.
  - [x] `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

## Dev Notes

- `NavigateTo` empile l'Historique au-dessus de la création : son ViewModel, donc la saisie, survit au retour.
- `ObservePartyHistoryUseCase(gameId)` filtre déjà par Jeu (Story 6.1) : aucun changement de domaine.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 8.2]
- [Source: `_bmad-output/implementation-artifacts/8-1-suppression-visible-dans-l-historique.md`]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- `testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin` : 347 tests JVM, 0 échec (deux tests de la liste « Parties précédentes » retirés, trois tests d'Historique par Jeu ajoutés).
- `connectedDebugAndroidTest` sur l'AVD `Pixel_API_36` : 182 tests, 0 échec.
- Vérifié à la main sur l'émulateur : horloge dans l'en-tête de la création du Papayoo, « Historique · Papayoo » vide, retour avec le joueur déjà ajouté conservé.
- Non vérifié à la main : l'horloge de la création de Belote (couverte par `BeloteScreensTest` et `BeloteViewModelsTest`).

### Completion Notes List

- Une seule destination d'Historique : `Routes.History` (tous les Jeux) et `Routes.GameHistory(gameId)` mènent au même écran ; `HistoryViewModel` lit `gameId` dans son `SavedStateHandle`, où la navigation typée range l'argument de la route.
- `HistoryButton` (horloge) est partagé par l'Accueil et les deux créations.
- `SetupViewModel` ne dépend plus d'`ObservePartyHistoryUseCase` ; la chaîne `setup_history_title`, devenue inutile, est retirée.

### File List

- `_bmad-output/implementation-artifacts/8-2-historique-d-un-jeu-depuis-sa-creation.md` (nouveau), `sprint-status.yaml`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/components/HistoryButton.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/Routes.kt`, `NavGraph.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryViewModel.kt`, `HistoryRoute.kt`, `HistoryScreen.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/home/HomeScreen.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupScreen.kt`, `SetupUiState.kt`, `SetupViewModel.kt`, `SetupRoute.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/belote/BeloteSetupScreen.kt`, `BeloteSetupViewModel.kt`, `BeloteSetupRoute.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryViewModelTest.kt`, `ui/screens/setup/SetupViewModelTest.kt`, `ui/screens/belote/BeloteViewModelsTest.kt`
- `app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryScreenTest.kt`, `ui/screens/setup/SetupScreenTest.kt`, `ui/screens/belote/BeloteScreensTest.kt`

### Change Log

- 2026-10-21 : Historique d'un Jeu ouvert depuis sa création de Partie ; liste « Parties précédentes » du Papayoo retirée.
