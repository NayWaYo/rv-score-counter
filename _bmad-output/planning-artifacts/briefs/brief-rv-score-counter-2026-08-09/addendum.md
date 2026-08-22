---
title: rv-score-counter — Addendum
status: final
created: 2026-08-09
updated: 2026-08-09
---

# Addendum

Contexte complémentaire au brief, destiné aux documents en aval (PRD, architecture) — ne fait pas partie du récit du brief lui-même.

## Organisation de la documentation par jeu

Convention à appliquer dès que plusieurs jeux sont supportés :

- **Documentation par jeu** — chaque jeu de l'application (Papayoo, puis Belote, etc.) a son propre document décrivant ses spécificités : règles de calcul des points, nombre de joueurs, conditions particulières de saisie, etc.
- **Documentation générale** — un document transverse couvre les spécifications communes à l'application, indépendantes du jeu joué : création de partie, gestion des participants, historique, relance de partie, navigation.

Objectif : garder la logique propre à un jeu isolée de la logique générale de l'application, pour que l'ajout d'un nouveau jeu (ex. Belote) n'impacte que sa propre documentation et pas le document général.
