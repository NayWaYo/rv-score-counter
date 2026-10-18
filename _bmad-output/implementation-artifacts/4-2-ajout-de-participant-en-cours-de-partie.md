---
story_id: "4.2"
story_key: "4-2-ajout-de-participant-en-cours-de-partie"
epic: 4
status: done
created: 2026-10-18
baseline_commit: 409c347
---

# Story 4.2 : Ajout de Participant en cours de Partie

**Epic :** 4 - Suivi de Partie en cours
**Story ID :** 4.2
**Status :** done

## Story

En tant qu'utilisateur,
Je veux pouvoir ajouter un joueur qui arrive en retard pendant la Partie,
Afin qu'il puisse participer sans recommencer la Partie depuis le début.

## Acceptance Criteria

1. **Given** une Partie `IN_PROGRESS` avec moins de `maxPlayers` Participants (8 pour le Papayoo, lu sur le moteur) **When** l'utilisateur tape « Ajouter un joueur » **Then** un champ « Prénom du joueur » apparaît, avec « Choisir la place » et « Annuler ».

2. **Given** un prénom non vide (espaces retirés) **When** l'utilisateur tape « Choisir la place » **Then** un bottom sheet liste les places possibles, une par Participant dans l'ordre de la table : « Après {nom} » (la place « après le dernier » couvre aussi l'espace avant le premier, la table étant circulaire) **And** un prénom vide affiche un message sans ouvrir le bottom sheet.

3. **Given** le bottom sheet **When** l'utilisateur choisit une place **Then** le Participant est inséré à cette position (`listPosition` des suivants décalée de 1) avec un Score Cumulé de 0, et le classement se met à jour via l'observation Room de la Story 4.1.

4. **And** règle du Donneur (PRD FR-4, révisée le 2026-10-18) : si la place choisie est juste avant le prochain Donneur, c'est-à-dire juste après le dernier Donneur (`listPosition = currentDealerIndex`, ou après le dernier Participant quand le prochain Donneur est le premier), le nouveau Participant devient le prochain Donneur et le Donneur prévu distribue la Manche d'après; sinon le prochain Donneur reste le même Participant (son index est décalé si l'insertion se fait avant lui). Personne ne perd son tour **And** l'option concernée l'indique dans le bottom sheet (« Sera le prochain donneur »).

5. **And** `AddParticipantUseCase` porte la validation (Partie `IN_PROGRESS`, moteur, nombre maximal, place dans `0..n`, prénom) et le calcul du Donneur; l'insertion, le décalage des positions et la mise à jour de la Partie sont faits dans une seule transaction Room.

6. **And** à `maxPlayers` Participants, « Ajouter un joueur » est désactivé et « Maximum de 8 joueurs atteint » est affiché **And** le bouton n'apparaît pas pour une Partie terminée.

7. **And** pendant l'écriture la sélection est désactivée; un échec affiche un message et laisse le bottom sheet ouvert; « Annuler » ou la fermeture du bottom sheet abandonne l'ajout sans écriture.

8. **And** aucune migration Room (seule une requête `UPDATE` est ajoutée au DAO), aucune dépendance; toutes les chaînes visibles viennent de `strings.xml`.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 4, 5)
  - [x] `nextDealerIndexAfterInsertion()` pur.
  - [x] `AddParticipantUseCase(partyId, name, position)` avec exceptions scellées.
- [x] **T2 - Persistance** (AC: 3, 5, 8)
  - [x] `ParticipantDao.shiftPositionsFrom()`; `PartyRepository.addParticipant(participant, updatedParty)` transactionnel.
- [x] **T3 - Interface** (AC: 1, 2, 4, 6, 7)
  - [x] État d'ajout dans `PartyUiState.Loaded` (prénom, étape, places, enregistrement, erreur), `maxPlayers` lu sur le moteur.
  - [x] Bouton, champ de prénom, bottom sheet des places, message de maximum.
- [x] **T4 - Tests et validations** (AC: 1-8)
  - [x] JVM : règle du Donneur, use case, ViewModel.
  - [x] Instrumentés : transaction Room, écran.
  - [x] `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

## Dev Notes

- Règle du Donneur : la version initiale du PRD (« position N+1, juste après le prochain Donneur ») faisait passer son tour au Donneur prévu. Corrigée le 2026-10-18 à la demande du porteur du produit : la distribution suit l'ordre de la table, le nouveau Participant ne devient Donneur que s'il s'assoit juste avant le Donneur prévu. PRD, epics et UX mis à jour en conséquence.
- La place 0 n'est pas proposée : à une table circulaire, elle correspond à « après le dernier ».

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 4.2]
- [Source: `_bmad-output/planning-artifacts/prds/` — FR-4]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md` — Flow 3, « Ajout Participant : max atteint »]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- 2026-10-18 : `testDebugUnitTest` : 156 tests, 0 échec (`AddParticipantUseCaseTest` réécrit : 8 tests; 6 nouveaux `PartyViewModelTest`).
- 2026-10-18 : `kspDebugKotlin`, `assembleDebug`, `compileDebugAndroidTestKotlin` réussis; `git diff --check` propre.
- 2026-10-18 : `connectedDebugAndroidTest` sur `Pixel_API_36` : 87 tests, 0 échec (2 nouveaux tests de transaction `addParticipant`, 4 nouveaux `PartyScreenTest`).

### Completion Notes List

- `AddParticipantUseCase(partyId, name, position)` remplace l'ajout en fin de liste (aucun appelant en production); `nextDealerIndexAfterInsertion()` applique la règle FR-4.
- `PartyRepository.addParticipant(participant, updatedParty)` : décalage des places (`ParticipantDao.shiftPositionsFrom`), insertion et Donneur dans une transaction.
- PartyScreen : « Ajouter un joueur », champ de prénom, bottom sheet « Après {nom} » avec la mention « Sera le prochain donneur », message de maximum. Le classement se met à jour par l'observation Room de 4.1.
- Règle du Donneur corrigée le 2026-10-18 : plus aucun Participant ne perd son tour (voir Dev Notes).

### File List

- _bmad-output/implementation-artifacts/4-2-ajout-de-participant-en-cours-de-partie.md
- _bmad-output/implementation-artifacts/sprint-status.yaml
- app/src/main/java/com/rvetiveau/rvscorecount/data/dao/ParticipantDao.kt
- app/src/main/java/com/rvetiveau/rvscorecount/data/repository/PartyRepositoryImpl.kt
- app/src/main/java/com/rvetiveau/rvscorecount/domain/repository/PartyRepository.kt
- app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/AddParticipantUseCase.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyRoute.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyScreen.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyUiState.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModel.kt
- app/src/main/res/values/strings.xml
- app/src/androidTest/java/com/rvetiveau/rvscorecount/data/repository/PartyRepositoryImplTest.kt
- app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyScreenTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/domain/usecase/AddParticipantUseCaseTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModelTest.kt
- Faux repositories mis à la nouvelle signature de `addParticipant` : `CreatePartyUseCaseTest`, `ResumePartyUseCaseTest`, `ValidateRoundUseCaseTest`, `RootViewModelTest`, `RoundEntryViewModelTest`, `SetupViewModelTest`

### Change Log

- 2026-10-18 : Story créée et passée `in-progress`.
- 2026-10-18 : Ajout en cours de Partie avec choix de la place et règle du Donneur implémentés et testés (JVM + API 36); story passée `done`.
- 2026-10-18 : Règle du Donneur corrigée : le nouveau Participant devient Donneur s'il s'assoit juste avant le Donneur prévu (et non plus juste après), sans faire sauter de tour; `nextDealerIndexAfterInsertion` prend le nombre de Participants pour gérer la table circulaire.
