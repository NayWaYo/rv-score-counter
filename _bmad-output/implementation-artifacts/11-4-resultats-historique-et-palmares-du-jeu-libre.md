---
story_id: "11.4"
story_key: "11-4-resultats-historique-et-palmares-du-jeu-libre"
epic: 11
status: done
created: 2026-10-22
baseline_commit: 416af32
---

# Story 11.4 : Résultats, historique et palmarès du Jeu libre

**Epic :** 11 - Jeu libre
**Story ID :** 11.4
**Status :** done

## Story

En tant qu'utilisateur,
Je veux retrouver mes Parties de Jeu libre et le palmarès de chacun,
Afin de comparer les joueurs sur tous nos jeux.

## Acceptance Criteria

1. **Given** une Partie de Jeu libre qui vient de se terminer (par sa règle de fin ou par « Fin de partie ») **Then** les **Résultats** s'ouvrent à la place de la Partie : le nom du jeu, le ou les gagnants, le classement final et le tableau des Manches, avec « **Rejouer** » et « **Retour à l'accueil** ».

2. **And** « Rejouer » crée une nouvelle Partie en cours avec le même jeu, les mêmes règles (sens de victoire, fin de partie) et les mêmes Joueurs dans le même ordre, le premier donnant, et l'ouvre ; un échec le dit et laisse les Résultats.

3. **And** l'**Historique** affiche le **nom du jeu** (« Skyjo ») à la place de « Jeu libre » pour ces Parties ; les toucher ouvre leur détail en **lecture seule** (gagnants, classement final, tableau), sans « Rejouer ».

4. **And** le **profil** d'un Joueur (Story 10.3) gagne une section « Jeu libre » : son taux de victoire tous jeux libres confondus, puis **par nom de jeu**, du plus joué au moins joué (à égalité, par nom). Une victoire est un total au meilleur rang, égalités comprises ; une Partie terminée sans Manche compte sans victoire. Les noms qui ne diffèrent que par la casse ou les espaces sont un même jeu, affiché comme dans sa Partie la plus récente.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 2-4) : `CreateFreePartyUseCase.fromParty`, nom du jeu dans `PartySummary`, `FreeRecords` et `freeRecords`, section Jeu libre de `GetPlayerRecordsUseCase` ; tests JVM
- [x] **T2 - Interface** (AC: 1-4) : route des Résultats, mode Résultats de l'écran de Partie avec « Rejouer », navigation vers les Résultats à la fin, nom du jeu dans l'Historique, section du profil ; tests ViewModel et Compose
- [x] **T3 - Validation** : `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` API 36

## Dev Notes

- Choix validés le 2026-10-22 (epics.md, Epic 11). Aucun changement Room : le nom du jeu et les règles sont déjà portés par la Partie (Story 11.1).
- Les Résultats et le détail réutilisent la vue « Partie terminée » de la Story 11.3 ; seuls les Résultats proposent « Rejouer ».
- « Rejouer » relie les Joueurs par leur prénom, comme la Belote (Story 7.3).

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 11, Story 11.4)
- Story 10.3 (profil et palmarès), Story 7.3 (« Rejouer » de la Belote)

## Dev Agent Record

### Debug Log References

- `testDebugUnitTest` : 479 tests, 0 échec (470 avant ; dont `FreeRecordsAndReplayTest` 6 et `FreePartyViewModelsTest` +3). `kspDebugKotlin`, `assembleDebug` et `compileDebugAndroidTestKotlin` passent.
- `connectedDebugAndroidTest` sur `Pixel_API_36` : 241 tests, 0 échec (238 avant), dont les Résultats et le détail (`FreeScreensTest`), le nom du jeu dans l'Historique et la section Jeu libre du profil.
- Non vérifié : une Partie complète jouée à la main jusqu'aux Résultats puis « Rejouer », ni l'installation sur le téléphone.

### Completion Notes List

- Les Résultats (`Routes.FreeResults`) sont l'écran de la Partie en mode Résultats : gagnants, classement final, tableau, « Rejouer » et « Retour à l'accueil ». Ils s'ouvrent à la place de toute la pile quand une Manche, une correction ou « Fin de partie » termine la Partie.
- Le détail ouvert depuis l'Historique est le même écran en lecture seule, sans bouton : le retour système ramène à l'Historique.
- « Rejouer » (`CreateFreePartyUseCase.fromParty`) refuse une Partie qui n'est pas un Jeu libre terminé et repasse par toutes les validations de la création.
- `PartySummary.gameName` porte le nom du jeu de toute Partie qui en a un ; l'Historique l'affiche à la place du nom du Jeu.
- Profil : section « Jeu libre » avec le taux global puis une ligne par jeu (« Par jeu »), sur le modèle des coéquipiers de la Belote, dont la ligne est devenue `RecordRow`.

### File List

- `domain/model/PlayerRecords.kt` (`FreeRecords`, `FreeGameRecord`, `FreePlay`, `freeRecords`), `domain/model/PartySummary.kt`, `domain/usecase/CreateFreePartyUseCase.kt`, `domain/usecase/GetPlayerRecordsUseCase.kt`, `domain/usecase/ObservePartyHistoryUseCase.kt`
- `ui/screens/free/` (`FreePartyViewModel`, `FreePartyScreen`, `FreePartyRoute`, `FreeRoundViewModel`), `ui/screens/profile/PlayerProfileScreen.kt`, `ui/components/PartySummaryRow.kt`, `ui/navigation/Routes.kt`, `NavGraph.kt`
- `res/values/strings.xml`
- Tests : `FreeRecordsAndReplayTest`, `FakeFreeStore`, `FreePartyViewModelsTest`, `GetPlayerRecordsUseCaseTest`, `PlayerProfileViewModelTest`, `FreeScreensTest`, `HistoryScreenTest`, `PlayerProfileScreenTest`

### Change Log

- 2026-10-22 : story créée et implémentée (résultats, historique et palmarès du Jeu libre).
