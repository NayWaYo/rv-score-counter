---
name: rv-score-counter
description: Application Android de comptage de points pour jeux de cartes. Fond sombre par défaut, sobre, lisible à distance de table. L'identité de chaque jeu est portée par une tuile dédiée avec logo sur l'écran d'accueil. Les couleurs du jeu de cartes (enseignes) colorent uniquement les lignes d'historique de manches.
status: final
colors:
  # Surfaces (thème sombre — défaut confirmé)
  surface-base: '#121212'
  surface-raised: '#1E1E1E'
  surface-container: '#2C2C2C'
  surface-container-high: '#363636'
  # Textes
  ink-primary: '#E8E0D5'
  ink-secondary: '#9E9890'
  ink-disabled: '#5E5B55'
  # Texte atténué : pire score, indications, badges neutres (jamais une erreur)
  ink-muted: '#A8A097'
  # Accent principal (actions primaires et podium uniquement)
  accent: '#FFB74D'
  accent-on: '#1A1200'
  # États
  error: '#CF6679'
  on-error: '#1A000D'
  success: '#4CAF82'
  # Enseignes (CardSuit) — usage strict : historique manches + résultat dé
  suit-spade: '#D5DCE2'
  suit-heart: '#EF5350'
  suit-club: '#66BB6A'
  suit-diamond: '#42A5F5'
  # Teintes de fond pour lignes d'historique (suit couleur à faible opacité)
  suit-spade-tint: '#1C2024'
  suit-heart-tint: '#2D1517'
  suit-club-tint: '#182A1A'
  suit-diamond-tint: '#0E2440'
typography:
  display:
    note: 'Material3 DisplaySmall — scores cumulés mis en valeur'
  title:
    note: 'Material3 HeadlineMedium — titres d'écran, noms de joueurs'
  body:
    note: 'Material3 BodyLarge — contenu courant, listes, champs'
  label:
    note: 'Material3 LabelLarge — boutons, badges'
  meta:
    note: 'Material3 BodySmall — infos secondaires, numéros de manche'
rounded:
  sm: 8dp
  md: 16dp
  lg: 24dp
  tile: 20dp
spacing:
  '1': 4dp
  '2': 8dp
  '3': 12dp
  '4': 16dp
  '5': 24dp
  '6': 32dp
  '7': 48dp
theme-mode: dark-only    # V1 : thème sombre uniquement. Pas de variante claire. À réévaluer si publication Play Store.
components:
  game-tile:
    shape: rounded/tile
    background: surface-raised
    note: 'Logo centré (72×72dp), nom du jeu en label sous le logo. Grille 2 colonnes.'
  scoreboard-row:
    background: surface-container
    note: 'Rang à gauche (meta), nom joueur (body), score cumulé à droite (title). Badge Donneur en accent.'
  score-field:
    shape: rounded/sm
    note: 'Champ numérique pleine largeur, clavier numérique Android. Dernier champ pré-rempli en teinte accent.'
  die-roller:
    note: 'Bouton large (min 56dp hauteur). Résultat affiché : pictogramme enseigne + nom en couleur suit-*.'
  round-history-row:
    note: 'Fond suit-*-tint selon Couleur du 7 de la manche. Pictogramme enseigne aligné à droite.'
  primary-button:
    note: 'FilledButton Material3, couleur accent. "Valider la Manche", "Confirmer".'
  secondary-button:
    note: 'OutlinedButton Material3. "Fin de partie", "Annuler".'
  danger-button:
    note: 'TextButton couleur error — uniquement pour confirmer une action destructive.'
---

## Brand & Style

rv-score-counter n'est pas un jeu — c'est un arbitre discret posé au centre de la table. Son rôle est d'être là, lisible, fiable, et de ne pas voler la vedette à la partie qui se joue.

Le fond sombre est une contrainte d'usage, pas un choix décoratif : les soirées jeux se déroulent souvent en lumière tamisée. La typographie est généreuse, les contrastes élevés, les cibles tactiles larges — l'écran doit se lire d'un coup d'œil depuis n'importe quel angle de table, sans avoir à le ramasser.

L'identité visuelle de chaque jeu est portée par sa tuile sur l'accueil (logo + couleur d'accentuation propre au jeu). Le reste de l'interface est intentionnellement neutre : une couleur accent unique (ambre chaud) pour les actions principales, et les enseignes de cartes (♠ ♥ ♣ ♦) utilisées uniquement là où le jeu l'exige — dans l'historique des manches et le résultat du dé. Jamais comme décoration générale.

## Colors

- **Surface base (`#121212`)** — fond canonique Material3 dark. Tout part de là.
- **Surface raised (`#1E1E1E`)** — tuiles, cartes, éléments élevés.
- **Surface container (`#2C2C2C`)** — lignes de tableau de bord, champs de saisie.
- **Ink primary (`#E8E0D5`)** — texte principal, légèrement chaud pour réduire la fatigue oculaire.
- **Accent (`#FFB74D`)** — actions primaires uniquement. Ambre chaud : évoque le feutre d'un tapis de jeu sans tomber dans le casino kitsch.
- **Enseignes** — réservées strictement à deux contextes : le pictogramme + résultat du dé, et le fond teinté des lignes d'historique de manche. Jamais dans la chrome UI.

À éviter : gradients, surfaces colorées derrière du texte, rouge pour les erreurs dans un contexte de scores (un score négatif n'est pas une erreur).

## Typography

Conventions Material3 natives. Pas de police personnalisée en V1.

- Scores cumulés importants affichés en `display` — le chiffre doit sauter aux yeux depuis 50 cm.
- Noms de joueurs en `title`.
- Numéros de manche, infos secondaires en `meta`.
- Dynamic Type Android honoré — pas de troncature aux réglages d'accessibilité élevés.

## Layout & Spacing

Grille 4dp. Marges horizontales : 16dp (convention Android). Espacement vertical entre sections : 24dp minimum.

Règle de lisibilité à distance : tout texte porteur d'information (rang, score, nom de joueur) ≥ `body` (16sp). Les `meta` sont réservés aux infos consultées délibérément (numéros de manche, date en historique).

Cibles tactiles minimum : 48dp de hauteur (Android). Les champs de saisie de scores et le bouton "Valider la Manche" visent 56dp pour une frappe fiable sans ramasser le téléphone.

## Elevation & Depth

Elevation Material3 par défaut. Les tuiles jeux et les cartes de classement utilisent le tonal surface system (pas d'ombres portées sur fond sombre — contre-productif). L'élévation est marquée par la différence de teinte surface-base → surface-raised → surface-container.

## Shapes

- Tuiles jeux : `rounded/tile` (20dp) — large, mémorisable, "joueur".
- Cards, panneaux : `rounded/md` (16dp).
- Champs de saisie, lignes : `rounded/sm` (8dp).
- Pas de formes entièrement rondes pour les surfaces. Les boutons FAB ne sont pas utilisés — navigation linéaire.

## Components

**Tuile Jeu (game-tile)**
Surface raised, coins tile. Logo centré 72×72dp, nom du jeu en label centré dessous. Grille 2 colonnes à l'accueil. Chaque jeu peut surcharger une couleur d'accentuation de tuile propre [ASSUMPTION : à définir jeu par jeu — Papayoo V1 utilise l'accent global].

**Ligne de classement (scoreboard-row)**
Fond surface-container. Trois zones horizontales : rang (meta, gauche), nom joueur (body, centre-gauche avec flex), score cumulé (title, droite). Badge "Donneur" en accent, petit, à droite du nom. Hauteur 56dp minimum.

**Champ de score (score-field)**
Pleine largeur, fond surface-container, coins sm. Clavier numérique Android (`inputType="number"`). Le champ pré-rempli (dernier joueur) a un fond en teinte accent très légère et une icône de calcul discrète. Si la somme dépasse 250, tous les champs passent en border error sans remplissage rouge.

**Dé Virtuel (die-roller)**
Grand bouton centré, hauteur 56dp. Avant lancer : icône de dé + "Lancer". Après lancer : pictogramme enseigne (♠/♥/♣/♦) + nom de la couleur, affichés dans la couleur `suit-*` correspondante. Bouton secondaire "Relancer" texte.

**Ligne d'historique de manche (round-history-row)**
Fond `suit-*-tint` selon la Couleur du 7. Numéro de manche (meta, gauche), scores joueurs (body), pictogramme enseigne (24dp, extrême droite). Hauteur 48dp.

**Boutons d'action**
- Primaire (FilledButton) : accent, `label`, coins md. "Valider la Manche", "Confirmer".
- Secondaire (OutlinedButton) : border ink-secondary, texte ink-primary. "Fin de partie", "Retour".
- Danger (TextButton) : couleur error. Confirmation de clôture uniquement, dans un dialog.

## Do's and Don'ts

| Do | Don't |
|---|---|
| Texte porteur d'info ≥ 16sp, lisible à 50 cm | Miniaturiser les scores pour faire tenir plus de contenu |
| Cibles tactiles ≥ 48dp, idéalement 56dp pour les actions fréquentes | Buttons trop petits, groupés sans espace de séparation |
| Couleurs enseignes strictement dans l'historique et le résultat du dé | Enseigne rouge sur les erreurs, ou comme décoration générale |
| Un seul accent (ambre) pour toutes les actions primaires | Multi-accent, dégradés, couleurs saturées dans la chrome |
| Dialog de confirmation avant toute action destructive | Clôturer une partie sans confirmation |
| Fond teinté pour les lignes de manche — discret, pas flashy | Fond enseigne saturé sur toute la ligne |
