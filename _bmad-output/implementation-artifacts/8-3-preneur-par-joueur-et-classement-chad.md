---
story_id: "8.3"
story_key: "8-3-preneur-par-joueur-et-classement-chad"
epic: 8
status: done
created: 2026-10-21
baseline_commit: aedf68e
---

# Story 8.3 : Preneur par joueur et classement « 🗿 Chad de la game »

**Epic :** 8 - Historique par Jeu et preneur de Belote
**Story ID :** 8.3
**Status :** done

## Story

En tant qu'utilisateur,
Je veux indiquer quel joueur a pris à chaque Manche de Belote,
Afin de classer les joueurs selon le nombre de fois où ils ont pris.

## Acceptance Criteria

1. **Given** la saisie ou la correction d'une Manche de Belote **When** je choisis qui a pris **Then** les 4 joueurs sont proposés, regroupés par Équipe (une rangée par Équipe, sous son nom), et l'Équipe preneuse en est déduite ; le calcul des points ne change pas.

2. **And** une nouvelle Manche ne peut être validée sans joueur preneur ; la correction d'une Manche jouée avant cette story part de son Équipe preneuse et demande aussi de choisir le joueur.

3. **Given** le schéma Room en version 3 **When** l'application passe en version 4 **Then** `belote_rounds.takerPlayerId` mémorise le joueur preneur, nul pour les Manches déjà jouées, par une `AutoMigration` 3 → 4 testée sans perte.

4. **And** un joueur preneur qui n'est pas un joueur de la Partie dans l'Équipe preneuse est refusé par le domaine.

5. **Given** les Statistiques d'une Partie de Belote **When** au moins une Manche a un joueur preneur **Then** une carte « 🗿 Chad de la game » à cadre lagon (sombre : fond #10302B, encre #7FCBB8 ; clair : fond #D9F0EA, encre #1E6B5D) met en avant le joueur qui a pris le plus souvent, puis liste les 4 joueurs du plus grand nombre de prises au plus petit, avec « N prises · M faites ».

6. **And** une prise est « faite » quand le preneur fait son contrat ou capote ; à égalité de prises, le plus de contrats faits passe devant, puis l'ordre de table ; les Manches sans joueur preneur ne comptent pas, et la carte n'apparaît pas si aucune n'en a.

7. **And** le tableau des Manches (Partie en cours, détail) indique « Pris par <joueur> » quand il est connu, l'Équipe sinon.

## Tasks / Subtasks

- [x] **T1 - Données** (AC: 3)
  - [x] `BeloteRound.takerPlayerId`, colonne Room, `updateInput` étendu ; version 4, `AutoMigration(3, 4)`, `4.json`.
- [x] **T2 - Domaine** (AC: 2, 4, 6)
  - [x] `PlayBeloteRoundUseCase` et `CorrectBeloteRoundUseCase` reçoivent le joueur preneur et le valident.
  - [x] `BelotePartyState.takerRanking()` : prises et contrats faits par joueur, triés.
- [x] **T3 - Interface** (AC: 1, 2, 5, 7)
  - [x] Choix du joueur dans la saisie ; « Pris par » dans le tableau ; carte « 🗿 Chad de la game » et couleurs lagon dans `RvColors`.
- [x] **T4 - Tests et validations** (AC: 1-7)
  - [x] JVM : use cases, classement, ViewModels. Instrumentés : migration 3 → 4, repository, écrans.
  - [x] `testDebugUnitTest`, `kspDebugKotlin`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

## Dev Notes

- Le joueur preneur n'entre pas dans `BeloteRoundInput` : le moteur ne compte que par Équipe. Il est porté par `BeloteRound` et passé à côté de la saisie.
- Pas de clé étrangère vers `participants` : l'ajouter imposerait de recréer la table ; le domaine vérifie l'appartenance du joueur à la Partie et à l'Équipe preneuse.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 8.3]
- [Source: `_bmad-output/planning-artifacts/games/belote.md`]
- [Source: `_bmad-output/implementation-artifacts/7-2-donnees-equipes-mode-de-fin-et-manches-belote.md`]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- `testDebugUnitTest kspDebugKotlin assembleDebug compileDebugAndroidTestKotlin` : 354 tests JVM, 0 échec ; schéma `4.json` exporté.
- `connectedDebugAndroidTest` sur l'AVD `Pixel_API_36` : 186 tests ; deux échecs corrigés (un ancien jeu de données de test sans joueur preneur, et le `testTag` de la carte Chad effacé par `clearAndSetSemantics`, même défaut qu'en 7.4), puis `BeloteScreensTest` (19) relancé sans échec.
- Vérifié à la main sur l'émulateur : choix « Chloe » parmi les 4 joueurs, ligne « Pris par Chloe », carte « 🗿 Chad de la game » lagon avec le classement des 4 joueurs.
- Non vérifié à la main : la migration v3 → v4 sur la base réelle du téléphone (testée par `AppDatabaseMigrationTest`), la correction d'une Manche jouée avant cette story, le thème sombre de la carte. L'émulateur étant en anglais, il affiche « 0 prises » ; en français, Android applique la forme « one » à 0 (« 0 prise »).

### Completion Notes List

- Le joueur preneur est porté par `BeloteRound`, pas par `BeloteRoundInput` : le moteur reste par Équipe.
- `requireTaker` refuse un joueur hors de la Partie ou hors de l'Équipe preneuse ; la correction exige aussi un joueur, la saisie d'une Manche ancienne affichant « Indiquez quel joueur de <Équipe> a pris. ».
- `BelotePartyState.takerRanking()` : tri stable par prises puis contrats faits, l'ordre de table départageant ; un capot de la défense n'est pas une prise faite.
- Couleurs lagon ajoutées à `RvColors` (`chadTint`, `chadInk`) pour les deux thèmes.

### File List

- `_bmad-output/implementation-artifacts/8-3-preneur-par-joueur-et-classement-chad.md` (nouveau), `sprint-status.yaml`
- `app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/4.json` (nouveau)
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/model/BeloteRound.kt`, `BelotePartyState.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/PlayBeloteRoundUseCase.kt`, `CorrectBeloteRoundUseCase.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/data/entity/BeloteRoundEntity.kt`, `data/dao/BeloteRoundDao.kt`, `data/db/AppDatabase.kt`, `data/repository/BeloteRepositoryImpl.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/theme/Color.kt`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/belote/BeloteRoundUiState.kt`, `BeloteRoundViewModel.kt`, `BeloteRoundScreen.kt`, `BeloteRoundRoute.kt`, `BelotePartyViewModel.kt`, `BeloteStatsViewModel.kt`, `BeloteStatsScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/rvetiveau/rvscorecount/domain/usecase/FakeBeloteStore.kt`, `BeloteUseCasesTest.kt`, `ui/screens/belote/BeloteViewModelsTest.kt`
- `app/src/androidTest/java/com/rvetiveau/rvscorecount/data/db/AppDatabaseMigrationTest.kt`, `data/repository/BeloteRepositoryImplTest.kt`, `ui/screens/belote/BeloteScreensTest.kt`

### Change Log

- 2026-10-21 : joueur preneur à la Belote (schéma v4) et carte « 🗿 Chad de la game ».
