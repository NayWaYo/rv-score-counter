---
story_id: "7.3"
story_key: "7-3-partie-belote-creation-saisie-et-suivi"
epic: 7
status: done
created: 2026-10-21
baseline_commit: 2daa73e
---

# Story 7.3 : Partie de Belote — création, saisie et suivi

**Epic :** 7 - Belote classique
**Story ID :** 7.3
**Status :** done

Fusion des anciennes stories 7.3, 7.4 et 7.5 (décision de l'utilisateur, 2026-10-21) : la Belote n'est livrée qu'une fois jouable de bout en bout.

## Story

En tant qu'utilisateur,
Je veux créer une Partie de Belote, saisir chaque Manche en quelques gestes et la voir se terminer d'elle-même,
Afin de jouer une Partie complète sans calcul ni erreur.

## Acceptance Criteria

1. **Given** l'Accueil **When** il s'affiche **Then** la Belote entre au catalogue (interface `Game`, AD-8) et sa tuile suit celle du Papayoo (`GameIds.CATALOG_ORDER`), avec un logo ambre au gabarit du Papayoo : le roi et la dame de cœur en éventail, le bas libre pour le nom.

2. **Given** la tuile Belote touchée **When** l'écran de création s'affiche **Then** on saisit le nom des deux Équipes (« Nous » et « Eux » par défaut) et deux joueurs par Équipe, et on choisit la fin : score cible (1000 par défaut) ou nombre de Manches (10 par défaut), modifiable. L'ordre de table A1, B1, A2, B2 et le premier Donneur (A1) sont visibles. « Commencer » n'est actif qu'avec 4 joueurs, 2 noms d'Équipes et une valeur de fin entre 1 et 9999.

3. **Given** une Partie de Belote en cours **When** l'écran de Partie s'affiche, y compris à la reprise au lancement **Then** il montre les deux Équipes, leurs joueurs et leur score cumulé (l'Équipe en tête mise en avant), la progression vers la fin (points restants ou Manche n sur N), le Donneur, le rappel de distribution, la réserve de litige en attente et le tableau des Manches (Équipe preneuse, résultat, points de chaque Équipe).

4. **Given** la saisie d'une Manche **When** je choisis l'Équipe preneuse et saisis les points de plis d'une Équipe **Then** ceux de l'autre sont déduits sur 162 ; je peux attribuer la belote-rebelote et le capot à une Équipe ; le résultat et les points de chaque Équipe, réserve comprise, s'affichent avant validation ; une valeur hors de 0 à 162 est refusée avec un message.

5. **And** une Manche validée se corrige en la touchant dans le tableau, avec les mêmes contrôles ; les Manches suivantes sont recalculées par le moteur.

6. **Given** une Manche validée ou corrigée qui remplit la condition de fin **When** elle est enregistrée **Then** la Partie passe à l'état terminé dans la même transaction et l'écran des résultats affiche l'Équipe gagnante et les scores finaux ; « Rejouer » relance une Partie avec les mêmes Équipes, joueurs et règle de fin.

7. **And** les use cases du Papayoo refusent une Partie de Belote, l'Historique n'affiche que le Papayoo jusqu'à la story 7.4, et toutes les chaînes viennent de `strings.xml`.

## Tasks / Subtasks

- [x] **T1 - Catalogue, logo et routes** (AC: 1, 7)
  - [x] `Game` dans `domain/engine/`, `GameEngine` et `BeloteRules` en héritent ; `GameIds.CATALOG_ORDER` ; `GetGameCatalogUseCase` ; `@IntoMap Game` pour les deux moteurs.
  - [x] `ic_belote_logo`, `gamePresentation` ; routes Belote ; Accueil et reprise selon le Jeu ; Historique limité au Papayoo.
- [x] **T2 - Use cases** (AC: 2-6)
  - [x] `CreateBelotePartyUseCase` (et `fromParty`), `ObserveBelotePartyUseCase`, `PlayBeloteRoundUseCase`, `CorrectBeloteRoundUseCase`.
  - [x] `BeloteRepository.correctRound` reçoit la Partie mise à jour (fin de Partie atomique).
- [x] **T3 - Écrans** (AC: 2-6)
  - [x] Création, Partie en cours, saisie et correction de Manche, résultats.
- [x] **T4 - Tests et validations** (AC: 1-7)
  - [x] JVM : catalogue, use cases, ViewModels.
  - [x] Compose : écrans Belote, Accueil ; Room : correction avec fin de Partie.
  - [x] `testDebugUnitTest`, `kspDebugKotlin`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

## Dev Notes

- AD-8 précisé : `Game` est la part commune ; la map de `GameEngine` reste au Papayoo.
- Donneur : `Party.currentDealerIndex` sur l'ordre de table (positions 0 à 3), avancé à chaque Manche validée, inchangé par une correction.
- Une Partie de Belote ne se termine que par sa règle de fin (pas de « Fin de partie » manuelle dans cette story).

### References

- [Source: `_bmad-output/planning-artifacts/games/belote.md`]
- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 7.3]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — AD-2, AD-4, AD-6, AD-8]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- `testDebugUnitTest kspDebugKotlin assembleDebug compileDebugAndroidTestKotlin` : 337 tests JVM, 0 échec (dont 18 de `BeloteUseCasesTest` et 20 de `BeloteViewModelsTest`).
- `connectedDebugAndroidTest` sur l'AVD `Pixel_API_36` : 176 tests, 0 échec après correction de deux assertions de test (`substring`, texte porté par un enfant de la carte) ; `BeloteScreensTest` (11) relancé après les retouches visuelles, 0 échec.
- Parcours à la main sur l'émulateur : tuile Belote après le Papayoo, création, Manche 1 en litige avec belote (aperçu 91 / 20, réserve 71), reprise au lancement sur la Partie de Belote, aperçu d'un capot avec réserve gagnée (323), thèmes clair et sombre.
- Non vérifié à la main : une Partie menée jusqu'à sa fin et l'écran des résultats (couverts par les tests JVM et Compose), la correction d'une Manche depuis l'application.

### Completion Notes List

- Stories 7.3, 7.4 et 7.5 fusionnées à la demande de l'utilisateur ; `epics.md` et `sprint-status.yaml` renumérotés (l'historique devient 7.4).
- AD-8 précisé : interface `Game` commune à `GameEngine` et `BeloteRules` ; catalogue `Map<String, Game>` ordonné par `GameIds.CATALOG_ORDER` (Papayoo puis Belote, choix de l'utilisateur). La map de `GameEngine` reste au Papayoo, donc ses use cases refusent une Partie de Belote.
- `BeloteRepository.correctRound` reçoit la Partie mise à jour pour qu'une correction atteignant la fin termine la Partie dans la même transaction (test Room adapté).
- Logo ambre choisi par l'utilisateur parmi trois maquettes (vert, ambre, bleu) : roi et dame de cœur en éventail.
- Retouches après le parcours à la main : en-tête de colonnes du tableau des Manches, segments sélectionnés sur la teinte de l'app au lieu du lavande par défaut de Material, libellé de progression en cas d'égalité, places non encore nommées affichées « Nous 1 », « Eux 1 ».
- L'Historique reste limité au Papayoo jusqu'à la story 7.4.

### File List

- `_bmad-output/planning-artifacts/epics.md`, `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`, `_bmad-output/implementation-artifacts/7-3-partie-belote-creation-saisie-et-suivi.md` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/engine/Game.kt` (nouveau), `GameEngine.kt`, `BeloteRules.kt`, `GameIds.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/BelotePartyState.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/repository/BeloteRepository.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/GetGameCatalogUseCase.kt`, `CreateBelotePartyUseCase.kt`, `ObserveBelotePartyUseCase.kt`, `PlayBeloteRoundUseCase.kt`, `CorrectBeloteRoundUseCase.kt` (nouveaux)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/repository/BeloteRepositoryImpl.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/di/EngineModule.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/components/GamePresentation.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/Routes.kt`, `NavGraph.kt`, `RootViewModel.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/home/HomeViewModel.kt`, `ui/screens/history/HistoryViewModel.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/belote/` (nouveau : création, Partie, saisie, résultats)
- `app/src/main/res/drawable/ic_belote_logo.xml` (nouveau), `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/rvetiveau/rvscorecount/domain/usecase/FakeBeloteStore.kt`, `BeloteUseCasesTest.kt` (nouveaux)
- `app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/belote/BeloteViewModelsTest.kt` (nouveau), `ui/screens/home/HomeViewModelTest.kt`, `ui/navigation/RootViewModelTest.kt`
- `app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/belote/BeloteScreensTest.kt` (nouveau), `data/repository/BeloteRepositoryImplTest.kt`

### Change Log

- 2026-10-21 : Belote jouable de bout en bout (catalogue, logo, création, Partie en cours, saisie et correction de Manche, fin automatique, résultats et « Rejouer »).
