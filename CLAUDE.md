# rv-score-counter

Application Android (Kotlin, Jetpack Compose, Room, Hilt) de comptage des scores pour jeux de cartes, à commencer par le Papayoo.

## À lire avant toute implémentation

- `_bmad-output/project-context.md` : règles de code, d'architecture et de test (obligatoires).
- `_bmad-output/planning-artifacts/architecture/*/ARCHITECTURE-SPINE.md` : décisions d'architecture (AD-1 à AD-6).
- `_bmad-output/planning-artifacts/epics.md` : epics, stories et critères d'acceptation.
- `_bmad-output/implementation-artifacts/sprint-status.yaml` : avancement des stories.

## Déroulé d'une story

Le déroulé complet, avec ses commandes et ses pièges connus, est dans la skill projet `story` (`.claude/skills/story/SKILL.md`). En résumé :

1. Prendre la prochaine story `backlog` dans `sprint-status.yaml` et ses critères dans `epics.md`.
2. Rédiger le fichier de story dans `_bmad-output/implementation-artifacts/` (même format que les stories précédentes).
3. Implémenter dans l'ordre des critères, avec tests JVM (domaine, moteurs, ViewModels) et tests instrumentés si Room ou Compose changent.
4. Valider (voir ci-dessous), relire le diff, puis passer la story et `sprint-status.yaml` à `done`.

Ne pas committer, pousser ni fusionner sans demande explicite.

## Commandes

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

`kspDebugKotlin` et `assembleDebug` sont requis dès que Room, Hilt ou le build sont touchés. Les tests instrumentés demandent un émulateur ou un appareil en API 36 : sur API 37, tous les tests Compose échouent (`NoSuchMethodException: InputManager.getInstance`, Espresso incompatible). Sur ce poste, l'AVD `Pixel_API_36` est prévu pour ça :

```bash
emulator -avd Pixel_API_36 -no-window -no-audio
```

Quand le téléphone est aussi branché, fixer `ANDROID_SERIAL=emulator-5554` pour que les tests instrumentés ne s'exécutent que sur l'émulateur.

Gradle doit tourner sur un JDK 17 ou plus. Sur ce poste, `~/.gradle/gradle.properties` définit `org.gradle.java.home` vers le JDK d'Android Studio.
