---
story_id: "13.1"
story_key: "13-1-moteur-tarot"
epic: 13
status: done
created: 2026-10-23
baseline_commit: 631e0fc
---

# Story 13.1 : Moteur Tarot

**Epic :** 13 - Tarot
**Story ID :** 13.1
**Status :** done

## Story

En tant qu'utilisateur,
Je veux que l'application applique les règles du Tarot,
Afin de ne plus calculer à la main contrats, bouts, primes et répartition.

## Acceptance Criteria

1. **Given** le nombre de joueurs (3 à 5), le preneur, le contrat, le partenaire éventuel (à 5), les bouts et les points de l'attaque, le petit au bout, les poignées et le chelem **When** le moteur calcule la Manche **Then** il renvoie, selon `games/tarot.md` : le seuil selon les bouts (56, 51, 41, 36), le contrat fait (égalité comprise) ou chuté, la mise de l'attaque contre chaque défenseur `S` et les points de chaque joueur.

2. **And** `S` = contrat `(25 + écart) × multiplicateur` (Prise 1, Garde 2, Garde sans 4, Garde contre 6), signé selon le résultat, + petit au bout `10 × multiplicateur` pour le camp qui l'a mené, + poignées 20 / 30 / 40 non multipliées pour le camp qui gagne le contrat, + chelem (+400 annoncé et réussi, +200 réussi sans annonce, −200 annoncé et raté, −200 réussi par la défense).

3. **And** la répartition donne au preneur `S` fois le nombre de défenseurs, moins la part `S` de son partenaire à 5, et `−S` à chaque défenseur : à 3, 2S ; à 4, 3S ; à 5 avec partenaire, 2S et S ; à 5 seul, 4S ; la somme est toujours nulle.

4. **And** le moteur signale une saisie impossible par un type fermé, sans calcul : nombre de joueurs hors de 3 à 5, preneur absent de la Partie, bouts hors de 0 à 3, points hors de 0 à 91, partenaire hors d'une Partie à 5, égal au preneur ou absent de la Partie.

5. **And** le moteur donne le classement (plus grand total en tête, égalités au même rang, puis ordre de table) et l'état de la Partie : en cours sans règle de fin, terminée après le nombre de Manches prévu avec le ou les gagnants ; une règle « score cible » est refusée.

6. **And** le contrat de domaine `TarotRules` (AD-10) est exposé sous la clé canonique `GameIds.TAROT`, injectable par un `@Binds` simple ; il n'entre pas encore dans le catalogue de l'Accueil.

7. **And** chaque règle est couverte par un test JVM.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 1-6)
  - [x] `GameIds.TAROT`, hors de `CATALOG_ORDER` jusqu'à la Story 13.3 (la vérification des sauvegardes accepterait sinon une Partie de Tarot dont les Manches ne sont pas encore enregistrées) ; modèles `TarotContract`, `TarotSide`, `Poignee`, `Chelem`, `TarotRoundInput`, `TarotRoundResult`, `TarotRound`, `TarotStanding`, `TarotPartyStatus`, `TarotInputError`
  - [x] Contrat `TarotRules` dans `domain/engine/`
- [x] **T2 - Moteur** (AC: 1-5)
  - [x] `game/tarot/TarotEngine`, `@Binds` dans `EngineModule` sans `@IntoMap`
  - [x] AD-10 dans l'architecture
- [x] **T3 - Tests JVM** (AC: 7) : seuils, fait et chuté, chaque contrat, chaque prime, chaque répartition, somme nulle, refus, classement, fin
- [x] **T4 - Validation** : `testDebugUnitTest`, `assembleDebug`

## Dev Notes

- Choix validés le 2026-10-23 (Epic 13) : 3 à 5 joueurs, toutes les primes, points entiers, fin manuelle ou nombre de Manches.
- Les noms de jeu restent en français dans le code quand l'anglais n'a pas d'équivalent clair, comme `capot` et `belote` pour la Belote : `bouts`, `petitAuBout`, `Poignee`, `Chelem`, et les contrats `PRISE`, `GARDE`, `GARDE_SANS`, `GARDE_CONTRE` (noms persistés en 13.2).
- Formule de répartition unique : l'attaque gagne `S × défenseurs` ; le partenaire en prend `S`, le preneur le reste.
- Les poignées sont saisies par camp (attaque, défense) ; toutes vont au camp qui gagne le contrat.
- Aucune évolution Room ni écran dans cette story.

## References

- `_bmad-output/planning-artifacts/games/tarot.md`
- `_bmad-output/planning-artifacts/epics.md` (Epic 13, Story 13.1)
- `ARCHITECTURE-SPINE.md` (AD-8, AD-9)

## Dev Agent Record

### Debug Log References

- JVM : `./gradlew testDebugUnitTest` → 525 tests, 0 échec (508 avant la story) ; nouveau : `TarotEngineTest` (17), dont un balayage de plus de 4 000 Manches (3, 4 et 5 joueurs, tous contrats, bouts, points de 7 en 7, partenaires et primes variés) qui vérifie que chaque Manche fait une somme nulle.
- Build : `kspDebugKotlin assembleDebug` OK (binding Hilt de `TarotRules`).
- Instrumentés non relancés : ni Room ni Compose ne changent.
- `git diff --check` propre.
- Premier essai : ajouter `TAROT` à `CATALOG_ORDER` faisait échouer `BackupReaderTest` et `BeloteUseCasesTest`, qui prennent « tarot » comme Jeu inconnu ; le Tarot reste donc hors de l'ordre du catalogue jusqu'à la Story 13.3.

### Completion Notes List

- `TarotEngine` calcule `S` (contrat signé, petit au bout multiplié, poignées et chelem non multipliés) puis répartit : l'attaque gagne `S × défenseurs`, le partenaire en prend `S`, chaque défenseur perd `S`.
- `inputError` renvoie la première erreur de saisie (`TarotInputError`) ; `scoreRound` refuse toute saisie en erreur. Les écrans de la Story 13.3 s'en serviront pour leurs messages.
- Classement : plus grand total en tête, égalités au même rang, puis ordre de table ; fin par `EndRule.RoundCount` ou à la main, `TargetScore` refusée.
- AD-10 ajoutée à `ARCHITECTURE-SPINE.md`.

### File List

- `app/src/main/java/com/rvetiveau/rvscorecount/domain/engine/GameIds.kt` (`TAROT`)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/engine/TarotRules.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/TarotRound.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/game/tarot/TarotEngine.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/di/EngineModule.kt`
- `app/src/test/java/com/rvetiveau/rvscorecount/game/tarot/TarotEngineTest.kt` (nouveau)
- `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` (AD-10)
- `_bmad-output/planning-artifacts/games/tarot.md` (nouveau), `_bmad-output/planning-artifacts/epics.md` (Epic 13), `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-10-23 : Story 13.1 implémentée (moteur Tarot, contrat `TarotRules`, AD-10), testée en JVM.
