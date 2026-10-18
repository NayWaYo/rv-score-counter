---
story_id: "4.1"
story_key: "4-1-ecran-partie-en-cours-classement-temps-reel"
epic: 4
status: done
created: 2026-10-18
baseline_commit: e9b9e02
---

# Story 4.1 : Écran Partie en cours — classement temps réel

**Epic :** 4 - Suivi de Partie en cours
**Story ID :** 4.1
**Status :** done

## Story

En tant qu'utilisateur,
Je veux voir le classement mis à jour en permanence pendant la Partie,
Afin que tous les joueurs puissent consulter leur rang sans interaction.

## Acceptance Criteria

1. **Given** une Partie `IN_PROGRESS` **When** PartyScreen est affiché **Then** le classement affiche tous les Participants triés par Score Cumulé croissant, calculé par `computeStandings()` (Story 3.4) à partir des Manches enregistrées, égalités départagées par `listPosition`.

2. **And** chaque ligne affiche : rang, nom du Participant, Score Cumulé **And** deux Participants à égalité de score partagent le même rang (classement « 1, 1, 3 »).

3. **And** le Donneur de la Manche suivante (`currentDealerIndex`) porte un badge « Donneur » en accent sur sa ligne, lu par TalkBack.

4. **And** le sous-titre affiche « Manche {N} — Donneur : {nom} », N étant le plus grand numéro de Manche enregistré + 1 **And** « Lancez la première manche pour commencer. » n'apparaît qu'avant la première Manche.

5. **And** le classement tient sans défilement pour 8 Participants sur un écran standard (lignes de 56dp minimum); il défile au-delà ou avec une grande taille de police.

6. **And** « Nouvelle manche » (`Button` plein, accent) et « Fin de partie » (`OutlinedButton`) sont en bas d'écran et visuellement distincts **And** « Fin de partie » reste désactivé tant que la clôture n'existe pas (Story 5.1).

7. **And** l'écran observe la Partie et ses Participants via `PartyRepository.observeParty()` / `observeParticipants()` dans un unique `StateFlow` : toute écriture Room (validation de Manche, ajout de Participant) met l'écran à jour sans action de l'utilisateur, ce qui remplace la relecture au retour au premier plan ajoutée en 3.4.

8. **And** une incohérence (score d'un Participant inconnu, positions non continues, Donneur introuvable) affiche l'état « données incohérentes » au lieu d'un classement faux; une erreur de lecture affiche l'état d'erreur avec « Réessayer ».

9. **And** aucune dépendance, migration Room ni nouvelle requête DAO n'est ajoutée; toutes les chaînes visibles viennent de `strings.xml`.

## Tasks / Subtasks

- [x] **T1 - État observé** (AC: 1, 2, 4, 7, 8)
  - [x] `PartyUiState.Loaded` porte les lignes de classement (`StandingRow` : id, rang, nom, score, Donneur).
  - [x] `PartyViewModel` combine `observeParty` et `observeParticipants`, relit Manches et scores à chaque émission et calcule le classement.
- [x] **T2 - Écran** (AC: 2-6)
  - [x] Lignes de classement (surface-container, 56dp, rang / nom + badge / score).
  - [x] Boutons « Nouvelle manche » et « Fin de partie » en bas d'écran.
- [x] **T3 - Tests et validations** (AC: 1-9)
  - [x] JVM : classement, rangs ex æquo, mise à jour sur émission Room, incohérences, erreur et réessai.
  - [x] Compose : lignes, badge, boutons, sous-titre, état vide.
  - [x] `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

## Dev Notes

- `saveRound` (3.4) met à jour la ligne `parties` dans la même transaction que le Round : l'émission de `observeParty` suffit pour savoir qu'une Manche a été validée, sans nouvelle requête Flow sur `rounds`.
- Le rang « 1, 1, 3 » est une règle d'affichage, calculée dans le ViewModel à partir de l'ordre de `computeStandings()`.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 4.1]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/DESIGN.md` — scoreboard-row, boutons d'action]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md` — Partie en cours, Flow 5]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — AD-4, FR-10]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- 2026-10-18 : `testDebugUnitTest` : 145 tests, 0 échec (5 nouveaux `PartyViewModelTest`; le test de relecture au retour au premier plan de 3.4 est remplacé par l'observation Room).
- 2026-10-18 : `assembleDebug` et `compileDebugAndroidTestKotlin` réussis; `git diff --check` propre.
- 2026-10-18 : `connectedDebugAndroidTest` sur `Pixel_API_36` : 81 tests, 0 échec (5 nouveaux `PartyScreenTest`, dont 8 lignes affichées sans défilement).

### Completion Notes List

- `PartyViewModel` combine `observeParty` et `observeParticipants` et recalcule Classement, Donneur et numéro de Manche à chaque émission Room; incohérences et erreurs gardent leurs états dédiés.
- Rang « 1, 1, 3 » pour les ex æquo; badge « Donneur » en accent sur la ligne du prochain Donneur.
- PartyScreen : titre, sous-titre « Manche N — Donneur », classement en `LazyColumn`, « Nouvelle manche » (plein) et « Fin de partie » (contour, désactivé jusqu'à la Story 5.1).

### File List

- _bmad-output/implementation-artifacts/4-1-ecran-partie-en-cours-classement-temps-reel.md
- _bmad-output/implementation-artifacts/sprint-status.yaml
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyRoute.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyScreen.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyUiState.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModel.kt
- app/src/main/res/values/strings.xml
- app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyScreenTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModelTest.kt

### Change Log

- 2026-10-18 : Story créée et passée `in-progress`.
- 2026-10-18 : Classement temps réel, badge Donneur et actions de bas d'écran implémentés et testés (JVM + API 36); story passée `done`.
