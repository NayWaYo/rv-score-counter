---
title: rv-score-counter — Addendum PRD
created: 2026-08-09
updated: 2026-08-09
---

# Addendum PRD

Contexte et décisions complémentaires au PRD, destinés aux documents d'architecture et aux épics en aval.

---

## Convention de documentation par Jeu

*(Repris et précisé depuis l'addendum du brief produit)*

Dès que plusieurs Jeux sont supportés dans l'application, la documentation technique doit suivre cette organisation :

- **Document par Jeu** — chaque Jeu (Papayoo, puis Belote, etc.) dispose de son propre document spécifiant ses règles de calcul des points, le nombre de Participants autorisé, les contraintes de saisie propres au jeu (ex. Total Fixe, Couleur du 7 pour le Papayoo), et toute logique métier spécifique.
- **Document général** — un document transverse couvre les spécifications communes à l'application, indépendantes du Jeu : création de Partie, gestion des Participants, Historique, Relance, navigation entre écrans.

**Objectif :** isoler la logique propre à un Jeu de la logique générale, afin que l'ajout d'un nouveau Jeu (Belote en premier candidat) n'impacte que sa propre documentation et son propre module, sans toucher à l'application générale.

**Implication architecturale :** cette convention doit se refléter dans la structure du code dès la V1 (ex. module/package séparé par Jeu, interface commune que chaque Jeu implémente) — même si seul le Papayoo est livré en V1.

---

## Décisions techniques différées à l'architecture

Les points suivants ont été évoqués pendant la rédaction du PRD et sont à trancher à l'étape d'architecture :

- **Choix du moteur de stockage local** — Room (ORM Android) sur SQLite est le candidat naturel ; l'architecture valide ce choix ou en propose un autre.
- **Structure de données de la Partie en cours** — la reprise de Partie après fermeture de l'application (FR-16) impose une persistance de l'état en temps réel (pas uniquement à la clôture) ; la stratégie de sauvegarde (auto-save après chaque Manche ? en continu ?) est à définir à l'architecture.
- **Représentation des couleurs de carte** — les 4 couleurs (♠ ♥ ♣ ♦) ont des codes couleur à définir pour l'affichage (fond de ligne dans l'historique, pictogramme) ; choisir des teintes suffisamment contrastées pour une lisibilité en soirée.
