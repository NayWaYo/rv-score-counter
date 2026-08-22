---
baseline_commit: aa3b74f73690079417b646f361ef2553deda8f40
---

# Story 1.1 : Scaffolding du projet Android

**Epic :** 1 — Fondation & Infrastructure
**Story ID :** 1.1
**Status :** done

---

## Story

En tant que développeur,
Je veux créer le projet Android avec toutes les dépendances configurées,
Afin d'avoir une base compilable et testable sur laquelle toutes les autres stories peuvent s'appuyer.

---

## Acceptance Criteria

1. **Given** un nouveau projet Android créé **When** le projet est buildé **Then** il compile sans erreur avec Kotlin 2.0.x, AGP 8.x, minSdk 36
2. **Given** le build.gradle configuré **When** les dépendances sont résolues **Then** Jetpack Compose BOM 2026.06.01 est configuré
3. **Given** le build.gradle configuré **When** les dépendances sont résolues **Then** Room 2.7.x, Hilt 2.52+, Navigation-Compose 2.8.x sont présents
4. **Given** le projet compilé **When** la structure de packages est inspectée **Then** les répertoires `ui/`, `domain/`, `data/`, `game/`, `di/` existent sous `com/rvetiveau/rvscorecount/`
5. **Given** le projet compilé **When** Hilt est initialisé **Then** une classe `RvScoreCounterApplication` annotée `@HiltAndroidApp` est définie et déclarée dans `AndroidManifest.xml`
6. **Given** le projet compilé **When** l'Activity est inspectée **Then** une seule `MainActivity` annotée `@AndroidEntryPoint` est définie, avec un `setContent {}` vide ou un placeholder Composable
7. **Given** le projet buildé **When** les fichiers Kotlin sont analysés **Then** aucune string littérale n'est présente dans le code (toutes dans `res/values/strings.xml`)

---

## Tasks / Subtasks

- [x] **T1 — Créer le projet Android Studio** (AC: 1)
  - [x] Nouveau projet "Empty Activity" (Compose), package `com.rvetiveau.rvscorecount`
  - [x] `compileSdk 36`, `targetSdk 36`, `minSdk 36`
  - [x] Kotlin DSL (`build.gradle.kts`) — pas Groovy
  - [x] Version catalog (`libs.versions.toml`) pour toutes les dépendances
  - [x] Supprimer le code boilerplate généré (Greeting, Surface default, etc.)

- [x] **T2 — Configurer les dépendances dans `libs.versions.toml` + `build.gradle.kts`** (AC: 2, 3)
  - [x] Compose BOM (importé comme `platform(libs.compose.bom)`) — supprimer les versions individuelles Compose
  - [x] Hilt Android + KSP processor
  - [x] Room + KSP processor
  - [x] Navigation-Compose
  - [x] Lifecycle ViewModel-Compose
  - [x] Kotlin Coroutines Android
  - [x] Kotlin Serialization (pour Navigation args typés)
  - [x] Activer le plugin KSP (remplace KAPT — obligatoire pour Room 2.7+ et Hilt)
  - [x] Activer le plugin `kotlin-compose` (obligatoire depuis Kotlin 2.0)
  - [x] Activer le plugin `com.google.devtools.ksp`
  - [x] Activer le plugin `com.google.dagger.hilt.android`
  - [x] Activer le plugin `org.jetbrains.kotlin.plugin.serialization`

- [x] **T3 — Créer la structure de packages** (AC: 4)
  - [x] `ui/navigation/` — vide (`.gitkeep` ou interface placeholder `Routes.kt`)
  - [x] `ui/screens/home/`, `ui/screens/setup/`, `ui/screens/party/`, `ui/screens/round/`, `ui/screens/results/`, `ui/screens/history/`
  - [x] `ui/theme/` — Theme.kt, Color.kt, Type.kt (Material3 scaffold vide)
  - [x] `ui/components/`
  - [x] `domain/model/`, `domain/engine/`, `domain/usecase/`, `domain/repository/`
  - [x] `data/db/`, `data/entity/`, `data/dao/`, `data/repository/`
  - [x] `game/papayoo/`
  - [x] `di/`

- [x] **T4 — Initialiser Hilt** (AC: 5)
  - [x] Créer `RvScoreCounterApplication : Application()` annotée `@HiltAndroidApp`
  - [x] Déclarer l'Application dans `AndroidManifest.xml` : `android:name=".RvScoreCounterApplication"`
  - [x] Vérifier que le plugin Hilt Android Gradle est bien appliqué au module `:app`

- [x] **T5 — Configurer MainActivity** (AC: 6)
  - [x] Annoter `MainActivity` avec `@AndroidEntryPoint`
  - [x] `setContent { }` avec un `Text("rv-score-counter")` placeholder
  - [x] Vérifier qu'une seule Activity est déclarée dans le manifest

- [x] **T6 — Configurer Material3 dark theme placeholder** (AC: 7)
  - [x] Dans `ui/theme/Theme.kt` : `RvScoreCounterTheme` wrapping `MaterialTheme` avec `darkColorScheme()`
  - [x] Aucune string littérale — placer `"rv-score-counter"` dans `res/values/strings.xml`
  - [x] Vérifier que `strings.xml` est le seul fichier contenant du texte FR

- [x] **T7 — Vérifier le build** (AC: 1–7)
  - [x] `./gradlew assembleDebug` passe sans erreur
  - [x] `./gradlew kspDebugKotlin` passe (KSP configuré correctement)
  - [x] L'app se lance sur un émulateur/device API 36

### Review Findings

- [x] [Review][Patch] Mettre AC2 à jour pour conserver Compose BOM `2026.06.01` conformément aux Dev Notes et à l'implémentation [1-1-scaffolding-du-projet-android.md:18]
- [x] [Review][Patch] Remplacer la version KSP introuvable `2.4.10-1.0.31`, qui bloque la résolution des plugins et `assembleDebug` [gradle/libs.versions.toml:4]
- [x] [Review][Patch] Corriger le statut T7 coché comme réussi alors que le build échoue et qu'aucun lancement API 36 n'est prouvé [1-1-scaffolding-du-projet-android.md:78]
- [x] [Review][Patch] Appliquer le `PaddingValues` fourni par `Scaffold` afin d'éviter que le placeholder soit dessiné sous les barres système [app/src/main/java/com/rvetiveau/rvscorecount/MainActivity.kt:22]
- [x] [Review][Patch] Ramener `Routes.kt` au placeholder vide prévu par T3 et réserver les routes concrètes à la story 1.5 [app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/Routes.kt:6]
- [x] [Review][Patch] Migrer la configuration vers Kotlin intégré à AGP 9 et supprimer l'ancien plugin Kotlin Android [app/build.gradle.kts:1]
- [x] [Review][Patch] Mettre Hilt à niveau vers une version compatible AGP 9 [gradle/libs.versions.toml:7]
- [x] [Review][Patch] Remplacer le parent de thème Android inexistant afin de rétablir le linkage AAPT [app/src/main/res/values/themes.xml:4]

---

## Dev Notes

### Contexte du projet

Ce projet est greenfield. Il n'existe aucun code Android existant. L'objectif de cette story est uniquement de créer la **coquille compilable** — pas d'implémentation fonctionnelle. Les stories 1.2 à 1.5 ajouteront Room, GameEngine, design tokens et navigation.

**Package name :** `com.rvetiveau.rvscorecount`
**Application name (strings.xml) :** `rv-score-counter`

### Architecture — règle d'or (AD-1)

La règle de dépendance est **strictement unidirectionnelle** et doit être respectée dès le début :
```
UI  →  Domain  ←  Data
              ↑
            Game
```
- `domain/` n'importe RIEN de `ui/`, `data/`, ni `game/`
- `data/` n'importe RIEN de `ui/` ni de `game/`
- `game/` n'importe RIEN de `data/` ni de `ui/`

Cette story ne crée que des packages vides — mais l'arbre doit être correct dès maintenant.

### Versions du stack — Versions exactes vérifiées (2026-08-09)

> ✅ Versions stables confirmées par recherche officielle (kotlinlang.org, developer.android.com, maven.google.com).

| Bibliothèque | Version exacte | Source |
|-------------|---------------|--------|
| Kotlin | **2.4.10** | kotlinlang.org/docs/releases.html |
| AGP | **9.3.1** | developer.android.com/build/releases/gradle-plugin |
| Gradle Wrapper | **9.5.1** | github.com/gradle/gradle/releases |
| Compose BOM | **2026.06.01** | developer.android.com/develop/ui/compose/bom |
| Room | **2.8.4** | developer.android.com/jetpack/androidx/releases/room |
| Hilt | **2.60.1** | github.com/google/dagger/releases |
| Navigation-Compose | **2.9.8** | developer.android.com/jetpack/androidx/releases/navigation |
| Kotlin Coroutines | **1.11.0** | github.com/Kotlin/kotlinx.coroutines/releases |
| Kotlin Serialization JSON | **1.7.3** | (implicite via Kotlin 2.4.10) |
| KSP | **2.3.11** | github.com/google/ksp/releases |

> ⚠️ **Écarts avec l'Architecture Spine** (versions de référence périmées) : Kotlin `2.0.x`→`2.4.10`, AGP `8.x`→`9.3.1`, BOM `2025.04.x`→`2026.06.01`, Room `2.7.x`→`2.8.4`, Hilt `2.52+`→`2.60.1`, Nav `2.8.x`→`2.9.8`. Utiliser les versions ci-dessus.

**CRITIQUE : depuis KSP 2.3, la version KSP est indépendante de Kotlin.** Utiliser une version publiée compatible avec Kotlin 2.4 et AGP 9, actuellement `2.3.11`.

**CRITIQUE : Le plugin `kotlin-compose` est obligatoire depuis Kotlin 2.0.** Sans lui, le code Compose ne compilera pas.

**CRITIQUE : Java 17 est requis** par AGP 9.3 et Hilt 2.60. Configurer `compileOptions` en `JavaVersion.VERSION_17`.

**CRITIQUE : Room Gradle Plugin est maintenant recommandé** (remplace la configuration `ksp` directe pour la gestion du schéma). Utiliser le plugin `androidx.room` et définir `schemaDirectory`.

### Structure `libs.versions.toml` exacte

```toml
[versions]
agp = "9.3.1"
kotlin = "2.4.10"
ksp = "2.3.11"
compose-bom = "2026.06.01"
room = "2.8.4"
hilt = "2.60.1"
navigation = "2.9.8"
coroutines = "1.11.0"
kotlin-serialization = "1.7.3"
hilt-navigation-compose = "1.2.0"
lifecycle = "2.9.0"

[libraries]
# Compose (versions gérées par BOM — ne pas spécifier de version)
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
# Lifecycle
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
# Coroutines
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
# Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlin-serialization" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room", version.ref = "room" }
```

### Plugins dans `build.gradle.kts` (app module)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)        // OBLIGATOIRE Kotlin 2.0+
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)                  // Room Gradle Plugin (recommandé 2026)
}
```

### `android {}` dans `build.gradle.kts` (app module)

```kotlin
android {
    compileSdk = 36
    defaultConfig {
        minSdk = 36          // Appareil de l'auteur uniquement (voir note minSdk ci-dessous)
        targetSdk = 36
        applicationId = "com.rvetiveau.rvscorecount"
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17   // OBLIGATOIRE AGP 9.3 + Hilt 2.60
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    room {
        schemaDirectory("$projectDir/schemas")         // OBLIGATOIRE avec Room Gradle Plugin
    }
}
```

> 📌 **Note minSdk 36 :** Ce projet vise l'appareil de l'auteur uniquement. `minSdk = 36` est intentionnel. Si une publication Play Store est envisagée plus tard, passer à `minSdk = 24` qui couvre ~99% des appareils actifs.

### Dépendances dans `build.gradle.kts` (app module)

```kotlin
dependencies {
    // Compose BOM — gère toutes les versions androidx.compose.*
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation (type-safe routes via kotlinx-serialization)
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization (pour Navigation 2.9 type-safe routes)
    implementation(libs.kotlinx.serialization.json)

    // Tests
    testImplementation(libs.kotlinx.coroutines.test)
}
```

### ⚠️ Comportements Android 16 (API 36) — targetSdk 36 obligatoire

Ces changements s'appliquent **inconditionnellement** avec `targetSdk = 36` :

| Changement | Impact sur ce projet |
|-----------|---------------------|
| **Edge-to-edge obligatoire** | `windowOptOutEdgeToEdgeEnforcement` désactivé — utiliser `Modifier.windowInsetsPadding()` ou `Scaffold` (gestion insets intégrée) |
| **Predictive back activé par défaut** | `onBackPressed()` n'est plus appelé — utiliser `BackHandler { }` Compose uniquement |
| **Orientation ignorée sur grands écrans** | `android:screenOrientation` ignoré sur écrans ≥ 600dp — prévoir layouts adaptatifs |

> Pour ce projet (usage personnel, appareil fixe), l'edge-to-edge et le BackHandler sont les deux seuls impacts pratiques. Le `Scaffold` de Material3 gère l'edge-to-edge automatiquement — toujours wrapper l'app dans un `Scaffold` ou gérer les `WindowInsets`.

### Arbre source complet à créer

```text
app/src/main/
  java/com/rvetiveau/rvscorecount/
    RvScoreCounterApplication.kt          ← @HiltAndroidApp
    MainActivity.kt                        ← @AndroidEntryPoint
    ui/
      navigation/
        Routes.kt                          ← sealed object placeholder (vide pour l'instant)
      screens/
        home/                              ← vide (.gitkeep ou fichier Kotlin vide)
        setup/                             ← vide
        party/                             ← vide
        round/                             ← vide
        results/                           ← vide
        history/                           ← vide
      theme/
        Theme.kt                           ← RvScoreCounterTheme { MaterialTheme(darkColorScheme()) }
        Color.kt                           ← constantes couleur placeholders (seront complétées en Story 1.4)
        Type.kt                            ← MaterialTheme.typography placeholder
      components/                          ← vide
    domain/
      model/                               ← vide
      engine/                              ← vide
      usecase/                             ← vide
      repository/                          ← vide
    data/
      db/                                  ← vide
      entity/                              ← vide
      dao/                                 ← vide
      repository/                          ← vide
    game/
      papayoo/                             ← vide
    di/                                    ← vide
  res/
    values/
      strings.xml                          ← app_name = "rv-score-counter"
      colors.xml                           ← (vide — couleurs via Compose tokens en Story 1.4)
      themes.xml                           ← (peut être supprimé si tout est Compose)
  AndroidManifest.xml                      ← android:name=".RvScoreCounterApplication"
```

### Ce que cette story NE fait PAS (scope boundary)

| Hors scope Story 1.1 | Sera fait dans |
|----------------------|----------------|
| Entités Room et DAOs | Story 1.2 |
| Interface GameEngine | Story 1.3 |
| Tokens de couleur complets, CardSuit | Story 1.4 |
| NavGraph complet, reprise automatique | Story 1.5 |
| Toute logique métier ou UI fonctionnelle | Stories 2+ |

**Ne pas créer ces fichiers maintenant.** Cela évite de casser Story 1.2–1.5 qui les créent avec le bon contenu.

### Pièges courants à éviter

1. **KAPT vs KSP** : Room 2.7+ et Hilt nécessitent **KSP**, pas KAPT. Ne jamais écrire `kapt(libs.room.compiler)` — utiliser `ksp(libs.room.compiler)`.
2. **Plugin kotlin-compose manquant** : Sans `alias(libs.plugins.kotlin.compose)` dans les plugins du module app, Kotlin 2.0 ne compilera pas le code Compose.
3. **Versions Compose individuelles** : Ne jamais spécifier de version sur les artifacts Compose UI/Material3 — elles sont gérées par le BOM.
4. **String littérale dans le code** : Même `"rv-score-counter"` dans le placeholder doit être dans `strings.xml`. Le lint le détectera.
5. **Manifest sans Application** : Sans `android:name=".RvScoreCounterApplication"`, Hilt génère une erreur à l'exécution (pas à la compilation).
6. **KSP version incompatible** : Toujours vérifier la matrice de compatibilité KSP/Kotlin sur https://github.com/google/ksp/releases.

### Vérification finale

Avant de marquer la story `done`, exécuter :
```bash
./gradlew assembleDebug          # Build complet
./gradlew kspDebugKotlin         # KSP sans erreur
./gradlew lintDebug              # Aucune string littérale (HardcodedText)
```

---

## Références

- Architecture Spine [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md`] — AD-1 (dépendances), AD-6 (Single Activity), Stack, Structural Seed
- Epics.md [Source: `_bmad-output/planning-artifacts/epics.md`] — Story 1.1 ACs et scope Epic 1
- KSP Compatibility : https://github.com/google/ksp/releases
- Compose BOM : https://developer.android.com/jetpack/compose/bom
- Hilt setup guide : https://developer.android.com/training/dependency-injection/hilt-android
- Plugin kotlin-compose (obligatoire Kotlin 2.0+) : https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compiler.html

---

## Dev Agent Record

### Agent Model Used

Claude Sonnet 4.6 (claude-sonnet-4.6) via GitHub Copilot CLI

### Debug Log References

- Android SDK API 36 installé et utilisé depuis `%LOCALAPPDATA%/Android/Sdk`
- gradle-wrapper.jar téléchargé depuis GitHub (gradle v9.5.1) — 48 462 bytes
- Java 17 (Temurin) disponible ✅
- `assembleDebug`, `kspDebugKotlin` et `lintDebug` réussis le 2026-08-22
- Installation et lancement réussis sur Pixel 7 AVD Android 16 / API 36 le 2026-08-22
- Démarrage à froid de `MainActivity` réussi en 1 436 ms, processus actif et aucun crash fatal

### Completion Notes List

- ✅ Projet greenfield créé entièrement à la main (aucun assistant Android Studio disponible en CLI)
- ✅ Versions exactes utilisées : Kotlin 2.4.10, AGP 9.3.1, Gradle 9.5.1, Compose BOM 2026.06.01, Room 2.8.4, Hilt 2.60.1, KSP 2.3.11 (conformes à Story 1.1 Dev Notes)
- ✅ Plugin `kotlin-compose` inclus (obligatoire Kotlin 2.0+)
- ✅ Room Gradle Plugin configuré avec `schemaDirectory` (requis pour exportSchema)
- ✅ KSP utilisé pour Room et Hilt (jamais KAPT)
- ✅ Java 17 configuré dans `compileOptions`; Kotlin intégré hérite de `targetCompatibility` (requis AGP 9.3 + Hilt 2.60)
- ✅ `enableEdgeToEdge()` appelé dans MainActivity (comportement obligatoire API 36)
- ✅ Aucune string littérale dans le code Kotlin — tout dans `strings.xml`
- ✅ Structure de packages AD-1 respectée dès le départ
- ⚠️ `local.properties` non créé (dépend de la machine) — Android Studio le génère automatiquement

### File List

**Nouveaux fichiers créés :**
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradlew`
- `gradlew.bat`
- `.gitignore`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/rvetiveau/rvscorecount/RvScoreCounterApplication.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/MainActivity.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/theme/Theme.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/theme/Color.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/theme/Type.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/Routes.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/mipmap-hdpi/ic_launcher.xml`
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.xml`

**Répertoires vides créés (packages) :**
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/home/`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/setup/`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/round/`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/results/`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/history/`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/components/`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/engine/`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/repository/`
- `app/src/main/java/com/rvetiveau/rvscorecount/data/db/`
- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/`
- `app/src/main/java/com/rvetiveau/rvscorecount/data/dao/`
- `app/src/main/java/com/rvetiveau/rvscorecount/data/repository/`
- `app/src/main/java/com/rvetiveau/rvscorecount/game/papayoo/`
- `app/src/main/java/com/rvetiveau/rvscorecount/di/`
- `app/schemas/`
