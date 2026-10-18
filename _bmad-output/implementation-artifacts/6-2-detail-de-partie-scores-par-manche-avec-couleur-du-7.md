---
story_id: "6.2"
story_key: "6-2-detail-de-partie-scores-par-manche-avec-couleur-du-7"
epic: 6
status: done
created: 2026-10-18
baseline_commit: f50ae3d
---

# Story 6.2 : Détail de Partie — scores par Manche avec couleur du 7

**Epic :** 6 - Historique
**Story ID :** 6.2
**Status :** done

## Story

En tant qu'utilisateur,
Je veux consulter le détail complet d'une Partie passée Manche par Manche,
Afin de retrouver précisément les scores de chaque joueur à chaque tour et de relancer avec le même groupe.

## Acceptance Criteria

1. **Given** PartyDetailScreen pour une Partie `FINISHED` (ouvert depuis l'Historique) **When** l'écran se charge **Then** l'en-tête rappelle la date et le Jeu, puis le Classement final est affiché en haut, trié par Score Cumulé croissant, avec le ou les vainqueurs mis en avant (même bandeau que les Résultats finaux).

2. **And** en dessous, la liste des Manches est affichée dans l'ordre chronologique (Manche 1 en premier); une Partie sans Manche affiche « Aucune manche jouée ».

3. **And** chaque ligne de Manche réutilise `RoundHistoryRow` (Story 4.3) : numéro, scores de chaque Participant dans l'ordre de la table, total, fond `suit-*-tint`, pictogramme de l'enseigne à droite.

4. **And** l'écran est en lecture seule (aucune modification de score, de Manche ni de Participant).

5. **And** « Rejouer avec ces joueurs » (`Button` plein) en bas d'écran déclenche la relance de la Story 5.2 (`CreatePartyUseCase.fromParty`) et remplace la pile par la nouvelle Partie; il est désactivé pendant la création et un échec affiche un message.

6. **And** une Partie absente, en cours ou incohérente affiche « Détail indisponible pour cette partie. »; une erreur de lecture affiche « Réessayer »; le retour arrière revient à l'Historique.

7. **And** aucune migration, requête DAO ni dépendance; toutes les chaînes viennent de `strings.xml`.

## Tasks / Subtasks

- [x] **T1 - Composants partagés** (AC: 1, 3)
  - [x] `roundHistoryItems()` dans `RoundHistoryRow.kt`, utilisé par PartyViewModel et le Détail.
  - [x] Bandeau vainqueur extrait des Résultats vers `ui/components/WinnerBanner.kt`.
- [x] **T2 - Écran** (AC: 1-6)
  - [x] `PartyDetailViewModel`, `PartyDetailUiState`, `PartyDetailScreen`, `PartyDetailRoute`; branchement dans `NavGraph` à la place du dernier placeholder.
- [x] **T3 - Tests et validations** (AC: 1-7)
  - [x] JVM : `PartyDetailViewModelTest`.
  - [x] Compose : `PartyDetailScreenTest`.
  - [x] `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 6.2]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md` — Détail de Partie]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/DESIGN.md` — round-history-row]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- 2026-10-18 : `testDebugUnitTest` : 192 tests, 0 échec (6 `PartyDetailViewModelTest`; un premier jeu de données à 2 joueurs était rejeté à juste titre par la relance, Papayoo exigeant 3 joueurs).
- 2026-10-18 : `assembleDebug` réussi; `git diff --check` propre.
- 2026-10-18 : `connectedDebugAndroidTest` sur `Pixel_API_36` : 112 tests, 0 échec (6 `PartyDetailScreenTest`).
- 2026-10-18 : APK debug installé et lancé sur l'émulateur : Accueil puis Historique (état vide) affichés, aucune erreur `AndroidRuntime`. Le parcours complet d'une partie n'a pas été joué à la main.

### Completion Notes List

- PartyDetailScreen : date et Jeu, bandeau du ou des vainqueurs, classement final, Manches chronologiques en lecture seule via `RoundHistoryRow`, « Rejouer avec ces joueurs » via `CreatePartyUseCase.fromParty`.
- `roundHistoryItems()` partagé (PartyViewModel l'inverse pour afficher la plus récente d'abord); `WinnerBanner` extrait des Résultats vers `ui/components/`.
- Dernier placeholder du `NavGraph` remplacé : toutes les routes ont désormais leur écran; `RoutePlaceholder` et `route_placeholder_party_detail` supprimés.

### File List

- _bmad-output/implementation-artifacts/6-2-detail-de-partie-scores-par-manche-avec-couleur-du-7.md
- _bmad-output/implementation-artifacts/sprint-status.yaml
- app/src/main/java/com/rvetiveau/rvscorecount/ui/components/RoundHistoryRow.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/components/WinnerBanner.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/NavGraph.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModel.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/partydetail/PartyDetailRoute.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/partydetail/PartyDetailScreen.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/partydetail/PartyDetailUiState.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/partydetail/PartyDetailViewModel.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsScreen.kt
- app/src/main/res/values/strings.xml
- app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/partydetail/PartyDetailScreenTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/partydetail/PartyDetailViewModelTest.kt

### Change Log

- 2026-10-18 : Story créée et passée `in-progress`.
- 2026-10-18 : Détail de Partie avec Manches colorées et relance implémenté et testé (JVM + API 36); story passée `done`.
