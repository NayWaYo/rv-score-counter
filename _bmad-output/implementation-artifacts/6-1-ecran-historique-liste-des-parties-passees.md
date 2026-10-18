---
story_id: "6.1"
story_key: "6-1-ecran-historique-liste-des-parties-passees"
epic: 6
status: done
created: 2026-10-18
baseline_commit: 399ff85
---

# Story 6.1 : Écran Historique — liste des Parties passées

**Epic :** 6 - Historique
**Story ID :** 6.1
**Status :** done

## Story

En tant qu'utilisateur,
Je veux consulter la liste de toutes mes Parties terminées,
Afin de retrouver les scores d'une soirée précédente ou relancer une Partie avec le même groupe.

## Acceptance Criteria

1. **Given** l'écran Historique (ouvert depuis l'Accueil) **When** des Parties `FINISHED` existent **Then** la liste les affiche de la plus récente à la plus ancienne (`createdAt` décroissant); les Parties en cours n'y figurent pas.

2. **And** chaque entrée affiche : la date (format court localisé), le nom du Jeu, le nombre de Manches, le ou les vainqueurs (Score Cumulé le plus bas, ex æquo compris) et leur Score Cumulé; une Partie terminée sans Manche affiche « Aucune manche jouée » à la place du vainqueur.

3. **And** taper une entrée navigue vers `PartyDetailScreen(partyId)` (contenu livré par la Story 6.2).

4. **And** un swipe vers la gauche sur une entrée, ou l'action d'accessibilité « Supprimer », ouvre un dialog « Supprimer cette partie ? » (Supprimer en couleur error, Annuler) **And** la suppression n'a lieu qu'après confirmation, est définitive (Partie, Participants, Manches et scores, par cascade Room) et la liste se met à jour d'elle-même.

5. **And** `DeletePartyUseCase` refuse de supprimer une Partie absente ou en cours; un échec affiche un message.

6. **And** sans Partie terminée, l'écran affiche « Aucune partie terminée pour l'instant. »; une erreur de lecture affiche « Réessayer »; une Partie dont les données sont incohérentes reste listée avec la mention « Données incohérentes » plutôt qu'un vainqueur faux.

7. **And** la liste est observée depuis Room (`PartyDao.observeByStatus`), les résumés sont construits par `ObservePartyHistoryUseCase` dans le domaine **And** seules des requêtes DAO sont ajoutées, sans migration ni dépendance; toutes les chaînes viennent de `strings.xml`.

## Tasks / Subtasks

- [x] **T1 - Données et domaine** (AC: 1, 2, 4, 5, 7)
  - [x] `PartyDao.observeByStatus`, `PartyDao.deleteById`; `PartyRepository.observeFinishedParties`, `deleteParty`.
  - [x] `PartySummary`, `ObservePartyHistoryUseCase`, `DeletePartyUseCase`.
- [x] **T2 - Interface** (AC: 1-6)
  - [x] `HistoryViewModel`, `HistoryUiState`, `HistoryScreen`, `HistoryRoute`; branchement dans `NavGraph` à la place du placeholder.
  - [x] Nom du Jeu partagé entre Accueil et Historique (`ui/components/GamePresentation.kt`).
- [x] **T3 - Tests et validations** (AC: 1-7)
  - [x] JVM : use cases, ViewModel.
  - [x] Instrumentés : requêtes et cascade Room, écran (liste, vide, swipe, dialog).
  - [x] `testDebugUnitTest`, `kspDebugKotlin`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 6.1]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md` — Historique, Dialog de confirmation]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- 2026-10-18 : `testDebugUnitTest` : 186 tests, 0 échec (5 `PartyHistoryUseCasesTest`, 8 `HistoryViewModelTest`).
- 2026-10-18 : `kspDebugKotlin`, `assembleDebug`, `compileDebugAndroidTestKotlin` réussis, sans avertissement nouveau (`rememberSwipeToDismissBoxState(confirmValueChange)`, déprécié, remplacé par `SwipeToDismissBox(onDismiss)`); `git diff --check` propre.
- 2026-10-18 : `connectedDebugAndroidTest` sur `Pixel_API_36` : 106 tests, 0 échec (7 `HistoryScreenTest` dont le swipe, 2 nouveaux tests Room : tri/filtrage et cascade de suppression).

### Completion Notes List

- Room : `PartyDao.observeByStatus` (tri `createdAt` décroissant) et `deleteById` (cascade existante vers Participants, Manches et scores); aucune migration.
- Domaine : `PartySummary`, `ObservePartyHistoryUseCase` (vainqueurs ex æquo, Partie sans Manche, incohérence signalée), `DeletePartyUseCase` (FINISHED uniquement).
- HistoryScreen : entrées date · Jeu · nombre de Manches + vainqueur, tap vers le Détail, swipe ou action d'accessibilité « Supprimer » puis dialog de confirmation, états vide / erreur.
- `gamePresentation()` partagé entre Accueil et Historique (`ui/components/GamePresentation.kt`); placeholder et chaîne `route_placeholder_history` supprimés.

### File List

- _bmad-output/implementation-artifacts/6-1-ecran-historique-liste-des-parties-passees.md
- _bmad-output/implementation-artifacts/sprint-status.yaml
- app/src/main/java/com/rvetiveau/rvscorecount/data/dao/PartyDao.kt
- app/src/main/java/com/rvetiveau/rvscorecount/data/repository/PartyRepositoryImpl.kt
- app/src/main/java/com/rvetiveau/rvscorecount/domain/model/PartySummary.kt
- app/src/main/java/com/rvetiveau/rvscorecount/domain/repository/PartyRepository.kt
- app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/DeletePartyUseCase.kt
- app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/ObservePartyHistoryUseCase.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/components/GamePresentation.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/NavGraph.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryRoute.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryScreen.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryUiState.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryViewModel.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/home/HomeViewModel.kt
- app/src/main/res/values/strings.xml
- app/src/androidTest/java/com/rvetiveau/rvscorecount/data/repository/PartyRepositoryImplTest.kt
- app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryScreenTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/domain/usecase/PartyHistoryUseCasesTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryViewModelTest.kt
- Faux repositories complétés (`observeFinishedParties`, `deleteParty`) dans les 10 tests JVM existants qui implémentent `PartyRepository`

### Change Log

- 2026-10-18 : Story créée et passée `in-progress`.
- 2026-10-18 : Historique des Parties terminées avec suppression confirmée implémenté et testé (JVM + API 36); story passée `done`.
