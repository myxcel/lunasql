/*
 * Package *PLAY*
 *
 * Joue un son Wave
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 0;

plugin lunasql.misc.CmdPlay;
if '$(_CMD_STATE)'=='E' {exit 9 "Chargement de la classe Play impossible";};

----------- Aide du package -----------

def -cr help-play {
  print "Module *PLAY* : joue un son Wave depuis la console !
 Commandes ajoutées
  - PLAY  : Joue un fichier son .wav

 Tapez help <cmd> pour de l'aide sur une commande";
};

---------------------------------------
return 1;
