/*
 * Package *FRM*
 *
 * Outil de formatage de requête SQL
 * Nécessite la bibliothèque jsqlparser
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 0;

plugin lunasql.misc.CmdFrm;
if '$(_CMD_STATE)'=='E' {
  exit 9 "Classes SQLParser de formatage de requête inaccessible.
Avez-vous ajouté la bibliothèque correspondante au CLASSPATH ?
Lien : https://github.com/JSQLParser/JSqlParser";
};

----------- Aide du package -----------

def -cr help-frm {
  print "Module *FRM* : outil de formatage de requête SQL
 Commandes ajoutées
  - FRM   : formatage SQL de requête
 Tapez help <cmd> pour de l'aide sur une commande

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
