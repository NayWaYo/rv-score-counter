---
title: Document de jeu — Tarot
created: 2026-10-23
status: validated
references:
  - planning-artifacts/games/belote.md (même structure)
  - planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md (AD-8, AD-9)
---

# Tarot

Règles de comptage de la Fédération Française de Tarot (FFT), retenues avec l'utilisateur le 2026-10-23. Seule la logique propre au Tarot est décrite ici ; le reste (accueil, historique, thème, reprise, Joueurs) suit le document général. Dans l'application, une donne est une **Manche**, comme pour les autres Jeux.

## Joueurs

- De **3 à 5 joueurs**, scores tenus **par joueur**. Le Donneur tourne dans l'ordre de table à chaque Manche.
- À 5, le preneur appelle un roi : son **partenaire** est le joueur qui l'a, ou personne si le preneur s'est appelé lui-même (« seul »).

## Fin de Partie

Choisie à la création : **manuelle** (« Fin de partie », par défaut) ou **nombre de Manches** (la Partie s'arrête après la dernière). Le plus grand total gagne ; une égalité donne plusieurs gagnants.

## Saisie d'une Manche

Le preneur, le contrat, le partenaire (à 5), le nombre de **bouts** de l'attaque (0 à 3), les **points de l'attaque** (entiers, 0 à 91 ; ceux de la défense sont déduits), et les primes : petit au bout (attaque ou défense), poignée de l'attaque et poignée de la défense (simple, double ou triple), chelem.

## Contrat

| Contrat | Multiplicateur |
|---|---|
| Prise | × 1 |
| Garde | × 2 |
| Garde sans | × 4 |
| Garde contre | × 6 |

Points à faire selon les bouts de l'attaque : 0 bout → 56, 1 → 51, 2 → 41, 3 → 36. Le contrat est **fait** si les points de l'attaque atteignent ce seuil (égalité comprise), **chuté** sinon.

## Calcul

On calcule `S`, ce que l'attaque gagne contre **chaque** défenseur (négatif si elle perd) :

- **Contrat** : `(25 + |points − seuil|) × multiplicateur`, positif si fait, négatif si chuté.
- **Petit au bout** : `10 × multiplicateur`, au camp qui l'a mené au dernier pli, que le contrat soit fait ou non.
- **Poignées** : simple 20, double 30, triple 40, non multipliées ; chaque poignée montrée va au camp **qui gagne le contrat**, quel que soit le camp qui l'a montrée.
- **Chelem**, non multiplié : annoncé et réussi +400 ; réussi sans annonce +200 ; annoncé et raté −200 ; réussi par la défense −200.

Seuils de poignée (rappel affiché, non vérifié) : à 3 joueurs 13 / 15 / 18 atouts, à 4 joueurs 10 / 13 / 15, à 5 joueurs 8 / 10 / 13.

## Répartition

La somme des points d'une Manche est toujours nulle.

| Joueurs | Preneur | Partenaire | Chaque défenseur |
|---|---|---|---|
| 3 | 2 × S | — | −S |
| 4 | 3 × S | — | −S |
| 5 avec partenaire | 2 × S | S | −S |
| 5 seul | 4 × S | — | −S |

## Distribution (rappel affiché)

78 cartes distribuées 3 par 3. À 3 joueurs : 24 cartes chacun, chien de 6. À 4 : 18 chacun, chien de 6. À 5 : 15 chacun, chien de 3.

## Décisions confirmées (2026-10-23)

- 3, 4 et 5 joueurs ; à 5, partenaire ou « seul ».
- Toutes les primes : petit au bout, poignées, chelem.
- Points de l'attaque entiers, sans demi-point.
- Fin manuelle par défaut, ou après un nombre de Manches.
