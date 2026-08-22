---
baseline_commit: 1bdafc1965bfabcd6768054d9cf6c620fcb63386
---

# Story 1.2 : Schéma de base de données Room

**Epic :** 1 — Fondation & Infrastructure
**Story ID :** 1.2
**Status :** done

---

## Story

En tant que développeur,
Je veux créer le schéma Room complet avec toutes les entités et DAOs,
Afin que la couche de persistance soit prête à stocker l'état complet d'une Partie.

---

## Acceptance Criteria

1. **Given** le projet Android configuré (Story 1.1) **When** la base de données est instanciée **Then** les entités suivantes existent :
   - `PartyEntity` (id: UUID, gameId: String, status: PartyState, currentDealerIndex: Int, createdAt: Long)
   - `ParticipantEntity` (id: UUID, partyId: UUID FK, name: String, listPosition: Int)
   - `RoundEntity` (id: UUID, partyId: UUID FK, roundNumber: Int, suit: CardSuit)
   - `RoundScoreEntity` (roundId: UUID FK, participantId: UUID FK, points: Int)

2. **Given** les entités créées **When** AppDatabase est inspectée **Then** les 4 entités sont toutes enregistrées dans `@Database(entities = [...])`

3. **Given** la base configurée **When** les DAOs sont inspectés **Then** `PartyDao`, `RoundDao`, `ParticipantDao` existent avec les opérations minimales requises :
   - `PartyDao` : `insert(party)`, `update(party)`, `getById(id)`, `observeById(id): Flow<PartyEntity?>`, `getPartyInProgress(): PartyEntity?`
   - `ParticipantDao` : `insert(participant)`, `insertAll(participants)`, `getByPartyId(partyId): List<ParticipantEntity>`, `observeByPartyId(partyId): Flow<List<ParticipantEntity>>`
   - `RoundDao` : `insert(round)`, `getByPartyId(partyId): List<RoundEntity>`
   - `RoundScoreDao` : `insert(score)`, `insertAll(scores)`, `getByRoundId(roundId): List<RoundScoreEntity>`

4. **Given** la base configurée **When** Hilt est actif **Then** `DatabaseModule` dans `di/` expose `AppDatabase`, `PartyDao`, `ParticipantDao`, `RoundDao`, `RoundScoreDao` via `@Singleton @Provides`

5. **Given** les DAOs injectés **When** `PartyRepository` est utilisé **Then** l'interface `PartyRepository` existe dans `domain/repository/` et `PartyRepositoryImpl` dans `data/repository/` l'implémente — aucun DAO n'est exposé hors de la couche `data/`

6. **Given** la base configurée **When** le schéma est inspecté **Then** Room a exporté le fichier de schéma JSON dans `app/schemas/` (configuration via Room Gradle Plugin `schemaDirectory`)

---

## Tasks / Subtasks

- [x] **T1 — Créer les enums domaine requis** (AC: 1)
  - [x] `domain/model/PartyState.kt` → `enum class PartyState { SETUP, IN_PROGRESS, FINISHED }`
  - [x] `domain/model/CardSuit.kt` → `enum class CardSuit { SPADE, HEART, CLUB, DIAMOND }`
  - [x] Vérifier : pas d'import Compose, pas d'import Room dans ces fichiers (règle AD-1)

- [x] **T2 — Créer les type aliases / value classes domaine** (AC: 1)
  - [x] `domain/model/ParticipantId.kt` → `typealias ParticipantId = UUID` (ou `@JvmInline value class ParticipantId(val value: UUID)`)
  - [x] Utiliser `java.util.UUID` — pas de dépendance externe

- [x] **T3 — Créer les entités Room** (AC: 1, 2)
  - [x] `data/entity/PartyEntity.kt`
  - [x] `data/entity/ParticipantEntity.kt`
  - [x] `data/entity/RoundEntity.kt`
  - [x] `data/entity/RoundScoreEntity.kt`
  - [x] Voir section **Dev Notes — Entités Room** pour le code exact

- [x] **T4 — Créer les converters de type** (AC: 1)
  - [x] `data/db/Converters.kt` avec `@TypeConverter` pour : `UUID ↔ String`, `PartyState ↔ String`, `CardSuit ↔ String`
  - [x] Annoter `AppDatabase` avec `@TypeConverters(Converters::class)`

- [x] **T5 — Créer les DAOs** (AC: 3)
  - [x] `data/dao/PartyDao.kt`
  - [x] `data/dao/ParticipantDao.kt`
  - [x] `data/dao/RoundDao.kt`
  - [x] `data/dao/RoundScoreDao.kt`
  - [x] Voir section **Dev Notes — DAOs** pour le code exact

- [x] **T6 — Créer AppDatabase** (AC: 2, 4)
  - [x] `data/db/AppDatabase.kt` → `@Database(entities = [...], version = 1, exportSchema = true)`
  - [x] Déclarer les 4 abstract DAO getters

- [x] **T7 — Créer DatabaseModule Hilt** (AC: 4)
  - [x] `di/DatabaseModule.kt` → `@Module @InstallIn(SingletonComponent::class)`
  - [x] Voir section **Dev Notes — DatabaseModule** pour le code exact

- [x] **T8 — Créer PartyRepository** (AC: 5)
  - [x] `domain/repository/PartyRepository.kt` (interface)
  - [x] `data/repository/PartyRepositoryImpl.kt` (implémentation)
  - [x] `di/RepositoryModule.kt` → binding `PartyRepository → PartyRepositoryImpl`
  - [x] Voir section **Dev Notes — Repository** pour le code exact

- [x] **T9 — Vérifier build et schéma** (AC: 6)
  - [x] `./gradlew kspDebugKotlin` → pas d'erreur KSP
  - [x] `./gradlew assembleDebug` → build sans erreur
  - [x] Vérifier que `app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/1.json` est généré

### Review Findings

- [x] [Review][Patch] Garantir l'unicité de `(partyId, roundNumber)` afin d'empêcher plusieurs manches logiques portant le même numéro [app/src/main/java/com/rvetiveau/rvscorecount/data/entity/RoundEntity.kt:17]
- [x] [Review][Patch] Refuser une manche sans aucun score afin de préserver l'invariant atomique Round + RoundScores [app/src/main/java/com/rvetiveau/rvscorecount/data/repository/PartyRepositoryImpl.kt:59]
- [x] [Review][Patch] Vérifier que chaque participant scoré appartient à la partie de la manche [app/src/main/java/com/rvetiveau/rvscorecount/data/repository/PartyRepositoryImpl.kt:63]
- [x] [Review][Patch] Remplacer les insertions `REPLACE` de Party et Participant, qui suppriment silencieusement leurs enfants via les cascades de clés étrangères en cas d'identifiant existant [app/src/main/java/com/rvetiveau/rvscorecount/data/dao/PartyDao.kt:12; app/src/main/java/com/rvetiveau/rvscorecount/data/dao/ParticipantDao.kt:11]

---

## Dev Notes

### Contexte — ce que Story 1.1 a livré

Story 1.1 a créé :
- La structure de packages vide (`data/entity/`, `data/dao/`, `data/db/`, `data/repository/`, `domain/model/`, `domain/repository/`, `di/`)
- `libs.versions.toml` avec Room 2.8.4, KSP 2.4.10-1.0.31, Room Gradle Plugin
- `build.gradle.kts` avec les plugins `ksp` et `room` activés et `room { schemaDirectory(...) }` configuré

**Ne pas reconfigurer les dépendances Room** — elles sont déjà dans `libs.versions.toml` et `build.gradle.kts`.

### Versions exactes (héritées de Story 1.1 — NE PAS MODIFIER)

| Bibliothèque | Version |
|-------------|---------|
| Room | **2.8.4** |
| KSP | **2.3.11** |
| Kotlin | **2.4.10** |

> ⚠️ L'Architecture Spine mentionne Room 2.7.x — **utiliser 2.8.4** (version vérifiée en Story 1.1).

### Règle d'or AD-1 — ce que cette story DOIT respecter

```
data/ → domain/    ✅  (PartyRepositoryImpl importe domain/model et domain/repository)
domain/ → data/    ❌  INTERDIT
domain/ → ui/      ❌  INTERDIT
```

- `PartyState`, `CardSuit`, `ParticipantId` vivent dans `domain/model/` → importés par `data/entity/`
- `PartyRepository` (interface) vit dans `domain/repository/` → implémentée par `data/repository/PartyRepositoryImpl`
- **Aucun** import `androidx.room` dans le package `domain/`

### Entités Room — code de référence

```kotlin
// data/entity/PartyEntity.kt
@Entity(tableName = "parties")
data class PartyEntity(
    @PrimaryKey val id: String,          // UUID.toString()
    val gameId: String,
    val status: String,                  // PartyState.name
    val currentDealerIndex: Int,
    val createdAt: Long                  // System.currentTimeMillis()
)

// data/entity/ParticipantEntity.kt
@Entity(
    tableName = "participants",
    foreignKeys = [ForeignKey(
        entity = PartyEntity::class,
        parentColumns = ["id"],
        childColumns = ["partyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("partyId")]
)
data class ParticipantEntity(
    @PrimaryKey val id: String,          // UUID.toString()
    val partyId: String,
    val name: String,
    val listPosition: Int
)

// data/entity/RoundEntity.kt
@Entity(
    tableName = "rounds",
    foreignKeys = [ForeignKey(
        entity = PartyEntity::class,
        parentColumns = ["id"],
        childColumns = ["partyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("partyId")]
)
data class RoundEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val roundNumber: Int,
    val suit: String                     // CardSuit.name
)

// data/entity/RoundScoreEntity.kt
@Entity(
    tableName = "round_scores",
    primaryKeys = ["roundId", "participantId"],
    foreignKeys = [
        ForeignKey(entity = RoundEntity::class,
            parentColumns = ["id"], childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ParticipantEntity::class,
            parentColumns = ["id"], childColumns = ["participantId"],
            onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("roundId"), Index("participantId")]
)
data class RoundScoreEntity(
    val roundId: String,
    val participantId: String,
    val points: Int
)
```

> **Pourquoi `String` pour les UUID et enums ?** Room ne sait pas nativement sérialiser `UUID` ni des enums dans le même `@Database`. Stocker en `String` (via `.toString()` / `.name`) est simple, lisible dans SQLite Browser, et ne nécessite pas de TypeConverter complexe. Les TypeConverters sont néanmoins ajoutés pour faciliter les requêtes Room typées si besoin futur.

### TypeConverters — code de référence

```kotlin
// data/db/Converters.kt
class Converters {
    @TypeConverter fun fromUUID(value: UUID?): String? = value?.toString()
    @TypeConverter fun toUUID(value: String?): UUID? = value?.let { UUID.fromString(it) }

    @TypeConverter fun fromPartyState(value: PartyState?): String? = value?.name
    @TypeConverter fun toPartyState(value: String?): PartyState? =
        value?.let { PartyState.valueOf(it) }

    @TypeConverter fun fromCardSuit(value: CardSuit?): String? = value?.name
    @TypeConverter fun toCardSuit(value: String?): CardSuit? =
        value?.let { CardSuit.valueOf(it) }
}
```

### DAOs — code de référence

```kotlin
// data/dao/PartyDao.kt
@Dao
interface PartyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(party: PartyEntity)

    @Update
    suspend fun update(party: PartyEntity)

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getById(id: String): PartyEntity?

    @Query("SELECT * FROM parties WHERE id = :id")
    fun observeById(id: String): Flow<PartyEntity?>

    @Query("SELECT * FROM parties WHERE status = :status LIMIT 1")
    suspend fun getPartyInProgress(status: String = "IN_PROGRESS"): PartyEntity?
}

// data/dao/ParticipantDao.kt
@Dao
interface ParticipantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(participant: ParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(participants: List<ParticipantEntity>)

    @Query("SELECT * FROM participants WHERE partyId = :partyId ORDER BY listPosition ASC")
    suspend fun getByPartyId(partyId: String): List<ParticipantEntity>

    @Query("SELECT * FROM participants WHERE partyId = :partyId ORDER BY listPosition ASC")
    fun observeByPartyId(partyId: String): Flow<List<ParticipantEntity>>
}

// data/dao/RoundDao.kt
@Dao
interface RoundDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)     // ABORT — immuabilité FR-9
    suspend fun insert(round: RoundEntity)

    @Query("SELECT * FROM rounds WHERE partyId = :partyId ORDER BY roundNumber ASC")
    suspend fun getByPartyId(partyId: String): List<RoundEntity>
}

// data/dao/RoundScoreDao.kt
@Dao
interface RoundScoreDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)     // ABORT — immuabilité FR-9
    suspend fun insert(score: RoundScoreEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(scores: List<RoundScoreEntity>)

    @Query("SELECT * FROM round_scores WHERE roundId = :roundId")
    suspend fun getByRoundId(roundId: String): List<RoundScoreEntity>
}
```

> **`RoundDao` et `RoundScoreDao` utilisent `OnConflictStrategy.ABORT`** — conforme à FR-9 (immuabilité des manches validées). Aucune opération `UPDATE` ni `DELETE` sur ces tables.

### AppDatabase — code de référence

```kotlin
// data/db/AppDatabase.kt
@Database(
    entities = [
        PartyEntity::class,
        ParticipantEntity::class,
        RoundEntity::class,
        RoundScoreEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partyDao(): PartyDao
    abstract fun participantDao(): ParticipantDao
    abstract fun roundDao(): RoundDao
    abstract fun roundScoreDao(): RoundScoreDao
}
```

### DatabaseModule Hilt — code de référence

```kotlin
// di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "rv_score_counter.db")
            .build()

    @Provides @Singleton fun providePartyDao(db: AppDatabase): PartyDao = db.partyDao()
    @Provides @Singleton fun provideParticipantDao(db: AppDatabase): ParticipantDao = db.participantDao()
    @Provides @Singleton fun provideRoundDao(db: AppDatabase): RoundDao = db.roundDao()
    @Provides @Singleton fun provideRoundScoreDao(db: AppDatabase): RoundScoreDao = db.roundScoreDao()
}
```

### Repository — code de référence

```kotlin
// domain/repository/PartyRepository.kt  (couche domain — AUCUN import Room)
interface PartyRepository {
    suspend fun createParty(party: Party): Unit
    suspend fun updateParty(party: Party): Unit
    suspend fun getPartyById(id: UUID): Party?
    fun observeParty(id: UUID): Flow<Party?>
    suspend fun getPartyInProgress(): Party?
    suspend fun addParticipant(participant: Participant): Unit
    suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant>
    fun observeParticipants(partyId: UUID): Flow<List<Participant>>
    suspend fun saveRound(round: Round, scores: Map<UUID, Int>): Unit
    suspend fun getRoundsByPartyId(partyId: UUID): List<Round>
    suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore>
}

// data/repository/PartyRepositoryImpl.kt  (couche data)
@Singleton
class PartyRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val partyDao: PartyDao,
    private val participantDao: ParticipantDao,
    private val roundDao: RoundDao,
    private val roundScoreDao: RoundScoreDao
) : PartyRepository {
    // Implémenter chaque méthode en convertissant Entity ↔ domain model
    // Ex: getPartyById(id) = partyDao.getById(id.toString())?.toDomain()
    // saveRound DOIT être atomique : appDatabase.withTransaction { roundDao.insert(...); roundScoreDao.insertAll(...) }
    // sinon un crash entre les deux inserts laisse un Round orphelin sans scores (viole AD-3)
}

// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPartyRepository(impl: PartyRepositoryImpl): PartyRepository
}
```

> **Les modèles domaine `Party`, `Round`, `Participant`, `RoundScore`** sont des `data class` simples dans `domain/model/` — **sans annotations Room**. Cette story les crée en même temps que les entités, car le repository en a besoin. Ils serviront aux stories suivantes (use cases, ViewModels).

### Modèles domaine à créer

```kotlin
// domain/model/Party.kt
data class Party(
    val id: UUID,
    val gameId: String,
    val status: PartyState,
    val currentDealerIndex: Int,
    val createdAt: Long
)

// domain/model/Participant.kt
data class Participant(
    val id: UUID,
    val partyId: UUID,
    val name: String,
    val listPosition: Int
)

// domain/model/Round.kt
data class Round(
    val id: UUID,
    val partyId: UUID,
    val roundNumber: Int,
    val suit: CardSuit
)

// domain/model/RoundScore.kt
data class RoundScore(
    val roundId: UUID,
    val participantId: UUID,
    val points: Int
)
```

### Fonctions d'extension de mapping (à placer dans `data/entity/`)

```kotlin
// data/entity/PartyEntity.kt (ou fichier Extensions.kt dans data/entity/)
fun PartyEntity.toDomain() = Party(
    id = UUID.fromString(id),
    gameId = gameId,
    status = PartyState.valueOf(status),
    currentDealerIndex = currentDealerIndex,
    createdAt = createdAt
)

fun Party.toEntity() = PartyEntity(
    id = id.toString(),
    gameId = gameId,
    status = status.name,
    currentDealerIndex = currentDealerIndex,
    createdAt = createdAt
)
// Idem pour Participant, Round, RoundScore
```

### Fichiers créés par cette story

```text
domain/
  model/
    PartyState.kt           ← NEW
    CardSuit.kt             ← NEW
    ParticipantId.kt        ← NEW (typealias ou value class)
    Party.kt                ← NEW
    Participant.kt          ← NEW
    Round.kt                ← NEW
    RoundScore.kt           ← NEW
  repository/
    PartyRepository.kt      ← NEW (interface)
data/
  entity/
    PartyEntity.kt          ← NEW
    ParticipantEntity.kt    ← NEW
    RoundEntity.kt          ← NEW
    RoundScoreEntity.kt     ← NEW
  dao/
    PartyDao.kt             ← NEW
    ParticipantDao.kt       ← NEW
    RoundDao.kt             ← NEW
    RoundScoreDao.kt        ← NEW
  db/
    AppDatabase.kt          ← NEW
    Converters.kt           ← NEW
  repository/
    PartyRepositoryImpl.kt  ← NEW
di/
  DatabaseModule.kt         ← NEW
  RepositoryModule.kt       ← NEW
```

### Ce que cette story NE fait PAS (scope boundary)

| Hors scope Story 1.2 | Sera fait dans |
|----------------------|----------------|
| `GameEngine` interface et `PapayooEngine` | Story 1.3 |
| `CardSuitColors.kt` et tokens de couleur UI | Story 1.4 |
| `NavGraph`, `ResumePartyUseCase` | Story 1.5 |
| Use cases métier (`CreatePartyUseCase`, etc.) | Stories 2+ |
| Tests instrumentés Room (in-memory) | Hors scope V1 |

**Ne pas créer de ViewModels, Composables ou use cases dans cette story.**

### Pièges courants à éviter

1. **`@Singleton` manquant sur `AppDatabase`** : sans `@Singleton`, Hilt créera une nouvelle instance de base à chaque injection → corruption.
2. **Import Room dans `domain/`** : toute annotation `@Entity`, `@Dao`, `@Query` dans `domain/` viole AD-1. Les modèles domaine sont des `data class` pures.
3. **Pas de `ForeignKey` avec `CASCADE`** : sans la suppression en cascade, supprimer une `Party` laisserait des `Participant` et `Round` orphelins.
4. **`exportSchema = false`** dans `@Database` : toujours `true` — le Room Gradle Plugin a besoin du `schemaDirectory` configuré en Story 1.1 pour générer le JSON.
5. **`OnConflictStrategy.REPLACE` sur `RoundDao`** : utiliser `ABORT` pour respecter FR-9 (immuabilité des manches).
6. **`suspend` manquant sur les fonctions DAO** : toutes les fonctions Room hors `Flow` doivent être `suspend` pour ne pas bloquer le thread principal.
7. **`saveRound` non transactionnel** : insérer le `Round` puis les `RoundScore` séparément expose à un `Round` orphelin si le processus meurt entre les deux — encapsuler dans `appDatabase.withTransaction { }` (AD-3).

### Vérification finale

```bash
./gradlew kspDebugKotlin     # KSP génère le code Room sans erreur
./gradlew assembleDebug      # Build complet sans erreur
# Vérifier la présence de :
# app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/1.json
```

---

## Références

- Architecture Spine — AD-1 (règle dépendances), AD-3 (Room source unique de vérité), AD-5 (PartyState), AD-7 (CardSuit), Structural Seed (arbre source et modèle de données) [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md`]
- Epics.md — Story 1.2 ACs, FR-9 (immuabilité manches) [Source: `_bmad-output/planning-artifacts/epics.md`]
- Story 1.1 — versions exactes du stack, configuration Room Gradle Plugin [Source: `_bmad-output/implementation-artifacts/1-1-scaffolding-du-projet-android.md`]
- Room documentation : https://developer.android.com/training/data-storage/room
- Hilt + Room : https://developer.android.com/training/dependency-injection/hilt-android#component-scopes

---

## Dev Agent Record

### Agent Model Used

Claude Sonnet 4.5 (GitHub Copilot)

### Debug Log References

- `./gradlew kspDebugKotlin` → BUILD SUCCESSFUL (KSP génère Room + Hilt sans erreur)
- `./gradlew assembleDebug` → BUILD SUCCESSFUL
- `app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/1.json` généré et vérifié présent

### Completion Notes List

- Les 4 entités Room, leurs enums domaine associés (`PartyState`, `CardSuit`), et les modèles domaine purs (`Party`, `Participant`, `Round`, `RoundScore`) ont été créés conformément à la règle AD-1 (aucun import Room dans `domain/`).
- `Converters.kt` gère `UUID`, `PartyState`, `CardSuit` ↔ `String`.
- 4 DAOs créés (`PartyDao`, `ParticipantDao`, `RoundDao`, `RoundScoreDao`), `RoundDao`/`RoundScoreDao` en `OnConflictStrategy.ABORT` pour respecter l'immuabilité FR-9.
- `AppDatabase` enregistre les 4 entités, `version = 1`, `exportSchema = true`.
- `DatabaseModule` (Hilt) expose `AppDatabase` et les 4 DAOs, tous annotés `@Singleton @Provides` (correctif de la validation de story appliqué).
- `PartyRepository` (interface, `domain/repository/`) et `PartyRepositoryImpl` (`data/repository/`) créés ; aucun DAO n'est exposé hors de la couche `data/`.
- `saveRound()` encapsulé dans `appDatabase.withTransaction { }` pour garantir l'atomicité Round + RoundScores (correctif de la validation de story appliqué, cohérent avec AD-3).
- `getPartyInProgress()` paramétrée via `:status` plutôt qu'un literal SQL en dur (correctif de la validation de story appliqué).
- `local.properties` (sdk.dir) créé localement pour permettre le build — fichier non versionné, spécifique à la machine.
- Build vérifié : `kspDebugKotlin` et `assembleDebug` passent sans erreur ; schéma JSON exporté avec succès.
- Aucun test instrumenté Room ajouté — explicitement hors scope V1 selon la section "Ce que cette story NE fait PAS" de la story.

### File List

- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/PartyState.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/CardSuit.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/ParticipantId.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/Party.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/Participant.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/Round.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/RoundScore.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/repository/PartyRepository.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/PartyEntity.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/ParticipantEntity.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/RoundEntity.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/RoundScoreEntity.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/db/Converters.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/db/AppDatabase.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/dao/PartyDao.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/dao/ParticipantDao.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/dao/RoundDao.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/dao/RoundScoreDao.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/data/repository/PartyRepositoryImpl.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/di/DatabaseModule.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/di/RepositoryModule.kt` (NEW)
- `app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/1.json` (NEW — généré par Room)
- `local.properties` (NEW — local, non versionné)

### Change Log

- 2026-08-22 : Implémentation complète du schéma Room (entités, DAOs, converters, AppDatabase, modules Hilt, repository). Toutes les tâches T1–T9 complétées. Build vérifié (`kspDebugKotlin`, `assembleDebug`). Statut → review.
- 2026-08-22 : Revue de code terminée. Quatre correctifs d'intégrité appliqués et validés par `kspDebugKotlin` et `assembleDebug`. Statut → done.
