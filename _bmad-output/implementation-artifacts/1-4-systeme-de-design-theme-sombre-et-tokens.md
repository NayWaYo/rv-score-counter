---
story_id: "1.4"
story_key: "1-4-systeme-de-design-theme-sombre-et-tokens"
epic: 1
status: done
created: 2026-08-22
baseline_commit: 275f7ef
---

# Story 1.4 : Système de design — thème sombre & tokens

**Epic :** 1 — Fondation & Infrastructure
**Story ID :** 1.4
**Status :** done

## Story

En tant qu'utilisateur,
Je veux une interface en thème sombre avec les couleurs et composants définis,
Afin que l'app soit lisible en soirée depuis la distance d'une table.

## Acceptance Criteria

1. **Given** l'app lancée **When** n'importe quel écran est affiché **Then** le thème Material3 dark est appliqué globalement (surface-base #121212).

2. **And** les tokens de couleur sont définis : surface-raised #1E1E1E, surface-container #2C2C2C, ink-primary #E8E0D5, accent #FFB74D, error #CF6679, success #4CAF82.

3. **And** CardSuit enum existe dans domain/model/ avec SPADE, HEART, CLUB, DIAMOND.

4. **And** CardSuitColors.kt dans ui/theme/ mappe chaque CardSuit vers : SPADE #90A4AE, HEART #EF5350, CLUB #66BB6A, DIAMOND #42A5F5.

5. **And** les teintes de fond suit-*-tint sont définies (SPADE #1A2328, HEART #2D1517, CLUB #182A1A, DIAMOND #16222E).

6. **And** toutes les strings UI sont dans res/values/strings.xml (aucune string littérale dans le code).

## Tasks / Subtasks

- [x] **T1 — Finaliser les tokens de couleur** (AC: 2, 4, 5)
  - [x] Conserver dans `ui/theme/Color.kt` les tokens de surface, texte, action, erreur, succès et teintes déjà exacts.
  - [x] Ajouter les quatre couleurs d'enseigne avec les valeurs ARGB opaques exactes des AC.
  - [x] Remplacer le commentaire indiquant que les teintes sont des placeholders : leurs valeurs sont déjà finales.
  - [x] Aligner `OnAccent` sur `#1A1200`, valeur `accent-on` de la spécification UX; ne pas ajouter les autres tokens UX hors AC sans consommateur dans cette story.

- [x] **T2 — Exposer le mapping UI de `CardSuit`** (AC: 3, 4, 5)
  - [x] Créer `ui/theme/CardSuitColors.kt`; importer le `CardSuit` domaine existant et `androidx.compose.ui.graphics.Color` uniquement dans la couche UI.
  - [x] Exposer deux mappings réutilisables, un pour la couleur d'enseigne et un pour sa teinte de fond, sous forme de propriétés d'extension exhaustives sur `CardSuit` (noms cibles : `color` et `tint`).
  - [x] Utiliser un `when` exhaustif sans branche `else`, afin qu'une future valeur de l'enum provoque une erreur de compilation jusqu'à ce que ses couleurs soient définies.
  - [x] Ne pas dupliquer, déplacer ni renommer `domain/model/CardSuit.kt`; ses noms sont persistés par Room via `enum.name`.

- [x] **T3 — Compléter le thème Material 3 sombre global** (AC: 1, 2)
  - [x] Conserver un unique `darkColorScheme` dans `ui/theme/Theme.kt`; ne pas ajouter de thème clair, de sélection système ou de couleurs dynamiques.
  - [x] Conserver `background = BackgroundBase`, `surface = SurfaceRaised`, `surfaceVariant = SurfaceContainer`, `primary = Accent`, `onPrimary = OnAccent` et `error = Error`.
  - [x] Câbler explicitement `onBackground`, `onSurface` et `onSurfaceVariant` vers `InkPrimary` pour que le texte Material utilise le token lisible attendu.
  - [x] Préserver `RvScoreCounterTheme`, `Typography` et son application racine existante dans `MainActivity`; ne pas créer un second wrapper de thème.

- [x] **T4 — Ajouter les vérifications ciblées** (AC: 1–5)
  - [x] Créer `app/src/test/java/com/rvetiveau/rvscorecount/ui/theme/CardSuitColorsTest.kt` avec JUnit 4 déjà configuré.
  - [x] Vérifier pour les quatre valeurs de `CardSuit` la couleur d'enseigne et la teinte exactes; comparer des `Color(0xFF...)` opaques.
  - [x] Ajouter un test JVM des tokens et des rôles essentiels du schéma sombre si les membres `internal` sont accessibles depuis le source set de test; sinon les vérifier par compilation réussie et revue manuelle de `Theme.kt`, sans élargir leur visibilité uniquement pour le test. (`DarkColorScheme` reste `private`; vérifié par compilation réussie et revue manuelle de `Theme.kt`.)
  - [x] Vérifier qu'aucune référence Compose ou couleur n'a été ajoutée sous `domain/`.

- [x] **T5 — Vérifier les ressources et le build** (AC: 1, 6)
  - [x] Conserver `R.string.app_name` et `stringResource` dans `MainActivity`; cette story n'introduit normalement aucune nouvelle chaîne visible.
  - [x] Rechercher les littéraux passés à `Text`, `contentDescription`, `Button` ou composants équivalents dans `app/src/main`; toute nouvelle chaîne visible doit être ajoutée à `res/values/strings.xml`.
  - [x] Exécuter d'abord le test ciblé, puis `./gradlew.bat testDebugUnitTest` et `./gradlew.bat assembleDebug` avec JDK 17.
  - [x] Exécuter `git diff --check` et contrôler que seuls les fichiers prévus ont changé.

### Review Findings

- [x] [Review][Patch] Valider aussi les fichiers non suivis avec `git diff --check` et corriger la preuve consignée [_bmad-output/implementation-artifacts/1-4-systeme-de-design-theme-sombre-et-tokens.md:178]
- [x] [Review][Patch] Compléter la File List avec la story et `sprint-status.yaml`, présents dans le diff réel [_bmad-output/implementation-artifacts/1-4-systeme-de-design-theme-sombre-et-tokens.md:189]
- [x] [Review][Patch] Renseigner le champ Agent Model Used avant la clôture de la revue [_bmad-output/implementation-artifacts/1-4-systeme-de-design-theme-sombre-et-tokens.md:170]
- [x] [Review][Defer] Forcer un style sombre pour les barres système lorsque l'application est dark-only [app/src/main/java/com/rvetiveau/rvscorecount/MainActivity.kt:20] — deferred, pre-existing

## Dev Notes

### État actuel et delta exact

La story complète un scaffold déjà fonctionnel; elle ne crée pas le système de design depuis zéro.

- `ui/theme/Color.kt` contient déjà les six tokens demandés et les quatre teintes avec les bonnes valeurs. Il manque les quatre couleurs d'enseigne. Le commentaire « placeholders » est devenu faux. `OnAccent` vaut actuellement `#000000`, alors que la référence UX fixe `accent-on` à `#1A1200`.
- `ui/theme/Theme.kt` fournit déjà un `darkColorScheme` unique à `RvScoreCounterTheme`. Les rôles `background`, `surface`, `surfaceVariant`, `primary`, `onPrimary` et `error` sont câblés; les rôles de contenu doivent être reliés à `InkPrimary`.
- `ui/theme/Type.kt` définit déjà `bodyLarge` à 16sp et utilise les conventions Material 3. Ne pas ajouter de police ni reconstruire toute l'échelle typographique dans cette story.
- `domain/model/CardSuit.kt` existe déjà avec exactement `SPADE`, `HEART`, `CLUB`, `DIAMOND`. C'est une enum persistée : ne modifier ni le package, ni l'ordre utile au code existant, ni les noms.
- `MainActivity.kt` applique déjà `RvScoreCounterTheme` autour de tout le contenu, gère l'edge-to-edge et consomme le padding du `Scaffold`. Son texte visible utilise déjà `R.string.app_name`.
- `res/values/strings.xml` contient `app_name`. Aucun nouveau texte visible n'est requis pour livrer les AC.
- `ui/theme/CardSuitColors.kt` et son test n'existent pas encore : ce sont les deux créations attendues.

### Contrat cible des mappings

Le fichier `CardSuitColors.kt` doit centraliser les deux décisions visuelles afin que les futurs écrans ne recopient pas de `when` ni de valeurs hexadécimales :

```kotlin
val CardSuit.color: Color
    get() = when (this) {
        CardSuit.SPADE -> Spade
        CardSuit.HEART -> Heart
        CardSuit.CLUB -> Club
        CardSuit.DIAMOND -> Diamond
    }

val CardSuit.tint: Color
    get() = when (this) {
        CardSuit.SPADE -> SpadeTint
        CardSuit.HEART -> HeartTint
        CardSuit.CLUB -> ClubTint
        CardSuit.DIAMOND -> DiamondTint
    }
```

Les noms des constantes d'enseigne doivent rester explicites et cohérents avec les constantes `*Tint`. Les propriétés sont une API UI, jamais une propriété de `CardSuit` dans le domaine.

### Architecture et limites de responsabilité

- AD-7 impose `CardSuit` dans `domain/model/` et tout mapping vers `Color` dans `ui/theme/CardSuitColors.kt`. La dépendance autorisée est `ui → domain`; l'inverse est interdit.
- Les couleurs d'enseigne ont un usage UX strict : résultat du dé et historique des manches uniquement. Elles ne remplacent pas l'accent ambre et ne décorent pas la chrome générale.
- Le thème V1 est `dark-only`. Ne pas utiliser `isSystemInDarkTheme`, `lightColorScheme`, `dynamicDarkColorScheme` ou `dynamicLightColorScheme`.
- Utiliser les rôles Material (`MaterialTheme.colorScheme`) dans les futurs composants plutôt que réintroduire les hexadécimales. Cette story définit les tokens et mappings; elle ne crée aucun écran, composant métier, ViewModel ou navigation.
- `Success` reste un token autonome : Material 3 ne fournit pas de rôle sémantique `success`. Ne pas le détourner vers `secondary` ou `tertiary` sans besoin de composant.
- Ne modifier aucune dépendance, aucun fichier Room, aucun schéma exporté et aucun moteur de jeu.

`OnAccent` n'apparaît pas dans l'AC 2, mais son alignement est dans le périmètre : `DESIGN.md` fixe `accent-on` à `#1A1200` et le schéma existant le consomme déjà via `onPrimary`. Les autres tokens UX absents des AC restent hors périmètre faute de consommateur confirmé.

### Accessibilité et validation visuelle

- `InkPrimary` doit être utilisé comme contenu des surfaces sombres; les paires `on*`/conteneur évitent que les composants Material choisissent leurs valeurs par défaut.
- La lisibilité à environ 50 cm en lumière tamisée est la raison fonctionnelle du thème, pas une préférence décorative.
- Ne pas valider une couleur uniquement par une capture d'écran : les tests JVM doivent verrouiller les valeurs exactes. Une vérification visuelle de l'écran scaffold peut compléter, mais pas remplacer, ces assertions.
- Toutes les couleurs exigées sont opaques et doivent garder le préfixe alpha `FF` dans `Color(0xFFRRGGBB)`.

### Fichiers prévus

```text
app/src/main/java/com/rvetiveau/rvscorecount/
  ui/theme/Color.kt                              UPDATE
  ui/theme/Theme.kt                              UPDATE
  ui/theme/CardSuitColors.kt                     NEW
app/src/test/java/com/rvetiveau/rvscorecount/
  ui/theme/CardSuitColorsTest.kt                 NEW
```

`CardSuit.kt`, `Type.kt`, `MainActivity.kt` et `strings.xml` sont des fichiers à contrôler et préserver, pas des modifications attendues sauf écart découvert pendant l'implémentation.

### Acquis de la story 1.3

- La branche dédiée et le wrapper Gradle sous JDK 17 sont le mode de travail validé.
- JUnit 4.13.2 et `testImplementation(libs.junit)` sont déjà configurés; ne pas les ajouter une seconde fois.
- La revue précédente a confirmé l'importance de garder le domaine pur et de documenter précisément les limites de contrat. Ici, cela interdit toute représentation Compose dans `CardSuit`.
- Les validations `testDebugUnitTest`, `kspDebugKotlin` et `assembleDebug` passaient à la fin de la story 1.3. Cette story ne touche pas Hilt/Room/KSP; `kspDebugKotlin` n'est donc pas une validation ciblée obligatoire.

### Historique Git pertinent

- `275f7ef` (`project-context creation`) : ajoute les règles IA faisant autorité pour l'implémentation.
- `b087ccb` (`merge: complete story 1.3 game engine`) : baseline intégrée de la story précédente.
- `764d56e` (`feat: add Papayoo game engine`) : confirme les tests JVM JUnit 4 et la séparation `game → domain`.
- Branche active lors de la création : `story/1-4-design-system-dark-theme`; worktree propre.

### Stack et informations Material 3

- Conserver Kotlin 2.4.10, Java 17, AGP 9.3.1, Gradle 9.5.1 et Compose BOM 2026.06.01. Les fichiers Gradle exécutables priment sur les versions historiques des planning artifacts.
- La documentation Android actuelle confirme que `MaterialTheme` fournit les sous-systèmes couleur, typographie et formes, et que `darkColorScheme` est le constructeur dédié au schéma sombre.
- Material 3 recommande d'associer chaque couleur de conteneur à son rôle `on*` pour le contraste. Cette recommandation justifie le câblage explicite d'`InkPrimary` sans introduire un nouveau système de thème.
- Aucune mise à niveau n'est nécessaire ou autorisée par cette story.

## Références

- Story et AC contractuels : [Source: `_bmad-output/planning-artifacts/epics.md` — « Story 1.4: Système de design — thème sombre & tokens »]
- Séparation domaine/UI et emplacement du mapping : [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — « AD-7 », « Consistency Conventions », « Structural Seed »]
- Palette, mode `dark-only`, usages et accessibilité : [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/DESIGN.md` — frontmatter `colors`, « Brand & Style », « Colors », « Typography »]
- Contexte de lecture à distance : [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md` — « Platform Strategy », « Accessibility »]
- Règles durables du dépôt : [Source: `_bmad-output/project-context.md` — « Pile technologique », « Règles Android et frameworks », « Règles de test »]
- Material 3 dans Compose : https://developer.android.com/develop/ui/compose/designsystems/material3
- Référence API `darkColorScheme` et `MaterialTheme` : https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary

## Dev Agent Record

### Agent Model Used

GitHub Copilot

### Debug Log References

- `./gradlew.bat testDebugUnitTest --tests "com.rvetiveau.rvscorecount.ui.theme.CardSuitColorsTest"` → BUILD SUCCESSFUL.
- `./gradlew.bat testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL (aucune régression).
- `git diff --check` pour les fichiers suivis et `git diff --no-index --check -- NUL <fichier>` pour chaque fichier non suivi → aucune erreur d'espace blanc; seuls les fichiers prévus modifiés.

### Completion Notes List

- Story préparée avec analyse des epics, de l'architecture, de l'UX, du code existant, de la story précédente, de l'historique Git et de la documentation Material 3 actuelle.
- Ajout des quatre couleurs d'enseigne (`Spade`, `Heart`, `Club`, `Diamond`) dans `Color.kt`, suppression du commentaire « placeholders » devenu faux, et alignement d'`OnAccent` sur `#1A1200`.
- Création de `ui/theme/CardSuitColors.kt` exposant les propriétés d'extension `CardSuit.color` et `CardSuit.tint` via des `when` exhaustifs sans branche `else`.
- Câblage explicite de `onBackground`, `onSurface` et `onSurfaceVariant` vers `InkPrimary` dans le `darkColorScheme` de `Theme.kt`.
- Ajout de `CardSuitColorsTest.kt` (JUnit 4) verrouillant les 4 couleurs et 4 teintes exactes; `DarkColorScheme` restant `private`, ses rôles ont été vérifiés par compilation réussie et revue manuelle plutôt qu'en élargissant sa visibilité.
- Aucune nouvelle chaîne visible introduite; `strings.xml` inchangé. Aucun fichier `domain/` touché.

### File List

- app/src/main/java/com/rvetiveau/rvscorecount/ui/theme/Color.kt (UPDATE)
- app/src/main/java/com/rvetiveau/rvscorecount/ui/theme/Theme.kt (UPDATE)
- app/src/main/java/com/rvetiveau/rvscorecount/ui/theme/CardSuitColors.kt (NEW)
- app/src/test/java/com/rvetiveau/rvscorecount/ui/theme/CardSuitColorsTest.kt (NEW)
- _bmad-output/implementation-artifacts/1-4-systeme-de-design-theme-sombre-et-tokens.md (NEW)
- _bmad-output/implementation-artifacts/sprint-status.yaml (UPDATE)
- _bmad-output/implementation-artifacts/deferred-work.md (NEW)

### Change Log

- 2026-08-22 : Story créée et marquée `ready-for-dev`.
- 2026-08-22 : Implémentation complète (T1–T5) : tokens de couleur finalisés, mapping `CardSuitColors` créé, thème sombre complété, tests JVM ajoutés, build et tests validés. Statut passé à `review`.
- 2026-08-22 : Revue de code terminée : 3 correctifs de traçabilité appliqués, 1 sujet préexistant différé, statut passé à `done`.