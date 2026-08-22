---
story_id: "1.3"
story_key: "1-3-interface-gameengine-et-implementation-papayoo"
epic: 1
status: done
created: 2026-08-22
baseline_commit: 6c6a21aab136935711f0e6c0163d5edea19cceaf
---

# Story 1.3 : Interface GameEngine et implémentation Papayoo

**Epic :** 1 — Fondation & Infrastructure  
**Story ID :** 1.3  
**Status :** done

## Story

En tant que développeur,
Je veux créer l'interface `GameEngine` et son implémentation Papayoo,
Afin que la logique métier du jeu soit isolée et extensible pour les futurs jeux.

## Acceptance Criteria

1. **Given** la structure `domain/` créée **When** `PapayooEngine` est instancié **Then** l'interface `GameEngine` existe dans `domain/engine/` avec :
   - `gameId: String`
   - `minPlayers: Int`
   - `maxPlayers: Int`
   - `totalPointsPerRound: Int`
   - `precomputeLastScore(entered: Map<ParticipantId, Int>): Int`
   - `validateRound(scores: Map<ParticipantId, Int>): RoundValidation`

2. **Given** l'interface `GameEngine` définie **When** `PapayooEngine` est instancié **Then** il implémente `GameEngine` dans `game/papayoo/` avec `gameId = "papayoo"`, `minPlayers = 3`, `maxPlayers = 8` et `totalPointsPerRound = 250`.

3. **Given** un `PapayooEngine` **When** `validateRound(scores)` est appelée **Then** elle retourne un succès si la somme vaut exactement 250, sinon une erreur contenant l'écart.

4. **Given** les scores déjà saisis pour N-1 participants **When** `precomputeLastScore(entered)` est appelée **Then** elle retourne `250 - somme(entered)`.

5. **Given** le module Hilt `EngineModule` configuré **When** le graphe d'injection est généré **Then** il expose une `Map<String, GameEngine>` contenant l'entrée `"papayoo" -> PapayooEngine`.

6. **Given** les use cases du domaine **When** ils accèdent aux règles d'un jeu **Then** ils dépendent uniquement de `GameEngine`, jamais directement de `PapayooEngine`.

## Tasks / Subtasks

- [x] **T1 — Définir le contrat domaine** (AC: 1, 3)
  - [x] Créer `domain/engine/GameEngine.kt` avec exactement les quatre propriétés et les deux opérations des AC.
  - [x] Créer `domain/engine/RoundValidation.kt` sous forme de résultat scellé avec `Success` et `Error(difference: Int)`.
  - [x] Définir `difference = somme réelle - totalPointsPerRound` : négatif sous 250, positif au-dessus de 250.
  - [x] Réutiliser `domain/model/ParticipantId.kt`; ne pas créer de second type d'identifiant.

- [x] **T2 — Implémenter les règles Papayoo** (AC: 2, 3, 4)
  - [x] Créer `game/papayoo/PapayooEngine.kt` et implémenter `GameEngine`.
  - [x] Centraliser l'identifiant `"papayoo"` dans une constante réutilisable par le moteur et le binding DI.
  - [x] Implémenter `precomputeLastScore` par `totalPointsPerRound - entered.values.sum()`.
  - [x] Implémenter `validateRound` comme une fonction pure et sans état mutable.
  - [x] Ne pas ajouter ici la persistance, le contrôle du nombre de joueurs ou un use case : ces responsabilités appartiennent aux stories consommatrices.

- [x] **T3 — Enregistrer le moteur avec Hilt/Dagger** (AC: 5)
  - [x] Créer `di/EngineModule.kt`, installé dans `SingletonComponent`.
  - [x] Utiliser le multibinding Dagger `@IntoMap` avec `@StringKey(PapayooEngine.GAME_ID)` afin que chaque futur jeu contribue sa propre entrée sans modifier un registre central.
  - [x] Rendre `PapayooEngine` constructible par Hilt (`@Inject constructor`).
  - [x] Ne pas ajouter `@Singleton` au moteur sans besoin mesuré : il est sans état et peu coûteux.

- [x] **T4 — Ajouter les tests JVM du moteur** (AC: 2, 3, 4, 5)
  - [x] Déclarer `junit = "4.13.2"` dans `[versions]`, puis `junit = { group = "junit", name = "junit", version.ref = "junit" }` dans `[libraries]` de `gradle/libs.versions.toml`.
  - [x] Ajouter `testImplementation(libs.junit)` dans `app/build.gradle.kts`; ne modifier aucune autre dépendance.
  - [x] Créer `app/src/test/java/com/rvetiveau/rvscorecount/game/papayoo/PapayooEngineTest.kt`.
  - [x] Tester les métadonnées `papayoo`, 3, 8 et 250.
  - [x] Tester `validateRound` pour une somme exacte, inférieure et supérieure à 250, y compris un score nul valide.
  - [x] Vérifier le signe de `difference` : par exemple 240 donne `-10`, 260 donne `10`.
  - [x] Tester `precomputeLastScore` pour un complément positif (`100 -> 150`), zéro (`250 -> 0`) et négatif (`300 -> -50`).
  - [x] Faire vérifier le binding `Map<String, GameEngine>` par la compilation KSP; ne pas introduire un test instrumenté uniquement pour Hilt.

- [x] **T5 — Vérifier l'architecture et le build** (AC: 5, 6)
  - [x] Vérifier qu'aucun fichier sous `domain/` n'importe `game`, `data`, `ui`, Room, Compose ou Android.
  - [x] Vérifier que `PapayooEngine` n'est référencé hors de `game/papayoo/` que par `di/EngineModule.kt` et les tests.
  - [x] Exécuter `./gradlew.bat testDebugUnitTest`.
  - [x] Exécuter `./gradlew.bat kspDebugKotlin` puis `./gradlew.bat assembleDebug`.

### Review Findings

- [x] [Review][Patch] Documenter la précondition de bornage des scores — décision utilisateur : conserver le contrat `Int`; les futurs use cases doivent garantir des valeurs métier bornées avant d'appeler le moteur. [app/src/main/java/com/rvetiveau/rvscorecount/game/papayoo/PapayooEngine.kt:14]
- [x] [Review][Patch] Supprimer la completion note générique non traçable [_bmad-output/implementation-artifacts/1-3-interface-gameengine-et-implementation-papayoo.md:220]

  Contrôle PowerShell attendu pour AD-1 :

  ```powershell
  $forbidden = 'import .*(\.game\.|\.data\.|\.ui\.|androidx\.room|androidx\.compose|android\.)'
  Get-ChildItem app/src/main/java/com/rvetiveau/rvscorecount/domain -Recurse -Filter *.kt |
      Select-String -Pattern $forbidden
  # Résultat attendu : aucune correspondance.
  ```

## Dev Notes

### Contrat de validation

Le type concret de `RoundValidation` n'est pas imposé par les epics. Utiliser un contrat minimal et exhaustif :

```kotlin
sealed interface RoundValidation {
    data object Success : RoundValidation
    data class Error(val difference: Int) : RoundValidation
}
```

`difference` est signé selon la convention `somme réelle - total attendu`. Cette convention permet aux futures stories UI de distinguer directement un manque (`< 0`) d'un dépassement (`> 0`). Le succès dépend uniquement de la somme égale à 250, conformément à l'AC 3.

Ne renforcer pas silencieusement ce contrat dans cette story :

- une map vide produit une erreur avec `difference = -250`;
- une somme supérieure à 250 produit une erreur et `precomputeLastScore` peut retourner une valeur négative;
- le score individuel `0` est valide;
- la validation des scores négatifs, du nombre de participants et de leur appartenance à la partie relève des entrées UI/use cases ultérieures, sauf évolution explicite des exigences.

### Forme cible de l'injection

Le registre multi-jeux doit être assemblé par Dagger plutôt que par un `mapOf(...)` central :

```kotlin
class PapayooEngine @Inject constructor() : GameEngine {
    override val gameId = GAME_ID

    companion object {
        const val GAME_ID = "papayoo"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {
    @Binds
    @IntoMap
    @StringKey(PapayooEngine.GAME_ID)
    abstract fun bindPapayooEngine(engine: PapayooEngine): GameEngine
}
```

Ce pattern produit directement la `Map<String, GameEngine>` injectable et fait échouer la compilation en cas de clé dupliquée. Avec Kotlin, si un futur site d'injection expose une erreur de wildcard Dagger, appliquer `@JvmSuppressWildcards` au type valeur sur ce site, pas partout par anticipation.

### Architecture et limites de responsabilité

```text
domain/engine/GameEngine.kt             <- contrat pur
domain/engine/RoundValidation.kt        <- résultat pur
              ^
              |
game/papayoo/PapayooEngine.kt           <- règles Papayoo
              |
di/EngineModule.kt                      <- assemblage Hilt/Dagger
```

- `domain` ne connaît jamais `game`; `game` dépend de `domain`.
- `GameEngine` ne lit ni n'écrit Room et ne conserve aucun état de partie.
- Aucun ViewModel, Composable, repository ou use case n'est créé ou modifié par cette story.
- `ValidateRoundUseCase` est prévu par la story 3.4; le créer maintenant élargirait le périmètre et anticiperait son contrat de persistance.
- Aucune string UI n'est introduite. `RoundValidation` transporte des données, pas un message localisé.
- Belote et tout autre moteur restent hors périmètre V1.

### État actuel des fichiers touchés

Tous les fichiers métier et DI de cette story sont nouveaux. Les seuls fichiers existants à modifier lors de l'implémentation sont la configuration de tests :

- `gradle/libs.versions.toml` : versions et alias existants pour Kotlin/Hilt, mais aucun alias JUnit;
- `app/build.gradle.kts` : Hilt et KSP sont déjà actifs; ajouter seulement `testImplementation(libs.junit)`;
- `domain/model/ParticipantId.kt` : existe déjà comme `typealias ParticipantId = UUID`; le réutiliser sans modification;
- `di/DatabaseModule.kt` et `di/RepositoryModule.kt` : modèles locaux de modules Hilt à préserver, sans les fusionner avec `EngineModule`.

### Fichiers prévus

```text
app/src/main/java/com/rvetiveau/rvscorecount/
  domain/engine/GameEngine.kt                         NEW
  domain/engine/RoundValidation.kt                    NEW
  game/papayoo/PapayooEngine.kt                       NEW
  di/EngineModule.kt                                  NEW
app/src/test/java/com/rvetiveau/rvscorecount/
  game/papayoo/PapayooEngineTest.kt                   NEW
gradle/libs.versions.toml                              UPDATE (JUnit uniquement)
app/build.gradle.kts                                   UPDATE (testImplementation uniquement)
```

### Acquis de la story 1.2

- Les modèles domaine sont purs et séparés des entités Room; conserver cette frontière.
- `ParticipantId` est déjà l'alias de `java.util.UUID` attendu par `GameEngine`.
- Les modules Hilt vivent dans `di/` et sont installés dans `SingletonComponent`.
- La revue 1.2 a renforcé les invariants de persistance (`ABORT`, transaction, appartenance des participants). Le moteur ne doit pas dupliquer ces contrôles ni contourner `PartyRepository`.
- Les commandes `kspDebugKotlin` et `assembleDebug` passaient après la story 1.2.

### Historique Git pertinent

- `b20ae70` (`fix: address story 1.2 review findings`) : intégrité des manches et participants renforcée; préserver ces comportements.
- `2f66dfa` (`init`) : socle initial du projet. Le dépôt ne contient que ces deux commits au moment de la création de cette story.

### Stack à conserver

Ne mettre à niveau aucune dépendance pour cette story. Le dépôt compile avec ses versions verrouillées, qui priment sur les hypothèses plus anciennes de l'architecture : Kotlin 2.4.10, AGP 9.3.1, Hilt 2.60.1, KSP 2.3.11, Room 2.8.4 et Java 17.

## Références

- Story 1.3 et exigences ARCH-1/ARCH-2 : [Source: `_bmad-output/planning-artifacts/epics.md` — "Story 1.3: Interface GameEngine & implémentation Papayoo", "Additional Requirements"]
- Contrat et frontières de couches : [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — "AD-1", "AD-2", "Structural Seed"]
- Règles FR-6 à FR-8 : [Source: `_bmad-output/planning-artifacts/prds/prd-rv-score-counter-2026-08-09/prd.md` — "Saisie de Manche"]
- Contexte UX futur de l'écart et du pré-remplissage : [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/DESIGN.md` — indicateur de somme et champ pré-rempli]
- Modèles, DI et leçons de persistance existants : [Source: `_bmad-output/implementation-artifacts/1-2-schema-de-base-de-donnees-room.md` — "Dev Notes", "Review Findings"]
- Multibinding de map Dagger : https://dagger.dev/dev-guide/multibindings.html
- Modules et scopes Hilt : https://developer.android.com/training/dependency-injection/hilt-android

## Dev Agent Record

### Agent Model Used

Claude Sonnet 4.5 (GitHub Copilot)

### Debug Log References

- `./gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL (8 tests, `PapayooEngineTest`)
- `./gradlew.bat kspDebugKotlin` → BUILD SUCCESSFUL (multibinding `Map<String, GameEngine>` généré sans conflit de clé)
- `./gradlew.bat assembleDebug` → BUILD SUCCESSFUL
- Contrôle PowerShell AD-1 (imports interdits sous `domain/`) → aucune correspondance
- Contrôle des références à `PapayooEngine` hors de `game/papayoo/` → limitées à `di/EngineModule.kt` et `PapayooEngineTest.kt`

### Completion Notes List

- Interface `GameEngine` et `RoundValidation` (résultat scellé) créées dans `domain/engine/`, sans dépendance vers `game`/`data`/`ui`/Room/Compose/Android.
- `PapayooEngine` implémente `GameEngine` avec `gameId = "papayoo"`, `minPlayers = 3`, `maxPlayers = 8`, `totalPointsPerRound = 250`; `validateRound` et `precomputeLastScore` sont des fonctions pures sans état mutable.
- `EngineModule` assemble le multibinding Dagger (`@IntoMap` + `@StringKey`) exposant `Map<String, GameEngine>` sans registre central.
- Alias JUnit 4.13.2 ajouté à `gradle/libs.versions.toml`; `testImplementation(libs.junit)` ajouté à `app/build.gradle.kts` (seule dépendance modifiée).
- 8 tests JVM couvrent métadonnées, `validateRound` (succès exact, succès score nul, écart négatif, écart positif) et `precomputeLastScore` (positif, zéro, négatif).
- Tous les tests passent, la compilation KSP valide le graphe Hilt, et `assembleDebug` réussit.

### File List

- `app/src/main/java/com/rvetiveau/rvscorecount/domain/engine/GameEngine.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/engine/RoundValidation.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/game/papayoo/PapayooEngine.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/di/EngineModule.kt` (NEW)
- `app/src/test/java/com/rvetiveau/rvscorecount/game/papayoo/PapayooEngineTest.kt` (NEW)
- `gradle/libs.versions.toml` (UPDATE — alias JUnit)
- `app/build.gradle.kts` (UPDATE — `testImplementation(libs.junit)`)

### Change Log

- 2026-08-22 : Story créée et marquée `ready-for-dev`.
- 2026-08-22 : Implémentation complète (T1–T5) : `GameEngine`, `RoundValidation`, `PapayooEngine`, `EngineModule`, tests JVM. Statut passé à `review`.
- 2026-08-22 : Revue de code terminée, 2 constats résolus et validations complètes réussies. Statut passé à `done`.