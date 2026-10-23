---
story_id: "13.2"
story_key: "13-2-donnees-du-tarot"
epic: 13
status: done
created: 2026-10-23
baseline_commit: 11f7976
---

# Story 13.2 : Données du Tarot

**Epic :** 13 - Tarot
**Story ID :** 13.2
**Status :** done

## Story

En tant qu'utilisateur,
Je veux que mes Parties de Tarot soient enregistrées comme les autres,
Afin de les reprendre après une fermeture de l'application et de les retrouver dans l'historique et les sauvegardes.

## Acceptance Criteria

1. **Given** le schéma Room en version 6 **When** l'application passe en version 7 **Then** une table `tarot_rounds` mémorise, par Manche de Tarot, ce qui a été saisi : preneur et partenaire (Participants), contrat, bouts et points de l'attaque, petit au bout, poignées de l'attaque et de la défense, chelem ; les points par joueur ne sont jamais stockés.

2. **And** la migration 6 → 7 est automatique et testée sans perte sur un vrai fichier de version 6.

3. **And** `TarotRepository` enregistre une Manche avec la Partie mise à jour (Donneur, fin) en une transaction, la corrige (même numéro, Partie en cours seulement) en une transaction, et lit ou observe les Manches d'une Partie ; une Manche d'une Partie qui n'est pas de Tarot ou dont le preneur ou le partenaire n'est pas un Participant de la Partie est refusée. La Partie et ses Participants passent par `PartyRepository`, comme pour le Jeu libre.

4. **And** les valeurs persistées invalides (contrat, camp, poignée, chelem inconnus, identifiants invalides) font échouer la lecture explicitement.

5. **And** la sauvegarde (Epic 12) contient les Manches de Tarot : le format passe à 2 ; une sauvegarde au format 1 reste restaurable ; la vérification refuse une Manche de Tarot invalide (valeurs, bouts hors de 0 à 3, points hors de 0 à 91, preneur ou partenaire hors de la Partie, partenaire égal au preneur, doublons) ; une Partie de Tarot est un Jeu connu des sauvegardes.

## Tasks / Subtasks

- [x] **T1 - Room** (AC: 1, 2, 4)
  - [x] `TarotRoundEntity` et ses mappings, `TarotRoundDao`, version 7 avec `AutoMigration(6, 7)`, schéma `7.json`
  - [x] Test JVM des mappings ; test instrumenté de migration depuis un vrai fichier de version 6
- [x] **T2 - Repository** (AC: 3)
  - [x] `TarotRepository` (domaine) et `TarotRepositoryImpl`, binding Hilt
  - [x] Tests instrumentés : écriture et correction atomiques, refus
- [x] **T3 - Sauvegarde** (AC: 5)
  - [x] `GameIds.STORED` (Jeux que les données savent garder), format 2, `BackupTarotRound`, lecture et vérification, remplacement
  - [x] Tests JVM (aller-retour, format 1, refus) et instrumentés (export et restauration avec une Manche de Tarot)
- [x] **T4 - Validation** : `testDebugUnitTest`, `kspDebugKotlin`, `assembleDebug`, `connectedDebugAndroidTest` API 36

## Dev Notes

- Comme pour la Belote (AD-8, AD-10), la Manche mémorise la saisie ; le moteur recalcule les points.
- Le preneur et le partenaire sont des identifiants de Participant ; seule la Partie a une clé étrangère (CASCADE), comme `belote_rounds`. La cohérence des références est vérifiée par le repository à l'écriture et par la sauvegarde à la lecture.
- Les sauvegardes acceptent les Jeux que les données savent garder (`GameIds.STORED`), indépendamment du catalogue de l'Accueil, où le Tarot n'entre qu'en 13.3.
- Le format 2 ajoute `tarotRounds` à chaque Partie (liste vide par défaut) : un fichier au format 1 se lit sans changement ; une version 1.8 refusera un fichier au format 2 comme « plus récent », ce qui est voulu.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 13, Story 13.2)
- `_bmad-output/planning-artifacts/games/tarot.md`
- Stories 12.1 et 12.2 (sauvegarde)

## Dev Agent Record

### Debug Log References

- JVM : `./gradlew testDebugUnitTest` → 532 tests, 0 échec (525 avant la story) ; nouveaux : `TarotEntityMappingTest` (3), `BackupReaderTest` +3 (format 1 toujours valide, Manche de Tarot impossible, chaque Manche dans une Partie de son Jeu), `BackupFileTest` +1 (aller-retour d'une Partie de Tarot à 5).
- Build : `kspDebugKotlin assembleDebug compileDebugAndroidTestKotlin` OK ; schéma `app/schemas/.../7.json` exporté.
- Instrumentés API 36 : `connectedDebugAndroidTest` → 262 tests (255 avant) ; nouveaux : `TarotRepositoryImplTest` (6), migration 6 → 7 (1). Un échec au premier passage : `BackupRepositoryImplTest.aRefusedFileChangesNothing` prenait le format 2 pour « plus récent » ; corrigé en `BACKUP_FORMAT + 1`, classe relancée seule : 7/7.
- `git diff --check` propre.
- Non vérifié : rien de visible dans l'application (pas d'écran Tarot avant la Story 13.3) ; la migration n'a pas tourné sur la base du téléphone.

### Completion Notes List

- `tarot_rounds` (schéma 7, `AutoMigration(6, 7)`) mémorise la saisie ; seule la Partie a une clé étrangère (CASCADE), comme `belote_rounds`.
- `TarotRepositoryImpl` vérifie dans la transaction que la Partie est de Tarot et que preneur et partenaire en sont des Participants ; la correction exige une Partie en cours et le même numéro.
- Sauvegarde : format 2 (`BackupParty.tarotRounds`, vide par défaut, donc un fichier au format 1 se lit tel quel) ; la vérification accepte les Jeux de `GameIds.STORED` (Tarot compris, indépendamment du catalogue de l'Accueil), refuse une Manche de Tarot impossible et rattache désormais chaque sorte de Manche à une Partie de son Jeu (Manches à scores : Papayoo ou Jeu libre ; Belote ; Tarot).
- `BackupTables.tarotRounds` est obligatoire, pour qu'aucun appelant ne l'oublie.

### File List

- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/TarotRoundEntity.kt`, `data/dao/TarotRoundDao.kt` (nouveaux)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/db/AppDatabase.kt` (v7), `di/DatabaseModule.kt`
- `app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/7.json` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/repository/TarotRepository.kt`, `data/repository/TarotRepositoryImpl.kt` (nouveaux), `di/RepositoryModule.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/engine/GameIds.kt` (`STORED`)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/backup/BackupFile.kt`, `BackupReader.kt`, `data/repository/BackupRepositoryImpl.kt`
- Tests : `app/src/test/.../data/entity/TarotEntityMappingTest.kt` (nouveau), `app/src/test/.../data/backup/BackupFileTest.kt`, `BackupReaderTest.kt`, `app/src/androidTest/.../data/repository/TarotRepositoryImplTest.kt` (nouveau), `BackupRepositoryImplTest.kt`, `app/src/androidTest/.../data/db/AppDatabaseMigrationTest.kt`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-10-23 : Story 13.2 implémentée (Room v7, `TarotRepository`, sauvegarde au format 2), testée JVM et API 36.
