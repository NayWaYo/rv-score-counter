---
story_id: "7.1"
story_key: "7-1-moteur-belote-calcul-manche-et-fin-de-partie"
epic: 7
status: done
created: 2026-10-21
baseline_commit: 8786ed3
---

# Story 7.1 : Moteur Belote — calcul d'une Manche et fin de Partie

**Epic :** 7 - Belote classique
**Story ID :** 7.1
**Status :** done

## Story

En tant qu'utilisateur,
Je veux que l'application applique les règles de la Belote classique,
Afin de ne plus calculer à la main les cas fait, dedans, capot et litige.

## Acceptance Criteria

1. **Given** l'Équipe preneuse, ses points de plis (0 à 162, la défense ayant le reste), l'Équipe de la belote-rebelote éventuelle et l'Équipe capot éventuelle **When** le moteur calcule la Manche **Then** il renvoie le résultat (fait, dedans, litige, capot) et les points de chaque Équipe selon `belote.md` : fait si le total du preneur, belote comprise, dépasse celui de la défense ; dedans à 162 pour la défense ; capot à 252 ; belote toujours gardée par l'Équipe qui l'a annoncée.

2. **And** une égalité, belote comprise, produit un litige : la défense marque ses plis et sa belote, le preneur sa belote seulement, et ses plis passent en réserve.

3. **And** la réserve, cumulée sur plusieurs litiges d'affilée, revient à l'Équipe gagnante de la Manche suivante (le preneur s'il fait ou capote, la défense s'il est dedans ou capoté) ; le calcul d'une Partie entière rejoue les Manches dans l'ordre pour la reconstruire.

4. **And** le moteur indique si la Partie est finie selon sa règle de fin (score cible ou nombre de Manches) et quelle Équipe gagne ; une égalité de totaux à la fin impose une Manche de plus.

5. **And** des entrées incohérentes (points hors de 0 à 162, capot contredit par les points, règle de fin non positive) sont refusées par `require`.

6. **And** le moteur expose le contrat de domaine `BeloteRules` (AD-8) sous la clé canonique `GameIds.BELOTE`, injectable par Hilt, mais n'entre pas dans le catalogue des Jeux : l'Accueil et le Papayoo sont inchangés.

7. **And** chaque règle est couverte par un test JVM.

## Tasks / Subtasks

- [x] **T1 - Modèles de domaine** (AC: 1-5)
  - [x] `TeamSide`, `TeamPoints`, `BeloteRoundInput`, `BeloteOutcome`, `BeloteRoundResult`, `BeloteEndRule`, `BelotePartyStatus` dans `domain/model/`.
- [x] **T2 - Contrat et moteur** (AC: 1-6)
  - [x] `BeloteRules` dans `domain/engine/`, `GameIds.BELOTE`.
  - [x] `BeloteEngine` dans `game/belote/` ; `@Binds` simple dans `EngineModule`.
- [x] **T3 - Tests et validations** (AC: 7)
  - [x] JVM : `BeloteEngineTest`.
  - [x] `testDebugUnitTest`, `kspDebugKotlin`, `assembleDebug`.

## Dev Notes

- AD-8 amende AD-2 : `GameEngine` reste le contrat par joueur ; la Belote a son propre contrat de domaine. Le moteur n'implémente pas `GameEngine` dans cette story : sa part commune (catalogue, joueurs) est fixée en 7.3, quand la tuile et l'écran de création existent. L'enregistrer dans la map `@IntoMap` dès maintenant ferait planter l'Accueil (`gamePresentation` échoue sur un `gameId` inconnu).
- Aucune persistance ni UI : Room arrive en 7.2.
- La saisie indique les points de plis du preneur ; la déduction depuis l'autre Équipe (`162 - x`) est exposée par le moteur pour la story 7.4.

### References

- [Source: `_bmad-output/planning-artifacts/games/belote.md`]
- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 7.1]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — AD-2, AD-8]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- `testDebugUnitTest --tests game.belote.*` : 33 tests, 0 échec.
- `testDebugUnitTest kspDebugKotlin assembleDebug compileDebugAndroidTestKotlin` : 288 tests JVM, 0 échec ; graphe Hilt compilé avec le `@Binds` de `BeloteRules`.
- Tests instrumentés non lancés : aucun changement Room ni Compose.
- Non vérifié : aucun écran n'utilise encore le moteur, rien n'est visible dans l'app.

### Completion Notes List

- Conflit relevé avant implémentation : AD-2 impose le contrat `GameEngine` par joueur, inadapté au comptage par Équipe. Choix validé par l'utilisateur : contrat dédié `BeloteRules` (AD-8 ajouté à l'architecture), enregistrement au catalogue repoussé à 7.3 ; critères de 7.1 et 7.3 amendés dans `epics.md`.
- Le résultat d'une Manche inclut la réserve gagnée (`reserveWon`) ; `scoreParty` rejoue les Manches dans l'ordre pour que la correction d'une Manche (7.4) recalcule la réserve des suivantes.
- La réserve encore en attente à la fin d'une Partie est ignorée par `partyStatus`, conformément à `belote.md`.

### File List

- `_bmad-output/planning-artifacts/games/belote.md` (nouveau)
- `_bmad-output/planning-artifacts/epics.md`
- `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/TeamSide.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/TeamPoints.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/BeloteRound.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/BeloteEndRule.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/engine/BeloteRules.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/engine/GameIds.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/game/belote/BeloteEngine.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/di/EngineModule.kt`
- `app/src/test/java/com/rvetiveau/rvscorecount/game/belote/BeloteEngineTest.kt` (nouveau)

### Change Log

- 2026-10-21 : document de jeu Belote, epic 7, AD-8, moteur Belote et ses tests.
