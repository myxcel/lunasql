/*
 * Package *REPORT*
 *
 * Outil de génération de rapports simples en HTML
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 0;

plugin lunasql.misc.CmdReport;
if '$(_CMD_STATE)'=='E' {exit 9 "Chargement de la classe Report impossible";};

----------- Aide du package -----------

def -cr help-report {
  print "Module *REPORT* : outil de génération de rapports HTML simples
 Commandes ajoutées
  - REPORT : génération de rapports HTML
 Tapez help <cmd> pour de l'aide sur une commande

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
