---
story_id: "10.3"
story_key: "10-3-profil-et-palmares-d-un-joueur"
epic: 10
status: done
created: 2026-10-22
baseline_commit: 3abac5d
---

# Story 10.3 : Profil et palmarès d'un Joueur

**Epic :** 10 - Profils de joueurs et palmarès
**Story ID :** 10.3
**Status :** done

## Story

En tant qu'utilisateur,
Je veux consulter le palmarès d'un joueur sur toutes ses Parties,
Afin de savoir qui gagne le plus et de chambrer les autres.

## Acceptance Criteria

1. **Given** l'écran « Joueurs » **When** je touche la ligne d'un Joueur **Then** son profil s'ouvre : son prénom en titre, puis ses Parties terminées et la date de sa dernière Partie, tous Jeux confondus ; sans Partie terminée, un message le dit.

2. **And** chaque Jeu a sa section, présente seulement si le Joueur y a terminé au moins une Partie, dont la statistique principale, mise en avant, est le taux de victoire, avec le nombre de victoires et de Parties.

3. **Given** la section Papayoo **Then** une victoire est une Partie terminée au plus petit Score Cumulé, égalités comprises ; elle montre :
   - les **40 pris** : leur total, et leur part des Manches jouées où le détenteur du 7 est noté, en pourcentage (sans pourcentage si aucune ne l'est) ;
   - les **Manches à 0 point** : leur pourcentage des Manches jouées, avec leur nombre ;
   - la **pire Manche** : son score et la date de sa Partie ; à égalité, la plus ancienne ;
   - les **Manches à 250** (tous les points de la Manche) : leur nombre, 0 compris.

4. **Given** la section Belote **Then** une victoire est une Partie terminée gagnée par l'Équipe du Joueur ; elle montre :
   - les **prises** : leur nombre moyen par Partie et le taux de contrats remplis (règle de « 🗿 Chad de la game ») ; seules les Manches où le joueur preneur est noté comptent, et sans prise notée le taux n'apparaît pas ;
   - le **meilleur et le pire coéquipier** : plus haut et plus bas taux de victoire avec le Joueur, avec ce taux et le nombre de Parties ensemble ; à égalité de taux, le plus de Parties ensemble passe devant, puis l'ordre alphabétique ; avec un seul coéquipier, seul le meilleur apparaît.

5. **And** les Parties en cours ne comptent pas ; une Partie terminée dont les données sont incohérentes est ignorée plutôt que de fausser le palmarès.

6. **And** les calculs vivent dans le domaine, réutilisent les classements et le « 🗿 Chad » existants et sont couverts par des tests JVM (égalités, Manches sans 7 ou sans preneur noté, coéquipier unique, Joueur sans Partie terminée).

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 2-6) : `PlayerRecords`, calcul pur par Jeu, `GetPlayerRecordsUseCase` ; tests JVM
- [x] **T2 - Données** (AC: 1, 5) : places d'un Joueur et Joueur de chaque place (`PlayerRepository`) ; tests Room
- [x] **T3 - Interface** (AC: 1-4) : route `PlayerProfile`, ligne cliquable dans « Joueurs », écran du profil ; tests ViewModel et Compose
- [x] **T4 - Validation** : `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` API 36, essai sur le téléphone

## Dev Notes

- Statistiques choisies par l'utilisateur le 2026-10-22 (epics.md, Story 10.3). Le pourcentage des 40 est rapporté aux Manches jouées où le 7 est noté, pour rester comparable entre Parties courtes et longues.
- Les coéquipiers sont regroupés par Joueur (Story 10.1), pas par prénom de la Partie ; leur prénom affiché est celui du Joueur.
- « 250 » est le total de points d'une Manche du moteur (`totalPointsPerRound`), pas une valeur écrite dans l'écran.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 10, Story 10.3)
- `domain/model/Standing.kt` (`computeStandings`), `domain/model/BelotePartyState.kt` (`takerRanking`)

## Dev Agent Record

### Debug Log References

- `testDebugUnitTest` : 408 tests, 0 échec (dont `PlayerRecordsTest` 6, `GetPlayerRecordsUseCaseTest` 2, `PlayerProfileViewModelTest` 3, `PlayersViewModelTest` +1).
- `connectedDebugAndroidTest` sur `Pixel_API_36` : 214 tests, 1 échec au premier passage (`PlayersScreenTest` : la ligne désormais cliquable fusionne ses textes pour TalkBack, le test lit maintenant l'arbre non fusionné) ; `PlayersScreenTest` relancé seul : 5/5. La suite complète n'a pas été relancée après cette correction de test.
- Release installée sur le téléphone, lancement sans erreur ; parcours manuel non fait par moi.

### Completion Notes List

- Le pire coéquipier est choisi parmi les autres que le meilleur : avec des taux égaux, le même joueur ne peut pas être les deux.
- La valeur 250 vient du moteur du Papayoo (`totalPointsPerRound`) et voyage dans `PapayooRecords.fullRoundPoints`.
- Une Partie terminée incohérente (scores hors des Participants, Manche manquante, Belote mal formée) est écartée du palmarès.
- Le profil est calculé à l'ouverture ; il ne se met pas à jour en direct (aucune Partie ne se termine pendant qu'on le regarde).

### File List

- `domain/model/PlayerRecords.kt`, `domain/repository/PlayerRepository.kt` (`PlayerSeat`, `getSeats`, `getPlayerIdsBySeat`), `domain/usecase/GetPlayerRecordsUseCase.kt`
- `data/dao/PlayerDao.kt`, `data/repository/PlayerRepositoryImpl.kt`
- `ui/screens/profile/` (`PlayerProfileViewModel`, `PlayerProfileScreen`, `PlayerProfileRoute`), `ui/screens/players/PlayersScreen.kt`, `PlayersViewModel.kt`, `PlayersRoute.kt`, `ui/navigation/Routes.kt`, `NavGraph.kt`, `res/values/strings.xml`
- Tests : `PlayerRecordsTest`, `GetPlayerRecordsUseCaseTest`, `PlayerProfileViewModelTest`, `PlayersViewModelTest`, `FakePlayerRepository`, `PlayerRepositoryImplTest`, `PlayersScreenTest`, `PlayerProfileScreenTest`

### Change Log

- 2026-10-22 : story créée et implémentée (profil et palmarès d'un Joueur).
