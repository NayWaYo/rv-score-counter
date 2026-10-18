---
name: story
description: Implement a feature or a backlog story of rv-score-counter end to end, following the project's lightened BMAD workflow (story file, implementation, JVM and API 36 instrumented tests, sprint-status update, commit). Use when the user asks to start, continue or implement a story (e.g. "attaque la story 4.2", "enchaîne avec la suivante") or a new feature of the app.
---

# Story workflow — rv-score-counter

Android app (Kotlin, Compose, Room, Hilt). Follow these steps in order; each one has a check that must pass before the next.

## 1. Frame the work

- Read `_bmad-output/project-context.md` (mandatory rules) before touching code.
- Backlog story: take the next `backlog` entry in `_bmad-output/implementation-artifacts/sprint-status.yaml`, and its acceptance criteria in `_bmad-output/planning-artifacts/epics.md`. Check the PRD (`planning-artifacts/prds/`) and UX (`planning-artifacts/ux-designs/`) sections it relies on.
- Feature outside the backlog: no story file is required; state the product choices you make (mandatory or optional input, where it is displayed…) before coding.
- If PRD, epics and UX disagree, or a rule looks illogical, say so to the user before implementing.
- Work on the branch the user chose (so far `main`); never commit, push or merge without an explicit request.

## 2. Write the story file (backlog stories)

Create `_bmad-output/implementation-artifacts/<story_key>.md` with the same structure as the previous ones: frontmatter (`story_id`, `story_key`, `epic`, `status: in-progress`, `created`, `baseline_commit`), Story, numbered Acceptance Criteria, Tasks / Subtasks (`- [ ]`), Dev Notes, References, Dev Agent Record. Set the story (and its epic if it was `backlog`) to `in-progress` in `sprint-status.yaml`.

## 3. Implement

- Layers: `domain/` (pure Kotlin, use cases, rules) ← `data/` (Room) and `game/` (engines); `ui/` reads one `StateFlow` per screen. Reusable composables go to `ui/components/`.
- Business rules live in the domain or in the `GameEngine`, never in a composable or a repository. Expected business failures are sealed types; multi-table writes go through `AppDatabase.withTransaction`.
- Every visible string goes to `res/values/strings.xml` (escape `'` as `\'`).
- Changing `PartyRepository` breaks every test fake implementing it (about ten files under `app/src/test`): update them all.
- Room change: bump `AppDatabase.version`, prefer an `AutoMigration`, commit the generated `app/schemas/.../<version>.json`, and extend `AppDatabaseMigrationTest` (it builds a real file of the previous version from the exported SQL; `room-testing` is not used because it conflicts with the app's kotlinx-serialization 1.7.3).

## 4. Test

Gradle runs on Android Studio's JDK through `~/.gradle/gradle.properties` (`org.gradle.java.home`).

```bash
./gradlew testDebugUnitTest kspDebugKotlin assembleDebug compileDebugAndroidTestKotlin
```

Instrumented tests need an **API 36** device: on API 37 every Compose test fails (`NoSuchMethodException: InputManager.getInstance`). Use the `Pixel_API_36` AVD, and set `ANDROID_SERIAL` so nothing runs on the user's phone if it is plugged in:

```bash
emulator -avd Pixel_API_36 -no-window -no-audio -no-boot-anim -no-snapshot-save
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
adb -s emulator-5554 emu kill
```

Run the emulator in the background and wait for `adb -s emulator-5554 shell getprop sys.boot_completed` to return `1`. Add JVM tests for the domain and ViewModels, and Compose or Room tests for screens and transactions. Finish with `git diff --check`.

## 5. Close

- Story file: tick the tasks, fill Debug Log References (exact test counts, what was not verified), Completion Notes, File List and Change Log, set `status: done`; same for the story in `sprint-status.yaml`. An epic goes to `done` only when the user agrees.
- Say plainly what was not verified (for example a full game played by hand in the app).
- On the user's request, commit with a message file written as UTF-8 **without BOM** (PowerShell `Set-Content -Encoding utf8` adds one), e.g. `feat: complete story X.Y <title>`, ending with the attribution line; never stage `gradle/gradle-daemon-jvm.properties` or `local.properties`.

## 6. Install on the phone (when asked)

The user's Pixel 7 (Android 17) accepts the debug APK signed with this PC's `~/.android/debug.keystore`:

```bash
adb devices
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

An `INSTALL_FAILED_UPDATE_INCOMPATIBLE` error means a different signing key: uninstalling deletes the user's parties, so ask first.
