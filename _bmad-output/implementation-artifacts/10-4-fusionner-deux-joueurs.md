---
story_id: "10.4"
story_key: "10-4-fusionner-deux-joueurs"
epic: 10
status: done
created: 2026-10-22
baseline_commit: e51b39d
---

# Story 10.4 : Fusionner deux Joueurs

**Epic :** 10 - Profils de joueurs et palmarès
**Story ID :** 10.4
**Status :** done

## Story

En tant qu'utilisateur,
Je veux réunir deux joueurs qui sont la même personne,
Afin que « Val » et « Valentin » n'aient qu'un seul palmarès.

## Acceptance Criteria

1. **Given** le profil d'un Joueur **When** je touche « Fusionner avec… » **Then** la liste des autres Joueurs s'affiche (ordre des suggestions) ; en choisir un ouvre une confirmation « Fusionner <autre> dans <Joueur> ? » qui explique que les Parties de l'autre passent au Joueur, sous son prénom, et que l'autre disparaît, définitivement.

2. **And** confirmer rattache tous les Participants de l'autre Joueur au Joueur du profil, les renomme à son prénom et supprime l'autre Joueur, en une transaction ; le profil se recalcule avec ses nouvelles Parties.

3. **And** la fusion est refusée si les deux Joueurs ont joué dans une même Partie, avec un message nommant le Jeu et la date de cette Partie ; rien ne change.

4. **Given** le profil d'un Joueur sans aucune Partie **Then** « Supprimer ce joueur » est proposé ; après confirmation, le Joueur est supprimé et l'écran revient à la liste des Joueurs. Un Joueur qui a encore une Partie ne peut pas être supprimé (le domaine le refuse, et le bouton n'apparaît pas).

5. **And** un échec d'écriture laisse la fenêtre ouverte avec un message, sans rien changer.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 2-4) : `MergePlayersUseCase`, `DeletePlayerUseCase` (résultats scellés), `PlayerRepository.mergePlayers` / `deletePlayer` ; tests JVM
- [x] **T2 - Données** (AC: 2, 4) : transactions Room ; tests instrumentés
- [x] **T3 - Interface** (AC: 1-5) : boutons du profil, fenêtres de choix, de confirmation et de refus, retour après suppression ; tests ViewModel et Compose
- [x] **T4 - Validation** : `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` API 36, essai sur le téléphone

## Dev Notes

- Les Participants gardent leur identifiant : les Manches, scores, détenteurs du 7 et preneurs de Belote qui les référencent ne bougent pas.
- La clé étrangère `participants.playerId` (sans cascade) empêche aussi en base de supprimer un Joueur qui a encore un Participant.
- Le Joueur conservé est celui du profil ouvert : pour garder l'autre prénom, ouvrir le profil de l'autre.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 10, Story 10.4)
- Stories 10.1 à 10.3

## Dev Agent Record

### Debug Log References

- `testDebugUnitTest` : 418 tests, 0 échec (dont `MergeAndDeletePlayerUseCasesTest` 4, `PlayerProfileViewModelTest` +6).
- `connectedDebugAndroidTest` sur `Pixel_API_36` : 220 tests, 0 échec, dont la fusion en base (places déplacées et renommées, autre Joueur supprimé) et le refus de supprimer un Joueur qui a encore une place (clé étrangère).
- Release installée sur le téléphone, lancement sans erreur ; parcours manuel non fait par moi.

### Completion Notes List

- Le Joueur conservé est celui du profil ; la liste « Fusionner avec… » suit l'ordre des suggestions.
- Le refus nomme la plus ancienne Partie commune.
- Après une fusion, le profil se recalcule ; après une suppression, l'écran revient à « Joueurs ».

### File List

- `domain/repository/PlayerRepository.kt` (`mergePlayers`, `deletePlayer`), `domain/usecase/MergePlayersUseCase.kt`, `domain/usecase/DeletePlayerUseCase.kt`
- `data/dao/PlayerDao.kt`, `data/dao/ParticipantDao.kt`, `data/repository/PlayerRepositoryImpl.kt`
- `ui/screens/profile/PlayerProfileViewModel.kt`, `PlayerProfileScreen.kt`, `PlayerProfileRoute.kt`, `ProfileDialogs.kt`, `ui/navigation/NavGraph.kt`, `res/values/strings.xml`
- Tests : `FakePlayerRepository`, `MergeAndDeletePlayerUseCasesTest`, `PlayerProfileViewModelTest`, `PlayerRepositoryImplTest`, `PlayerProfileScreenTest`

### Change Log

- 2026-10-22 : story créée et implémentée (fusion et suppression de Joueurs).
