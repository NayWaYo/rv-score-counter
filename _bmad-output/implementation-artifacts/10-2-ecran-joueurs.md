---
story_id: "10.2"
story_key: "10-2-ecran-joueurs"
epic: 10
status: done
created: 2026-10-22
baseline_commit: 22b76bb
---

# Story 10.2 : Écran « Joueurs »

**Epic :** 10 - Profils de joueurs et palmarès
**Story ID :** 10.2
**Status :** done

## Story

En tant qu'utilisateur,
Je veux voir la liste des joueurs et pouvoir renommer l'un d'eux,
Afin de garder des prénoms propres d'une soirée à l'autre.

## Acceptance Criteria

1. **Given** l'écran d'accueil **When** il s'affiche **Then** une icône « Joueurs » (48dp, décrite « Joueurs » pour TalkBack) dans l'en-tête, à côté de l'Historique, ouvre l'écran « Joueurs ».

2. **And** l'écran liste chaque Joueur avec son nombre de Parties terminées et la date de sa dernière Partie (tous Jeux, en cours comprises), du plus récemment joué au plus ancien ; un Joueur sans Partie restante affiche « Aucune partie » et vient en fin de liste, par ordre alphabétique (même ordre que les suggestions, Story 10.1).

3. **And** sans aucun Joueur, l'écran affiche son propre message.

4. **Given** un Joueur **When** je touche son crayon (48dp, « Renommer <prénom> ») **Then** une fenêtre propose son prénom à modifier ; valider renomme le Joueur et tous ses Participants, Historique et Parties en cours compris, en une transaction.

5. **And** un prénom vide est refusé (« Veuillez entrer un prénom ») ; un prénom déjà porté par un autre Joueur, casse et espaces ignorés, est refusé (« <prénom> existe déjà ») ; changer seulement la casse ou les espaces de son propre prénom est accepté ; la fenêtre reste ouverte tant que le prénom est refusé.

6. **And** toucher la ligne d'un Joueur ouvrira son profil avec la Story 10.3 ; d'ici là, la ligne n'est pas cliquable.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 2, 4, 5)
  - [x] `PlayerSummary`, tri commun avec les suggestions ; `PlayerRepository.observePlayers` et `renamePlayer`
  - [x] `RenamePlayerUseCase` (résultat scellé : renommé, vide, déjà pris, Joueur inconnu) ; tests JVM
- [x] **T2 - Données** (AC: 2, 4)
  - [x] Requête des résumés (Parties terminées, dernière Partie) ; renommage `players` + `participants` en transaction
  - [x] Tests instrumentés Room
- [x] **T3 - Interface** (AC: 1-6)
  - [x] Icône Joueurs sur l'accueil, route `Players`, `PlayersScreen`, `PlayersViewModel`, fenêtre de renommage
  - [x] Tests JVM du ViewModel, tests Compose
- [x] **T4 - Validation** : `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` API 36, essai sur le téléphone

## Dev Notes

- La date affichée est celle de la dernière Partie, terminée ou non, comme pour les suggestions ; le compteur ne compte que les Parties terminées (choix validé : le palmarès ne compte que les Parties terminées).
- Renommer écrit aussi `participants.name` : les écrans existants lisent le prénom du Participant, ils suivent donc le renommage sans changement.
- La clé unique `players.nameKey` protège la base ; le domaine vérifie avant d'écrire pour donner un message clair.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 10, Story 10.2)
- Story 10.1 (`10-1-joueurs-enregistres.md`)

## Dev Agent Record

### Debug Log References

- `testDebugUnitTest` : 396 tests, 0 échec (dont `RenamePlayerUseCaseTest` 5, `PlayersViewModelTest` 5, `HomeViewModelTest` +1).
- `connectedDebugAndroidTest` sur `Pixel_API_36` : 209 tests ; 1 échec au premier passage (mon test exigeait 48dp de hauteur pour l'icône « Joueurs », qui en dessine 40 comme l'Historique : Material 3 garantit la zone tactile de 48dp par `minimumInteractiveComponentSize`) ; test corrigé, `HomeScreenTest` (8) et `PlayersScreenTest` (4) relancés : 0 échec. La suite complète n'a pas été relancée après cette correction de test.
- Release installée sur le téléphone de l'utilisateur, lancement sans erreur ; parcours manuel non fait par moi.

### Completion Notes List

- L'icône « Joueurs » est un composant partagé `PlayersButton`, sur le modèle de `HistoryButton`.
- Les faux `PlayerRepository` des tests sont réunis dans `FakePlayerRepository` (tests JVM).
- AC 6 : la ligne d'un Joueur n'est pas encore cliquable ; la Story 10.3 la reliera au profil.

### File List

- `domain/model/PlayerSummary.kt`, `domain/model/KnownPlayer.kt` (tri commun), `domain/repository/PlayerRepository.kt`, `domain/usecase/ObservePlayersUseCase.kt`, `domain/usecase/RenamePlayerUseCase.kt`
- `data/dao/PlayerDao.kt`, `data/dao/ParticipantDao.kt`, `data/repository/PlayerRepositoryImpl.kt`
- `ui/components/PlayersButton.kt`, `ui/screens/players/` (`PlayersUiState`, `PlayersViewModel`, `PlayersScreen`, `PlayersRoute`), `ui/screens/home/HomeScreen.kt`, `HomeViewModel.kt`, `HomeRoute.kt`, `ui/navigation/Routes.kt`, `NavGraph.kt`
- `res/drawable/ic_players.xml`, `res/drawable/ic_edit.xml`, `res/values/strings.xml`
- Tests : `FakePlayerRepository`, `RenamePlayerUseCaseTest`, `PlayersViewModelTest`, `HomeViewModelTest`, `SetupViewModelTest`, `BeloteViewModelsTest`, `PlayerRepositoryImplTest`, `PlayersScreenTest`, `HomeScreenTest`

### Change Log

- 2026-10-22 : story créée et implémentée (écran « Joueurs », renommage).
