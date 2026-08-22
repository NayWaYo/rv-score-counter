---
story_id: "1.3"
story_key: "1-3-interface-gameengine-et-implementation-papayoo"
epic: 1
status: ready-for-dev
created: 2026-08-22
---

# Story 1.3 : Interface GameEngine et implémentation Papayoo

**Epic :** 1 — Fondation & Infrastructure  
**Story ID :** 1.3  
**Status :** ready-for-dev

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

- [ ] **T1 — Définir le contrat domaine** (AC: 1, 3)
  - [ ] Créer `domain/engine/GameEngine.kt` avec exactement les quatre propriétés et les deux opérations des AC.
  - [ ] Créer `domain/engine/RoundValidation.kt` sous forme de résultat scellé avec `Success` et `Error(difference: Int)`.
  - [ ] Définir `difference = somme réelle - totalPointsPerRound` : négatif sous 250, positif au-dessus de 250.
  - [ ] Réutiliser `domain/model/ParticipantId.kt`; ne pas créer de second type d'identifiant.

- [ ] **T2 — Implémenter les règles Papayoo** (AC: 2, 3, 4)
  - [ ] Créer `game/papayoo/PapayooEngine.kt` et implémenter `GameEngine`.
  - [ ] Centraliser l'identifiant `"papayoo"` dans une constante réutilisable par le moteur et le binding DI.
  - [ ] Implémenter `precomputeLastScore` par `totalPointsPerRound - entered.values.sum()`.
  - [ ] Implémenter `validateRound` comme une fonction pure et sans état mutable.
  - [ ] Ne pas ajouter ici la persistance, le contrôle du nombre de joueurs ou un use case : ces responsabilités appartiennent aux stories consommatrices.

- [ ] **T3 — Enregistrer le moteur avec Hilt/Dagger** (AC: 5)
  - [ ] Créer `di/EngineModule.kt`, installé dans `SingletonComponent`.
  - [ ] Utiliser le multibinding Dagger `@IntoMap` avec `@StringKey(PapayooEngine.GAME_ID)` afin que chaque futur jeu contribue sa propre entrée sans modifier un registre central.
  - [ ] Rendre `PapayooEngine` constructible par Hilt (`@Inject constructor`).
  - [ ] Ne pas ajouter `@Singleton` au moteur sans besoin mesuré : il est sans état et peu coûteux.

- [ ] **T4 — Ajouter les tests JVM du moteur** (AC: 2, 3, 4, 5)
  - [ ] Déclarer `junit = "4.13.2"` dans `[versions]`, puis `junit = { group = "junit", name = "junit", version.ref = "junit" }` dans `[libraries]` de `gradle/libs.versions.toml`.
  - [ ] Ajouter `testImplementation(libs.junit)` dans `app/build.gradle.kts`; ne modifier aucune autre dépendance.
  - [ ] Créer `app/src/test/java/com/rvetiveau/rvscorecount/game/papayoo/PapayooEngineTest.kt`.
  - [ ] Tester les métadonnées `papayoo`, 3, 8 et 250.
  - [ ] Tester `validateRound` pour une somme exacte, inférieure et supérieure à 250, y compris un score nul valide.
  - [ ] Vérifier le signe de `difference` : par exemple 240 donne `-10`, 260 donne `10`.
  - [ ] Tester `precomputeLastScore` pour un complément positif (`100 -> 150`), zéro (`250 -> 0`) et négatif (`300 -> -50`).
  - [ ] Faire vérifier le binding `Map<String, GameEngine>` par la compilation KSP; ne pas introduire un test instrumenté uniquement pour Hilt.

- [ ] **T5 — Vérifier l'architecture et le build** (AC: 5, 6)
  - [ ] Vérifier qu'aucun fichier sous `domain/` n'importe `game`, `data`, `ui`, Room, Compose ou Android.
  - [ ] Vérifier que `PapayooEngine` n'est référencé hors de `game/papayoo/` que par `di/EngineModule.kt` et les tests.
  - [ ] Exécuter `./gradlew.bat testDebugUnitTest`.
  - [ ] Exécuter `./gradlew.bat kspDebugKotlin` puis `./gradlew.bat assembleDebug`.

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

À renseigner par l'agent d'implémentation.

### Debug Log References

### Completion Notes List

- Ultimate context engine analysis completed - comprehensive developer guide created.

### File List

### Change Log

- 2026-08-22 : Story créée et marquée `ready-for-dev`.