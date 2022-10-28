/*
 * Package *XML*
 *
 * Outil d'interrogation de chaîne au format XML
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 0;

plugin lunasql.misc.CmdXML;
if '$(_CMD_STATE)'=='E' {exit 9 "Chargement de la classe XML impossible";};

----------- Aide du package -----------

def -cr help-xml {
  print "Module *XML* : Outil d'interrogation de chaîne XML
 Commandes ajoutées
  - XML   : interrogation de chaîne XML
 Tapez help <cmd> pour de l'aide sur une commande

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
