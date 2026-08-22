---
name: rv-score-counter
status: final
created: 2026-08-09
updated: 2026-08-09
sources:
  - _bmad-output/planning-artifacts/prds/prd-rv-score-counter-2026-08-09/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/ux-designs/ux-rv-score-counter-2026-08-09/DESIGN.md
---

# rv-score-counter — Experience Spine

## Foundation

Surface unique : Android natif (API 36). Système UI : Jetpack Compose + Material3. `DESIGN.md` est la référence d'identité visuelle — cette spine est l'expérience. Thème sombre par défaut, sans option de thème clair en V1. Aucune connectivité réseau requise. Application mono-activité, navigation Compose linéaire (back stack Android standard), sans tabs.

Contexte d'usage primaire : téléphone posé au centre d'une table, consulté par plusieurs personnes à distance (~50 cm), en lumière de soirée. Les interactions sont pilotées par une seule personne (le tenancier du score) ; les autres sont en lecture seule.

## Information Architecture

| Surface | Atteinte depuis | Rôle |
|---------|-----------------|------|
| **Accueil** | Lancement app (si aucune Partie en cours) | Sélection du Jeu (grille de tuiles) + accès Historique |
| **Reprise de Partie** | Lancement app (si Partie IN_PROGRESS) | Navigation automatique, bypass Accueil |
| **Création de Partie** | Tap sur tuile Jeu | Ajout et réordonnancement des Participants |
| **Partie en cours** | Validation de la Création / Reprise | Classement temps réel, donneur, actions de manche |
| **Saisie de Manche** | Bouton "Nouvelle manche" | Dé Virtuel + saisie des scores |
| **Résultats finaux** | Bouton "Fin de partie" (confirmé) | Classement final, rejouer, retour accueil |
| **Historique** | Bouton Historique depuis Accueil | Liste des Parties passées |
| **Détail de Partie** | Tap sur une entrée Historique | Scores par Manche + Classement final |

Navigation : back stack Android natif. Aucune bottom bar, aucun drawer. La navigation est strictement linéaire sauf l'accès à l'Historique depuis l'Accueil (push) et le retour depuis les Résultats finaux (retour à l'Accueil, pas à la Partie).

**Règle de reprise :** au lancement, si une Partie `IN_PROGRESS` existe en base, la route initiale est `PartyScreen(partyId)` — l'Accueil n'est pas affiché.

## Voice and Tone

Microcopy. Voix de marque et posture dans `DESIGN.md.Brand & Style`.

| Faire | Éviter |
|-------|--------|
| "Manche 4 — Donneur : Alice" | "C'est parti ! Alice doit distribuer" |
| "Total : 270 — corrigez les scores pour atteindre 250." | "Erreur ! Somme incorrecte." |
| "Fin de partie ?" (dialog, question courte) | "Voulez-vous vraiment mettre fin à cette partie ?" |
| "Rejouer avec les mêmes joueurs" | "Recommencer" (ambigu) |
| Noms des joueurs tels que saisis | Majuscules forcées, troncature |
| Chiffres bruts pour les scores | Icônes de trophée, animations de célébration |

Pas de gamification. Pas d'encouragements. L'app est un arbitre, pas un animateur.

## Component Patterns

Comportemental. Spécifications visuelles dans `DESIGN.md.Components`.

| Composant | Usage | Règles comportementales |
|-----------|-------|------------------------|
| **Tuile Jeu** | Accueil | Tap → Création de Partie. Pas de long-press. Grisée + "Bientôt" si jeu non disponible. [ASSUMPTION : pas de grisage en V1 — Papayoo uniquement disponible.] |
| **Ligne de classement** | Partie en cours | Triée par Score Cumulé croissant après chaque Manche validée. Badge Donneur sur le joueur dont c'est le tour. Pas de réordonnancement manuel. |
| **Champ de score** | Saisie de Manche | Clavier numérique systématique. Focus auto sur le premier champ vide. Le N-ième champ se pré-remplit dès que N-1 champs ont une valeur. Pré-remplissage modifiable. |
| **Indicateur de somme** | Saisie de Manche | Affiche la somme courante en temps réel. Couleur success si = 250, error si > 250, neutre sinon. |
| **Dé Virtuel** | Saisie de Manche | Affiché en haut de l'écran. Résultat persistant tout au long de la saisie. Relancer possible avant la validation. |
| **Dialog de confirmation** | Fin de partie, Suppression historique | Deux actions : confirmer (danger/primary) + annuler. Pas de checkbox "ne plus demander". |
| **Ligne d'historique de manche** | Détail de Partie | Fond teinté selon Couleur du 7. Pictogramme enseigne extrême droite. Lecture seule. |
| **Drag-to-reorder** | Création de Partie (avant Manche 1) | Long-press + drag sur la poignée de réordonnancement. Verrouillé après validation de la Manche 1. |
| **Position picker** | Ajout Participant en cours de Partie | Bottom sheet ou dialog : liste des positions d'insertion possibles entre les joueurs existants. Bouton de confirmation. |

## State Patterns

| État | Surface | Traitement |
|------|---------|------------|
| Lancement avec Partie en cours | App open | Navigation directe vers Partie en cours. Aucun écran intermédiaire. |
| Lancement sans Partie en cours | App open | Accueil (grille de tuiles). |
| Création : moins de 3 participants | Création de Partie | Bouton "Commencer" désactivé. Compteur de participants visible. |
| Saisie : somme < 250, N-1 champs remplis | Saisie de Manche | Pré-remplissage du dernier champ. Indicateur neutre. |
| Saisie : somme = 250 | Saisie de Manche | Indicateur success. Bouton "Valider" actif. |
| Saisie : somme > 250 | Saisie de Manche | Indicateur error. Bouton "Valider" désactivé. Message : "Total : {n} — corrigez pour atteindre 250." |
| Validation réussie | Saisie de Manche | Retour vers Partie en cours. Classement mis à jour immédiatement. |
| Partie vide (aucune manche) | Partie en cours | "Lancez la première manche pour commencer." Bouton "Nouvelle manche" proéminent. |
| Historique vide | Historique | "Aucune partie terminée pour l'instant." Pas de suggestion d'action (l'historique est passif). |
| Suppression d'entrée Historique | Historique | Swipe-to-delete + dialog de confirmation. Suppression irréversible. |
| Ajout Participant : max atteint (8) | Partie en cours | Bouton d'ajout masqué ou désactivé avec tooltip "Maximum de 8 joueurs atteint". |
| Reprise après Fin de partie | Résultats finaux | Tap "Rejouer" → nouvelle Création de Partie pré-chargée. La Partie précédente est déjà en Historique. |

## Interaction Primitives

- **Tap** — action principale. Toutes les actions sont déclenchées par tap simple.
- **Long-press** — drag-to-reorder des Participants uniquement (dans Création de Partie, avant Manche 1). Non utilisé ailleurs.
- **Swipe-to-delete** — uniquement dans l'Historique (pattern natif Android, confirmation obligatoire).
- **Clavier numérique** — déclenché automatiquement sur focus d'un champ de score. Pas de clavier alphanumérique dans les champs de score.
- **Bottom sheet** — pour la sélection de position d'insertion d'un Participant en cours de Partie.
- **Dialog** — pour les confirmations destructives uniquement (Fin de partie, Suppression historique).
- **Interdit :** carousels, hero animations au lancement, célébrations animées de victoire, notifications push, gestes de swipe inter-écrans (le back Android standard suffit).

## Accessibility Floor

Comportemental. Contrastes visuels dans `DESIGN.md`.

- **TalkBack :** chaque élément interactif annoté avec rôle + état. Le badge Donneur annonce "Donneur" à la lecture. Les champs de score annoncent le nom du joueur associé.
- **Dynamic Type :** tokens typographiques Material3 honorés. Aucun texte porteur d'information ne tronque aux réglages d'accessibilité élevés.
- **Reduce Motion :** aucune animation de transition entre les chiffres du classement. Mise à jour instantanée.
- **Cibles tactiles :** minimum 48dp ; champs de score et bouton "Valider la Manche" à 56dp.
- **Ordre de focus :** suit l'ordre de lecture naturel (rang → nom → score). Sur l'écran de Saisie, focus suit l'ordre des joueurs dans la liste, dernier champ en dernier.
- **Lisibilité à distance :** contrastes `ink-primary` sur `surface-container` respectent WCAG AA au minimum — contrainte d'usage (lecture à 50 cm en lumière d'ambiance variable).

## Écrans spécifiques — règles comportementales

### Accueil
Grille 2 colonnes de tuiles Jeu. En V1, une seule tuile (Papayoo) affichée et active. Bouton "Historique" en haut à droite (icône). Pas d'état vide — la grille est toujours peuplée d'au moins un jeu disponible.

### Création de Partie
Liste ordonnée des Participants avec poignées de drag. Champ de saisie de prénom + bouton "Ajouter". Le premier de la liste est désigné Donneur de la Manche 1 — indicateur visible (badge ou annotation). Bouton "Commencer" en bas, désactivé < 3 joueurs.

### Partie en cours
Tableau de classement full-width en haut (scroll si > 8 joueurs). Indicateur "Manche X — Donneur : {nom}" en sous-titre. Deux boutons en bas : "Nouvelle manche" (primaire) et "Fin de partie" (secondaire). Accès au détail des manches passées via section dépliable ou lien discret.

### Saisie de Manche
En haut : résultat du Dé Virtuel (enseigne + couleur) avec bouton "Relancer". Centre : liste des champs de score, un par joueur, avec son nom. Indicateur de somme courante en temps réel. En bas : bouton "Valider la Manche" (désactivé si somme ≠ 250 ou champs vides).

### Résultats finaux
Podium simple (1er, 2e, 3e visuellement mis en avant) puis liste complète. Scores cumulés finaux. Deux boutons : "Rejouer" (primaire) et "Retour à l'accueil" (secondaire).

### Historique
Liste chronologique inverse des Parties terminées. Chaque entrée : date, Jeu, nombre de manches, vainqueur et score. Tap → Détail. Swipe-to-delete. Bouton "Rejouer" sur chaque entrée (ou dans le Détail).

### Détail de Partie
Tableau de classement final en haut. En dessous : liste des manches, chaque ligne colorée par la Couleur du 7 tirée (fond teinté + pictogramme enseigne à droite). Lecture seule. Bouton "Rejouer avec ces joueurs" en bas — déclenche le même flow que FR-14 (nouvelle Partie pré-chargée).

## Key Flows

### Flow 1 — Rémy lance une partie de Papayoo (UJ-1)

1. Rémy ouvre l'app — aucune Partie en cours → Accueil s'affiche.
2. Il tape la tuile Papayoo.
3. Écran Création : il saisit les prénoms de ses 4 amis un par un.
4. Il fait glisser les lignes pour refléter l'ordre autour de la table.
5. Il tape "Commencer" (5 joueurs, bouton actif).
6. **Climax :** Partie en cours s'affiche — classement vide, "Manche 1 — Donneur : Rémy" visible. La partie peut commencer.

Cas limite : Rémy oublie un joueur et revient en arrière depuis l'écran Partie (Manche 1 pas encore saisie) → le bouton d'édition de liste est encore accessible.

---

### Flow 2 — Rémy saisit les scores de la manche 3 (UJ-2)

1. Depuis Partie en cours, Rémy tape "Nouvelle manche".
2. Écran Saisie — le Dé Virtuel est en attente. Il tape "Lancer".
3. Résultat : ♥ affiché en rouge. Il saisit les scores dans l'ordre qui lui convient.
4. Dès le 4e score saisi (sur 5), le champ de Sophie se pré-remplit avec 250 − somme.
5. L'indicateur passe au vert (total = 250). Il tape "Valider la Manche".
6. **Climax :** retour vers Partie en cours — le classement est mis à jour. La ligne de Manche 3 apparaîtra dans l'historique avec un fond rose teinté.

Cas limite : un score est erroné — l'indicateur reste en rouge (total ≠ 250) et le bouton "Valider" reste désactivé. Rémy corrige avant de valider.

---

### Flow 3 — Un ami rejoint la partie en cours (FR-4)

1. Depuis Partie en cours, Rémy tape le bouton d'ajout de joueur (visible si < 8 joueurs).
2. Il saisit le prénom du nouvel arrivant.
3. Un bottom sheet s'ouvre avec la liste des positions possibles entre les joueurs.
4. Il choisit la position — par exemple juste après l'actuel prochain Donneur.
5. **Climax :** le nouvel arrivant apparaît dans le classement avec 0 point. Il sera Donneur de la prochaine manche.

---

### Flow 4 — Le groupe veut une revanche (UJ-3)

1. Depuis les Résultats finaux, Rémy tape "Rejouer".
2. L'écran Création s'ouvre pré-chargé avec les mêmes joueurs dans le même ordre.
3. Un tap "Confirmer" suffit.
4. **Climax :** Partie en cours de la nouvelle partie s'affiche en moins de 5 secondes.

---

### Flow 5 — Sophie consulte le classement (UJ-4)

Sophie regarde l'écran posé au centre de la table. Le classement est visible en permanence sur l'écran Partie en cours, sans interaction nécessaire. Elle voit son rang et son score d'un coup d'œil. Elle ne touche pas au téléphone.
