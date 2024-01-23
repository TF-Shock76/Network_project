Guillaume Coufourier
Théo Crauffon
Steven Hue
D1


----------------------------------------------------------------------------------------------------
- Exécuter les codes

  Sous linux : exécutez ./runserver.sh pour lancer le serveur, puis ./runclient.sh pour chaque client que vous souhaitez
créer. Lors de l'exécution, le programme vous demandera l'IP ainsi que le port du serveur à rejoindre
  Sous Windows : même chose en utilisant les .bat 
   
Si vous exécutez un client depuis la même machine que le serveur, entrez "localhost" dans l'IP du serveur.
Le port du serveur est 6000 par défaut. Si vous voulez le changer, il faut le changer manuellement dans le code de la
classe "SimpleServer.java".

S'il y a besoin de compiler, utilisez cette ligne dans votre terminal : javac "@comp" -encoding utf-8

----------------------------------------------------------------------------------------------------
- Fonctionnalités attendues : un serveur de jeux en local, avec le Morpion comme exemple

Résultats : 
  Le jeu en local fonctionne parfaitement, avec même une gestion des déconnexions "anormales" (CTRL+C, etc...).

Ajouts : 
  Nous avons même eu le temps d'intégrer un 2e jeu, le Black-Jack, afin d'illustrer plus amplement l'utilisation des threads.



----------------------------------------------------------------------------------------------------
- Détail des fonctionnalités

Au niveau du serveur, il est possible de taper la commande 'help' permettant par la suite de 
choisir différentes aides de commande.

Au niveau des clients, ils doivent faire un choix de jeu. Ils ont la possibilité de jouer soit au Morpion, soit au Black-Jack.
Concernant le Black-Jack, les clients ont aussi le choix du nombre de personnes avec qui ils veulent jouer (de 2 à 7). Ensuite,
ils sont placés dans la queue correspondante en attendant qu'il y ait suffisamment de joueur pour lancer le jeu choisi.²

Pour le Morpion, chacun leur tour, les joueurs devront choisir une ligne ainsi qu'une colonne afin de placer leur symbole. La
partie est finie dès lors qu'un joueur a gagné, c'est à dire qu'il a aligné 3 de ses symboles, ou lorsque la grille est pleine
(égalité).

Pour le Black-Jack, voici le déroulement de la partie :
  - Chaque joueur indique sa mise (entre 2 et 100).
  - Puis, tour par tour, le joueur indique l'action qu'il veut effectuer :
    - Stand : Ne tire plus de carte. Fin du tour.
    - Hit : Tire une nouvelle carte.
    - Double : disponible seulement lors de la première action. Double sa mise et tire une unique carte. Fin du tour.
  - Une fois que tous les joueurs ont joué, le croupier tire ses cartes restantes et le calcul des scores est effectué.
  - Enfin, le jeu demande à chaque joueur s'il veut continuer la partie ou arrêter : 
    - Si tout le monde veut continuer, une nouvelle manche se lance avec les même joueurs.
    - Sinon, la partie s'arrête.



----------------------------------------------------------------------------------------------------
- Améliorations possibles

  Il est possible d'alimenter le serveur avec encore plus de jeux. Une partie du code au niveau du serveur est modulaire et est
  prévue à cet effet.
  La gestion des déconnexions "anormales" est gérée, mais n'est pas forcément harmonisée sur tous les plans. Cela rend le code
  un peu lourd par endroit. Une amélioration du code sur ce point est possible, mais un peu difficile avec nos outils et 
  conaissances actuelles.

