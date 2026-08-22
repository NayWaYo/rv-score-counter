---
title: rv-score-counter — Brief Produit
status: final
created: 2026-08-09
updated: 2026-08-09
---

# Product Brief: rv-score-counter

## Résumé Exécutif

rv-score-counter est une application Android qui remplace le papier et le stylo pour compter les points lors de parties de jeux de cartes entre amis. Un seul téléphone reste posé au centre de la table ; à chaque manche, le score de chaque joueur est saisi (dans un ordre quelconque), et l'application maintient le cumul, l'historique et l'ordre des joueurs autour de la table.

Le point de départ est le **Papayoo**, un jeu de plis à levées négatives pour 3 à 8 joueurs, dont le calcul manuel des scores est source d'erreurs répétées en cours de soirée. L'application vise d'abord un usage personnel — la couverture d'un seul jeu, sans compte ni réseau — avec une architecture assez propre pour, si l'usage le justifie, étendre le catalogue de jeux (ex : la Belote) et envisager une publication sur le Play Store.

## Le Problème

En soirée jeux de cartes, le calcul des points est fait à la main : addition papier, ou tableur improvisé sur un téléphone. Deux problèmes reviennent systématiquement :

- **Erreurs de saisie non détectées** — une manche mal recopiée ou mal additionnée fausse tout le reste de la partie, souvent découvert bien plus tard (voire jamais).
- **Frictions de mise en place répétées** — à chaque nouvelle partie avec le même groupe d'amis, il faut renoter qui joue, dans quel ordre autour de la table, et qui distribue.

Le coût est faible mais réel : temps perdu, disputes sur les scores, désengagement des joueurs les moins à l'aise avec le calcul mental.

## La Solution

Une application à écran unique, pensée pour être posée au centre de la table :

- **Création de partie** — ajout des participants (3 à 8 pour le Papayoo), réordonnancement pour refléter la disposition physique autour de la table. Le donneur de la manche 1 est le premier participant de cet ordre ; la distribution suit ensuite ce même ordre pour les manches suivantes.
- **Tirage de la couleur du 7** — à chaque manche, un dé désigne une couleur (pique, cœur, trèfle, carreau) dont le 7 vaut 40 points. L'application propose un lancer de dé virtuel pour les tables sans dé physique.
- **Saisie de manche assistée** — à chaque manche, le total des points distribués entre tous les joueurs est fixe (250 points). Le score de chaque joueur est saisi dans un ordre libre ; l'application pré-remplit le score du dernier joueur restant par complément à 250, ce qui permet de vérifier qu'aucune erreur de saisie n'a été commise.
- **Fin de partie décidée par les joueurs** — au Papayoo il n'y a pas de limite de manches théorique : les joueurs relancent des manches tant qu'ils veulent. Un bouton "Fin de partie" distinct du bouton "Nouvelle manche" permet de clore la partie à tout moment.
- **Historique local** — les parties passées (scores, classement, participants) sont conservées sur l'appareil, sans compte ni synchronisation.
- **Relance rapide** — à la fin d'une partie, possibilité d'en relancer une nouvelle avec les mêmes participants et le même ordre de table, sans ressaisie.

## Ce Qui Différencie ce Projet

Pas de prétention à un avantage concurrentiel : de nombreux compteurs de scores existent déjà. La différence tient à l'usage réel et récurrent visé — coller précisément aux habitudes de jeu de l'auteur et de son groupe d'amis (Papayoo d'abord, vérification anti-erreur, relance rapide) plutôt qu'à un compteur générique. Si l'usage prouve sa valeur, l'architecture doit permettre d'ajouter d'autres jeux (Belote en premier candidat) sans réécriture.

## À Qui Ça S'adresse

**Utilisateur principal** — l'auteur de l'app, joueur régulier de Papayoo (et à terme d'autres jeux de cartes) avec un groupe d'amis stable. Pose son téléphone au centre de la table et saisit les scores à chaque manche. Veut passer le moins de temps possible sur l'admin de la partie (mise en place, vérification) et le plus de temps sur le jeu.

**Utilisateurs secondaires** — les autres joueurs à table, qui consultent occasionnellement l'écran pour voir le classement en cours, mais ne saisissent pas de scores eux-mêmes.

## Critères de Succès

- L'application est utilisée en remplacement complet du papier lors des prochaines parties de Papayoo de l'auteur.
- Le mécanisme de vérification (score du dernier joueur proposé) permet de détecter au moins une erreur de saisie qui serait passée inaperçue sur papier.
- Recréer une partie avec le même groupe prend moins de temps que ressaisir les participants à la main.
- (Si publication envisagée plus tard) au moins un groupe d'amis externe adopte l'app pour ses propres parties.

## Périmètre

**Dans le périmètre (V1)**
- Une partie est toujours liée à un seul type de jeu (V1 : Papayoo uniquement) ; pas de partie mixant plusieurs jeux.
- Création de partie : choix du jeu, ajout des participants (3 à 8 pour le Papayoo), réordonnancement selon la position à table ; le premier de la liste est le donneur de la manche 1, rotation du donneur selon ce même ordre pour les manches suivantes.
- Lancer de dé virtuel pour déterminer la couleur du 7 (40 points) de la manche, en alternative à un dé physique.
- Saisie des scores manche par manche (total fixe de 250 points par manche), dans un ordre de saisie libre, avec pré-remplissage du score du dernier joueur restant par complément à 250.
- Cumul des scores et classement en cours de partie, sur un nombre de manches illimité.
- Bouton "Fin de partie" (à côté du bouton "Nouvelle manche") pour clore la partie à la décision des joueurs, sans condition automatique.
- Historique local des parties (aucun compte, aucune synchronisation cloud).
- Relance d'une nouvelle partie avec les mêmes participants et le même ordre de table.

**Hors périmètre (V1)**
- Autres jeux de cartes (Belote et suivants) — prévus pour une itération ultérieure comme jeux additionnels sélectionnables, pas dans le premier livrable, et jamais combinés dans une même partie.
- Comptes utilisateurs, multi-appareils, synchronisation ou partage réseau.
- Publication Play Store — pas un objectif de la V1 ; à réévaluer selon l'usage réel.
- Règles automatiques de distribution des plis, gestion des paquets de cartes physiques, ou calcul du détail du jeu (l'app compte les scores fournis, elle ne joue pas les cartes).

## Vision

À terme, si l'usage confirme la valeur de l'application : un compteur de scores multi-jeux (Papayoo, Belote, et d'autres jeux du groupe), toujours local-first, éventuellement publié pour d'autres groupes d'amis qui vivent le même problème. Le succès se mesure à l'usage réel en soirée, pas à l'adoption externe — tout le reste est un bonus.
