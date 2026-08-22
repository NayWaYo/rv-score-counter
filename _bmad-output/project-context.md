---
project_name: 'rv-score-counter'
user_name: 'boss'
date: '2026-08-22'
sections_completed: ['technology_stack', 'language_rules', 'framework_rules', 'testing_rules', 'quality_rules', 'workflow_rules', 'anti_patterns']
existing_patterns_found: 12
status: 'complete'
rule_count: 84
optimized_for_llm: true
---

# Contexte projet pour les agents IA

_Ce fichier contient les règles et motifs critiques que les agents IA doivent respecter lorsqu'ils implémentent du code dans ce projet. Il se concentre sur les détails non évidents qui risqueraient autrement d'être manqués._

---

## Pile technologique et versions

- Kotlin 2.4.10, Java 17
- Android Gradle Plugin 9.3.1, Gradle 9.5.1, KSP 2.3.11
- Android compileSdk/minSdk/targetSdk 36
- Jetpack Compose BOM 2026.06.01, Material 3
- Room 2.8.4, Hilt 2.60.1
- Navigation Compose 2.9.8, Hilt Navigation Compose 1.2.0
- Coroutines 1.11.0, Lifecycle 2.9.0
- Kotlin Serialization 1.7.3, JUnit 4.13.2

Règles de version :

- `gradle/libs.versions.toml`, `app/build.gradle.kts` et le wrapper Gradle sont toujours les sources de vérité; actualiser ce document lorsqu'ils changent.
- Exécuter Gradle avec JDK 17; corriger `JAVA_HOME` si le terminal sélectionne un JDK antérieur.
- Ne mettre à niveau aucune dépendance sans exigence explicite de la story; valider ensemble les versions Kotlin, AGP et KSP.
- Les bibliothèques Compose utilisent la BOM, sans version individuelle.
- Room et Hilt utilisent KSP; ne pas introduire KAPT sans exigence explicite.
- Les repositories sont centralisés dans `settings.gradle.kts`; ne pas ajouter de repository au niveau projet ou module.

## Règles critiques d'implémentation

### Règles Kotlin

- Garder `domain/` en Kotlin pur : aucun import Android, Compose, Room, `data`, `game` ou `ui`.
- Préférer les modèles immuables (`data class`, propriétés `val`) et les fonctions métier pures.
- Utiliser `UUID` pour les identifiants et réutiliser les alias existants comme `ParticipantId`; ne pas créer de type concurrent.
- Colocaliser les extensions `toDomain()` et `toEntity()` avec l'entité Room concernée.
- Utiliser `suspend` pour les lectures/écritures ponctuelles et `Flow` pour les observations continues.
- Modéliser les résultats métier fermés avec des types scellés; ne pas transporter de message UI localisé dans le domaine.
- Les valeurs d'enum persistées utilisent leur `name`; ne jamais les renommer sans migration Room.
- Les appelants de `GameEngine` doivent fournir des scores déjà bornés aux valeurs métier.
- Les chaînes visibles par l'utilisateur vont dans les ressources Android; les messages techniques de `require` peuvent rester dans le code.
- Respecter `kotlin.code.style=official`.

### Règles Android et frameworks

- Respecter les dépendances `UI → Domain ← Data` et `Game → Domain`; `Data` et `Game` ne se connaissent jamais.
- Conserver une seule `Activity` et un seul `NavHost`, conformément à l'architecture cible.
- Les Composables restent stateless : ils lisent un `UiState` collecté de manière lifecycle-aware et émettent des événements; l'état durable appartient au ViewModel dans un unique `StateFlow`.
- Ne pas placer dans `mutableStateOf` une valeur qui doit survivre à une recomposition ou une navigation.
- Les Composables n'utilisent pas directement `NavController`; ils émettent des événements de navigation.
- Centraliser couleurs, typographie et dimensions dans `ui/theme/`; ne pas introduire de valeurs visuelles dans `domain/`.
- Consommer les insets edge-to-edge et le padding fourni par `Scaffold`.
- Room est la source unique de vérité pour l'état d'une partie; aucun cache mutable ne fait autorité.
- Effectuer toute écriture multi-table dans `AppDatabase.withTransaction`; conserver les manches et scores en insertion `ABORT`, sans mise à jour.
- Après une modification Room, contrôler le schéma JSON exporté, augmenter la version et fournir une migration avant livraison; ne pas utiliser de fallback destructif.
- Installer les modules Hilt dans `SingletonComponent`; définir explicitement les scopes et ne pas ajouter `@Singleton` aux moteurs stateless.
- Enregistrer chaque `GameEngine` via `@IntoMap` et `@StringKey`, sans registre central ni dépendance directe à une implémentation.
- Garder `CardSuit` dans le domaine et son mapping vers `Color` exclusivement dans l'UI.

### Règles de test

- Utiliser JUnit 4 pour les tests JVM existants.
- Tester le domaine et les `GameEngine` en JVM, sans Android, Room ni Hilt.
- Pour chaque moteur, couvrir cas nominal, limites métier, entrées invalides, déterminisme et absence d'état global.
- Ne tester l'indépendance à l'ordre des participants que lorsque la règle du jeu l'exige.
- Vérifier les préconditions avec `assertThrows`.
- Tester les mappings Room dans les deux sens, notamment avec des valeurs persistées invalides ou incohérentes.
- Tester les DAO, transactions réelles et migrations Room avec des tests instrumentés; toute migration ajoutée doit avoir un test de migration.
- Pour les repositories, couvrir les flux observés, les écritures atomiques et les violations de cohérence.
- Ajouter des tests Compose pour les interactions, états visibles et propriétés sémantiques importantes.
- Chaque correction de bug doit ajouter un test de non-régression au niveau le plus bas pertinent.
- Exécuter d'abord le test ciblé, puis `testDebugUnitTest`; exécuter aussi `kspDebugKotlin` et `assembleDebug` lorsque Room, Hilt ou le build sont touchés.

### Règles de qualité et de style

- Respecter les packages `domain`, `data`, `game`, `ui` et `di`; ne pas créer de package transversal fourre-tout.
- Préserver les API publiques et les invariants existants sauf exigence explicite de la story.
- Garder les changements strictement limités à la story; ne pas mêler refactorisation opportuniste et évolution fonctionnelle.
- Préférer une implémentation directe aux abstractions anticipées; extraire uniquement lorsqu'une duplication ou une responsabilité réelle le justifie.
- Utiliser des noms métier explicites en anglais, conformément au code existant; éviter abréviations et booléens ambigus.
- Donner aux clés de moteur et autres identifiants techniques une source canonique; ne pas dupliquer leurs chaînes.
- Éviter `!!` et les valeurs de repli qui transforment silencieusement une donnée invalide en donnée valide.
- Utiliser `require` ou `check` pour les violations de contrat de programmation; détecter explicitement les incohérences provenant de Room.
- Représenter les échecs métier attendus dans les types du domaine.
- Documenter uniquement les contrats ou contraintes non évidents; ne pas commenter ce que le code exprime déjà.
- Ne pas modifier les fichiers générés sous `build/`; ne versionner les schémas Room que lorsqu'une évolution de base les produit intentionnellement.
- Avant livraison, inspecter le diff, exécuter `git diff --check`, vérifier les diagnostics des fichiers modifiés et lancer les validations adaptées au changement.

### Règles de workflow

- Utiliser la story sous `_bmad-output/implementation-artifacts/` comme source de vérité du périmètre et des critères d'acceptation.
- Lire la story, ce contexte projet et les documents d'architecture référencés avant toute implémentation.
- Vérifier la branche et `git status` avant modification; préserver tous les changements existants non liés.
- Travailler sur une branche `story/<identifiant>-<slug>` dédiée à une seule story.
- Mettre à jour `sprint-status.yaml` uniquement aux transitions prévues par le workflow BMad.
- Implémenter les tâches dans l'ordre des critères d'acceptation et ne cocher une tâche qu'après validation.
- Après chaque changement substantiel, lancer d'abord le contrôle ciblé le moins coûteux capable de le réfuter.
- Avant livraison, exécuter les validations adaptées avec le wrapper Gradle et JDK 17, puis inspecter le diff et l'état Git.
- Mettre à jour uniquement les sections autorisées de la story : tâches, Dev Agent Record, File List, Change Log et statut.
- Ne jamais inclure `build/`, `local.properties`, secrets ou fichiers temporaires dans une livraison.
- Ne pas créer de commit, pousser, fusionner ou supprimer une branche sans demande explicite de l'utilisateur.

### Points critiques à ne jamais manquer

- Ne jamais importer Android, Compose, Room ou Hilt dans `domain/`.
- Ne jamais renommer une valeur d'enum persistée sans migration Room explicite.
- Ne jamais convertir une valeur Room inconnue ou invalide vers une valeur métier par défaut.
- Ne jamais modifier une manche validée : manches et scores sont immuables après insertion.
- Toute création ou validation impliquant plusieurs tables doit être atomique dans une transaction Room.
- Échouer explicitement pendant la reconstruction du domaine face à une ligne orpheline, un identifiant de partie incohérent ou une agrégation incomplète.
- Les scores restent des `Int`; les appelants d'un `GameEngine` doivent garantir que les entrées et le résultat total restent dans les bornes métier et dans l'intervalle de `Int`.
- Un `GameEngine` reste pur et stateless; toute règle de score appartient au moteur, jamais à un Composable, ViewModel ou repository.
- Toute nouvelle implémentation de moteur doit être enregistrée par multibinding Hilt avec sa clé canonique et accompagnée de tests JVM.
- Les couleurs de cartes restent des valeurs métier comme `CardSuit`; leur représentation Compose appartient exclusivement à l'UI.
- Toute chaîne visible doit provenir des ressources Android.
- Les fichiers de build font autorité pour les versions, la story active pour son comportement attendu et l'architecture pour ses invariants.
- En cas de contradiction entre ces sources, arrêter l'implémentation concernée et signaler précisément le conflit au lieu de choisir silencieusement.

---

## Consignes d'utilisation

**Pour les agents IA :**

- Lire ce fichier avant toute implémentation.
- Respecter toutes les règles documentées.
- En cas de doute, appliquer l'option la plus restrictive compatible avec la story et l'architecture.
- Actualiser ce fichier lorsqu'un nouveau motif durable est établi.

**Pour les humains :**

- Garder ce fichier concis et centré sur les besoins des agents.
- L'actualiser lors d'un changement de pile technologique ou d'architecture.
- Le réviser périodiquement pour retirer les règles obsolètes ou devenues évidentes.

Dernière mise à jour : 2026-08-22