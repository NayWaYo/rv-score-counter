---
story_id: "7.4"
story_key: "7-4-historique-et-statistiques-belote"
epic: 7
status: done
created: 2026-10-21
baseline_commit: 555e24f
---

# Story 7.4 : Historique et statistiques de Belote

**Epic :** 7 - Belote classique
**Story ID :** 7.4
**Status :** done

## Story

En tant qu'utilisateur,
Je veux retrouver mes Parties de Belote dans l'historique,
Afin de consulter leurs scores Manche par Manche.

## Acceptance Criteria

1. **Given** l'Historique **When** il contient des Parties de Belote terminées **Then** elles y figurent avec celles du Papayoo, la plus récente d'abord, et chaque ligne montre la date, le Jeu, le nombre de Manches, le score des deux Équipes et l'Équipe gagnante.

2. **Given** une Partie de Belote de l'Historique touchée **When** son détail s'affiche **Then** il montre la date, le Jeu, les deux Équipes (joueurs, score final, gagnante mise en avant) et le tableau des Manches comme pendant la Partie, en lecture seule ; « Rejouer avec ces joueurs » relance la Partie (mêmes Équipes, joueurs et règle de fin).

3. **Given** une Partie de Belote, en cours ou terminée **When** j'ouvre ses Statistiques (depuis la Partie en cours, les résultats ou le détail, dès une Manche jouée) **Then** l'écran affiche la courbe des scores cumulés de chaque Équipe ; les cartes propres au Papayoo n'y apparaissent pas.

4. **And** la suppression d'une Partie de Belote depuis l'Historique emporte ses Équipes et ses Manches ; l'écran de création du Papayoo ne liste toujours que les Parties de Papayoo.

5. **And** une Partie de Belote incohérente apparaît dans l'Historique sans vainqueur (« Données incohérentes ») au lieu de faire échouer la liste ; toutes les chaînes viennent de `strings.xml`.

## Tasks / Subtasks

- [x] **T1 - Historique** (AC: 1, 4, 5)
  - [x] `PartySummary.teamScores` ; résumé des Parties de Belote dans `ObservePartyHistoryUseCase` ; filtre Papayoo retiré de l'Historique.
  - [x] Ligne d'Historique pour la Belote ; ouverture du détail selon le Jeu.
- [x] **T2 - Détail** (AC: 2)
  - [x] Route, ViewModel et écran de détail Belote, composants de la Partie en cours réutilisés en lecture seule.
- [x] **T3 - Statistiques** (AC: 3)
  - [x] `ScoreChart` accepte des séries ; écran Statistiques Belote ; liens depuis la Partie, les résultats et le détail.
- [x] **T4 - Tests et validations** (AC: 1-5)
  - [x] JVM : résumé d'Historique, ViewModels. Compose : détail et Statistiques.
  - [x] `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

## Dev Notes

- Aucune migration : tout est déjà stocké depuis 7.2 ; les scores sont recalculés par le moteur.
- `ScoreChart` reste inchangé pour le Papayoo : une surcharge par séries est ajoutée, la version par `RoundTable` lui délègue.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 7.4]
- [Source: `_bmad-output/planning-artifacts/games/belote.md`]
- [Source: `_bmad-output/implementation-artifacts/6-1-ecran-historique-liste-des-parties-passees.md`, `6-2-detail-de-partie-scores-par-manche-avec-couleur-du-7.md`]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- `testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin` : 346 tests JVM, 0 échec (une attente de test corrigée : le litige de la Manche 1 compte bien pour la défense).
- `connectedDebugAndroidTest` sur l'AVD `Pixel_API_36` : 180 tests ; un échec révélait un vrai défaut (le `testTag` des lignes de Manche, placé après `clearAndSetSemantics`, était effacé), corrigé, puis `BeloteScreensTest` (15) relancé sans échec.
- Parcours à la main sur l'émulateur : Partie en 1 Manche jusqu'aux résultats, Statistiques (courbe Nous 100 / Eux 62), Historique (« Nous 100 · Eux 62 — Nous gagne »), détail en lecture seule avec « Rejouer avec ces joueurs ».
- Non vérifié à la main : la suppression d'une Partie de Belote depuis l'Historique (cascade couverte par `BeloteRepositoryImplTest`), le lien Statistiques de la Partie en cours.

### Completion Notes List

- `PartySummary.teamScores` porte le score des deux Équipes ; une Partie de Belote terminée sans Équipe gagnante, ou dont les données ne tiennent plus, est résumée « Données incohérentes » sans faire échouer la liste.
- `ObservePartyHistoryUseCase` reçoit `BeloteRepository` et `BeloteRules` ; les trois tests qui le construisaient passent un `FakeBeloteStore` et le moteur.
- Les sections du tableau des Manches et les cartes d'Équipe sont partagées entre Partie en cours, résultats et détail (`beloteRoundsSection`, `TeamScoreCard`, `ResultCard`) ; une ligne n'est cliquable que si la correction est possible.
- `ScoreChart` gagne une surcharge par séries, la version Papayoo lui délègue sans changement visible.

### File List

- `_bmad-output/implementation-artifacts/7-4-historique-et-statistiques-belote.md` (nouveau), `sprint-status.yaml`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/PartySummary.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/ObservePartyHistoryUseCase.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/components/PartySummaryRow.kt`, `ScoreChart.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/Routes.kt`, `NavGraph.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryViewModel.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/belote/BelotePartyScreen.kt`, `BelotePartyViewModel.kt`, `BelotePartyRoute.kt`, `BeloteResultsScreen.kt`, `BeloteResultsViewModel.kt`, `BeloteResultsRoute.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/belote/BelotePartyDetail{ViewModel,Screen,Route}.kt`, `BeloteStats{ViewModel,Screen,Route}.kt` (nouveaux)
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/rvetiveau/rvscorecount/domain/usecase/FakeBeloteStore.kt`, `BeloteUseCasesTest.kt`, `PartyHistoryUseCasesTest.kt`
- `app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/belote/BeloteViewModelsTest.kt`, `ui/screens/history/HistoryViewModelTest.kt`, `ui/screens/setup/SetupViewModelTest.kt`
- `app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/belote/BeloteScreensTest.kt`

### Change Log

- 2026-10-21 : Parties de Belote dans l'Historique, détail en lecture seule, Statistiques par Équipe.
