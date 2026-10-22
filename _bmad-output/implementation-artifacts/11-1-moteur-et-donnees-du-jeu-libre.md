---
story_id: "11.1"
story_key: "11-1-moteur-et-donnees-du-jeu-libre"
epic: 11
status: done
created: 2026-10-22
baseline_commit: b8471d2
---

# Story 11.1 : Moteur et données du Jeu libre

**Epic :** 11 - Jeu libre
**Story ID :** 11.1
**Status :** done

## Story

En tant qu'utilisateur,
Je veux que l'application sache compter n'importe quel jeu,
Afin de ne pas dépendre des seuls jeux qu'elle connaît.

## Acceptance Criteria

1. **Given** une Partie de Jeu libre **Then** elle enregistre le nom du jeu, le sens de victoire (le plus petit ou le plus grand total gagne) et sa fin : manuelle (aucune règle), score à atteindre ou nombre de Manches.

2. **And** un contrat de domaine `FreeGameRules` (AD-9), implémenté dans `game/free/`, accepte de 2 à 10 Joueurs et des scores entiers de −99 999 à 99 999 par Joueur et par Manche, négatifs compris, sans total imposé ; une Manche est valide quand chaque Joueur de la Partie a exactement un score.

3. **And** le classement suit le sens de victoire, égalités au même rang, puis l'ordre de table ; une Partie avec un score à atteindre se termine après la Manche où un total atteint ou dépasse ce score ; avec un nombre de Manches, après la dernière ; sans règle, seulement à la demande ; les gagnants sont tous les Joueurs au meilleur total.

4. **And** le schéma Room passe en version 6 : `parties.gameName` et `parties.winDirection` (nuls pour les autres Jeux) et une couleur du 7 facultative pour `rounds.suit`, par une `AutoMigration` 5 → 6 testée sans perte sur un vrai fichier de version 5 ; une Manche du Papayoo sans couleur reste une incohérence signalée.

5. **And** un `FreeGameRepository` enregistre une Manche avec tous ses scores et le Donneur suivant en une transaction, corrige les scores d'une Manche validée, et relit les Manches d'une Partie, en direct ou une fois ; la Partie et ses Participants sont créés par `PartyRepository` comme les autres Jeux.

6. **And** la règle de fin de la Belote devient la règle de fin commune (`EndRule`), sans changement de comportement ; le Jeu libre n'apparaît pas encore sur l'accueil (Story 11.2).

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 1-3, 6) : `EndRule` (renommage de `BeloteEndRule`), `WinDirection`, `FreeRound`, `FreeGameRules`, `GameIds.FREE` ; tests JVM
- [x] **T2 - Moteur** (AC: 2, 3) : `game/free/FreeGameEngine`, `@Binds` ; tests JVM
- [x] **T3 - Données** (AC: 4, 5) : colonnes, `AutoMigration` 5 → 6, schéma 6 exporté, `FreeGameRepository` ; tests instrumentés (migration, repository)
- [x] **T4 - Architecture** : AD-9 dans `ARCHITECTURE-SPINE.md`
- [x] **T5 - Validation** : `testDebugUnitTest`, `kspDebugKotlin`, `assembleDebug`, `connectedDebugAndroidTest` API 36

## Dev Notes

- Choix validés par l'utilisateur le 2026-10-22 (epics.md, Epic 11).
- Les Manches du Jeu libre réutilisent `rounds` et `round_scores` : une Manche sans couleur du 7 n'appartient qu'au Jeu libre. Le modèle `Round` du Papayoo garde une couleur obligatoire ; le Jeu libre a son propre modèle `FreeRound`.
- Score à atteindre : la Partie s'arrête quand un total atteint ou dépasse la cible, quel que soit le sens de victoire (Skyjo : 100, le plus petit total gagne).

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 11)
- AD-8 (contrat dédié de la Belote) dans `ARCHITECTURE-SPINE.md`

## Dev Agent Record

### Debug Log References

- `testDebugUnitTest` : 425 tests, 0 échec (dont `FreeGameEngineTest` 7 ; un premier échec venait d'un total mal calculé dans mon test, pas du moteur).
- `kspDebugKotlin`, `assembleDebug` : OK ; schéma `6.json` exporté (`rounds.suit` facultative, `parties.gameName` et `parties.winDirection`).
- `connectedDebugAndroidTest` sur `Pixel_API_36` : 228 tests, 0 échec, dont `openingAVersion5DatabaseAddsTheJeuLibreAndKeepsEverything` (vrai fichier v5) et `FreeGameRepositoryImplTest` (7).
- Non vérifié : la migration 5 → 6 sur la base du téléphone ; aucun écran n'utilise encore le Jeu libre.

### Completion Notes List

- `BeloteEndRule` est renommée `EndRule` (fichier `EndRule.kt`, 18 fichiers touchés par un renommage mécanique), sans changement de comportement ; ses valeurs par défaut restent celles de la Belote.
- Le Jeu libre n'est pas dans le catalogue de l'Accueil (`GameIds.CATALOG_ORDER` inchangé) : il y entrera avec son écran de création (Story 11.2, AD-9).
- Le classement donne le même rang aux égalités ; avant toute Manche, tous les Joueurs sont premiers à 0.
- AD-9 ajoutée à `ARCHITECTURE-SPINE.md`.

### File List

- `domain/model/EndRule.kt` (renommé), `domain/model/Party.kt`, `domain/model/FreeGame.kt`, `domain/engine/FreeGameRules.kt`, `domain/engine/GameIds.kt`, `domain/repository/FreeGameRepository.kt`
- `game/free/FreeGameEngine.kt`, `di/EngineModule.kt`, `di/RepositoryModule.kt`
- `data/entity/PartyEntity.kt`, `data/entity/RoundEntity.kt`, `data/dao/RoundDao.kt`, `data/dao/RoundScoreDao.kt`, `data/db/AppDatabase.kt`, `data/repository/FreeGameRepositoryImpl.kt`, `app/schemas/.../6.json`
- Renommage `EndRule` : Belote (moteur, use cases, écrans, tests)
- Tests : `FreeGameEngineTest`, `AppDatabaseMigrationTest`, `FreeGameRepositoryImplTest`
- `_bmad-output/planning-artifacts/architecture/.../ARCHITECTURE-SPINE.md` (AD-9), `epics.md` (choix validés)

### Change Log

- 2026-10-22 : story créée et implémentée (moteur et données du Jeu libre, schéma v6).
