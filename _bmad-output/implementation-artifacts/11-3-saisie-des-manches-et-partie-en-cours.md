---
story_id: "11.3"
story_key: "11-3-saisie-des-manches-et-partie-en-cours"
epic: 11
status: done
created: 2026-10-22
baseline_commit: 314ea8d
---

# Story 11.3 : Saisie des Manches et Partie en cours

**Epic :** 11 - Jeu libre
**Story ID :** 11.3
**Status :** done

## Story

En tant qu'utilisateur,
Je veux saisir les scores de chaque Manche et suivre le classement,
Afin de savoir qui mène à tout moment.

## Acceptance Criteria

1. **Given** une Partie de Jeu libre en cours **Then** son écran montre le nom du jeu et ses règles, le **classement** selon le sens de victoire (ex æquo au même rang) avec le badge Donneur, **ce qu'il reste avant la fin** (« Manche 3 sur 10 », ou les points qui séparent du score à atteindre le Joueur qui en est le plus proche ; rien pour une fin à la main) et le **tableau des Manches** (meilleur score de chaque Manche en gras selon le sens de victoire).

2. **And** « Manche N » ouvre la saisie : un champ par Joueur dans l'ordre de table, acceptant les nombres négatifs, avec un bouton **+/−** par champ ; « Valider la manche » n'est actif que lorsque chaque Joueur a un score entier (de −99 999 à 99 999). La Manche et le Donneur suivant s'enregistrent en une transaction.

3. **And** toucher une Manche du tableau la rouvre en **correction**, préremplie, tant que la Partie est en cours ; le Donneur ne bouge pas.

4. **And** la Partie se termine **d'elle-même** dès qu'une Manche (ou une correction) atteint la règle de fin, dans la même transaction ; ou par « **Fin de partie** » après confirmation.

5. **And** une Partie terminée affiche le classement final et le ou les gagnants, le tableau en lecture seule et « Retour à l'accueil » ; l'Historique la résume (gagnants selon le sens de victoire) et l'ouvre sur ce même écran. Les Résultats dédiés avec « Rejouer » et le nom du jeu dans l'Historique viennent avec la Story 11.4.

6. **And** l'écran reste allumé pendant la Partie et pendant la saisie.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 1-5) : `FreePartyState`, `ObserveFreePartyUseCase`, `PlayFreeRoundUseCase`, `CorrectFreeRoundUseCase` (la correction peut terminer la Partie : `FreeGameRepository.correctRound` reçoit la Partie mise à jour), résumé des Parties de Jeu libre dans l'Historique ; tests JVM
- [x] **T2 - Données** (AC: 3, 4) : correction et fin dans la même transaction ; test Room
- [x] **T3 - Interface** (AC: 1-6) : `ScoreField` signé (+/−), `RoundTable` sans couleur du 7 et selon le sens de victoire, `FreePartyScreen` (en cours et terminée), `FreeRoundScreen`/`ViewModel`, route de saisie, Historique ; tests ViewModel et Compose
- [x] **T4 - Validation** : `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` API 36

## Dev Notes

- Choix validés le 2026-10-22 (epics.md, Epic 11). Le moteur `free` (Story 11.1) porte le classement, la validité d'une Manche et la fin.
- Score à atteindre : la Partie finit quand un total l'atteint, quel que soit le sens de victoire (Skyjo : 100 points, le plus petit total gagne). Le reste affiché est donc mesuré depuis le plus grand total.
- Fin à la main possible à tout moment, comme au Papayoo.
- Une Partie de Jeu libre terminée n'a pas de couleur du 7 : sans résumé dédié, l'Historique échouait en relisant ses Manches comme des Manches de Papayoo.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 11, Story 11.3)
- AD-9 dans `ARCHITECTURE-SPINE.md`
- Story 7.3 (saisie et correction de la Belote), Story 5.1 (fin de partie confirmée)

## Dev Agent Record

### Debug Log References

- `testDebugUnitTest` : 470 tests, 0 échec (440 avant ; dont `FreeRoundUseCasesTest` 15, `FreePartyViewModelsTest` 14, `RoundTableTest` +1). `kspDebugKotlin`, `assembleDebug` et `compileDebugAndroidTestKotlin` passent.
- `connectedDebugAndroidTest` sur `Pixel_API_36` : 238 tests, 0 échec (233 avant), dont `FreeScreensTest` (8) et la correction qui termine la Partie dans `FreeGameRepositoryImplTest`.
- Non vérifié : une Partie complète jouée à la main dans l'app (saisie au clavier réel, signe « - » selon le clavier), ni l'installation sur le téléphone.

### Completion Notes List

- `FreeGameRepository.correctRound` reçoit la Partie mise à jour, comme la Belote : une correction qui atteint la règle de fin termine la Partie dans la même transaction.
- Score à atteindre : le reste affiché se mesure depuis le plus grand total, avec le ou les Joueurs qui en sont les plus proches.
- Une Partie terminée reste sur l'écran de la Partie : gagnants, classement final, tableau en lecture seule et « Retour à l'accueil » ; l'Historique l'ouvre sur ce même écran. L'écran ne reste allumé que tant que la Partie est en cours.
- L'Historique résume une Partie de Jeu libre par le moteur (gagnants selon le sens de victoire) : auparavant, une Partie de Jeu libre terminée aurait fait échouer toute la liste, ses Manches n'ayant pas de couleur du 7.
- `ScoreField` gagne un bouton +/− optionnel ; `RoundTable` accepte des Manches sans couleur du 7 et un meilleur score « le plus grand » ; le dialogue de fin de partie du Papayoo est partagé.
- Reste pour la Story 11.4 : Résultats dédiés avec « Rejouer », nom du jeu dans l'Historique, palmarès du profil.

### File List

- `domain/model/FreeGame.kt` (`FreePartyState`), `domain/usecase/ObserveFreePartyUseCase.kt`, `domain/usecase/PlayFreeRoundUseCase.kt` (et `CorrectFreeRoundUseCase`), `domain/usecase/ObservePartyHistoryUseCase.kt`, `domain/repository/FreeGameRepository.kt`
- `data/repository/FreeGameRepositoryImpl.kt`
- `ui/components/RoundTable.kt`, `ui/components/ScoreField.kt`, `ui/screens/free/` (`FreePartyViewModel`, `FreePartyScreen`, `FreePartyRoute`, `FreeRoundViewModel`, `FreeRoundScreen`, `FreeRoundRoute`), `ui/screens/party/PartyScreen.kt`, `ui/screens/history/HistoryViewModel.kt`, `ui/navigation/Routes.kt`, `NavGraph.kt`
- `res/values/strings.xml`
- Tests : `FakeFreeStore`, `FreeRoundUseCasesTest`, `FreePartyViewModelsTest`, `PartyRepositoryAdapter`, `RoundTableTest`, `BeloteUseCasesTest`, `PartyHistoryUseCasesTest`, `BeloteViewModelsTest`, `HistoryViewModelTest`, `FreeGameRepositoryImplTest`, `FreeScreensTest`

### Change Log

- 2026-10-22 : story créée et implémentée (saisie des Manches et Partie en cours du Jeu libre).
