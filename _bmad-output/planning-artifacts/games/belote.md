---
title: Document de jeu — Belote classique
created: 2026-10-21
status: validated
references:
  - planning-artifacts/briefs/brief-rv-score-counter-2026-08-09/addendum.md (un document par Jeu)
  - planning-artifacts/architecture/architecture-rv-score-counter-2026-08-09/ARCHITECTURE-SPINE.md (AD-2, contrat GameEngine)
---

# Belote classique

Règles de comptage retenues avec l'utilisateur le 2026-10-21. Seule la logique propre à la Belote est décrite ici ; le reste (accueil, historique, thème, reprise) suit le document général.

## Joueurs et Équipes

- Toujours **4 joueurs**, en **2 Équipes nommées** de 2. Noms par défaut : « Nous » et « Eux », modifiables à la création.
- Ordre de table : A1, B1, A2, B2 (les coéquipiers sont face à face). Le Donneur tourne dans cet ordre à chaque Manche.
- Les scores sont tenus **par Équipe**, jamais par joueur.

## Fin de Partie

Choisie à la création :

| Mode | Valeur par défaut | Fin |
|---|---|---|
| Score cible | 1000 (modifiable) | À la fin d'une Manche où au moins une Équipe atteint la cible. Si les deux l'atteignent, la plus haute gagne. |
| Nombre de Manches | 10 (modifiable) | Après la dernière Manche, l'Équipe la plus haute gagne. |

En cas d'égalité parfaite à la fin, on joue une Manche de plus. Une réserve de litige encore en attente à la fin de la Partie est perdue.

## Comptage d'une Manche

Total des plis : **162** points (152 de cartes + 10 de der). Pas d'annonces, sauf la **belote-rebelote** (20 points). Pas d'arrondi : les points sont comptés exacts.

Saisie assistée : on indique l'Équipe preneuse, les points de plis d'**une** Équipe (l'autre est déduite sur 162), l'Équipe qui a annoncé la belote-rebelote s'il y en a une, et un éventuel capot. L'application calcule le reste.

Soit `P` le total du preneur et `D` celui de la défense, belote-rebelote comprise.

| Cas | Condition | Preneur marque | Défense marque |
|---|---|---|---|
| Fait | `P > D` (sans belote : 82 points de plis ou plus) | ses plis + sa belote | ses plis + sa belote |
| Dedans | `P < D` | sa belote seulement | 162 + sa belote |
| Litige | `P = D` | sa belote ; ses plis vont en réserve | ses plis + sa belote |
| Capot | une Équipe fait tous les plis | Équipe capot : 252 + sa belote ; l'autre : sa belote seulement | |

- La belote-rebelote reste toujours à l'Équipe qui l'a annoncée, même dedans ou capotée.
- **Réserve de litige** : les points de plis du preneur en litige sont mis de côté et donnés à l'Équipe qui gagne la Manche suivante (le preneur s'il fait son contrat, la défense s'il est dedans). Plusieurs litiges d'affilée s'additionnent dans la réserve.
- Le capot est un choix explicite dans la saisie : 162 points de plis ne suffisent pas à le déduire.

## Distribution (rappel affiché)

32 cartes, 5 par joueur (3 puis 2), une carte retournée, puis 3 cartes (2 au preneur) au second tour de distribution. 8 plis par Manche.

## Décisions confirmées (2026-10-21)

- Seuil « fait » : les totaux sont comparés belote comprise (`P > D`). Le preneur qui a la belote fait son contrat dès 72 points de plis ; si c'est la défense qui l'a, il lui en faut 92.
- Dedans : la défense marque 162 (et non 160), faute d'arrondi.
