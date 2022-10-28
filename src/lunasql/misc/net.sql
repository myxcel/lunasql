/*
 * Package *NET*
 *
 * LAnce une commande HTTP
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 0;

plugin lunasql.misc.CmdNet;
if '$(_CMD_STATE)'=='E' {exit 9 "Chargement de la classe Net impossible";};

----------- Aide du package -----------

def -cr help-net {
  print "Module *NET* : lance une commande HTTP GET, POST
 Commandes ajoutées
  - NET  : lance une commande HTTP

 Tapez help <cmd> pour de l'aide sur une commande";
};

---------------------------------------
return 1;
