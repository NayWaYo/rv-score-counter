---
story_id: "13.3"
story_key: "13-3-partie-de-tarot-creation-saisie-et-suivi"
epic: 13
status: done
created: 2026-10-23
baseline_commit: cc4e91d
---

# Story 13.3 : Partie de Tarot — création, saisie et suivi

**Epic :** 13 - Tarot
**Story ID :** 13.3
**Status :** done

## Story

En tant qu'utilisateur,
Je veux créer une Partie de Tarot, saisir chaque Manche en quelques gestes et suivre le classement,
Afin de jouer une Partie complète sans calcul.

## Acceptance Criteria

1. **Given** l'accueil **Then** le Tarot entre dans le catalogue (`CATALOG_ORDER`, après la Belote et avant le Jeu libre) et sa tuile montre un logo au même gabarit que les autres.

2. **Given** la tuile Tarot touchée **Then** la création demande les joueurs (3 à 5, ordre de table modifiable, suggestions de prénoms, doublons refusés, même éditeur que les autres Jeux) et la fin (manuelle par défaut, ou nombre de Manches, 10 par défaut) ; « Commencer » n'est actif qu'avec 3 à 5 joueurs et une fin valide ; l'icône Historique ouvre l'historique du Tarot.

3. **Given** une Partie de Tarot en cours, y compris à la reprise au lancement de l'application **Then** l'écran montre la règle de fin, la progression (« Manche n sur N » s'il y a une limite), le classement avec le Donneur, le rappel de distribution pour le nombre de joueurs, chaque Manche jouée (preneur, partenaire, contrat, fait ou chuté et de combien) et le tableau des points par joueur ; l'écran reste allumé.

4. **Given** la saisie d'une Manche **Then** on choisit le preneur, le contrat, le partenaire (à 5 seulement, ou « Seul »), les bouts (0 à 3) et les points de l'attaque (0 à 91, ceux de la défense affichés), puis les primes : petit au bout (attaque ou défense), poignée de l'attaque, poignée de la défense, chelem ; le seuil, le résultat et les points de chaque joueur s'affichent avant validation ; « Valider » n'est actif qu'avec une saisie complète et possible.

5. **And** une Manche validée se corrige depuis la Partie en cours (tableau ou liste des Manches), avec la saisie préremplie et les mêmes contrôles ; le Donneur ne bouge pas.

6. **And** la Partie se termine d'elle-même après la dernière Manche prévue, ou par « Fin de partie » après confirmation ; les résultats affichent le ou les gagnants et le classement final, avec « Rejouer » (mêmes joueurs dans le même ordre, même fin) et « Retour à l'accueil ».

7. **And** une Partie de Tarot terminée apparaît dans l'Historique avec son ou ses gagnants et son meilleur total, et son détail est la Partie en lecture seule (avance sur la Story 13.4, qui ajoute les statistiques et le palmarès).

## Tasks / Subtasks

- [x] **T1 - Catalogue et accueil** (AC: 1)
  - [x] `CATALOG_ORDER` avec le Tarot, `@IntoMap` du moteur, présentation (nom, logo `ic_tarot_logo`), route de l'accueil
  - [x] Tests mis à jour (« tarot » n'est plus un Jeu inconnu)
- [x] **T2 - Domaine** (AC: 2-7)
  - [x] `TarotPartyState`, `ObserveTarotPartyUseCase`, `CreateTarotPartyUseCase` (et « Rejouer »), `PlayTarotRoundUseCase`, `CorrectTarotRoundUseCase`, rappel de distribution du moteur
  - [x] Résumé des Parties de Tarot dans `ObservePartyHistoryUseCase`, reprise et routes de l'Historique
  - [x] Tests JVM
- [x] **T3 - Interface** (AC: 2-7)
  - [x] Création, Partie (en cours, lecture seule, résultats), saisie et correction d'une Manche ; routes et navigation
  - [x] Tests JVM des ViewModels et tests Compose
- [x] **T4 - Validation** : `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` API 36

## Dev Notes

- Modèle : le Jeu libre (Stories 11.2 à 11.4), même découpage `Setup`, `Party` (avec un mode résultats), `Round`.
- Le tableau des Manches réutilise `RoundTable` (points par joueur, le plus grand étant le meilleur) ; la liste des Manches au-dessus donne preneur, contrat et résultat.
- Le rappel de distribution vient du moteur (`TarotRules.dealing`) : à 3 joueurs 24 cartes et chien de 6, à 4 joueurs 18 et 6, à 5 joueurs 15 et 3.
- Le Tarot entre dans `CATALOG_ORDER` : `BeloteUseCasesTest` prenait « tarot » comme Jeu inconnu, il prend désormais « poker ».

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 13, Story 13.3)
- `_bmad-output/planning-artifacts/games/tarot.md`
- Stories 11.2 à 11.4 (Jeu libre), 13.1 et 13.2

## Dev Agent Record

### Debug Log References

- JVM : `./gradlew testDebugUnitTest` → 558 tests, 0 échec (532 avant la story) ; nouveaux : `TarotUseCasesTest` (13), `TarotViewModelsTest` (12), `TarotEngineTest` +1 (distribution) ; les tests qui construisent `ObservePartyHistoryUseCase` reçoivent le dépôt et le moteur du Tarot ; `BeloteUseCasesTest` prend « poker » comme Jeu inconnu.
- Build : `kspDebugKotlin assembleDebug compileDebugAndroidTestKotlin` OK.
- Instrumentés API 36 : `connectedDebugAndroidTest` → 269 tests, 0 échec (262 avant) ; nouveau : `TarotScreensTest` (7).
- `git diff --check` propre.
- Non vérifié : une Partie complète jouée à la main dans l'application (aucun parcours de bout en bout automatisé) ; le rendu du logo et des puces de saisie sur le téléphone.

### Completion Notes List

- Le Tarot entre dans `CATALOG_ORDER` (Papayoo, Belote, Tarot, Jeu libre) et dans la map `Game` ; tuile avec le logo `ic_tarot_logo` (deux cartes en éventail, étoile ambre).
- Domaine : `TarotPartyState` (avec `dealing` et `callsPartner` venant du moteur), `ObserveTarotPartyUseCase` qui refuse une Partie incohérente (Jeu, nombre de joueurs, Donneur, trou dans les Manches, Manche impossible), `CreateTarotPartyUseCase` (et « Rejouer »), `PlayTarotRoundUseCase` et `CorrectTarotRoundUseCase` (même découpage que le Jeu libre). `TarotRules.dealing` donne le rappel de distribution.
- Interface : création (même éditeur de joueurs, fin à la main ou nombre de Manches de 1 à 99), Partie (règle de fin, progression, rappel de distribution, classement avec le Donneur, liste des contrats cliquable pour corriger, tableau des points), résultats avec « Rejouer », saisie par puces (preneur, contrat, partenaire à 5, bouts, primes) avec les points de l'attaque, le seuil et la défense affichés, et l'aperçu du résultat calculé par le moteur avant « Valider ».
- Avance sur la Story 13.4 : l'Historique résume les Parties de Tarot (gagnants, meilleur total) et ouvre la Partie en lecture seule ; la reprise au lancement rouvre une Partie de Tarot en cours. Restent pour la 13.4 : l'écran Statistiques et la section du profil.
- `Segments` (choix segmentés du Jeu libre) devient `internal` pour la création du Tarot.

### File List

- Domaine : `domain/engine/GameIds.kt`, `TarotRules.kt`, `domain/model/TarotRound.kt` (`TarotDealing`), `TarotParty.kt` (nouveau), `domain/usecase/ObserveTarotPartyUseCase.kt`, `CreateTarotPartyUseCase.kt`, `PlayTarotRoundUseCase.kt` (nouveaux), `ObservePartyHistoryUseCase.kt`
- Moteur et DI : `game/tarot/TarotEngine.kt`, `di/EngineModule.kt`
- Interface : `ui/screens/tarot/TarotSetupViewModel.kt`, `TarotSetupScreen.kt`, `TarotPartyViewModel.kt`, `TarotPartyScreen.kt`, `TarotRoundViewModel.kt`, `TarotRoundScreen.kt` (nouveaux), `ui/navigation/Routes.kt`, `NavGraph.kt`, `RootViewModel.kt`, `ui/screens/home/HomeViewModel.kt`, `ui/screens/history/HistoryViewModel.kt`, `ui/components/GamePresentation.kt`, `ui/screens/free/FreeSetupScreen.kt`
- Ressources : `res/values/strings.xml`, `res/drawable/ic_tarot_logo.xml` (nouveau)
- Tests : `app/src/test/.../domain/usecase/FakeTarotStore.kt`, `TarotUseCasesTest.kt`, `app/src/test/.../ui/screens/tarot/TarotViewModelsTest.kt`, `app/src/androidTest/.../ui/screens/tarot/TarotScreensTest.kt` (nouveaux) ; `PartyRepositoryAdapter.kt` (`FakeTarotRepository`), `TarotEngineTest.kt`, `BeloteUseCasesTest.kt`, `FreeRecordsAndReplayTest.kt`, `FreeRoundUseCasesTest.kt`, `PartyHistoryUseCasesTest.kt`, `BeloteViewModelsTest.kt`, `HistoryViewModelTest.kt`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-10-23 : Story 13.3 implémentée (Tarot jouable de bout en bout : création, saisie et correction, Partie, résultats, Historique), testée JVM et API 36.
