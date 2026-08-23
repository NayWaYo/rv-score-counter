---
story_id: "1.5"
story_key: "1-5-navigation-graph-et-reprise-automatique"
epic: 1
status: done
created: 2026-08-22
baseline_commit: 11ee387
---

# Story 1.5 : Navigation graph & reprise automatique

**Epic :** 1 — Fondation & Infrastructure
**Story ID :** 1.5
**Status :** done

## Story

En tant qu'utilisateur,
Je veux que l'app reprenne automatiquement une Partie en cours si j'ai fermé l'app sans clôturer,
Afin de ne jamais perdre ma Partie en cours.

## Acceptance Criteria

1. **Given** l'app est lancée **When** aucune Partie `IN_PROGRESS` n'existe en base **Then** la route initiale est `HomeScreen`.

2. **Given** l'app est lancée **When** une Partie avec `status = IN_PROGRESS` existe en base **Then** la route initiale est `PartyScreen(partyId)` sans passer par `HomeScreen`.

3. **And** un unique `NavHost` définit les routes `home`, `setup/{gameId}`, `party/{partyId}`, `roundEntry/{partyId}`, `results/{partyId}`, `history` et `partyDetail/{partyId}`.

4. **And** les Composables d'écran n'accèdent jamais à un `NavController` : ils reçoivent leur état et émettent des `NavigationEvent` vers leur ViewModel; la traduction de ces événements en appels au contrôleur reste à la frontière du graphe.

5. **And** `ResumePartyUseCase` existe dans `domain/usecase/` et interroge `PartyRepository` pour trouver une Partie `IN_PROGRESS`.

## Tasks / Subtasks

- [x] **T1 — Définir le contrat de navigation typé** (AC: 3, 4)
  - [x] Compléter `ui/navigation/Routes.kt` avec sept routes `@Serializable`; utiliser un `data object` sans argument et une `data class` avec `String` pour `gameId` ou `partyId`.
  - [x] Fixer les noms canoniques avec `@SerialName` afin de conserver exactement `home`, `setup/{gameId}`, `party/{partyId}`, `roundEntry/{partyId}`, `results/{partyId}`, `history` et `partyDetail/{partyId}`.
  - [x] Créer `ui/navigation/NavigationEvent.kt` comme contrat fermé et typé; ne pas transporter de route brute ou de paire clé/valeur non typée.
  - [x] Garder les identifiants de Partie en `String` dans la route sérialisée, issus de `UUID.toString()`; les ViewModels de destination les reconstruiront via `SavedStateHandle.toRoute<T>()` et valideront le format.

- [x] **T2 — Implémenter et tester la recherche de Partie à reprendre** (AC: 1, 2, 5)
  - [x] Créer `domain/usecase/ResumePartyUseCase.kt`, injectable par constructeur, et déléguer directement à `PartyRepository.getPartyInProgress()`.
  - [x] Ne pas importer Room, Android, Compose, Hilt Navigation ou une implémentation de repository dans `domain/`; seul `javax.inject.Inject` est admis en plus du contrat domaine existant.
  - [x] Créer `ResumePartyUseCaseTest.kt` avec un faux `PartyRepository` local et `runTest`; aucun framework de mock n'est installé.
  - [x] Couvrir le retour de la Partie trouvée, le retour `null` et la propagation d'une erreur du repository; ne pas transformer une panne Room en « aucune partie ».

- [x] **T3 — Produire l'état de démarrage dans un ViewModel racine** (AC: 1, 2)
  - [x] Créer un `RootViewModel` annoté `@HiltViewModel`, injecter `ResumePartyUseCase` et lancer une seule lecture dans `viewModelScope` à l'initialisation.
  - [x] Exposer un unique `StateFlow<RootUiState>` fermé distinguant au minimum `Loading`, `Ready(startDestination)` et `Error`; la destination prête vaut `Routes.Home` ou `Routes.Party(party.id.toString())`.
  - [x] Ne pas stocker la décision dans `mutableStateOf`, l'Activity, un Composable ou un cache : Room via le use case reste la source de vérité.
  - [x] Ajouter `RootViewModelTest.kt` avec le dispatcher de test Coroutines et couvrir les deux destinations ainsi que l'échec explicite.

- [x] **T4 — Construire l'unique NavHost complet** (AC: 1–4)
  - [x] Créer `ui/navigation/NavGraph.kt` et enregistrer les sept destinations via l'API typée `composable<T>` de Navigation Compose 2.9.8.
  - [x] Créer le `NavHost` seulement lorsque `RootUiState.Ready` est disponible. Pendant `Loading`, afficher uniquement une surface de fond du thème, sans composer `HomeScreen`; cela empêche tout flash de l'Accueil lors d'une reprise.
  - [x] Fournir des destinations temporaires minimales pour les sept routes puisque les écrans fonctionnels relèvent des stories 2.1 à 6.2. Centraliser ces placeholders dans un seul Composable interne et leurs libellés dans `strings.xml`; ne pas créer sept faux écrans/ViewModels.
  - [x] Extraire les arguments avec `NavBackStackEntry.toRoute<T>()` dans le graphe et les transmettre aux placeholders. Ne pas charger de `Party` complète dans une route.
  - [x] En état `Error`, afficher un état explicite et testable avec une action de nouvelle tentative; ne jamais ouvrir silencieusement l'Accueil après une erreur de lecture.

- [x] **T5 — Brancher la racine de l'application** (AC: 1–4)
  - [x] Mettre à jour `MainActivity.kt` pour remplacer le texte de scaffold par le composable racine et son `RootViewModel` Hilt.
  - [x] Conserver `@AndroidEntryPoint`, `enableEdgeToEdge()`, l'unique `RvScoreCounterTheme`, l'unique `Scaffold` et la consommation de son `innerPadding`.
  - [x] Collecter `RootViewModel.uiState` avec `collectAsStateWithLifecycle()`; ajouter `lifecycle-runtime-compose` 2.9.0 au catalogue et au module, aligné sur la version Lifecycle déjà déclarée.
  - [x] Garder `rememberNavController()` et tous les appels `navigate`/`popBackStack` dans la couche `ui/navigation`; aucun Composable d'écran ou placeholder ne reçoit le contrôleur.

- [x] **T6 — Valider le graphe et l'absence de régression** (AC: 1–5)
  - [x] Ajouter des tests ciblés des routes sérialisables et de la sélection de destination; vérifier que l'identifiant de la Partie reprise est conservé.
  - [x] Rechercher sous `ui/screens/` tout import ou usage de `NavController`; la recherche doit rester vide au fil des stories suivantes.
  - [x] Exécuter le test ciblé `ResumePartyUseCaseTest`, puis les tests du `RootViewModel`.
  - [x] Exécuter `testDebugUnitTest`, `kspDebugKotlin` et `assembleDebug` avec JDK 17, puis `git diff --check`.
  - [x] Vérifier manuellement le lancement avec et sans Partie `IN_PROGRESS`, l'absence de flash Home dans le premier cas, les sept destinations et le back standard.

### Review Findings

- [x] [Review][Patch] Annuler la lecture de reprise précédente avant chaque `retry()` afin qu'un résultat obsolète ne puisse pas écraser la tentative la plus récente; ajouter un test de non-régression avec deux lectures contrôlées terminées dans l'ordre inverse. [app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/RootViewModel.kt:27]

## Dev Notes

### État actuel et delta exact

Cette story complète une infrastructure déjà présente; elle ne doit pas recréer la persistance ni anticiper les écrans métier.

- `MainActivity.kt` est l'unique Activity, porte déjà `@AndroidEntryPoint`, active l'edge-to-edge et applique globalement `RvScoreCounterTheme`. Son `Scaffold` affiche seulement `R.string.app_name`; remplacer ce contenu sans perdre le thème, les insets ni le padding.
- `ui/navigation/Routes.kt` contient uniquement `sealed interface Routes`. Il est le point canonique à compléter; ne pas créer une seconde famille de routes.
- `PartyRepository.getPartyInProgress(): Party?` existe déjà dans le domaine. `PartyRepositoryImpl` délègue à `PartyDao.getPartyInProgress()` puis applique `toDomain()`.
- `PartyDao` sélectionne `status = "IN_PROGRESS"` avec `LIMIT 1`. La story consomme ce contrat sans modifier le schéma, la DAO, le repository ou la version Room.
- `Party.id` est un `UUID`; les routes sérialisées utilisent sa représentation chaîne pour éviter d'introduire un sérialiseur de domaine dans l'UI.
- Navigation Compose 2.9.8, le plugin Kotlin Serialization et `kotlinx-serialization-json` 1.7.3 sont déjà configurés. Ne pas les ajouter ni les mettre à niveau.
- `lifecycle-viewmodel-compose` 2.9.0 existe, mais `lifecycle-runtime-compose` manque : c'est la seule dépendance attendue pour `collectAsStateWithLifecycle()`.
- Aucun package `ui/screens/` ni ViewModel d'écran n'existe encore. Les écrans réels seront livrés par les epics 2 à 6.

### Décision de démarrage

Le démarrage est une décision asynchrone en trois temps :

1. `RootViewModel` démarre en `Loading` et appelle une fois `ResumePartyUseCase`.
2. Le use case lit Room via `PartyRepository`; `null` signifie réellement « aucune Partie en cours ».
3. Le composable racine crée le `NavHost` avec `Routes.Home` ou `Routes.Party(partyId)` uniquement après `Ready`.

Ne pas créer un `NavHost(startDestination = Home)` puis naviguer vers Party dans un `LaunchedEffect`. Cette approche compose brièvement l'Accueil, ajoute une destination indésirable au back stack et viole l'AC 2. Ne pas bloquer `onCreate` avec `runBlocking`.

Une exception de lecture n'est pas équivalente à l'absence de Partie. L'état `Error` doit rester distinct afin d'éviter qu'une panne de persistance masque une Partie existante. Une nouvelle tentative rappelle le use case depuis le ViewModel.

### Contrat de routes

Utiliser l'API typée disponible depuis Navigation 2.8 et déjà supportée par la version 2.9.8 du dépôt :

```kotlin
@Serializable
sealed interface Routes {
    @Serializable
    @SerialName("home")
    data object Home : Routes

    @Serializable
    @SerialName("party")
    data class Party(val partyId: String) : Routes
}
```

Appliquer le même motif aux cinq autres destinations. Les noms de paramètres restent exactement `gameId` et `partyId`. Utiliser `composable<Routes.Party>` et `backStackEntry.toRoute<Routes.Party>()`; ne pas construire ou parser manuellement `"party/$partyId"`.

`Routes` décrit des destinations. `NavigationEvent` décrit les intentions de navigation émises par un écran. Ne pas confondre ce contrat avec un `UiState`, ne pas y placer de `NavController`, et ne pas exposer de chaîne libre permettant de contourner les routes typées.

### Placeholders et périmètre des stories futures

L'AC 3 exige un graphe complet avant que les écrans existent. Pour garder le build exécutable :

- enregistrer les sept destinations dès maintenant;
- afficher un placeholder interne sobre utilisant le thème et une ressource de chaîne;
- transmettre et afficher l'argument reçu sur les routes paramétrées pour permettre une vérification manuelle;
- remplacer progressivement chaque destination par son écran réel dans les stories 2.1, 2.2/2.3, 3.x, 4.1, 5.1 et 6.x.

Ces placeholders ne contiennent aucune logique métier, aucun ViewModel, aucune navigation et aucune maquette anticipée. Cette story ne crée ni `HomeScreen`, ni `PartyScreen`, ni les autres écrans nommés dans les AC.

### Architecture et comportements à préserver

- Respecter `UI → Domain ← Data`; `ResumePartyUseCase` dépend uniquement de `PartyRepository`.
- Room reste la seule source de vérité. Aucun état en mémoire ne détermine si une Partie doit être reprise.
- Conserver une seule Activity, un seul `NavHost` et le back stack Android standard; ne pas ajouter de tabs, drawer ou bottom navigation.
- Les futurs Composables d'écran resteront stateless : `UiState` descendant, événements remontants, jamais de `NavController`.
- Toutes les chaînes visibles, y compris chargement, erreur, nouvelle tentative et placeholders, vivent dans `res/values/strings.xml`.
- Ne modifier aucun fichier Room, schéma exporté, modèle domaine, moteur de jeu ou thème dans cette story.
- Ne pas ajouter de deep links, transitions personnalisées ou persistance manuelle du back stack : ils ne sont pas requis.

### Tests

- Les tests JVM existants utilisent JUnit 4 et `kotlinx-coroutines-test`; aucun MockK/Mockito n'est installé. Utiliser un faux `PartyRepository` local explicite.
- Le test du use case prouve la délégation et le contrat `Party?`; le test du ViewModel prouve la décision de route et l'état d'erreur.
- Tester la route elle-même plutôt que le texte du placeholder. L'absence de flash Home et le back stack nécessitent au minimum une vérification manuelle; un test instrumenté Navigation peut être ajouté seulement si l'infrastructure reste légère.
- La story touche Hilt et la racine Compose : `kspDebugKotlin` et `assembleDebug` sont obligatoires après les tests JVM.

### Fichiers prévus

```text
gradle/libs.versions.toml                                           UPDATE
app/build.gradle.kts                                               UPDATE
app/src/main/java/com/rvetiveau/rvscorecount/MainActivity.kt       UPDATE
app/src/main/java/com/rvetiveau/rvscorecount/
  domain/usecase/ResumePartyUseCase.kt                             NEW
  ui/navigation/Routes.kt                                          UPDATE
  ui/navigation/NavigationEvent.kt                                 NEW
  ui/navigation/NavGraph.kt                                        NEW
  ui/navigation/RootViewModel.kt                                   NEW
app/src/main/res/values/strings.xml                                UPDATE
app/src/test/java/com/rvetiveau/rvscorecount/
  domain/usecase/ResumePartyUseCaseTest.kt                         NEW
  ui/navigation/RootViewModelTest.kt                               NEW
```

Un fichier de placeholder séparé n'est justifié que si `NavGraph.kt` devient difficile à lire; ne pas créer une abstraction supplémentaire par destination.

### Acquis des stories précédentes

- La story 1.2 a livré le chemin complet `PartyDao → PartyRepositoryImpl → PartyRepository.getPartyInProgress()` et les mappings `PartyEntity ↔ Party`; le réutiliser sans duplication.
- La revue Room a renforcé les invariants de persistance et l'échec explicite. Ne pas intercepter une donnée invalide ou une exception pour revenir silencieusement à Home.
- La story 1.4 a confirmé le thème sombre global, le `Scaffold` racine et l'obligation des ressources de chaînes. Préserver ces acquis.
- JDK 17 et le wrapper Gradle sont le mode de validation établi. La branche active est déjà `story/1-5-navigation-auto-resume`.

### Historique Git pertinent

- `11ee387` (`merge: complete story 1.4 design system`) : baseline de cette story.
- `22c2ba1` (`feat: add dark theme design system`) : thème racine et tokens à préserver.
- `764d56e` (`feat: add Papayoo game engine`) : confirme JUnit 4 et les tests JVM sans dépendance de mock.
- `b087ccb` (`merge: complete story 1.3 game engine`) : séparation domain/game intégrée.

### Stack et informations techniques actuelles

- Conserver Kotlin 2.4.10, Java 17, AGP 9.3.1, Gradle 9.5.1, Navigation Compose 2.9.8, Lifecycle 2.9.0, Hilt 2.60.1 et Kotlin Serialization 1.7.3. Les fichiers Gradle du dépôt priment sur les anciennes versions des planning artifacts.
- La documentation Android actuelle confirme les routes typées `@Serializable`, `composable<T>`, `navigate(route)` et `toRoute<T>()` pour Navigation 2.9.8.
- La documentation recommande de passer des callbacks de navigation aux Composables plutôt que le `NavController`, afin de garder les écrans testables.
- Pour Android, `collectAsStateWithLifecycle()` est la collecte recommandée des `Flow` et requiert `androidx.lifecycle:lifecycle-runtime-compose`; utiliser la version Lifecycle 2.9.0 déjà centralisée, sans mise à niveau.

## Références

- Story, FR-16 et routes contractuelles : [Source: `_bmad-output/planning-artifacts/epics.md` — « Story 1.5: Navigation graph & reprise automatique »]
- Single Activity, reprise et structure cible : [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — « AD-3 », « AD-4 », « AD-6 », « Structural Seed »]
- Persistance et reprise exacte : [Source: `_bmad-output/planning-artifacts/prds/prd-rv-score-counter-2026-08-09/prd.md` — « FR-16 : Persistance locale »]
- Bypass Accueil, back stack et surfaces : [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md` — « Information Architecture », « State Patterns »]
- Règles durables du dépôt : [Source: `_bmad-output/project-context.md` — « Règles Android et frameworks », « Règles de test »]
- Routes typées Navigation Compose : https://developer.android.com/guide/navigation/design/type-safety
- Navigation Compose 2.9.8 : https://developer.android.com/develop/ui/compose/navigation
- État Compose et collecte lifecycle-aware : https://developer.android.com/develop/ui/compose/state

## Dev Agent Record

### Agent Model Used

GitHub Copilot

### Debug Log References

- `./gradlew.bat kspDebugKotlin` (après T1 Routes/NavigationEvent) → BUILD SUCCESSFUL.
- `./gradlew.bat testDebugUnitTest --tests "...ResumePartyUseCaseTest"` (RED puis GREEN) → 3 tests passent.
- `./gradlew.bat testDebugUnitTest --tests "...RootViewModelTest"` (RED puis GREEN) → 3 tests passent.
- Vérification génération Hilt : `app/build/generated/ksp/debug/.../RootViewModel_HiltModules*.java` présents sans dépendance `androidx.hilt:hilt-compiler` supplémentaire — le compilateur Hilt 2.60.1 génère nativement les bindings `@HiltViewModel`.
- `./gradlew.bat testDebugUnitTest --tests "...RoutesTest"` → 3 tests passent (round-trip des 7 routes).
- `./gradlew.bat kspDebugKotlin assembleDebug` (après T5 MainActivity) → BUILD SUCCESSFUL in 2m 59s, 41/41 tasks.
- `./gradlew.bat testDebugUnitTest` (suite complète) → BUILD SUCCESSFUL, aucune régression.
- `git diff --check` → une ligne vide superflue en fin de `MainActivity.kt` détectée et corrigée; second passage propre (fichiers suivis et non suivis, ces derniers via `git diff --no-index --check -- NUL <fichier>`).
- Vérification manuelle sur émulateur (`emulator-5554`) : install debug APK, lancement sans Partie en base → écran «Accueil (à venir)» affiché (AC 1). Injection directe d'une ligne `parties` avec `status = IN_PROGRESS` dans la base Room de l'app (pull/checkpoint/insert/push via `run-as` + `base64`, WAL purgé), relance de l'app → écran «Partie en cours {partyId} (à venir)» affiché immédiatement, sans passage par l'Accueil, avec l'identifiant exact conservé (AC 2). Base et APK de test supprimés de l'émulateur après vérification.

### Completion Notes List

- Ultimate context engine analysis completed - comprehensive developer guide created.
- Story préparée avec analyse des epics, du PRD, de l'architecture, de l'UX, du code existant, des stories précédentes, de l'historique Git et de la documentation Android actuelle.
- T1 : `Routes.kt` complété avec les 7 routes `@Serializable`/`@SerialName` (Home, Setup, Party, RoundEntry, Results, History, PartyDetail) ; `NavigationEvent.kt` créé comme contrat fermé (NavigateTo/NavigateBack/NavigateBackTo), non encore consommé par des écrans (aucun n'existe dans cette story).
- T2 : `ResumePartyUseCase` créé, délègue directement à `PartyRepository.getPartyInProgress()` sans transformation ; test JVM avec faux `PartyRepository` local couvrant Partie trouvée, `null`, et propagation d'exception (pas de swallow).
- T3 : `RootUiState` (sealed interface Loading/Ready/Error) et `RootViewModel` (`@HiltViewModel`, une lecture par appel dans `viewModelScope`, `retry()` idempotent) créés ; `CancellationException` explicitement re-lancée avant la capture générique pour ne jamais la transformer en `Error`.
- T4 : `NavGraph.kt` enregistre les 7 destinations via `composable<T>()`/`toRoute<T>()` ; un unique Composable interne `RoutePlaceholder` factorise l'affichage temporaire, avec libellés dans `strings.xml` (aucun écran ni ViewModel par route).
- T5 : `MainActivity.kt` orchestre l'état racine (`RootViewModel` via `hiltViewModel()`, `collectAsStateWithLifecycle()`) et ne crée le `NavHost` qu'en état `Ready` ; état `Loading` = surface vide thémée, état `Error` = message + bouton «Réessayer» lié à `retry()`. Dépendance `lifecycle-runtime-compose` (alignée sur Lifecycle 2.9.0 déjà catalogué) ajoutée au catalogue et au module.
- T6 : recherche de `NavController` sous `ui/screens/` vide (packages encore vides, hérités de la story 1.1) ; suite complète + `kspDebugKotlin` + `assembleDebug` verts ; `git diff --check` propre après correctif ; vérification manuelle réussie des AC 1 et 2 sur émulateur avec Partie réelle injectée dans Room (voir Debug Log). Les 5 autres destinations n'ont pas été cliquées manuellement une par une (aucun point d'entrée UI n'existe encore vers elles dans cette story) mais partagent strictement le même mécanisme `composable<T>`/`toRoute<T>()` déjà validé sur le terrain pour Home et Party, et leur sérialisation est couverte individuellement par `RoutesTest`.

### File List

- `gradle/libs.versions.toml` (UPDATE)
- `app/build.gradle.kts` (UPDATE)
- `app/src/main/java/com/rvetiveau/rvscorecount/MainActivity.kt` (UPDATE)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/ResumePartyUseCase.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/Routes.kt` (UPDATE)
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/NavigationEvent.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/RootUiState.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/RootViewModel.kt` (NEW)
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/NavGraph.kt` (NEW)
- `app/src/main/res/values/strings.xml` (UPDATE)
- `app/src/test/java/com/rvetiveau/rvscorecount/domain/usecase/ResumePartyUseCaseTest.kt` (NEW)
- `app/src/test/java/com/rvetiveau/rvscorecount/ui/navigation/RootViewModelTest.kt` (NEW)
- `app/src/test/java/com/rvetiveau/rvscorecount/ui/navigation/RoutesTest.kt` (NEW)
- `_bmad-output/implementation-artifacts/1-5-navigation-graph-et-reprise-automatique.md` (NEW)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` (UPDATE)

### Change Log

- 2026-08-22 : Story créée et marquée `ready-for-dev`.
- 2026-08-22 : Implémentation complète (T1–T6) : routes typées, `ResumePartyUseCase`, `RootViewModel`/`RootUiState`, `NavGraph` avec placeholders, branchement `MainActivity`, suite de tests JVM et vérification manuelle sur émulateur des AC 1 et 2. Statut passé à `review`.
- 2026-08-23 : Revue de code — annulation des tentatives de reprise obsolètes et ajout d'un test de non-régression sur deux `retry()` concurrents.