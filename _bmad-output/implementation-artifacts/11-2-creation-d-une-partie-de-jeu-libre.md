---
story_id: "11.2"
story_key: "11-2-creation-d-une-partie-de-jeu-libre"
epic: 11
status: done
created: 2026-10-22
baseline_commit: 9e0be21
---

# Story 11.2 : Création d'une Partie de Jeu libre

**Epic :** 11 - Jeu libre
**Story ID :** 11.2
**Status :** done

## Story

En tant qu'utilisateur,
Je veux créer une Partie de n'importe quel jeu en quelques secondes,
Afin de compter les points dès la première Manche.

## Acceptance Criteria

1. **Given** l'accueil **Then** une tuile « Jeu libre », avec son logo au gabarit des autres, vient après le Papayoo et la Belote ; la toucher ouvre « Nouvelle partie libre ».

2. **And** la création demande le **nom du jeu** (30 caractères au plus), avec les noms déjà joués en Jeu libre en suggestions, du plus récent au plus ancien, filtrés par le texte saisi.

3. **And** les **Joueurs** s'ajoutent comme au Papayoo : de 2 à 10, ordre modifiable par glisser-déposer, le premier donne, suggestions de prénoms (Story 10.1) et prénoms en double refusés.

4. **And** le **sens de victoire** se choisit entre « Le plus petit total gagne » (par défaut) et « Le plus grand total gagne », et la **fin de partie** entre « À la main » (par défaut), « Score à atteindre » et « Nombre de manches », ces deux dernières avec une valeur de 1 à 9 999.

5. **And** « Commencer » n'est actif qu'avec un nom de jeu, au moins 2 Joueurs et une valeur de fin valide ; il crée la Partie (règles et Joueurs en une transaction, le domaine refusant toute configuration invalide) et ouvre l'écran de la Partie, qui montre le nom du jeu, ses règles et les Joueurs dans l'ordre de table avec le Donneur ; la saisie des Manches vient avec la Story 11.3.

6. **And** au lancement, une Partie de Jeu libre en cours rouvre cet écran ; l'icône Historique de la création ouvre l'historique des Jeux libres.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 2, 5) : `CreateFreePartyUseCase`, noms de jeux déjà joués ; tests JVM
- [x] **T2 - Données** (AC: 2) : requête des noms de jeux ; test Room
- [x] **T3 - Interface** (AC: 1-6) : logo et présentation, catalogue, éditeur de Joueurs partagé avec le Papayoo, `FreeSetupScreen`/`ViewModel`, première version de `FreePartyScreen`, routes, reprise au lancement ; tests ViewModel et Compose
- [x] **T4 - Validation** : `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` API 36

## Dev Notes

- Choix validés le 2026-10-22 (epics.md, Epic 11). Règles portées par la Partie (AD-9).
- L'ajout, la suppression et le déplacement des Joueurs, ainsi que leurs suggestions, sont repris de la création du Papayoo : la liste de Joueurs devient un composant partagé.
- Les Parties de Jeu libre ne peuvent pas encore se terminer : l'Historique ne les rencontrera qu'avec la Story 11.3, qui devra les résumer.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 11, Story 11.2)
- AD-9 dans `ARCHITECTURE-SPINE.md`

## Dev Agent Record

### Debug Log References

- `testDebugUnitTest` : 440 tests, 0 échec (dont `CreateFreePartyUseCaseTest` 5, `FreeSetupViewModelTest` 8, `RootViewModelTest` +1, `HomeViewModelTest` +1 ; la création du Papayoo, refactorée, garde ses 27 tests au vert). Un échec intermédiaire venait de mon test (le prénom refusé reste saisi et filtre les suggestions).
- `connectedDebugAndroidTest` sur `Pixel_API_36` : 233 tests, 0 échec, dont `FreeScreensTest` (4) et les noms de jeux en base. Les rendus temporaires (création, partie, accueil avec le nouveau logo) ont été regardés puis retirés ; `FreeScreensTest` relancé ensuite : 4/4.
- Non installé sur le téléphone : une Partie de Jeu libre ne peut pas encore avancer ni se terminer (Story 11.3).

### Completion Notes List

- La liste de Joueurs de la création est devenue le composant `ParticipantDraftEditor`, et ses règles d'ajout et de déplacement les fonctions `withAdded` et `moved`, partagées par le Papayoo et le Jeu libre.
- `PlayerSuggestionChips` accepte une description et un préfixe de tag : il sert aussi aux noms de jeux.
- Les règles (qui gagne, fin) sont sous la liste de Joueurs, compactes, pour que la liste garde sa place, clavier ouvert compris.
- Défauts : le plus petit total gagne, fin à la main ; 100 points et 10 Manches préremplis quand on choisit un score ou un nombre de Manches.

### File List

- `domain/usecase/CreateFreePartyUseCase.kt`, `domain/usecase/ObserveFreeGameNamesUseCase.kt`, `domain/model/FreeGame.kt` (`PlayedGameName`), `domain/repository/FreeGameRepository.kt`, `domain/engine/GameIds.kt`
- `data/dao/PartyDao.kt`, `data/repository/FreeGameRepositoryImpl.kt`, `di/EngineModule.kt`
- `ui/screens/free/` (`FreeSetupUiState`, `FreeSetupViewModel`, `FreeSetupScreen`, `FreeSetupRoute`, `FreePartyViewModel`, `FreePartyScreen`, `FreePartyRoute`), `ui/screens/setup/ParticipantDraftEditor.kt`, `ParticipantDrafts.kt`, `SetupScreen.kt`, `SetupViewModel.kt`, `ui/components/PlayerSuggestionChips.kt`, `GamePresentation.kt`, `ui/screens/home/HomeViewModel.kt`, `ui/navigation/Routes.kt`, `NavGraph.kt`, `RootViewModel.kt`
- `res/drawable/ic_free_logo.xml`, `res/values/strings.xml`
- Tests : `CreateFreePartyUseCaseTest`, `PartyRepositoryAdapter` (et `FakeFreeGameRepository`), `FreeSetupViewModelTest`, `RootViewModelTest`, `HomeViewModelTest`, `FreeGameRepositoryImplTest`, `FreeScreensTest`

### Change Log

- 2026-10-22 : story créée et implémentée (création d'une Partie de Jeu libre).
