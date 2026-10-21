---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-rv-score-counter-2026-08-09/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/DESIGN.md
  - _bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/EXPERIENCE.md
---

# rv-score-counter - Epic Breakdown

## Overview

Ce document fournit la décomposition complète en épics et stories pour rv-score-counter, une application Android de comptage de points pour jeux de cartes (Papayoo V1, extensible multi-jeux).

## Requirements Inventory

### Functional Requirements

FR-1: L'utilisateur peut sélectionner le Jeu de la Partie parmi les Jeux disponibles (V1 : Papayoo uniquement).
FR-2: L'utilisateur peut ajouter entre 3 et 8 Participants identifiés par prénom et réordonner la liste (glisser-déposer).
FR-3: Le premier Participant de la liste est automatiquement Donneur de la Manche 1 ; la rotation suit l'ordre de la liste.
FR-4: Après la Manche 1 validée, seul l'ajout d'un Participant est autorisé (si count < 8), avec choix de position d'insertion ; la rotation du Donneur suit la liste résultante.
FR-5: L'utilisateur peut lancer un Dé Virtuel pour obtenir aléatoirement une des 4 Couleurs du 7 (♠♥♣♦) ; la couleur est affichée pendant toute la saisie et stockée par Manche.
FR-6: L'utilisateur peut saisir le Score de Manche de chaque Participant dans n'importe quel ordre ; les champs acceptent des entiers ≥ 0.
FR-7: Lorsque N-1 champs sont remplis, le N-ième est pré-rempli par complément à 250 ; l'utilisateur peut le modifier.
FR-8: La validation de Manche est bloquée si la somme ≠ 250, avec message précisant l'écart ; un score de 0 est valide.
FR-9: Une Manche validée ne peut pas être supprimée. Tant que la Partie est en cours, ses scores et le détenteur du 7 peuvent être corrigés, avec les mêmes contrôles qu'à la validation ; son numéro, sa couleur et le Donneur ne changent pas (révisé le 2026-10-20 après la revue ergonomie).
FR-10: L'écran principal affiche le Classement (Score Cumulé croissant) mis à jour après chaque Manche, avec le Donneur identifié visuellement.
FR-11: L'utilisateur peut consulter le détail des scores de chaque Manche passée.
FR-12: L'utilisateur peut clore la Partie à tout moment via "Fin de partie" (distinct de "Nouvelle manche"), après confirmation.
FR-13: Après clôture, l'écran de résultats affiche le Classement final et propose "Rejouer" / "Retour accueil".
FR-14: Depuis les Résultats finaux ou l'Historique, l'utilisateur peut lancer une nouvelle Partie avec les mêmes Participants dans le même ordre.
FR-15: L'utilisateur peut consulter la liste des Parties passées et le détail de chacune (Participants, Scores, Classement, Couleur du 7 par Manche).
FR-16: Les données et l'état d'une Partie en cours persistent localement ; une Partie non clôturée est restaurée au prochain lancement.

### NonFunctional Requirements

NFR-1: Plateforme Android natif, API 36 minimum (appareil de l'auteur) ; iOS hors périmètre V1.
NFR-2: Aucune connectivité réseau requise — fonctionnement entièrement hors ligne.
NFR-3: Stockage local uniquement (Room/SQLite) ; aucune synchronisation cloud.
NFR-4: Aucune permission système requise (pas de caméra, réseau, contacts).
NFR-5: Interface en français uniquement (V1) — toutes les strings dans res/values/strings.xml.
NFR-6: Lisibilité à ~50 cm en lumière d'ambiance variable ; textes porteurs d'information ≥ 16sp.
NFR-7: Cibles tactiles ≥ 48dp ; champs de score et bouton "Valider la Manche" à 56dp.

### Additional Requirements

- ARCH-1 (AD-1): Paradigme MVVM + Clean Layered — dépendances strictement vers l'intérieur (UI→Domain←Data, Game→Domain).
- ARCH-2 (AD-2): Interface `GameEngine` dans domain/engine/ ; Papayoo l'implémente dans game/papayoo/ ; les use cases ne dépendent que de l'interface.
- ARCH-3 (AD-3): Room = source unique de vérité ; write synchrone après chaque mutation (Round validé, Participant ajouté, Partie clôturée).
- ARCH-4 (AD-4): ViewModel expose `StateFlow<UiState>` ; Composables stateless ; toute mutation transite par ViewModel → use case → repository.
- ARCH-5 (AD-5): `PartyState` enum SETUP/IN_PROGRESS/FINISHED ; règles de mutation définies dans le domain uniquement.
- ARCH-6 (AD-6): Single Activity + Compose Navigation ; au lancement, si Partie IN_PROGRESS en base → navigation directe vers PartyScreen.
- ARCH-7 (AD-7): `CardSuit` enum dans domain/model/ ; mapping couleur dans ui/theme/CardSuitColors.kt.
- STACK: Kotlin 2.0.x, Compose BOM 2025.04.x, Room 2.7.x, Hilt 2.52+, Navigation-Compose 2.8.x, AGP 8.x, minSdk 36.
- Injection Hilt : modules dans di/ ; `@HiltViewModel` sur tous les ViewModels.
- Conventions : suffix `Entity` pour les entités Room ; `UseCase` suffix pour les use cases ; `UUID` pour toutes les clés primaires.

### UX Design Requirements

UX-DR1: Thème sombre par défaut (dark-only V1) — Material3 dark theme ; surface-base #121212, surface-raised #1E1E1E, surface-container #2C2C2C.
UX-DR2: Accueil = grille 2 colonnes de tuiles Jeu ; chaque tuile contient un logo 72×72dp centré et le nom du jeu.
UX-DR3: Accent unique ambre #FFB74D pour toutes les actions primaires (FilledButton) ; OutlinedButton pour les secondaires.
UX-DR4: Indicateur de somme en temps réel sur l'écran de Saisie : neutre si < 250, success (#4CAF82) si = 250, error (#CF6679) si > 250.
UX-DR5: Champ pré-rempli du dernier Participant avec teinte accent distincte et icône de calcul discrète.
UX-DR6: Lignes d'historique de manche colorées par fond suit-*-tint (spade #1C2024, heart #2D1517, club #182A1A, diamond #0E2440 ; révisé le 2026-10-20) + pictogramme enseigne aligné à droite.
UX-DR7: Badge Donneur (en accent) visible sur la ligne du joueur courant dans le Classement.
UX-DR8: Bottom sheet pour sélection de la position d'insertion d'un Participant en cours de Partie.
UX-DR9: Dialog de confirmation obligatoire pour actions destructives (Fin de partie, suppression Historique).
UX-DR10: Cibles tactiles ≥ 48dp sur tous les éléments interactifs ; bouton "Valider la Manche" et champs de score à 56dp.
UX-DR11: Reprise automatique : au lancement, si Partie IN_PROGRESS → navigation directe vers PartyScreen (bypass Accueil).
UX-DR12: Navigation linéaire Single Activity, back stack Android standard — pas de tabs, pas de drawer.
UX-DR13: Drag-to-reorder Participants (long-press + drag sur poignée) dans Création de Partie ; verrouillé après Manche 1.
UX-DR14: Swipe-to-delete dans l'Historique avec dialog de confirmation obligatoire.

### FR Coverage Map

| FR | Epic | Story |
|----|------|-------|
| FR-1 | Epic 2 | 2.1 |
| FR-2 | Epic 2 | 2.2 |
| FR-3 | Epic 2 | 2.3 |
| FR-4 | Epic 4 | 4.2 |
| FR-5 | Epic 3 | 3.1 |
| FR-6 | Epic 3 | 3.2 |
| FR-7 | Epic 3 | 3.3 |
| FR-8 | Epic 3 | 3.4 |
| FR-9 | Epic 3 | 3.4 |
| FR-10 | Epic 4 | 4.1 |
| FR-11 | Epic 4 | 4.3 |
| FR-12 | Epic 5 | 5.1 |
| FR-13 | Epic 5 | 5.1 |
| FR-14 | Epic 5 | 5.2 |
| FR-15 | Epic 6 | 6.1 |
| FR-16 | Epic 1 | 1.5 |

## Epic List

- Epic 1 : Fondation & Infrastructure
- Epic 2 : Création de Partie
- Epic 3 : Saisie de Manche
- Epic 4 : Suivi de Partie en cours
- Epic 5 : Clôture & Relance
- Epic 6 : Historique
- Epic 7 : Belote classique

---

## Epic 1: Fondation & Infrastructure

Mettre en place le projet Android, le système de design, la couche de données et l'infrastructure de navigation — sans cette fondation, aucune autre story ne peut être construite.

### Story 1.1: Scaffolding du projet Android

En tant que développeur,
Je veux créer le projet Android avec toutes les dépendances configurées,
Afin d'avoir une base compilable et testable sur laquelle toutes les autres stories peuvent s'appuyer.

**Acceptance Criteria:**

**Given** un nouveau projet Android créé
**When** le projet est buildé
**Then** il compile sans erreur avec Kotlin 2.0.x, AGP 8.x, minSdk 36
**And** Jetpack Compose BOM 2025.04.x est configuré
**And** Room 2.7.x, Hilt 2.52+, Navigation-Compose 2.8.x sont présents dans les dépendances
**And** la structure de packages (ui/, domain/, data/, game/, di/) est créée
**And** Hilt est initialisé (Application class annotée @HiltAndroidApp)
**And** une seule Activity annotée @AndroidEntryPoint est définie

---

### Story 1.2: Schéma de base de données Room

En tant que développeur,
Je veux créer le schéma Room complet avec toutes les entités et DAOs,
Afin que la couche de persistance soit prête à stocker l'état complet d'une Partie.

**Acceptance Criteria:**

**Given** le projet Android configuré (Story 1.1)
**When** la base de données est instanciée
**Then** les entités suivantes existent : PartyEntity (id UUID, gameId, status PartyState, currentDealerIndex, createdAt), ParticipantEntity (id UUID, partyId FK, name, listPosition), RoundEntity (id UUID, partyId FK, roundNumber, suit CardSuit), RoundScoreEntity (roundId FK, participantId FK, points Int)
**And** les DAOs existent : PartyDao, RoundDao, ParticipantDao avec les opérations INSERT, SELECT et les queries nécessaires
**And** AppDatabase est configurée avec les 4 entités
**And** PartyRepository interface et PartyRepositoryImpl sont créés
**And** le module Hilt DatabaseModule expose la base et les DAOs

---

### Story 1.3: Interface GameEngine & implémentation Papayoo

En tant que développeur,
Je veux créer l'interface GameEngine et son implémentation Papayoo,
Afin que la logique métier du jeu soit isolée et extensible pour les futurs jeux.

**Acceptance Criteria:**

**Given** la structure domain/ créée
**When** PapayooEngine est instancié
**Then** l'interface GameEngine existe dans domain/engine/ avec : gameId: String, minPlayers: Int (3), maxPlayers: Int (8), totalPointsPerRound: Int (250), precomputeLastScore(entered: Map<ParticipantId, Int>): Int, validateRound(scores: Map<ParticipantId, Int>): RoundValidation
**And** PapayooEngine implémente GameEngine dans game/papayoo/ avec gameId = "papayoo", min=3, max=8, total=250
**And** validateRound retourne succès si somme = 250, erreur avec l'écart sinon
**And** precomputeLastScore retourne 250 − somme des scores entrés
**And** le module Hilt EngineModule expose une Map<String, GameEngine> avec "papayoo" → PapayooEngine
**And** les use cases domain ne référencent que GameEngine, jamais PapayooEngine directement

---

### Story 1.4: Système de design — thème sombre & tokens

En tant qu'utilisateur,
Je veux une interface en thème sombre avec les couleurs et composants définis,
Afin que l'app soit lisible en soirée depuis la distance d'une table.

**Acceptance Criteria:**

**Given** l'app lancée
**When** n'importe quel écran est affiché
**Then** le thème Material3 dark est appliqué globalement (surface-base #121212)
**And** les tokens de couleur sont définis : surface-raised #1E1E1E, surface-container #2C2C2C, ink-primary #E8E0D5, accent #FFB74D, error #CF6679, success #4CAF82
**And** CardSuit enum existe dans domain/model/ avec SPADE, HEART, CLUB, DIAMOND
**And** CardSuitColors.kt dans ui/theme/ mappe chaque CardSuit vers : SPADE #D5DCE2, HEART #EF5350, CLUB #66BB6A, DIAMOND #42A5F5
**And** les teintes de fond suit-*-tint sont définies (SPADE #1C2024, HEART #2D1517, CLUB #182A1A, DIAMOND #0E2440)
**And** toutes les strings UI sont dans res/values/strings.xml (aucune string littérale dans le code)

---

### Story 1.5: Navigation graph & reprise automatique

En tant qu'utilisateur,
Je veux que l'app reprenne automatiquement une Partie en cours si j'ai fermé l'app sans clôturer,
Afin de ne jamais perdre ma Partie en cours.

**Acceptance Criteria:**

**Given** l'app est lancée
**When** aucune Partie IN_PROGRESS n'existe en base
**Then** la route initiale est HomeScreen

**Given** l'app est lancée
**When** une Partie avec status IN_PROGRESS existe en base
**Then** la route initiale est PartyScreen(partyId) sans passer par HomeScreen

**And** le NavGraph complet est défini avec toutes les routes : home, setup/{gameId}, party/{partyId}, roundEntry/{partyId}, results/{partyId}, history, partyDetail/{partyId}
**And** les Composables n'appellent jamais NavController directement — ils émettent des NavigationEvent vers leur ViewModel
**And** ResumePartyUseCase existe dans domain/usecase/ et interroge PartyRepository pour trouver une Partie IN_PROGRESS

---

## Epic 2: Création de Partie

Permettre à l'utilisateur de démarrer une nouvelle Partie en sélectionnant un Jeu, ajoutant ses amis et définissant l'ordre de table — le flux d'entrée pour toute session de jeu.

### Story 2.1: Écran d'accueil — grille de tuiles Jeux

En tant qu'utilisateur,
Je veux voir une grille de tuiles représentant les jeux disponibles sur l'écran d'accueil,
Afin de sélectionner facilement le jeu de la soirée.

**Acceptance Criteria:**

**Given** l'écran d'accueil affiché
**When** la liste des Jeux est chargée
**Then** une grille 2 colonnes affiche une tuile par Jeu disponible
**And** chaque tuile contient un logo 72×72dp centré et le nom du Jeu en dessous
**And** en V1 la tuile Papayoo est la seule tuile active
**And** tapper une tuile navigue vers GameSetupScreen avec le gameId correspondant
**And** un bouton/icône "Historique" est accessible en haut à droite
**And** le fond de l'écran utilise surface-base (#121212)

---

### Story 2.2: Création de Partie — ajout et réordonnancement des Participants

En tant qu'utilisateur,
Je veux ajouter mes amis et les réordonner pour refléter leur position autour de la table,
Afin que le premier de la liste soit le premier Donneur et que l'ordre de distribution suive la réalité physique.

**Acceptance Criteria:**

**Given** l'écran Création de Partie affiché
**When** l'utilisateur saisit un prénom et tape "Ajouter"
**Then** le Participant apparaît dans la liste ordonnée
**And** un prénom vide ou uniquement composé d'espaces est refusé (message d'erreur inline)

**Given** la liste des Participants
**When** l'utilisateur fait un long-press sur la poignée de drag d'une ligne
**Then** il peut réordonner la liste par glisser-déposer
**And** le premier de la liste est visuellement identifié comme futur Donneur de la Manche 1

**Given** la liste des Participants
**When** le nombre de Participants est inférieur à 3
**Then** le bouton "Commencer" est désactivé avec un compteur visible
**And** lorsque ≥ 3 Participants sont ajoutés, le bouton "Commencer" devient actif

**Given** la liste atteint 8 Participants
**When** l'utilisateur tente d'en ajouter un neuvième
**Then** le champ d'ajout est désactivé avec un message "Maximum 8 joueurs atteint"

**And** chaque Participant de la liste peut être supprimé avant confirmation

---

### Story 2.3: Confirmation de Partie & initialisation de l'état

En tant qu'utilisateur,
Je veux confirmer la création de Partie et accéder immédiatement à l'écran de jeu,
Afin de commencer à jouer sans délai.

**Acceptance Criteria:**

**Given** ≥ 3 Participants dans la liste
**When** l'utilisateur tape "Commencer"
**Then** une Partie est créée en base avec status = SETUP puis immédiatement IN_PROGRESS (Manche 1 débute)
**And** les Participants sont persistés avec leur listPosition
**And** currentDealerIndex = 0 (premier Participant)
**And** la navigation va vers PartyScreen(partyId)
**And** PartyScreen affiche "Manche 1 — Donneur : {nom du premier Participant}"
**And** CreatePartyUseCase et AddParticipantUseCase existent dans domain/usecase/

---

## Epic 3: Saisie de Manche

Le cœur de l'application : saisir les scores d'une Manche avec vérification anti-erreur automatique. C'est la fonctionnalité utilisée le plus fréquemment pendant une soirée.

### Story 3.1: Dé Virtuel — tirage de la Couleur du 7

En tant qu'utilisateur,
Je veux pouvoir lancer un dé virtuel pour déterminer la Couleur du 7 de la Manche,
Afin de ne pas avoir besoin d'un dé physique et de garder la couleur affichée pendant toute la saisie.

**Acceptance Criteria:**

**Given** l'écran de Saisie de Manche affiché
**When** l'utilisateur tape "Lancer"
**Then** une des 4 couleurs (SPADE, HEART, CLUB, DIAMOND) est sélectionnée aléatoirement avec distribution uniforme
**And** le résultat affiche le pictogramme de l'enseigne et son nom dans la couleur suit-* correspondante
**And** la Couleur du 7 reste affichée en permanence pendant toute la saisie des scores
**And** un bouton "Relancer" permet de relancer avant que des scores soient saisis
**And** la Couleur du 7 est stockée sur l'entité Round lors de la validation
**And** RollDieUseCase existe dans domain/usecase/

---

### Story 3.2: Saisie des scores — ordre libre & clavier numérique

En tant qu'utilisateur,
Je veux saisir les scores de chaque joueur dans l'ordre qui me convient avec un clavier numérique,
Afin de reporter les scores rapidement sans être contraint par un ordre imposé.

**Acceptance Criteria:**

**Given** l'écran de Saisie de Manche
**When** l'utilisateur tape sur un champ de score
**Then** le clavier numérique Android s'ouvre automatiquement
**And** chaque champ affiche le nom du Participant correspondant
**And** les champs acceptent des entiers ≥ 0 uniquement (score de 0 valide)
**And** l'utilisateur peut remplir les champs dans n'importe quel ordre
**And** un indicateur de somme affiche la somme courante en temps réel (neutre si < 250)
**And** les champs ont une hauteur minimum de 56dp

---

### Story 3.3: Pré-remplissage automatique du dernier Participant

En tant qu'utilisateur,
Je veux que le score du dernier joueur restant soit calculé automatiquement,
Afin de détecter immédiatement toute erreur de saisie et de gagner du temps.

**Acceptance Criteria:**

**Given** N-1 champs de score remplis sur N
**When** le N-ième champ est le seul vide
**Then** il est automatiquement pré-rempli avec 250 − (somme des N-1 scores saisis)
**And** le champ pré-rempli a une apparence distincte (teinte accent légère + icône de calcul)
**And** l'utilisateur peut modifier la valeur pré-remplie
**And** si la somme résultante > 250, l'indicateur passe en error (#CF6679) et le bouton "Valider" est désactivé
**And** si la somme = 250, l'indicateur passe en success (#4CAF82) et le bouton "Valider" s'active
**And** le pré-remplissage est recalculé en temps réel à chaque modification d'un champ

---

### Story 3.4: Validation de Manche & persistance

En tant qu'utilisateur,
Je veux valider les scores d'une Manche pour qu'ils soient définitivement enregistrés,
Afin de mettre à jour le classement et de passer à la Manche suivante.

**Acceptance Criteria:**

**Given** tous les champs de score remplis et somme = 250
**When** l'utilisateur tape "Valider la Manche"
**Then** un Round est inséré en base avec la Couleur du 7 et les RoundScores de chaque Participant
**And** les Scores Cumulés sont recalculés pour chaque Participant
**And** le Classement est mis à jour (trié par Score Cumulé croissant)
**And** currentDealerIndex est incrémenté (rotation circulaire)
**And** la navigation retourne vers PartyScreen
**And** une Manche validée ne peut plus être modifiée (aucun bouton d'édition sur une Manche passée)
**And** ValidateRoundUseCase existe dans domain/usecase/ et délègue à GameEngine.validateRound()

---

## Epic 4: Suivi de Partie en cours

Afficher le classement en temps réel, gérer le flux de la Partie et permettre l'ajout de joueurs en cours de route — l'écran central que tout le monde autour de la table peut consulter.

### Story 4.1: Écran Partie en cours — classement temps réel

En tant qu'utilisateur,
Je veux voir le classement mis à jour en permanence pendant la Partie,
Afin que tous les joueurs puissent consulter leur rang sans interaction.

**Acceptance Criteria:**

**Given** une Partie IN_PROGRESS
**When** PartyScreen est affiché
**Then** le classement affiche tous les Participants triés par Score Cumulé croissant
**And** chaque ligne affiche : rang, nom du Participant, Score Cumulé
**And** le Donneur de la Manche suivante est identifié par un badge accent sur sa ligne
**And** le numéro de Manche courante est affiché en sous-titre ("Manche {N}")
**And** le classement est lisible sans défilement pour 8 Participants sur écran standard
**And** les boutons "Nouvelle manche" (FilledButton accent) et "Fin de partie" (OutlinedButton) sont visuellement distincts en bas d'écran
**And** le classement se met à jour via StateFlow (observation de PartyRepository.observeParty())

---

### Story 4.2: Ajout de Participant en cours de Partie

En tant qu'utilisateur,
Je veux pouvoir ajouter un joueur qui arrive en retard pendant la Partie,
Afin qu'il puisse participer sans recommencer la Partie depuis le début.

**Acceptance Criteria:**

**Given** une Partie IN_PROGRESS avec < 8 Participants
**When** l'utilisateur tape le bouton d'ajout de Participant
**Then** un champ de saisie de prénom apparaît
**And** après saisie du prénom, un bottom sheet s'affiche avec la liste des positions d'insertion possibles
**And** l'utilisateur sélectionne la position souhaitée dans la liste
**And** le nouveau Participant est inséré à la position choisie avec Score Cumulé = 0
**And** si la position choisie est juste avant l'actuel prochain Donneur (entre le dernier Donneur et lui), le nouveau Participant devient le prochain Donneur; sinon le prochain Donneur prévu reste inchangé (personne ne perd son tour — révisé le 2026-10-18)
**And** le classement se met à jour immédiatement
**And** le bouton d'ajout est masqué/désactivé si le nombre de Participants = 8
**And** AddParticipantUseCase gère la logique d'insertion et de mise à jour de currentDealerIndex

---

### Story 4.3: Historique des Manches dans la Partie

En tant qu'utilisateur,
Je veux consulter le détail des Manches jouées pendant la Partie en cours,
Afin de vérifier un score passé si nécessaire.

**Acceptance Criteria:**

**Given** une Partie avec au moins 1 Manche validée
**When** l'utilisateur accède à la vue des Manches
**Then** chaque Manche passée est affichée en liste
**And** chaque ligne de Manche affiche : numéro de manche, scores de chaque Participant, fond coloré par suit-*-tint, pictogramme enseigne à droite
**And** la liste est en lecture seule (aucun bouton de modification)
**And** la somme des scores de chaque Manche est vérifiable visuellement

---

## Epic 5: Clôture & Relance

Permettre de clore une Partie proprement, visualiser les résultats finaux et démarrer une revanche en un minimum d'actions.

### Story 5.1: Clôture de Partie & résultats finaux

En tant qu'utilisateur,
Je veux clore la Partie quand je décide que c'est terminé et voir le classement final,
Afin de célébrer le vainqueur et sauvegarder la Partie dans l'historique.

**Acceptance Criteria:**

**Given** une Partie IN_PROGRESS
**When** l'utilisateur tape "Fin de partie"
**Then** un dialog de confirmation s'affiche ("Fin de partie ?") avec boutons Confirmer et Annuler

**Given** l'utilisateur confirme
**When** la clôture est validée
**Then** la Partie passe en status FINISHED et est persistée en base
**And** la navigation va vers ResultsScreen(partyId)
**And** ResultsScreen affiche le Classement final complet avec les Scores Cumulés
**And** le vainqueur (Score Cumulé le plus bas) est visuellement mis en avant
**And** les boutons "Rejouer" (FilledButton) et "Retour à l'accueil" (OutlinedButton) sont affichés
**And** il n'est plus possible d'ajouter de Manche à une Partie FINISHED
**And** ClosePartyUseCase existe dans domain/usecase/

---

### Story 5.2: Relance rapide — rejouer avec les mêmes joueurs

En tant qu'utilisateur,
Je veux démarrer une revanche avec les mêmes joueurs dans le même ordre en un seul tap,
Afin de relancer une Partie sans ressaisir tous les prénoms.

**Acceptance Criteria:**

**Given** l'écran Résultats finaux ou Détail de Partie (Historique)
**When** l'utilisateur tape "Rejouer"
**Then** une nouvelle Partie est créée avec les mêmes Participants dans le même ordre
**And** la nouvelle Partie est distincte de la précédente dans l'Historique
**And** le Jeu est identique à la Partie de référence
**And** la navigation va vers PartyScreen de la nouvelle Partie
**And** la nouvelle Partie peut démarrer en moins de 5 secondes depuis le tap

---

## Epic 6: Historique

Conserver et consulter toutes les Parties passées, permettant à l'utilisateur de retrouver des scores anciens et de rejouer avec un groupe déjà enregistré.

### Story 6.1: Écran Historique — liste des Parties passées

En tant qu'utilisateur,
Je veux consulter la liste de toutes mes Parties terminées,
Afin de retrouver les scores d'une soirée précédente ou relancer une Partie avec le même groupe.

**Acceptance Criteria:**

**Given** l'écran Historique affiché
**When** des Parties FINISHED existent en base
**Then** la liste affiche les Parties par ordre chronologique inverse
**And** chaque entrée affiche : date, Jeu, nombre de Manches, vainqueur et son Score Cumulé
**And** tapper une entrée navigue vers PartyDetailScreen(partyId)
**And** un swipe-to-delete déclenche un dialog de confirmation avant suppression
**And** la suppression est définitive et irréversible
**And** si aucune Partie FINISHED n'existe, un message "Aucune partie terminée pour l'instant." est affiché

---

### Story 6.2: Détail de Partie — scores par Manche avec couleur du 7

En tant qu'utilisateur,
Je veux consulter le détail complet d'une Partie passée Manche par Manche,
Afin de retrouver précisément les scores de chaque joueur à chaque tour et de relancer avec le même groupe.

**Acceptance Criteria:**

**Given** PartyDetailScreen affiché pour une Partie FINISHED
**When** l'écran se charge
**Then** le Classement final est affiché en haut (trié par Score Cumulé croissant, vainqueur mis en avant)
**And** en dessous, la liste des Manches est affichée dans l'ordre chronologique
**And** chaque ligne de Manche affiche le numéro, les scores de chaque Participant, le fond coloré par suit-*-tint, le pictogramme enseigne à droite
**And** la liste est en lecture seule
**And** un bouton "Rejouer avec ces joueurs" est disponible en bas (déclenche Story 5.2)

---

## Epic 7: Belote classique

Ajouter la Belote classique comme second Jeu : 4 joueurs en 2 Équipes nommées, scores par Équipe, saisie assistée de la Manche et fin de Partie au score cible ou au nombre de Manches. Règles de comptage : `planning-artifacts/games/belote.md`.

### Story 7.1: Moteur Belote — calcul d'une Manche et fin de Partie

En tant qu'utilisateur,
Je veux que l'application applique les règles de la Belote classique,
Afin de ne plus calculer à la main les cas fait, dedans, capot et litige.

**Acceptance Criteria:**

**Given** l'Équipe preneuse, les points de plis d'une Équipe (0 à 162), l'Équipe de la belote-rebelote éventuelle et un capot éventuel
**When** le moteur calcule la Manche
**Then** il renvoie les points de chaque Équipe selon le tableau de `belote.md` (fait, dedans à 162, capot à 252, belote toujours gardée)
**And** une égalité belote comprise produit un litige : la défense marque ses points et les plis du preneur passent en réserve
**And** la réserve, cumulée sur plusieurs litiges, revient à l'Équipe gagnante de la Manche suivante
**And** le moteur indique si la Partie est finie selon le mode (score cible ou nombre de Manches) et quelle Équipe gagne, une égalité finale imposant une Manche de plus
**And** le moteur expose le contrat de domaine `BeloteRules` (AD-8) sous la clé canonique `GameIds.BELOTE`, injectable, mais n'entre pas encore dans le catalogue des Jeux : l'Accueil et le Papayoo sont inchangés
**And** chaque règle est couverte par un test JVM

### Story 7.2: Données — Équipes, mode de fin et Manches de Belote

En tant qu'utilisateur,
Je veux que mes Parties de Belote soient enregistrées comme celles du Papayoo,
Afin de les reprendre après une fermeture de l'application et de les retrouver dans l'historique.

**Acceptance Criteria:**

**Given** le schéma Room en version 2
**When** l'application passe en version 3
**Then** une table des Équipes (nom, position) rattachée à la Partie existe, et chaque Participant peut appartenir à une Équipe
**And** la Partie mémorise son mode de fin et sa valeur (nuls pour le Papayoo)
**And** chaque Manche de Belote mémorise l'Équipe preneuse, les points de plis saisis, l'Équipe de la belote, le capot et les points obtenus par Équipe
**And** les Parties de Papayoo existantes sont migrées sans perte (`AppDatabaseMigrationTest` étendu)
**And** l'enregistrement d'une Manche de Belote se fait en une transaction

### Story 7.3: Création d'une Partie de Belote

En tant qu'utilisateur,
Je veux créer une Partie de Belote avec mes deux Équipes et la règle de fin,
Afin de commencer à jouer en quelques secondes.

**Acceptance Criteria:**

**Given** l'écran d'accueil
**When** il s'affiche
**Then** le moteur Belote entre dans le catalogue des Jeux (`@IntoMap`, AD-8) et une tuile Belote apparaît à côté du Papayoo, avec un logo au même gabarit (carré plein, bas libre pour le nom)

**Given** la tuile Belote touchée
**When** l'écran de création s'affiche
**Then** on saisit le nom des deux Équipes (« Nous » et « Eux » par défaut) et deux joueurs par Équipe
**And** on choisit la fin : score cible (1000 par défaut, modifiable) ou nombre de Manches (10 par défaut, modifiable)
**And** le premier Donneur est identifié, et l'ordre de table A1, B1, A2, B2 est visible
**And** la Partie ne peut être lancée qu'avec 4 noms renseignés et une valeur de fin valide

### Story 7.4: Saisie assistée d'une Manche de Belote

En tant qu'utilisateur,
Je veux saisir une Manche en indiquant seulement le preneur, les points d'une Équipe, la belote et le capot,
Afin d'obtenir les scores sans calcul ni erreur.

**Acceptance Criteria:**

**Given** une Partie de Belote en cours
**When** j'ouvre la saisie d'une Manche
**Then** je choisis l'Équipe preneuse, je saisis les points de plis d'une Équipe et ceux de l'autre sont déduits sur 162
**And** je peux attribuer la belote-rebelote à une Équipe et cocher un capot
**And** l'écran affiche avant validation le résultat (fait, dedans, capot ou litige) et les points de chaque Équipe, réserve comprise
**And** une valeur hors de 0 à 162 est refusée avec un message
**And** une Manche validée peut être corrigée depuis le tableau des Manches, avec les mêmes contrôles, et les Manches suivantes sont recalculées (réserve de litige comprise)

### Story 7.5: Partie en cours et clôture de Belote

En tant qu'utilisateur,
Je veux suivre le score des deux Équipes et voir la Partie se terminer d'elle-même,
Afin de savoir à tout moment qui mène et qui a gagné.

**Acceptance Criteria:**

**Given** une Partie de Belote en cours
**When** l'écran de Partie s'affiche
**Then** les deux Équipes, leurs joueurs et leur score cumulé sont visibles, l'Équipe en tête mise en avant
**And** la progression vers la fin est affichée (points restants vers la cible, ou Manche n sur N)
**And** une réserve de litige en attente est signalée avec son montant
**And** le tableau des Manches montre, par Manche, le preneur, le résultat et les points de chaque Équipe
**And** le rappel de distribution de la Belote est affiché

**Given** une Manche validée qui remplit la condition de fin
**When** elle est enregistrée
**Then** la Partie passe à l'état terminé et l'écran des résultats affiche l'Équipe gagnante et les scores finaux
**And** « Rejouer » relance une Partie avec les mêmes Équipes et la même règle de fin

### Story 7.6: Historique et statistiques de Belote

En tant qu'utilisateur,
Je veux retrouver mes Parties de Belote dans l'historique,
Afin de consulter leurs scores Manche par Manche.

**Acceptance Criteria:**

**Given** l'historique
**When** il contient des Parties de Belote
**Then** chaque ligne montre le Jeu, les noms d'Équipes, le score final et le vainqueur
**And** le détail affiche les Manches de Belote comme dans la Partie en cours, en lecture seule
**And** l'écran Statistiques affiche, pour la Belote, la courbe des scores par Équipe, et les cartes propres au Papayoo n'y apparaissent pas
