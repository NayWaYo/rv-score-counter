---
story_id: "4.3"
story_key: "4-3-historique-des-manches-dans-la-partie"
epic: 4
status: done
created: 2026-10-18
baseline_commit: 927ec24
---

# Story 4.3 : Historique des Manches dans la Partie

**Epic :** 4 - Suivi de Partie en cours
**Story ID :** 4.3
**Status :** done

## Story

En tant qu'utilisateur,
Je veux consulter le détail des Manches jouées pendant la Partie en cours,
Afin de vérifier un score passé si nécessaire.

## Acceptance Criteria

1. **Given** une Partie avec au moins une Manche validée **When** PartyScreen est affiché **Then** un lien discret « Voir les manches ({n}) » est visible; il est absent avant la première Manche (UX : « section dépliable ou lien discret », pour garder le classement lisible sans défilement).

2. **When** l'utilisateur tape ce lien **Then** un bottom sheet liste chaque Manche validée, de la plus récente à la plus ancienne.

3. **And** chaque ligne (`round-history-row`) affiche : « Manche {N} », le score de chaque Participant de cette Manche dans l'ordre de la table, le total de la Manche, un fond `suit-*-tint` selon la Couleur du 7 et le pictogramme de l'enseigne à droite dans sa couleur `suit-*`.

4. **And** un Participant ajouté après une Manche (Story 4.2) n'apparaît pas dans les Manches où il n'a pas joué.

5. **And** la liste est en lecture seule : aucune action de modification ni de suppression.

6. **And** TalkBack lit chaque ligne d'un bloc (numéro, enseigne, scores, total) **And** les données viennent de la même observation Room que le classement (Story 4.1), sans nouvelle requête ni migration; toutes les chaînes viennent de `strings.xml`.

7. **And** le composant de ligne est placé dans `ui/components/` pour être réutilisé par le détail de Partie (Story 6.2); pictogrammes et noms d'enseigne sont partagés avec `DieRoller` dans `ui/theme/`.

## Tasks / Subtasks

- [x] **T1 - État** (AC: 2-4, 6)
  - [x] `RoundHistoryItem` (numéro, enseigne, scores nommés, total) dans `PartyUiState.Loaded`, construit dans `PartyViewModel.loadState`.
- [x] **T2 - Interface** (AC: 1-3, 5-7)
  - [x] Extensions `CardSuit.pictogramRes` / `nameRes` déplacées dans `ui/theme/CardSuitColors.kt`.
  - [x] Composant `RoundHistoryRow`; lien et bottom sheet sur PartyScreen.
- [x] **T3 - Tests et validations** (AC: 1-7)
  - [x] JVM : historique ordonné, scores dans l'ordre de la table, Participant ajouté plus tard absent.
  - [x] Compose : lien, bottom sheet, contenu et description de ligne.
  - [x] `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 4.3]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/DESIGN.md` — round-history-row]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md` — Partie en cours]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- 2026-10-18 : `testDebugUnitTest` : 159 tests, 0 échec (3 nouveaux `PartyViewModelTest`). Le faux repository émet désormais à chaque écriture, comme Room, même quand la ligne `parties` est inchangée.
- 2026-10-18 : `assembleDebug` et `compileDebugAndroidTestKotlin` réussis; `git diff --check` propre.
- 2026-10-18 : `connectedDebugAndroidTest` sur `Pixel_API_36` : 90 tests, 0 échec (3 nouveaux `PartyScreenTest`).

### Completion Notes List

- `PartyViewModel` construit l'historique (plus récent en premier, scores dans l'ordre de la table, total) depuis la même lecture Room que le classement.
- Composant réutilisable `RoundHistoryRow` (`ui/components/`), lu d'un bloc par TalkBack; `pictogramRes` / `nameRes` déplacés de `DieRoller` vers `ui/theme/CardSuitColors.kt`.
- PartyScreen : lien « Voir les manches (n) » après la première Manche, bottom sheet « Manches jouées » en lecture seule.

### File List

- _bmad-output/implementation-artifacts/4-3-historique-des-manches-dans-la-partie.md
- _bmad-output/implementation-artifacts/sprint-status.yaml
- app/src/main/java/com/rvetiveau/rvscorecount/ui/components/DieRoller.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/components/RoundHistoryRow.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/theme/CardSuitColors.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyRoute.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyScreen.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyUiState.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModel.kt
- app/src/main/res/values/strings.xml
- app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyScreenTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModelTest.kt

### Change Log

- 2026-10-18 : Story créée et passée `in-progress`.
- 2026-10-18 : Historique des Manches en lecture seule implémenté et testé (JVM + API 36); story passée `done`.
