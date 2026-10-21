---
story_id: "8.1"
story_key: "8-1-suppression-visible-dans-l-historique"
epic: 8
status: done
created: 2026-10-21
baseline_commit: b3e6860
---

# Story 8.1 : Suppression visible dans l'Historique

**Epic :** 8 - Historique par Jeu et preneur de Belote
**Story ID :** 8.1
**Status :** done

## Story

En tant qu'utilisateur,
Je veux voir comment supprimer une Partie de l'Historique,
Afin de ne pas dépendre d'un geste caché.

## Acceptance Criteria

1. **Given** une ligne de l'Historique **When** elle s'affiche **Then** une icône corbeille est visible au bout de la ligne, avec une cible d'au moins 48dp et, pour TalkBack, la description « Supprimer la partie du <date> ».

2. **And** toucher la corbeille ouvre la même confirmation qu'aujourd'hui (« Supprimer cette partie ? »), sans ouvrir le détail.

3. **And** le balayage de droite à gauche reste possible et mène à la même confirmation.

4. **And** le comportement est identique pour le Papayoo et la Belote ; la ligne de la création du Papayoo, sans suppression, n'a pas de corbeille.

## Tasks / Subtasks

- [x] **T1 - Ligne d'Historique** (AC: 1, 2, 4)
  - [x] `PartySummaryRow` : corbeille (`ic_delete`) quand `onDeleteRequested` est fourni ; elle remplace l'action TalkBack « Supprimer » cachée.
- [x] **T2 - Tests et validations** (AC: 1-4)
  - [x] Compose : `HistoryScreenTest` (corbeille, confirmation, balayage, absence sans suppression).
  - [x] `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

## Dev Notes

- Le bouton est un nœud cliquable distinct de la ligne fusionnée : TalkBack lit la ligne puis propose la corbeille, la ligne ouvre toujours le détail.
- Aucune donnée, aucun use case ni ViewModel ne change.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 8.1]
- [Source: `_bmad-output/implementation-artifacts/6-1-ecran-historique-liste-des-parties-passees.md`]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- `testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin` : 346 tests JVM, 0 échec.
- `connectedDebugAndroidTest` sur l'AVD `Pixel_API_36` : 181 tests ; un échec révélait que l'`IconButton` de Material 3 mesure 40dp, corrigé à 48dp, puis `HistoryScreenTest` (8) relancé sans échec.
- Vérifié à la main sur l'émulateur : corbeille visible au bout d'une ligne de Belote, confirmation ouverte au toucher.
- Remarque hors périmètre : la boîte de confirmation garde le fond lavande par défaut de Material (déjà le cas avant cette story).

### Completion Notes List

- La ligne entière reste le nœud fusionné et cliquable (ouverture du détail) ; la corbeille est un nœud cliquable distinct, décrit « Supprimer la partie du <date> ». L'action TalkBack « Supprimer » cachée est retirée, la corbeille la remplaçant.
- Le balayage de droite à gauche est conservé.

### File List

- `_bmad-output/implementation-artifacts/8-1-suppression-visible-dans-l-historique.md` (nouveau), `sprint-status.yaml`
- `app/src/main/java/com/rvetiveau/rvscorecount/ui/components/PartySummaryRow.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/history/HistoryScreenTest.kt`

### Change Log

- 2026-10-21 : corbeille visible sur chaque ligne de l'Historique.
