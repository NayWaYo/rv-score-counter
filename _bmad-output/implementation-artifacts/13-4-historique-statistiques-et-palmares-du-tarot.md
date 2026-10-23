---
story_id: "13.4"
story_key: "13-4-historique-statistiques-et-palmares-du-tarot"
epic: 13
status: done
created: 2026-10-23
baseline_commit: e37137e
---

# Story 13.4 : Historique, statistiques et palmarès du Tarot

**Epic :** 13 - Tarot
**Story ID :** 13.4
**Status :** done

## Story

En tant qu'utilisateur,
Je veux retrouver mes Parties de Tarot et le palmarès de chacun,
Afin de savoir qui prend le plus et qui réussit le mieux.

## Acceptance Criteria

1. **Given** une Partie de Tarot, en cours ou terminée, avec au moins une Manche **Then** un lien « Statistiques (N) » au-dessus des Manches ouvre l'écran Statistiques : la courbe des scores cumulés de chaque joueur, puis la carte « 🗿 Chad de la game » (le joueur qui a le plus pris, puis chacun classé par prises et contrats faits), comme pour la Belote.

2. **And** l'Historique liste les Parties de Tarot avec leur gagnant et ouvre leur détail en lecture seule (livré avec la Story 13.3).

3. **Given** le profil d'un Joueur (Story 10.3) qui a terminé au moins une Partie de Tarot **Then** une carte « Tarot » montre son taux de victoire (gagnant au plus grand total, égalités comprises ; une Partie terminée avant sa première Manche compte sans victoire), son nombre de prises (et leur moyenne par Partie), son taux de contrats faits et son contrat le plus pris (en cas d'égalité, le plus haut).

4. **And** une Partie de Tarot dont les données ne tiennent pas ensemble est ignorée du palmarès, comme pour les autres Jeux.

5. **And** les règles du palmarès et du classement des preneurs sont couvertes par des tests JVM, les écrans par des tests Compose.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 1, 3, 4)
  - [x] `TarotPartyState.takerRanking`, `TarotRecords`, `TarotPlay`, `tarotRecords` ; `PlayerRecords.tarot` ; `GetPlayerRecordsUseCase` lit les Parties de Tarot
  - [x] Tests JVM
- [x] **T2 - Statistiques** (AC: 1)
  - [x] Route `TarotStats`, `TarotStatsViewModel`, `TarotStatsScreen` (courbe et carte Chad partagée avec la Belote), lien depuis la Partie
  - [x] Tests JVM du ViewModel, tests Compose
- [x] **T3 - Profil** (AC: 3)
  - [x] Carte « Tarot » du profil ; tests Compose
- [x] **T4 - Validation** : `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` API 36

## Dev Notes

- La carte Chad (`ChadCard`, avec `BeloteTakerUiModel` renommé `TakerUiModel`) devient commune à la Belote et au Tarot ; au Tarot, chaque Manche a un preneur, la carte s'affiche donc dès la première Manche.
- Une prise faite au Tarot : `TarotRoundResult.made`. Le contrat le plus pris départage les égalités par le multiplicateur le plus haut.
- Aucune évolution Room.

## References

- `_bmad-output/planning-artifacts/epics.md` (Epic 13, Story 13.4)
- Stories 7.4, 8.3 (statistiques de la Belote), 10.3 et 11.4 (profil)

## Dev Agent Record

### Debug Log References

- JVM : `./gradlew testDebugUnitTest` → 565 tests, 0 échec (558 avant la story) ; nouveaux : `TarotRecordsTest` (5), `TarotViewModelsTest` +2 (statistiques) ; les tests qui construisent `GetPlayerRecordsUseCase` reçoivent le dépôt et le moteur du Tarot, ceux de la Belote utilisent `TakerUiModel`.
- Build : `kspDebugKotlin assembleDebug compileDebugAndroidTestKotlin` OK.
- Instrumentés API 36 : `connectedDebugAndroidTest` → 274 tests, 0 échec (269 avant) ; nouveaux : `TarotScreensTest` +3, `PlayerProfileScreenTest` +2.
- `git diff --check` propre.
- Non vérifié : l'écran Statistiques et la carte du profil sur le téléphone.

### Completion Notes List

- `TarotPartyState.takerRanking` classe chaque joueur par prises, puis contrats faits, puis ordre de table ; `tarotRecords` compte victoires, prises, contrats faits et le contrat le plus pris (le plus haut en cas d'égalité) ; `GetPlayerRecordsUseCase` lit les Parties de Tarot terminées et ignore une Partie incohérente.
- Statistiques : `TarotStatsScreen` (courbe des scores cumulés de `ScoreChart` et carte « 🗿 Chad de la game »), ouvert par « Statistiques (N) » au-dessus des Manches, en cours comme terminée.
- La carte Chad devient un composant commun (`ui/components/ChadCard.kt`, `TakerUiModel`), utilisé par la Belote et le Tarot.
- Profil : carte « Tarot » (taux de victoire, prises et moyenne par Partie, contrats faits, contrat préféré).
- L'AC « Historique » était déjà livré par la Story 13.3.
- Correction après l'essai sur le téléphone : la courbe partait toujours de 0 en bas, si bien que les totaux négatifs du Tarot sortaient du graphique et passaient sur la légende. `ScoreChart` calcule désormais un axe (`valueAxis`) du plus petit au plus grand total, 0 toujours compris, et marque la ligne du 0 quand des totaux sont négatifs ; le Jeu libre en profite aussi.
- Correction de la création (Story 13.3) après l'essai sur le téléphone : le champ du nombre de Manches ouvrait le clavier, qui repoussait ce champ et « Commencer » hors de l'écran. Il devient un compteur « − N + » (1 à 99), sans clavier.
- Correction générale après l'essai de la saisie des points du Tarot : le `Scaffold` racine décale déjà le contenu au-dessus du clavier (`safeDrawing`), et les écrans de saisie et de création (Belote, Jeu libre, Tarot) ajoutaient leur propre `imePadding()`, si bien que le clavier était compté deux fois et que leur contenu disparaissait. `MainActivity` consomme désormais ce décalage (`consumeWindowInsets`).

### File List

- Domaine : `domain/model/TarotParty.kt` (`takerRanking`), `PlayerRecords.kt` (`TarotRecords`, `TarotPlay`, `tarotRecords`), `domain/usecase/GetPlayerRecordsUseCase.kt`
- `MainActivity.kt` (clavier compté une seule fois)
- Interface : `ui/components/ScoreChart.kt` (axe des totaux négatifs), `ui/screens/tarot/TarotSetupViewModel.kt`, `TarotSetupScreen.kt` (compteur de Manches), `ui/components/ChadCard.kt` (nouveau), `ui/screens/belote/BeloteStatsScreen.kt`, `BeloteStatsViewModel.kt`, `ui/screens/tarot/TarotStatsScreen.kt` (nouveau), `TarotPartyScreen.kt`, `TarotPartyViewModel.kt`, `ui/screens/profile/PlayerProfileScreen.kt`, `ui/navigation/Routes.kt`, `NavGraph.kt`, `res/values/strings.xml`
- Tests : `app/src/test/.../domain/usecase/TarotRecordsTest.kt` (nouveau), `app/src/test/.../ui/components/ScoreChartTest.kt`, `TarotViewModelsTest.kt`, `FreeRecordsAndReplayTest.kt`, `GetPlayerRecordsUseCaseTest.kt`, `PlayerProfileViewModelTest.kt`, `BeloteViewModelsTest.kt` ; `app/src/androidTest/.../ui/screens/tarot/TarotScreensTest.kt`, `profile/PlayerProfileScreenTest.kt`, `belote/BeloteScreensTest.kt`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-10-23 : Story 13.4 implémentée (statistiques et palmarès du Tarot), testée JVM et API 36.
- 2026-10-23 : courbe des scores corrigée pour les totaux négatifs ; 567 tests JVM (dont `ScoreChartTest` +2), 215 tests d'interface API 36, 0 échec ; réinstallée sur le téléphone.
- 2026-10-23 : nombre de Manches de la création choisi par « − » et « + » ; 567 tests JVM, `TarotScreensTest` 10/10 sur API 36 ; réinstallée sur le téléphone.
- 2026-10-23 : clavier compté une seule fois à la racine ; 567 tests JVM ; réinstallée sur le téléphone (vérification du clavier faite à la main sur le téléphone).
