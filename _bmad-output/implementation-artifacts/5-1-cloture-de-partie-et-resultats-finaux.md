---
story_id: "5.1"
story_key: "5-1-cloture-de-partie-et-resultats-finaux"
epic: 5
status: done
created: 2026-10-18
baseline_commit: 2c1724d
---

# Story 5.1 : Clôture de Partie & résultats finaux

**Epic :** 5 - Clôture & Relance
**Story ID :** 5.1
**Status :** done

## Story

En tant qu'utilisateur,
Je veux clore la Partie quand je décide que c'est terminé et voir le classement final,
Afin de célébrer le vainqueur et sauvegarder la Partie dans l'historique.

## Acceptance Criteria

1. **Given** une Partie `IN_PROGRESS` **When** l'utilisateur tape « Fin de partie » (activé à partir de cette story) **Then** un dialog « Fin de partie ? » / « Voulez-vous vraiment mettre fin à cette partie ? » s'affiche avec « Confirmer » (danger, couleur error) et « Annuler ».

2. **Given** l'utilisateur confirme **Then** `ClosePartyUseCase` (dans `domain/usecase/`) passe la Partie en `FINISHED` et la persiste; une Partie absente ou déjà terminée est rejetée sans écriture; un échec affiche un message sur PartyScreen et laisse la Partie en cours.

3. **And** la navigation remplace toute la pile par `ResultsScreen(partyId)`; « Annuler » ferme le dialog sans écriture.

4. **And** ResultsScreen affiche le Classement final complet (rang, nom, Score Cumulé), calculé par `computeStandings()` depuis Room, avec le vainqueur (Score Cumulé le plus bas, tous les ex æquo) mis en avant en tête d'écran et les trois premiers rangs accentués.

5. **And** « Rejouer » (`Button` plein) et « Retour à l'accueil » (`OutlinedButton`) sont affichés **And** « Rejouer » reste désactivé jusqu'à la Story 5.2 **And** « Retour à l'accueil » et le retour arrière Android mènent à l'Accueil en vidant la pile (UX : retour depuis les Résultats vers l'Accueil, pas vers la Partie).

6. **And** une Partie `FINISHED` ne peut plus recevoir de Manche : PartyScreen masque « Nouvelle manche », « Fin de partie » et l'ajout de joueur; `ValidateRoundUseCase` et la Saisie de Manche la rejettent déjà (Stories 3.4, 3.2); au lancement, la reprise automatique ne la propose plus (seules les Parties `IN_PROGRESS` sont reprises).

7. **And** ResultsScreen affiche un état d'erreur (« Réessayer ») si la lecture échoue, et un état explicite si la Partie est introuvable, pas terminée ou incohérente.

8. **And** aucune migration ni dépendance; toutes les chaînes visibles viennent de `strings.xml`.

## Tasks / Subtasks

- [x] **T1 - Domaine** (AC: 2)
  - [x] `ClosePartyUseCase` avec exceptions scellées.
- [x] **T2 - Navigation** (AC: 3, 5)
  - [x] `NavigationEvent.ReplaceAll(route)` vide la pile avant de naviguer.
- [x] **T3 - PartyScreen** (AC: 1-3, 6)
  - [x] « Fin de partie » actif, dialog de confirmation, erreur de clôture.
  - [x] Ligne de classement extraite dans `ui/components/` pour être partagée avec ResultsScreen.
- [x] **T4 - ResultsScreen** (AC: 4, 5, 7)
  - [x] `ResultsViewModel`, `ResultsUiState`, `ResultsScreen`, `ResultsRoute`; branchement dans `NavGraph` à la place du placeholder.
- [x] **T5 - Tests et validations** (AC: 1-8)
  - [x] JVM : use case, `PartyViewModel` (dialog, clôture), `ResultsViewModel`.
  - [x] Compose : dialog, écran Résultats.
  - [x] `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest` sur API 36.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 5.1]
- [Source: `_bmad-output/planning-artifacts/prds/prd-rv-score-counter-2026-08-09/prd.md` — FR-12, FR-13]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md` — Résultats finaux, Dialog de confirmation, navigation]

## Dev Agent Record

### Agent Model Used

Claude Opus 5.5 (Claude Code)

### Debug Log References

- 2026-10-18 : `testDebugUnitTest` : 169 tests, 0 échec (3 `ClosePartyUseCaseTest`, 4 `ResultsViewModelTest`, 3 nouveaux `PartyViewModelTest`).
- 2026-10-18 : `kspDebugKotlin`, `assembleDebug`, `compileDebugAndroidTestKotlin` réussis; `git diff --check` propre.
- 2026-10-18 : `connectedDebugAndroidTest` sur `Pixel_API_36` : 96 tests, 0 échec (4 `ResultsScreenTest`, 2 nouveaux `PartyScreenTest`).
- Non vérifié de bout en bout dans l'application : l'enchaînement réel des écrans (`ReplaceAll`, retour arrière depuis les Résultats) n'est couvert qu'au niveau des événements de navigation émis.

### Completion Notes List

- `ClosePartyUseCase` : IN_PROGRESS -> FINISHED, rejet d'une Partie absente ou déjà terminée.
- `NavigationEvent.ReplaceAll` vide la pile : clôture -> Résultats, Résultats -> Accueil (bouton et retour arrière).
- PartyScreen : « Fin de partie » actif, dialog de confirmation (Confirmer en couleur error), message en cas d'échec.
- ResultsScreen : bandeau du ou des vainqueurs, classement final avec podium accentué, « Rejouer » désactivé jusqu'à la Story 5.2, « Retour à l'accueil ».
- Ligne de classement extraite dans `ui/components/StandingRowItem.kt` (avec `toStandingRows()`), partagée par PartyScreen et ResultsScreen; placeholder et chaîne `route_placeholder_results` supprimés.

### File List

- _bmad-output/implementation-artifacts/5-1-cloture-de-partie-et-resultats-finaux.md
- _bmad-output/implementation-artifacts/sprint-status.yaml
- app/src/main/java/com/rvetiveau/rvscorecount/domain/usecase/ClosePartyUseCase.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/components/StandingRowItem.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/NavGraph.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/navigation/NavigationEvent.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyRoute.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyScreen.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyUiState.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModel.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsRoute.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsScreen.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsUiState.kt
- app/src/main/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsViewModel.kt
- app/src/main/res/values/strings.xml
- app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyScreenTest.kt
- app/src/androidTest/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsScreenTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/domain/usecase/ClosePartyUseCaseTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/party/PartyViewModelTest.kt
- app/src/test/java/com/rvetiveau/rvscorecount/ui/screens/results/ResultsViewModelTest.kt

### Change Log

- 2026-10-18 : Story créée et passée `in-progress`.
- 2026-10-18 : Clôture confirmée, écran Résultats finaux et navigation vers l'Accueil implémentés et testés (JVM + API 36); story passée `done`.
