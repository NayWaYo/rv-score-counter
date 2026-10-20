# Revue ergonomie et couleurs — 2026-10-19

Base de travail pour la prochaine session, rédigée après la release v1.1 (`8350ca2`, tag `v1.1`).
Avis établi à partir du code et de rendus Compose sur l'émulateur, pas d'un parcours complet de l'app à la main.

**Statut au 2026-10-20 :** tout est traité — correction d'une manche (`2160a60`), écran « Statistiques » (`0fac30b`), couleurs (`a249a53`), « Qui a eu le 40 ? » en premier et validation fixée en bas (`848a7a7`).

## Ordre de travail proposé

1. **Corriger une manche validée** (manque le plus important).
2. **Un écran « Statistiques » unique**, à la place des stats répétées sur quatre écrans.
3. **Nettoyer les couleurs** (rouge réservé aux erreurs, ambre recentré, teintes Pique/Carreau, palette des courbes).

## Ergonomie

### Ce qui fonctionne

- Le déroulé d'une manche est direct : « Nouvelle manche » → dé → scores → Valider.
- Les zones tactiles sont grandes (56 dp minimum), adaptées à une table de jeu.
- La saisie tient au-dessus du clavier, et le dernier score est suggéré (v1.1).
- Le badge Donneur et le choix de la place d'un joueur ajouté collent à la vraie table.

### À améliorer, par priorité

1. **Aucune correction d'une manche validée.** Une faute de frappe après « Valider » est définitive : `ValidateRoundUseCase` enregistre, et rien ne permet de modifier ou supprimer une manche. Piste : depuis le tableau des manches, toucher une manche pour la rouvrir en saisie, avec le même contrôle de somme et du 40. Cela touche Room (`PartyRepository` et ses fakes de test, mise à jour transactionnelle de la manche et de ses scores).
2. **L'écran de la manche est surchargé.** Le panneau « Classement » empile classement, cartes de stats, courbe et tableau, donc beaucoup de défilement sur l'écran de jeu. Piste : n'y garder que le classement et ouvrir un écran « Statistiques » depuis la partie en cours, le détail et les résultats.
3. **Stats dupliquées à quatre endroits** : panneau Classement de la saisie, feuille « Voir les manches » de la partie, détail d'une partie, résultats finaux. Un écran unique serait plus clair pour l'utilisateur et plus simple à faire évoluer.
4. **« Qui a eu le 40 ? » arrive après les scores**, alors qu'il contraint le score du détenteur (≥ 40). Piste : poser la question avant les champs. Un bouton « Valider » fixé en bas aiderait aussi avec 6 à 8 joueurs.

## Couleurs

### Base

Fond `#121212`, surfaces `#1E1E1E` / `#2C2C2C`, texte crème `#E8E0D5`, accent ambre `#FFB74D` : identité cohérente, confortable le soir, bon contraste. Tokens dans `ui/theme/Color.kt`.

### Problèmes

Cinq familles cohabitent : accent ambre, 4 enseignes et leurs teintes, rouge d'erreur, or et violet des cartes de stats, 8 couleurs de courbes. Conflits relevés :

- **Le rouge d'erreur (`#CF6679`) marque le « pire score » du tableau des manches** (`RoundTable.kt`). Ce rouge signifie « saisie incorrecte » ailleurs et ressemble au Cœur (`#EF5350`). Un pire score n'est pas une erreur : préférer un texte atténué ou un repère neutre.
- **L'ambre est surchargé** : meilleur score du tableau, rang du podium, badge Donneur, et presque le même or que la carte « Roi du 40 » (`ChampionInk #FFD54F`). Il perd sa valeur d'accent.
- **Les couleurs des courbes recoupent les enseignes** (`ChartSeries` : bleu, rouge, vert contre Carreau `#42A5F5`, Cœur, Trèfle `#66BB6A`). Sur un écran qui montre courbe et tableau, une couleur de joueur peut sembler liée à une enseigne.
- **Teintes Pique et Carreau quasi identiques** (`SpadeTint #1A2328`, `DiamondTint #16222E`) : la case du 40 au Carreau ressemble à celle au Pique.
- **Pique en gris** (`#90A4AE`) : se lit un peu comme un élément désactivé.

### Pistes

- Réserver le rouge aux erreurs.
- Garder l'ambre pour les actions et le podium.
- Différencier nettement les teintes Pique et Carreau.
- Revoir la palette des courbes pour qu'elle se démarque des enseignes. La valider avec `validate_palette.js` de la skill dataviz : Node n'est pas installé sur ce poste, et la palette actuelle n'a été validée que pour un fond `#1a1a19`.
