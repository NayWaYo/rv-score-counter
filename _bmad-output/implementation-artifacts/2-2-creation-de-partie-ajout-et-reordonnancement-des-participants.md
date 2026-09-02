---
story_id: "2.2"
story_key: "2-2-creation-de-partie-ajout-et-reordonnancement-des-participants"
epic: 2
status: done
created: 2026-08-23
baseline_commit: 06ed7b8
---

# Story 2.2 : Création de Partie — ajout et réordonnancement des Participants

**Epic :** 2 — Création de Partie
**Story ID :** 2.2
**Status :** done

## Story

En tant qu'utilisateur,
Je veux ajouter mes amis et les réordonner pour refléter leur position autour de la table,
Afin que le premier de la liste soit le premier Donneur et que l'ordre de distribution suive la réalité physique.

## Acceptance Criteria

1. **Given** l'écran Création de Partie affiché **When** l'utilisateur saisit un prénom et tape « Ajouter » **Then** le Participant apparaît dans la liste ordonnée **And** un prénom vide ou uniquement composé d'espaces est refusé avec un message d'erreur inline.

2. **Given** la liste des Participants affichée **When** l'utilisateur fait un long-press sur la poignée de drag d'une ligne **Then** il peut réordonner la liste par glisser-déposer **And** le premier de la liste est visuellement identifié comme futur Donneur de la Manche 1.

3. **Given** la liste des Participants **When** leur nombre est inférieur au minimum du Jeu, soit 3 pour Papayoo **Then** le bouton « Commencer » est désactivé avec un compteur visible **And** il devient actif dès que le minimum est atteint.

4. **Given** la liste atteint le maximum du Jeu, soit 8 Participants pour Papayoo **When** l'écran est affiché **Then** le champ et l'action d'ajout sont désactivés **And** le message « Maximum 8 joueurs atteint » est visible **And** aucun neuvième Participant ne peut être ajouté.

5. **Given** un Participant présent avant confirmation **When** l'utilisateur actionne sa suppression **Then** ce Participant est retiré **And** les autres conservent leur ordre relatif **And** l'identification du futur Donneur et l'état du bouton « Commencer » sont recalculés.

6. **And** l'écran consomme les contraintes `minPlayers` et `maxPlayers` du `GameEngine` correspondant au `gameId` de `Routes.Setup`, sans valeur Papayoo codée en dur dans l'UI, et échoue explicitement si le Jeu demandé n'est pas enregistré.

7. **And** les contrôles interactifs ont une cible d'au moins `48dp`, les lignes ont une hauteur minimale de `56dp`, les informations restent lisibles avec Dynamic Type, la poignée et la suppression exposent des sémantiques TalkBack explicites, et le badge du premier Participant annonce « Donneur ».

8. **And** toutes les chaînes visibles proviennent de `res/values/strings.xml`, l'écran utilise les tokens Material du thème existant et les couleurs d'enseignes ne servent pas de décoration générale.

9. **And** cette story ne crée ni Partie ni Participant en Room : la création atomique, la persistance des `listPosition`, le passage `SETUP` vers `IN_PROGRESS` et la navigation vers `Routes.Party` appartiennent à la Story 2.3.

## Tasks / Subtasks

- [x] **T1 — Modéliser le brouillon ordonné et ses règles UI** (AC: 1–6, 9)
  - [x] Créer `ui/screens/setup/SetupUiState.kt` avec un état immuable contenant le `gameId`, les contraintes du Jeu, le texte saisi, une liste ordonnée de brouillons à identifiant stable, l'erreur de saisie éventuelle et les propriétés dérivées nécessaires à l'affichage.
  - [x] Représenter un Participant de préparation par un modèle UI dédié (`id`, `name`) : ne pas instancier prématurément `domain.model.Participant`, qui exige un `partyId` et une `listPosition` persistables appartenant à la Story 2.3.
  - [x] Déduire `canStart` de `participants.size >= minPlayers` et `canAdd` de `participants.size < maxPlayers`; l'ordre de la liste est la source du futur `listPosition`, sans stocker un second index susceptible de diverger.
  - [x] Utiliser un état d'erreur typé ou une ressource UI, jamais un message français dans le domaine. Accepter les prénoms dupliqués, hors périmètre du PRD, et normaliser un prénom valide avec `trim()`.

- [x] **T2 — Centraliser les intentions dans `SetupViewModel`** (AC: 1–6, 9)
  - [x] Créer un `SetupViewModel` `@HiltViewModel` exposant un unique `StateFlow<SetupUiState>`; fournir une initialisation idempotente par `gameId`, appelée par `SetupRoute` depuis la destination typée, afin qu'une recomposition ne réinitialise jamais le brouillon.
  - [x] Réutiliser le registre existant via `GetAvailableGamesUseCase`; sélectionner le moteur dont `gameId` correspond exactement à la route et signaler explicitement une incohérence ou un identifiant inconnu.
  - [x] Implémenter les intentions de modification du prénom, ajout, suppression et déplacement. Refuser dans le ViewModel les noms blancs, les indices de déplacement invalides et tout ajout à la limite maximale, même si l'UI désactive déjà les contrôles.
  - [x] Préserver l'identifiant stable lors d'un déplacement, conserver l'ordre relatif lors d'une suppression et effacer le champ/l'erreur après un ajout valide.
  - [x] Ne pas injecter `PartyRepository`, DAO, `PapayooEngine` concret ou `NavController`; ne pas créer `CreatePartyUseCase` / `AddParticipantUseCase`, explicitement attendus par la Story 2.3.

- [x] **T3 — Construire l'écran Setup stateless** (AC: 1–5, 7, 8)
  - [x] Créer `SetupScreen.kt` comme Composable pur recevant `SetupUiState` et des callbacks pour le texte, l'ajout, la suppression, le réordonnancement et « Commencer »; aucun accès Hilt, repository, moteur concret, `NavController` ou état durable local.
  - [x] Afficher le champ « Prénom du joueur », l'action « Ajouter », l'erreur inline, le compteur visible, la liste ordonnée et le bouton « Commencer » fixé en bas en consommant correctement le padding/insets existant.
  - [x] Afficher sur chaque ligne un nom non tronqué, une poignée de réordonnancement et une action de suppression familière; identifier en permanence l'élément d'index 0 avec un badge accent « Donneur M1 ».
  - [x] Implémenter le geste sur la poignée avec `Modifier.pointerInput` et `detectDragGesturesAfterLongPress`, déjà fournis par Compose Foundation. Ne pas ajouter de bibliothèque de réordonnancement ni de dépendance Material Icons Extended pour cette seule interaction.
  - [x] Utiliser des clés stables dans la `LazyColumn`; rendre le déplacement robuste vers le haut et le bas sans dupliquer, perdre ou recréer les éléments.
  - [x] Garder l'action « Commencer » injectable et activée selon l'état, mais ne pas lui attribuer de persistance ou de navigation dans cette story; son comportement de confirmation sera branché en 2.3.

- [x] **T4 — Brancher la destination Setup au graphe existant** (AC: 1, 6, 9)
  - [x] Créer `SetupRoute.kt` comme adaptateur Compose/Hilt : appeler l'initialisation idempotente dans un `LaunchedEffect(gameId)`, collecter l'état avec `collectAsStateWithLifecycle()`, transmettre les intentions et relayer le `gameId` de `Routes.Setup` sans chaîne de route parallèle.
  - [x] Dans `NavGraph.kt`, remplacer uniquement le `RoutePlaceholder` de `Routes.Setup` par `SetupRoute`; conserver l'unique `NavHost`, `toRoute<Routes.Setup>()`, le dispatcher de navigation et tous les autres placeholders.
  - [x] Préserver `HomeRoute`, `RootViewModel`, la reprise automatique et `launchSingleTop`; le retour système depuis Setup revient à l'Accueil et abandonne simplement le brouillon non persisté, sans créer de ligne Room orpheline ni afficher de confirmation non demandée.

- [x] **T5 — Ajouter les ressources françaises et les sémantiques** (AC: 1–5, 7, 8)
  - [x] Ajouter à `strings.xml` le titre de création, le libellé/hint du prénom, « Ajouter », l'erreur de prénom vide, le compteur paramétré, le minimum requis, « Maximum 8 joueurs atteint », « Commencer », « Donneur M1 » et les descriptions de réordonnancement/suppression.
  - [x] Retirer `route_placeholder_setup` seulement lorsqu'il n'est plus référencé; préserver les chaînes des destinations encore en placeholder.
  - [x] Utiliser un `VectorDrawable` local pour les icônes absentes du projet, ou une primitive Material déjà disponible, sans ajouter de dépendance de production.
  - [x] Exposer des tags/sémantiques stables pour tester champ, erreur, compteur, liste, poignées, suppression, badge et bouton principal sans dépendre de coordonnées fragiles.

- [x] **T6 — Couvrir les règles, interactions et régressions** (AC: 1–9)
  - [x] Ajouter des tests JVM JUnit 4 de `SetupViewModel` couvrant initialisation depuis le moteur, Jeu inconnu, ajout nominal, `trim`, nom vide/espaces, doublons acceptés, limite maximale, suppression et déplacements valides/invalides.
  - [x] Vérifier dans les tests que `canStart` bascule exactement au minimum fourni par un faux moteur, que `canAdd` bascule au maximum fourni et que le futur Donneur suit toujours l'index 0.
  - [x] Ajouter des tests Compose instrumentés de `SetupScreen` pour l'erreur inline, les états des contrôles à 0/2/3/8 Participants, le compteur, le badge Donneur, la suppression, le réordonnancement, les rôles/descriptions, les cibles tactiles et Dynamic Type.
  - [x] Réutiliser l'infrastructure Compose UI Test introduite en 2.1; ne modifier ni `libs.versions.toml` ni `app/build.gradle.kts` sauf manque réellement constaté.
  - [x] Ajouter ou adapter un test ciblé de `NavGraph`/route si nécessaire pour prouver que `Routes.Setup(gameId)` transmet l'identifiant exact et que les autres destinations restent enregistrées.
  - [x] Exécuter d'abord les tests JVM ciblés, puis `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `kspDebugKotlin`, `assembleDebug`, les tests instrumentés sur un émulateur stable et `git diff --check` avec JDK 17.

### Review Findings

- [x] [Review][Patch] Actualiser l'index lu par le `pointerInput` afin qu'un second drag ne réutilise pas un index capturé obsolète [app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupScreen.kt:201]
- [x] [Review][Patch] Calculer les déplacements de drag depuis les positions réelles des lignes plutôt qu'avec une hauteur fixe de `56dp` qui ignore l'espacement et Dynamic Type [app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupScreen.kt:185]
- [x] [Review][Patch] Effacer le message de capacité maximale après une suppression qui réautorise l'ajout [app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupViewModel.kt:62]
- [x] [Review][Patch] Faire appliquer le déplacement au test Compose et vérifier l'ordre final rendu, y compris lors d'un second drag [app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupScreenTest.kt:209]
- [x] [Review][Patch] Préserver une cible tactile réelle d'au moins `48dp` pour le bouton « Commencer » en plaçant son padding à l'extérieur de sa hauteur minimale [app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupScreen.kt:122]
- [x] [Review][Patch] Vérifier séparément le texte visible et la description TalkBack du badge Donneur selon l'arbre sémantique réel [app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupScreenTest.kt:126]

## Dev Notes

### Contexte d'implémentation

La Story 2.1 livre déjà le chemin `HomeScreen → NavigationEvent.NavigateTo(Routes.Setup(gameId)) → NavGraph`. La destination Setup désérialise actuellement `Routes.Setup` puis affiche un placeholder. Cette story remplace seulement ce placeholder par le flux suivant :

```text
Routes.Setup(gameId)
  → SetupRoute / SetupViewModel
  → GetAvailableGamesUseCase → GameEngine(minPlayers, maxPlayers)
  → SetupUiState (brouillon ordonné en mémoire)
  → SetupScreen
  → intentions add / remove / reorder vers SetupViewModel
```

La frontière avec la Story 2.3 est contractuelle. Les AC de 2.3 disent qu'au tap « Commencer », une Partie est créée en Room avec ses Participants et leurs `listPosition`, puis passe de `SETUP` à `IN_PROGRESS` avant navigation. La Story 2.3 prolongera le même `SetupViewModel` : son action de confirmation lira directement la liste ordonnée du `SetupUiState`, la transmettra aux use cases de création, puis émettra la navigation. Il n'existe donc ni handoff entre deux écrans, ni paramètres de route contenant les Participants, ni ViewModel partagé entre destinations. En conséquence, 2.2 ne doit pas créer une Partie `SETUP` à l'entrée de l'écran, étendre les DAO/repositories pour réordonner un brouillon, ni modifier le schéma Room. Cela évite les Parties abandonnées lorsque l'utilisateur revient en arrière et respecte le découpage explicite des stories.

### État actuel des fichiers à modifier

- `ui/navigation/NavGraph.kt` possède l'unique `NavHostController`. La branche `Routes.Setup` appelle déjà `toRoute()` et affiche `route_placeholder_setup`; remplacer uniquement son corps et préserver la fonction privée `dispatch` ainsi que `launchSingleTop`.
- `ui/screens/home/HomeRoute.kt` établit le motif local à suivre : adaptateur Hilt, `collectAsStateWithLifecycle()`, écran pur et frontière de navigation explicite.
- `domain/usecase/GetAvailableGamesUseCase.kt` expose déjà les `GameEngine` enregistrés dans un ordre déterministe et vérifie la cohérence clé/`gameId`; le réutiliser au lieu d'injecter une seconde Map ou de coder Papayoo dans Setup.
- `domain/engine/GameEngine.kt` expose `minPlayers` et `maxPlayers`; ces propriétés gouvernent les seuils. Les valeurs 3 et 8 ne servent que d'attendus Papayoo dans les tests/ressources, pas de constantes UI.
- `domain/model/Participant.kt` exige `id`, `partyId`, `name` et `listPosition`. Il représente une donnée de Partie persistable, pas une ligne de brouillon avant confirmation.
- `PartyRepository` et `PartyRepositoryImpl` ne possèdent volontairement aucun contrat de suppression/réordonnancement de brouillon. Ne pas élargir cette surface pour 2.2.
- `strings.xml` contient encore `route_placeholder_setup`; retirer uniquement cette ressource devenue inutilisée et conserver les autres placeholders.
- Les dépendances Compose UI Test et le runner AndroidX ont déjà été ajoutés par 2.1; aucune dépendance nouvelle n'est attendue.

### Architecture et garde-fous

- Respecter `UI → Domain ← Data` et `Game → Domain`; aucune règle d'écran dans `domain/`, aucun import `game.papayoo` dans l'UI.
- Le ViewModel est l'unique propriétaire du brouillon et expose un seul `StateFlow`; un état gestuel éphémère strictement nécessaire au drag peut rester local au Composable, mais la liste ordonnée finale doit toujours revenir au ViewModel.
- Une mutation de liste doit être atomique du point de vue de l'état : produire une nouvelle liste immuable, sans modifier en place la liste précédente.
- Ne pas inventer un registre de Jeux, un repository de joueurs, un profil réutilisable, une validation d'unicité des prénoms, un dialogue de confirmation ou une sauvegarde de brouillon.
- L'AD-3 impose Room comme source de vérité d'une Partie existante. Ici aucune Partie n'existe encore selon les AC de 2.3; dès sa création dans la story suivante, toutes ses mutations devront être persistées avant retour au ViewModel.
- Les versions effectives viennent des fichiers Gradle : Kotlin 2.4.10, Java 17, AGP 9.3.1, Compose BOM 2026.06.01, Navigation Compose 2.9.8, Hilt 2.60.1, Lifecycle 2.9.0 et Coroutines 1.11.0. Aucune mise à niveau n'est requise.

### Structure de fichiers prévue

```text
app/src/main/java/com/rvetiveau/rvscorecount/
  ui/navigation/NavGraph.kt                                      UPDATE
  ui/screens/setup/SetupUiState.kt                              NEW
  ui/screens/setup/SetupViewModel.kt                            NEW
  ui/screens/setup/SetupScreen.kt                               NEW
  ui/screens/setup/SetupRoute.kt                                NEW
app/src/main/res/
  drawable/ic_drag_handle.xml                                   NEW si requis
  drawable/ic_delete.xml                                        NEW si requis
  values/strings.xml                                             UPDATE
app/src/test/java/com/rvetiveau/rvscorecount/
  ui/screens/setup/SetupViewModelTest.kt                        NEW
app/src/androidTest/java/com/rvetiveau/rvscorecount/
  ui/screens/setup/SetupScreenTest.kt                           NEW
```

Garder les éléments de ligne privés ou colocalisés avec `SetupScreen` tant qu'aucune seconde surface ne les réutilise; ne pas créer prématurément un package de composants génériques.

### Acquis de la Story 2.1 et historique Git

- Le commit `06ed7b8` (`merge: complete story 2.1 home game grid`) est la baseline actuelle de la Story 2.2; il contient `HomeUiState` / `HomeViewModel` / `HomeRoute` / `HomeScreen`, le catalogue de Jeux et le branchement de `Routes.Setup`.
- La navigation one-shot utilise `Channel<NavigationEvent>(Channel.BUFFERED)` et `HomeRoute` emploie `LaunchedEffect` avec `rememberUpdatedState`; ne créer un flux de navigation Setup que lorsque 2.3 fournit une destination réelle.
- La revue 2.1 a ajouté `launchSingleTop` pour éviter l'empilement de destinations et renforcé le test Dynamic Type avec `TextLayoutResult.hasVisualOverflow`; préserver ces protections.
- Les tests instrumentés de 2.1 passent par exécution directe sur l'émulateur, mais Gradle/UTP a rencontré un appareil `offline`. Refaire la validation 2.2 sur un émulateur stable et distinguer un défaut de test d'une indisponibilité d'environnement.
- La rétrospective de l'Epic 1 impose un préflight de compatibilité lorsqu'une story modifie la stack et la boucle implémentation → revue indépendante → revalidation ciblée. Cette story ne doit normalement pas modifier la stack.

### Tests et définition de fini

- Les tests JVM existants utilisent JUnit 4, `kotlinx-coroutines-test` et des fakes locaux, sans MockK/Mockito.
- Tester avec un faux `GameEngine` dont les limites diffèrent de 3/8 prouve que l'UI ne code pas les contraintes Papayoo en dur.
- Un simple appel direct au callback ne prouve pas le drag : le test Compose doit déclencher le geste depuis la poignée et vérifier l'ordre sémantique final. Si l'API de test rend le geste instable, isoler et tester exhaustivement la transformation d'indices au niveau ViewModel, puis conserver au moins un test instrumenté du déclenchement de la poignée.
- Vérifier les déplacements adjacents et extrêmes dans les deux directions, ainsi que la suppression de l'index 0 et le transfert du badge Donneur.
- La story est prête pour revue seulement après compilation, tests ciblés, inspection des diagnostics et validation des régressions Home/navigation.

### Références

- Story et critères : [Source: `_bmad-output/planning-artifacts/epics.md` — « Story 2.2: Création de Partie — ajout et réordonnancement des Participants »]
- Frontière de confirmation/persistance : [Source: `_bmad-output/planning-artifacts/epics.md` — « Story 2.3: Confirmation de Partie & initialisation de l'état »]
- FR-2 et hors périmètre joueurs persistants : [Source: `_bmad-output/planning-artifacts/prds/prd-rv-score-counter-2026-08-09/prd.md` — « FR-2 : Ajout et gestion des Participants »]
- Registre moteur, état UI et navigation : [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — « AD-2 », « AD-4 », « AD-6 », « Capability → Architecture Map »]
- Source de vérité et mutations autorisées : [Source: `_bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md` — « AD-3 », « AD-5 »]
- Écran, drag, états et accessibilité : [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md` — « Création de Partie », « Drag-to-reorder », « State Patterns », « Accessibility Floor »]
- Tokens et dimensions : [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/DESIGN.md` — « Colors », « Typography », « Layout & Spacing », « Buttons »]
- Continuité d'implémentation : [Source: `_bmad-output/implementation-artifacts/2-1-ecran-d-accueil-grille-de-tuiles-jeux.md` — « Contexte d'implémentation », « Review Findings », « Completion Notes List »]
- Règles durables : [Source: `_bmad-output/project-context.md` — « Règles Android et frameworks », « Règles de test », « Points critiques à ne jamais manquer »]

## Dev Agent Record

### Agent Model Used

GitHub Copilot

### Debug Log References

- `testDebugUnitTest` (ciblé `SetupViewModelTest`, puis suite complète) : PASS — 14/14 tests.
- `compileDebugKotlin`, `kspDebugKotlin`, `compileDebugAndroidTestKotlin`, `assembleDebug` : PASS, sans avertissement nouveau.
- `connectedDebugAndroidTest` via Gradle/UTP sur `emulator-5554` : bloqué indéfiniment (aucune sortie, aucun crash) — même symptôme que la limitation déjà documentée en Story 2.1.
- Exécution directe `adb shell am instrument -w -r -e class ... SetupScreenTest` (et par méthode isolée) : chaque tentative reste bloquée sur le premier test sélectionné par le runner, avec le processus de l'app à 70–93 % CPU (`adb shell top`) sans FATAL EXCEPTION ni crash — donc un blocage actif, pas un deadlock silencieux.
- Diagnostic mené par élimination : suppression temporaire de la mesure `onGloballyPositioned` (hypothèse d'écriture d'état en phase de layout) puis passage de `createComposeRule()` (v1, `UnconfinedTestDispatcher`) à `androidx.compose.ui.test.junit4.v2.createComposeRule()` (`StandardTestDispatcher`, recommandé par l'avertissement de dépréciation) : aucune de ces deux pistes n'a résolu le blocage. Un test *sans aucun geste de drag* (juste l'affichage du badge Donneur) bloque également à l'identique.
- Preuve déterminante : l'historique logcat montre que `HomeScreenTest` (Story 2.1, sans geste pointerInput) a réussi 3 fois de suite (`run finished: 7 tests, 0 failed`) puis, à la 4ᵉ tentative sur ce même émulateur, s'est lui aussi bloqué sur son tout premier test sans jamais finir — avant même que `SetupScreenTest` soit exécuté. L'émulateur est ensuite devenu instable au point que `adb devices`, `adb shell getprop` et `adb emu kill` ne répondaient plus. Ceci confirme une dégradation progressive de l'environnement sandbox (indépendante du code de cette story), cohérente avec — et plus sévère que — la limitation déjà rapportée en Story 2.1.
- `git diff --check` sur les fichiers modifiés/créés : aucune erreur d'espacement.
- Conséquence initiale : la suite instrumentée complète de `SetupScreenTest` (14 tests) reste à exécuter sur un émulateur/CI stable avant fusion; le test de drag corrigé a depuis été validé séparément sur cible.
- Revue du 2026-09-02 : le test ciblé `draggingTheHandleUpdatesTheRenderedOrderAcrossConsecutiveMoves` passe sur `Pixel_7(AVD) - 16`; il applique quatre déplacements successifs et vérifie l'ordre rendu après chaque changement d'index.
- La première exécution complète du 2026-09-02 a atteint 14/14 tests et révélé deux défauts : assertion du badge sur le mauvais nœud sémantique et cible du bouton « Commencer » réduite à `36.19dp` par l'ordre des modifiers. Les deux sont corrigés; la relance s'est ensuite bloquée dès le test Dynamic Type avec `ComposeNotIdleException`, puis UTP n'a plus rendu la main, conformément à la dégradation d'émulateur déjà documentée.
- Validation finale hors émulateur après les dernières corrections : `testDebugUnitTest`, `compileDebugAndroidTestKotlin` et `assembleDebug` PASS; diagnostics IDE sans erreur et `git diff --check` propre.

### Completion Notes List

- Ultimate context engine analysis completed - comprehensive developer guide created.
- `SetupUiState`/`ParticipantDraft` modélisent un brouillon en mémoire uniquement (pas de `partyId`/`listPosition`); `canAdd`/`canStart`/`isMaxReached` sont dérivés et gardés par `isInitialized` pour éviter un état transitoire trompeur avant `initialize()`.
- `SetupViewModel.initialize(gameId)` est idempotent (no-op si déjà initialisé), résout le moteur via `GetAvailableGamesUseCase` et échoue explicitement (`error(...)`) si le `gameId` n'est pas enregistré — même convention que `HomeViewModel` pour un moteur non mappé.
- Toutes les mutations (ajout, suppression, déplacement) sont appliquées dans le ViewModel indépendamment de l'état désactivé de l'UI (défense en profondeur), avec un déplacement invalide/no-op silencieusement ignoré plutôt que de lever une exception.
- `SetupScreen` reste un Composable pur (état + callbacks) : réordonnancement implémenté à la main via `Modifier.pointerInput` + `detectDragGesturesAfterLongPress` sur la seule poignée, sans bibliothèque tierce ni Material Icons Extended (icônes `ic_drag_handle.xml`/`ic_delete.xml` ajoutées, dans le même style que `ic_history.xml`/`ic_papayoo_logo.xml`). Le seuil de déplacement utilise une hauteur de ligne approximée une seule fois depuis la densité (pas de mesure live en `onGloballyPositioned`), pour rester déterministe.
- `SetupRoute` initialise le brouillon dans un `LaunchedEffect(gameId)` et relaie les intentions; `onStartParty` est volontairement un no-op documenté (branché par la Story 2.3 sur ce même ViewModel).
- `NavGraph.kt` : seul le placeholder `Routes.Setup` est remplacé par `SetupRoute`; les six autres placeholders, le dispatcher de navigation et `launchSingleTop` sont inchangés. `route_placeholder_setup` retiré de `strings.xml` car devenu inutilisé.
- Aucun test dédié `NavGraph`/route n'a été ajouté pour le branchement Setup : `RoutesTest` (Story 1.5) prouve déjà que `Routes.Setup(gameId)` conserve l'identifiant exact au round-trip de sérialisation, `HomeViewModelTest` (Story 2.1) prouve déjà que l'événement de navigation transporte le `gameId` exact, et le branchement `NavGraph` lui-même est un simple relais d'une ligne (`route.gameId` → `SetupRoute(gameId = ...)`) sans logique propre à tester isolément — sous-tâche T6 correspondante donc jugée non nécessaire (« si nécessaire »).
- Suite de tests JVM `SetupViewModelTest` (14 tests) : état par défaut avant initialisation, initialisation idempotente, Jeu inconnu, ajout `trim`/vide/espaces, doublons acceptés, bascule exacte de `canStart`/`canAdd` aux bornes fournies par un faux moteur (≠ 3/8 Papayoo pour prouver l'absence de valeurs codées en dur), suppression (y compris de l'index 0 avec transfert du Donneur), déplacements valides (haut/bas) et invalides/no-op ignorés. Tous PASS.
- `SetupScreenTest` (14 tests instrumentés) écrits et compilés avec succès (`compileDebugAndroidTestKotlin`) : erreur inline, message « Maximum atteint » automatique, états activé/désactivé du champ/bouton d'ajout et du bouton Commencer selon le seuil, compteur formaté, badge Donneur, suppression, sémantiques explicites de la poignée et de la suppression, cibles tactiles ≥48dp, non-troncature à `fontScale=2f`, et un test de bout en bout du geste de drag (`performTouchInput` avec long-press puis déplacements). Le test de drag corrigé passe sur cible; la suite complète reste à rejouer sur un environnement stable (voir Debug Log).
- Revue corrigée : le drag utilise les positions et hauteurs réelles de `LazyListItemInfo`, l'index courant est lu sans redémarrer le geste, la suppression efface les messages obsolètes, et le test instrumenté valide quatre réordonnancements consécutifs dans les deux directions.

### File List

- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupUiState.kt (NEW)
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupViewModel.kt (NEW)
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupScreen.kt (NEW)
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupRoute.kt (NEW)
- app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/NavGraph.kt (UPDATE)
- app/src/main/res/drawable/ic_drag_handle.xml (NEW)
- app/src/main/res/drawable/ic_delete.xml (NEW)
- app/src/main/res/values/strings.xml (UPDATE)
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupViewModelTest.kt (NEW)
- app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/setup/SetupScreenTest.kt (NEW)

### Change Log

- 2026-08-23 : Story créée et marquée `ready-for-dev`.
- 2026-08-23 : Implémentation complète (T1–T6) : `SetupUiState`/`SetupViewModel` (brouillon en mémoire, sans persistance), `SetupScreen`/`SetupRoute` (saisie, drag-to-reorder, suppression, badge Donneur), branchement `NavGraph`, ressources FR + drawables, tests JVM (14, PASS) et instrumentés (14, compilés). Statut passé à `review`.
- 2026-09-02 : Revue adversariale terminée, 6 correctifs appliqués; validations JVM/build PASS, drag instrumenté ciblé PASS, relance instrumentée complète limitée par l'émulateur; story passée à `done`.