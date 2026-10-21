---
story_id: "7.2"
story_key: "7-2-donnees-equipes-mode-de-fin-et-manches-belote"
epic: 7
status: done
created: 2026-10-21
baseline_commit: f34dcc8
---

# Story 7.2 : Données — Équipes, mode de fin et Manches de Belote

**Epic :** 7 - Belote classique
**Story ID :** 7.2
**Status :** done

## Story

En tant qu'utilisateur,
Je veux que mes Parties de Belote soient enregistrées comme celles du Papayoo,
Afin de les reprendre après une fermeture de l'application et de les retrouver dans l'historique.

## Acceptance Criteria

1. **Given** le schéma Room en version 2 **When** l'application passe en version 3 **Then** une table `teams` (Partie, côté `FIRST`/`SECOND`, nom) existe, une par côté et par Partie, et chaque Participant peut appartenir à une Équipe (`participants.teamSide`, nul pour le Papayoo).

2. **And** la Partie mémorise sa règle de fin (`parties.endRuleType` et `parties.endRuleValue`, nuls pour le Papayoo) ; une ligne avec une seule des deux valeurs, un type inconnu ou une valeur non positive échoue à la lecture au lieu d'être corrigée.

3. **And** chaque Manche de Belote (`belote_rounds`) mémorise ce qui a été saisi : Équipe preneuse, points de plis du preneur, Équipe de la belote, Équipe capot. Les points par Équipe et la réserve de litige sont recalculés par le moteur (7.1), jamais stockés.

4. **And** les Parties de Papayoo existantes sont migrées sans perte par une `AutoMigration` 2 → 3 ; `AppDatabaseMigrationTest` couvre 2 → 3 et 1 → 3.

5. **And** un `BeloteRepository` de domaine crée une Partie de Belote (Partie, Équipes, Participants) en une transaction, enregistre une Manche avec le Donneur suivant en une transaction, corrige les saisies d'une Manche existante, et expose Équipes et Manches en lecture ponctuelle et observée ; la suppression d'une Partie emporte ses Équipes et Manches.

6. **And** `PartyRepository` et le Papayoo sont inchangés.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 1-3, 5)
  - [x] `Team`, `BeloteRound` ; `Party.endRule`, `Participant.teamSide` (défauts nuls).
  - [x] `BeloteRepository` dans `domain/repository/`.
- [x] **T2 - Room v3** (AC: 1-4)
  - [x] `TeamEntity`, `BeloteRoundEntity` et leurs DAO ; colonnes ajoutées à `parties` et `participants` ; mappings stricts.
  - [x] `AppDatabase` version 3, `AutoMigration(2, 3)`, schéma `3.json` exporté.
- [x] **T3 - Repository** (AC: 5, 6)
  - [x] `BeloteRepositoryImpl`, bind Hilt, DAO fournis par `DatabaseModule`.
- [x] **T4 - Tests et validations** (AC: 1-6)
  - [x] JVM : mappings des entités (aller-retour et valeurs invalides).
  - [x] Instrumentés : `AppDatabaseMigrationTest` (2 → 3, 1 → 3), `BeloteRepositoryImplTest`.
  - [x] `testDebugUnitTest`, `kspDebugKotlin`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

## Dev Notes

- Repository séparé : modifier `PartyRepository` casserait une dizaine de faux de test sans bénéfice ; la Belote partage la table `parties` (statut, Donneur, suppression, reprise) et a ses propres tables.
- `rounds.suit` est non nul et propre au dé du Papayoo : les Manches de Belote ont leur table.
- Correction : seules les saisies changent (numéro et Partie restent), la Partie doit être en cours ; le recalcul des Manches suivantes est fait par le moteur à la lecture.

### References

- [Source: `_bmad-output/planning-artifacts/games/belote.md`]
- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 7.2]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — AD-3, AD-8]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- `testDebugUnitTest kspDebugKotlin assembleDebug compileDebugAndroidTestKotlin` : 296 tests JVM, 0 échec (dont 8 de `BeloteEntityMappingTest`) ; aucun avertissement Room.
- `connectedDebugAndroidTest` sur l'AVD `Pixel_API_36` : 165 tests, 0 échec (dont 2 de `AppDatabaseMigrationTest` et 8 de `BeloteRepositoryImplTest`).
- Non vérifié : la migration sur la base réelle du téléphone (elle se fera à l'installation d'une version contenant cette story) ; aucun écran n'utilise encore ces données.

### Completion Notes List

- Écart assumé au critère initial d'`epics.md` (amendé) : les points par Équipe ne sont pas stockés. Une correction de Manche change la réserve de litige des Manches suivantes ; seules les saisies sont stockées et le moteur recalcule.
- `Party.endRule` et `Participant.teamSide` ont des valeurs par défaut nulles : aucun appel existant ni faux de test n'a changé ; `PartyRepository` est intact.
- La table `teams` a pour clé (`partyId`, `side`) : une Équipe par côté et par Partie est garantie par la base.
- Les mappings échouent sur une règle de fin incomplète, inconnue ou non positive, et sur un côté d'Équipe inconnu.

### File List

- `_bmad-output/planning-artifacts/epics.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/7-2-donnees-equipes-mode-de-fin-et-manches-belote.md` (nouveau)
- `app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/3.json` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/Party.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/Participant.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/Team.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/BeloteRound.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/repository/BeloteRepository.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/PartyEntity.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/ParticipantEntity.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/TeamEntity.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/BeloteRoundEntity.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/dao/TeamDao.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/dao/BeloteRoundDao.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/db/AppDatabase.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/data/repository/BeloteRepositoryImpl.kt` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/di/DatabaseModule.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/di/RepositoryModule.kt`
- `app/src/test/java/com/rvetiveau/rvscorecount/data/entity/BeloteEntityMappingTest.kt` (nouveau)
- `app/src/androidTest/java/com/rvetiveau/rvscorecount/data/db/AppDatabaseMigrationTest.kt`
- `app/src/androidTest/java/com/rvetiveau/rvscorecount/data/repository/BeloteRepositoryImplTest.kt` (nouveau)

### Change Log

- 2026-10-21 : schéma Room v3 (Équipes, règle de fin, Manches de Belote), `BeloteRepository` et leurs tests.
