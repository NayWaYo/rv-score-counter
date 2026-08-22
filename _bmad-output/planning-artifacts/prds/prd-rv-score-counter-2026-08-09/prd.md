---
title: rv-score-counter — PRD
status: final
created: 2026-08-09
updated: 2026-08-09
---

# PRD : rv-score-counter

## 0. Objet du document

Ce PRD s'adresse au développeur-auteur (PM et développeur sont la même personne pour la V1) ainsi qu'aux outils d'ingénierie en aval (architecture, épics, stories). Il décrit les exigences fonctionnelles et non-fonctionnelles de rv-score-counter, une application Android de comptage de points pour jeux de cartes, en commençant par le Papayoo.

Le document est structuré en spine essentiel (Vision, Utilisateurs, Glossaire, Fonctionnalités, Non-objectifs, Périmètre MVP, Métriques) complété par les sections **Plateforme & Contraintes** et **Architecture d'information** jugées utiles pour un projet mobile semi-sérieux. Les termes du Glossaire (§3) sont utilisés partout dans le PRD sans synonymes.

---

## 1. Vision

rv-score-counter remplace le papier et le stylo lors des soirées jeux de cartes. Un seul téléphone, posé au centre de la table, tient le rôle de marqueur officiel : il accueille la saisie des scores manche après manche, vérifie automatiquement leur cohérence, et conserve l'historique local des parties passées.

La V1 cible le **Papayoo** — jeu de plis à levées négatives pour 3 à 8 joueurs, dont le calcul manuel génère régulièrement des erreurs en soirée. Le cœur de la valeur est la **vérification anti-erreur** : le total de points distribués par manche est fixe (250), ce qui permet de pré-remplir le score du dernier joueur par complément et de détecter immédiatement toute anomalie de saisie.

L'application est conçue usage-first pour un groupe d'amis précis, mais avec une architecture extensible : si l'usage confirme la valeur, l'ajout d'autres jeux (Belote en premier candidat) ne devra pas nécessiter une réécriture.

---

## 2. Utilisateurs cibles

### 2.1 Jobs To Be Done

- **Fonctionnel :** Tenir le score de façon fiable, sans erreur de calcul, pendant une partie de Papayoo.
- **Fonctionnel :** Remettre en place une partie avec le même groupe d'amis le plus rapidement possible.
- **Fonctionnel :** Consulter rapidement le classement en cours de partie sans interrompre le jeu.
- **Émotionnel :** Éliminer les disputes sur les scores et les recalculs embarrassants en fin de soirée.
- **Contextuel :** Gérer le marquage depuis un seul téléphone posé au centre de la table, sans que chaque joueur ait besoin d'y toucher.

### 2.2 Non-utilisateurs (V1)

- Joueurs cherchant un compteur de score générique multi-jeux sans logique métier spécifique.
- Utilisateurs souhaitant partager les scores en temps réel sur plusieurs appareils.
- Groupes jouant à des jeux autres que le Papayoo (V1 uniquement).

### 2.3 Parcours utilisateurs clés

**UJ-1. Rémy lance une nouvelle partie de Papayoo.**
- **Persona + contexte :** Rémy, auteur de l'app, host de la soirée, 4 amis autour de la table.
- **État d'entrée :** Application ouverte, aucune partie en cours.
- **Parcours :** Il sélectionne "Nouvelle partie" → choisit Papayoo → ajoute les 5 participants par leur prénom → réordonne la liste pour refléter la disposition physique à table → confirme. L'app affiche que Rémy (premier de la liste) est le Donneur de la Manche 1.
- **Climax :** La partie est prête en moins d'une minute ; la table peut commencer à jouer.
- **Résolution :** L'écran de saisie de la Manche 1 s'affiche, avec le Donneur indiqué.
- **Cas limite :** Si Rémy oublie d'ajouter un joueur après confirmation, il peut modifier la liste tant que la première Manche n'a pas été validée (voir FR-4).

**UJ-2. Rémy saisit les scores de la manche 3.**
- **Persona + contexte :** Partie en cours, 5 joueurs, manche 3 terminée.
- **État d'entrée :** Écran de saisie de manche, les 4 scores précédents ont été entrés.
- **Parcours :** Rémy entre les points de chaque joueur dans l'ordre qui lui convient. Au moment de saisir le dernier joueur, le champ est pré-rempli par complément à 250. Il vérifie visuellement, confirme.
- **Climax :** L'app valide la manche et met à jour les scores cumulés et le classement.
- **Résolution :** L'écran de résumé de classement s'affiche, prêt pour la manche suivante.
- **Cas limite :** Si Rémy a commis une erreur sur un score précédent, le pré-remplissage donnera une valeur incohérente (ex. négative ou anormalement élevée) — signal visuel d'alerte permettant de corriger avant validation.

**UJ-3. Le groupe décide de rejouer avec les mêmes joueurs.**
- **Persona + contexte :** Fin de partie, tout le monde veut une revanche.
- **État d'entrée :** Écran de fin de partie / résultats.
- **Parcours :** Rémy appuie sur "Rejouer" → l'app pré-charge les mêmes participants dans le même ordre → confirme d'un tap.
- **Climax :** Nouvelle partie lancée en moins de 5 secondes.
- **Résolution :** Écran de saisie de la Manche 1 de la nouvelle partie, avec le Donneur indiqué.

**UJ-4. Sophie consulte le classement pendant la partie.**
- **Persona + contexte :** Sophie, joueuse à table, veut savoir où elle en est sans déranger Rémy.
- **État d'entrée :** Partie en cours, entre deux manches.
- **Parcours :** Elle regarde l'écran posé au centre — le classement est visible en permanence ou d'un simple coup d'œil.
- **Climax :** Elle voit son rang et son score total sans interaction.
- **Résolution :** Elle rend le téléphone ou le laisse au centre.

---

## 3. Glossaire

- **Partie** — Session de jeu complète entre un groupe de Participants sur un seul Jeu. Une Partie comprend une ou plusieurs Manches et se termine sur décision des joueurs.
- **Jeu** — Type de jeu de cartes supporté par l'application (V1 : Papayoo uniquement). Chaque Jeu a ses propres règles de calcul des points et son propre nombre de Participants autorisés.
- **Manche** — Tour de jeu au sein d'une Partie. Pour le Papayoo, chaque Manche distribue exactement 250 points entre tous les Participants.
- **Participant** — Joueur humain enregistré pour une Partie donnée, identifié par son prénom. Le même prénom peut apparaître dans des Parties distinctes sans lien entre elles.
- **Donneur** — Participant désigné comme distributeur des cartes pour une Manche donnée. Pour le Papayoo, le Donneur de la Manche 1 est le premier Participant de la liste ; la rotation suit l'ordre de la liste pour les Manches suivantes.
- **Score de Manche** — Points attribués à un Participant pour une Manche précise (valeur positive ou nulle pour le Papayoo).
- **Score Cumulé** — Somme des Scores de Manche d'un Participant depuis le début de la Partie.
- **Classement** — Ordre des Participants trié par Score Cumulé croissant (le moins de points gagnants au Papayoo).
- **Couleur du 7** — Couleur tirée au sort à chaque Manche du Papayoo, dont le 7 vaut 40 points pour le joueur qui le ramasse. Tirée via le Dé Virtuel ou un dé physique.
- **Dé Virtuel** — Fonctionnalité de l'application simulant le lancer d'un dé à 4 faces pour désigner la Couleur du 7.
- **Total Fixe** — Contrainte du Papayoo : la somme des Scores de Manche de tous les Participants est toujours égale à 250. Utilisée pour la vérification anti-erreur.
- **Pré-remplissage** — Calcul automatique du Score de Manche du dernier Participant saisi, par complément au Total Fixe. Permet de détecter les erreurs de saisie.
- **Historique** — Ensemble des Parties passées stockées localement sur l'appareil, avec leurs Participants, Scores et classement final.
- **Relance** — Action de démarrer une nouvelle Partie avec les mêmes Participants dans le même ordre qu'une Partie précédente.

---

## 4. Fonctionnalités

### 4.1 Création de Partie

**Description :** L'utilisateur crée une nouvelle Partie en choisissant un Jeu, en ajoutant les Participants et en définissant leur ordre autour de la table. Cet ordre détermine le Donneur de chaque Manche. Réalise UJ-1.

**Exigences fonctionnelles :**

#### FR-1 : Sélection du Jeu

L'utilisateur peut sélectionner le Jeu de la Partie parmi les Jeux disponibles dans l'application. En V1, seul le Papayoo est disponible.

**Conséquences (testables) :**
- L'écran de création affiche la liste des Jeux disponibles ; en V1 : Papayoo uniquement.
- La sélection d'un Jeu charge les règles de calcul et les contraintes de participants propres à ce Jeu (3 à 8 pour le Papayoo).

**Hors périmètre :** Création d'un jeu personnalisé par l'utilisateur.

#### FR-2 : Ajout et gestion des Participants

L'utilisateur peut ajouter entre 3 et 8 Participants identifiés par leur prénom, et réordonner la liste pour refléter la disposition physique autour de la table.

**Conséquences (testables) :**
- Le bouton de confirmation de Partie est désactivé si le nombre de Participants est inférieur à 3 ou supérieur à 8.
- L'utilisateur peut supprimer un Participant de la liste avant confirmation.
- L'utilisateur peut modifier l'ordre de la liste par glisser-déposer ou équivalent.
- Un prénom vide ou uniquement composé d'espaces est refusé.

**Hors périmètre :** Compte utilisateur, base de données de joueurs persistante entre les Parties.

#### FR-3 : Désignation du Donneur initial

Le premier Participant de la liste est automatiquement désigné Donneur de la Manche 1. La rotation du Donneur suit l'ordre de la liste pour les Manches suivantes.

**Conséquences (testables) :**
- L'écran de saisie de la Manche 1 affiche clairement le nom du Donneur.
- Après validation d'une Manche, le Donneur affiché pour la Manche suivante est le Participant suivant dans la liste (rotation circulaire).

#### FR-4 : Modification de la liste en cours de Partie

La liste des Participants peut être modifiée selon les règles suivantes :
- **Avant la première Manche :** ajout, suppression et réordonnancement sont tous autorisés.
- **Après la première Manche validée :** seul l'ajout d'un Participant est autorisé, à condition que le nombre de Participants actuels soit strictement inférieur à 8. La suppression et le réordonnancement sont verrouillés.

**Conséquences (testables) :**
- Le bouton d'ajout de Participant est accessible en cours de Partie si le nombre de Participants < 8.
- Le bouton d'ajout est désactivé si le nombre de Participants atteint 8.
- Les boutons de suppression et de réordonnancement sont masqués ou désactivés après la validation de la première Manche.
- Lors de l'ajout en cours de Partie, l'utilisateur choisit la position d'insertion du nouveau Participant dans la liste (glisser-déposer ou sélection de position).
- Un Participant ajouté en cours de Partie démarre avec un Score Cumulé de 0.
- **Rotation du Donneur après ajout :** la rotation continue sur la liste dans l'ordre résultant de l'insertion choisie. Si le nouveau Participant est inséré juste après le Donneur de la prochaine Manche, il devient le Donneur de la Manche suivante ; sinon, la rotation se poursuit normalement selon l'ordre de la liste mise à jour.

**Conséquences (testables) — rotation :**
- Si le nouveau Participant est inséré à la position N+1 (juste après le prochain Donneur), l'écran de la Manche suivante l'affiche comme Donneur.
- Dans tous les autres cas d'insertion, le prochain Donneur prévu reste inchangé.

---

### 4.2 Dé Virtuel (Couleur du 7)

**Description :** À chaque Manche du Papayoo, la Couleur du 7 est tirée au sort. L'application propose un Dé Virtuel pour les tables sans dé physique. Réalise UJ-2 (étape préalable à la saisie).

**Exigences fonctionnelles :**

#### FR-5 : Lancer du Dé Virtuel

L'utilisateur peut lancer le Dé Virtuel depuis l'écran de saisie de Manche pour obtenir aléatoirement une des quatre couleurs (♠ pique, ♥ cœur, ♣ trèfle, ♦ carreau).

**Conséquences (testables) :**
- Le résultat est l'une des 4 couleurs avec distribution aléatoirement uniforme.
- Le résultat est affiché clairement (nom et/ou symbole de la couleur).
- L'utilisateur peut relancer le dé avant de commencer la saisie des scores.
- La Couleur du 7 est affichée en permanence sur l'écran de saisie de Manche pendant toute la durée de la saisie.
- Dans l'historique des Manches, chaque Manche affiche la Couleur du 7 sous forme d'un pictogramme (petit logo de la couleur) en bout de ligne, et la ligne est colorée selon la Couleur du 7 de cette Manche.

**Hors périmètre :** Pondération du tirage selon les Manches précédentes.

---

### 4.3 Saisie de Manche et vérification anti-erreur

**Description :** Cœur de l'application. À chaque Manche, l'utilisateur saisit le Score de chaque Participant dans l'ordre de son choix. Le Score du dernier Participant est pré-rempli par complément au Total Fixe (250), ce qui constitue la vérification anti-erreur. Réalise UJ-2.

**Exigences fonctionnelles :**

#### FR-6 : Saisie libre des scores

L'utilisateur peut saisir le Score de Manche de chaque Participant dans n'importe quel ordre.

**Conséquences (testables) :**
- Chaque champ de saisie est associé au nom du Participant correspondant.
- L'utilisateur peut remplir les champs dans n'importe quel ordre.
- Les champs acceptent uniquement des entiers positifs ou nuls.

#### FR-7 : Pré-remplissage du dernier Participant

Lorsque tous les Participants sauf un ont leur Score de Manche renseigné, le champ du dernier Participant est automatiquement pré-rempli avec le complément au Total Fixe (250 − somme des scores déjà saisis).

**Conséquences (testables) :**
- Dès que N-1 champs sont remplis, le N-ième champ affiche le complément calculé.
- L'utilisateur peut modifier le champ pré-rempli.
- Si la somme totale dépasse 250 (que ce soit via la saisie libre ou la modification du champ pré-rempli), un message d'alerte indique que le total dépasse 250 et la validation est bloquée jusqu'à correction.
- Un score de 0 est une valeur valide pour un Participant.

#### FR-8 : Validation de la Manche

L'utilisateur valide la Manche une fois tous les scores renseignés. La somme doit être exactement égale à 250.

**Conséquences (testables) :**
- Le bouton "Valider la Manche" est actif uniquement si tous les champs sont remplis.
- Si la somme des Scores de Manche ≠ 250, la validation est bloquée avec un message précisant l'écart (ex. "Total : 270, il faut corriger pour atteindre 250").
- Si la somme dépasse 250, le message indique explicitement que le total dépasse 250.
- Un score de 0 est une valeur valide et souhaitable (objectif du joueur gagnant).
- Après validation réussie, les Scores Cumulés sont mis à jour et le Classement recalculé. Une Manche validée est définitive et ne peut plus être modifiée.

#### FR-9 : Immuabilité des Manches validées

Une Manche validée ne peut pas être modifiée. Les scores de Manche sont définitifs après validation.

**Conséquences (testables) :**
- Aucun bouton ou action de modification n'est proposé sur une Manche validée.
- L'historique des Manches est en lecture seule.

**NFRs spécifiques :**
- Le Pré-remplissage est recalculé en temps réel à chaque modification d'un champ.

---

### 4.4 Suivi de la Partie en cours

**Description :** L'écran principal de Partie affiche en permanence le Classement, les Scores Cumulés et le numéro de Manche en cours. Réalise UJ-2, UJ-4.

**Exigences fonctionnelles :**

#### FR-10 : Affichage du Classement en temps réel

L'écran principal affiche le Classement des Participants, trié par Score Cumulé croissant, mis à jour après chaque Manche validée.

**Conséquences (testables) :**
- Le Classement est visible sans défilement pour une partie à 8 Participants sur un écran de téléphone standard. [ASSUMPTION : affichage vertical compact suffisant pour 8 lignes.]
- Le Donneur de la Manche suivante est identifiable visuellement.

#### FR-11 : Navigation vers le détail des Manches

L'utilisateur peut consulter le détail des scores de chaque Manche passée.

**Conséquences (testables) :**
- Une liste des Manches passées est accessible depuis l'écran principal.
- Chaque Manche affiche le Score de Manche de chaque Participant et la somme totale.

---

### 4.5 Fin de Partie

**Description :** La Partie se termine sur décision explicite des joueurs, sans condition automatique de victoire. Réalise UJ-1, UJ-3.

**Exigences fonctionnelles :**

#### FR-12 : Clôture de Partie

L'utilisateur peut clore la Partie à tout moment via un bouton "Fin de partie" distinct du bouton "Nouvelle manche".

**Conséquences (testables) :**
- Les deux boutons ("Nouvelle manche" et "Fin de partie") sont visuellement distincts et non confondables.
- Une confirmation est demandée avant de clore la Partie (prévention des clics accidentels).
- Une fois clôturée, la Partie est sauvegardée dans l'Historique local.
- Il n'est plus possible d'ajouter de Manche à une Partie clôturée.

#### FR-13 : Écran de résultats finaux

Après clôture, l'application affiche le Classement final avec les Scores Cumulés de chaque Participant.

**Conséquences (testables) :**
- L'écran de résultats affiche clairement le vainqueur (Participant avec le Score Cumulé le plus bas).
- Les options "Rejouer" et "Retour à l'accueil" sont proposées.

---

### 4.6 Relance rapide

**Description :** Permet de démarrer une nouvelle Partie avec les mêmes Participants dans le même ordre qu'une Partie précédente, sans ressaisie. Réalise UJ-3.

**Exigences fonctionnelles :**

#### FR-14 : Rejouer avec les mêmes Participants

Depuis l'écran de résultats finaux ou depuis l'Historique, l'utilisateur peut démarrer une nouvelle Partie avec les mêmes Participants et le même ordre de liste.

**Conséquences (testables) :**
- La nouvelle Partie est créée en un seul tap de confirmation.
- La nouvelle Partie est une Partie distincte dans l'Historique (pas de modification de la Partie précédente).
- Le Jeu est le même que la Partie de référence.

---

### 4.7 Historique local

**Description :** Les Parties passées sont conservées sur l'appareil, sans compte ni synchronisation. Réalise UJ-3 (point d'entrée alternatif pour la Relance).

**Exigences fonctionnelles :**

#### FR-15 : Consultation de l'Historique

L'utilisateur peut consulter la liste des Parties passées et le détail de chacune (Participants, Scores, Classement final).

**Conséquences (testables) :**
- L'Historique est accessible depuis l'écran d'accueil.
- Chaque entrée affiche la date, le Jeu, les Participants et le vainqueur.
- L'utilisateur peut supprimer une entrée de l'Historique.
- Dans le détail d'une Partie, chaque ligne de Manche affiche la Couleur du 7 via un pictogramme en bout de ligne et un code couleur de fond de ligne correspondant à la couleur tirée.

#### FR-16 : Persistance locale

Les données de l'Historique et des Parties en cours sont stockées localement sur l'appareil, sans compte ni synchronisation réseau.

**Conséquences (testables) :**
- Les données persistent après fermeture et réouverture de l'application.
- Une Partie non clôturée est restaurée dans son état exact au prochain lancement de l'application (reprise possible plus tard).
- Aucune connexion réseau n'est requise pour accéder à l'Historique ou créer une Partie.

[ASSUMPTION : pas de limite de taille de l'Historique en V1, suppression manuelle uniquement.]

---

## 5. Non-objectifs (explicites)

- **Autres jeux de cartes en V1** — Belote et suivants sont explicitement hors périmètre V1. L'architecture doit permettre leur ajout ultérieur, mais aucune règle de Belote n'est implémentée.
- **Compte utilisateur / multi-appareils** — Pas de création de compte, pas de synchronisation cloud, pas de partage de Partie en réseau.
- **Publication Play Store en V1** — Pas un objectif du premier livrable ; à réévaluer selon l'usage réel.
- **Calcul du détail du jeu** — L'application compte les scores fournis par les joueurs ; elle ne gère pas les paquets de cartes, les plis, ni les règles de distribution.
- **Gestion automatique des règles de fin de partie** — Pas de condition automatique de victoire ; la fin est toujours décidée manuellement.
- **Fonctionnement multi-téléphones** — Un seul appareil par Partie.

---

## 6. Périmètre MVP

### 6.1 Dans le périmètre

- Création de Partie : sélection du Jeu (Papayoo), ajout de 3 à 8 Participants, réordonnancement, désignation automatique du Donneur.
- Dé Virtuel pour la Couleur du 7 (affichée pendant la saisie et conservée dans l'historique des Manches).
- Saisie de Manche avec Pré-remplissage et vérification anti-erreur (Total Fixe = 250). Manches validées immuables.
- Ajout de Participant en cours de Partie (si total < 8), avec choix de position d'insertion.
- Classement en temps réel, historique des Manches.
- Reprise d'une Partie non clôturée après fermeture de l'application.
- Clôture manuelle de Partie et écran de résultats finaux.
- Relance rapide (mêmes Participants, même ordre).
- Historique local des Parties.

### 6.2 Hors périmètre MVP

- Belote et autres jeux — déféré V2 ; l'architecture extensible est attendue dès la V1.
- Comptes utilisateurs et synchronisation cloud.
- Publication Play Store.
- Gestion des profils de joueurs persistants entre Parties.
- Export des scores (CSV, partage, etc.). [NOTE FOR PM : peut être utile si publication envisagée.]
- Thèmes visuels personnalisables.

---

## 7. Métriques de succès

**Primaires**
- **SM-1 :** L'application est utilisée en remplacement complet du papier lors des parties de Papayoo de l'auteur — cible : 100 % des parties dans le mois suivant la V1. Valide FR-6, FR-7, FR-8.
- **SM-2 :** Au moins une erreur de saisie détectée grâce au Pré-remplissage sur les 10 premières Parties. Valide FR-7.

**Secondaires**
- **SM-3 :** La Relance d'une Partie prend moins de temps que la ressaisie manuelle des Participants — cible subjective confirmée par l'auteur après 5 Parties. Valide FR-14.
- **SM-4 :** Aucun crash ou perte de données sur une session de jeu complète (toutes les Manches jusqu'à la clôture). Valide FR-16.

**Contre-métriques (ne pas optimiser)**
- **SM-C1 :** Ne pas optimiser la vitesse de création de Partie au détriment de la lisibilité du Classement — l'écran principal doit rester la priorité d'affichage. Contrebalance SM-3.

---

## 8. Questions ouvertes

*Toutes les questions ouvertes sont résolues. Aucun blocant restant.*

*Questions résolues :*
- ~~QO-1~~ — Score de 0 valide et souhaitable (objectif du joueur gagnant). ✅
- ~~QO-2~~ — Le pré-remplissage est modifiable ; si le total dépasse 250, la validation est bloquée avec message. ✅
- ~~QO-3~~ — La Couleur du 7 est affichée pendant toute la saisie de la Manche et conservée dans l'historique (pictogramme + couleur de ligne). ✅
- ~~QO-4~~ — Une Partie non clôturée survit à la fermeture de l'app et est reprise au prochain lancement. ✅
- ~~QO-5~~ — Cible Android minimum : Android 16 (API 36). À réévaluer si publication Play Store. ✅
- ~~FR-9~~ — Les Manches validées sont immuables (pas de correction possible). ✅

---

## 9. Index des hypothèses

*Hypothèses actives (à confirmer si le contexte change) :*
- **§4.4 FR-10** — Un affichage vertical compact est suffisant pour 8 lignes de Classement sur un écran de téléphone standard.
- **§4.7 FR-16** — Pas de limite de taille de l'Historique en V1 ; suppression manuelle uniquement.
- **§10** — Android 16 (API 36) en minimum couvre l'usage personnel ; à réévaluer si publication Play Store envisagée.
- **§10** — Room/SQLite est le choix de stockage local ; décision finale à l'architecture.
- **§10** — Interface en français uniquement en V1.
- **§11** — Navigation linéaire simple sans tabs ; back stack Android standard.

*Hypothèses levées et confirmées :*
- ~~Classement : moins de points = meilleur~~ → **Confirmé** (Papayoo, jeu à levées négatives). ✅
- ~~Modification liste avant Manche 1 uniquement~~ → **Confirmé** (FR-4 : ajout possible après, pas suppression/réordonnancement). ✅
- ~~Couleur du 7 non stockée~~ → **Infirmé** : Couleur du 7 stockée par Manche, affichée en saisie et dans l'historique. ✅
- ~~Partie non clôturée perdue à la fermeture~~ → **Infirmé** : Partie reprise au prochain lancement. ✅
- ~~Correction dernière Manche possible~~ → **Infirmé** : Manches validées immuables. ✅
- ~~Score de 0 à confirmer~~ → **Confirmé** valide et souhaitable. ✅
- ~~Pré-remplissage lecture seule~~ → **Infirmé** : modifiable ; total > 250 bloque la validation. ✅
- ~~Position insertion nouveau Participant : fin de liste~~ → **Infirmé** : position choisie par l'utilisateur. ✅

---

## 10. Plateforme & Contraintes

- **Plateforme cible :** Android natif (V1). iOS hors périmètre V1.
- **Version Android minimum :** Android 16 (API level 36) — appareil de l'auteur. [ASSUMPTION : cibler API 36 en minimum couvre l'usage personnel ; à réévaluer si publication Play Store envisagée, auquel cas une cible plus basse (ex. API 26+) serait probablement nécessaire pour une adoption large.]
- **Connectivité :** Aucune connexion réseau requise. L'application fonctionne entièrement hors ligne.
- **Stockage :** Données locales uniquement (base de données locale type Room/SQLite). [ASSUMPTION : Room est le choix naturel pour Android natif, mais la décision reste à l'architecture.]
- **Permissions :** Aucune permission système requise (pas de caméra, réseau, contacts, etc.).
- **Langue de l'interface :** Français (V1). [ASSUMPTION : une seule langue en V1.]
- **Accessibilité :** Tailles de texte lisibles sur un écran posé à distance de table (~50 cm). Contrastes suffisants pour une lecture en soirée (lumière variable). Les codes couleur des 4 couleurs de carte (pique, cœur, trèfle, carreau) doivent être distinguables dans des conditions d'éclairage variables. [NOTE FOR PM : aucune conformité WCAG formelle ciblée en V1, mais le contexte d'usage impose des standards visuels non négligeables.]

---

## 11. Architecture d'information

**Écrans principaux (V1) :**

1. **Accueil** — Bouton "Nouvelle partie" + accès à l'Historique.
2. **Création de Partie** — Sélection du Jeu, ajout / réordonnancement des Participants, confirmation.
3. **Partie en cours** — Classement temps réel, Donneur de la Manche suivante, boutons "Nouvelle manche" / "Fin de partie".
4. **Saisie de Manche** — Dé Virtuel, champs de saisie des scores (avec Pré-remplissage), validation.
5. **Résultats finaux** — Classement final, boutons "Rejouer" / "Retour accueil".
6. **Historique** — Liste des Parties passées, accès au détail de chaque Partie.
7. **Détail de Partie (Historique)** — Scores par Manche, Classement final, option "Rejouer".

[ASSUMPTION : navigation linéaire simple sans tabs ; un seul back stack Android standard.]
