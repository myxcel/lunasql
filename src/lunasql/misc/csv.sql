/*
 * Package *CSV*
 *
 * Outil d'interrogation de fichier CSV par SQL
 * Nécessite la bibliothèque jdbccsv
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 0;

plugin lunasql.misc.CmdCSV;
if '$(_CMD_STATE)'=='E' {
  exit 9 "Classes CsvJdbc de requête inaccessible.
Avez-vous ajouté la bibliothèque correspondante au CLASSPATH ?
Lien : http://csvjdbc.sourceforge.net";
};

----------- Aide du package -----------

def -cr help-csv {
  print "Module *CSV* : Outil d'interrogation de fichier CSV par SQL
 Commandes ajoutées
  - CSV   : interrogation de fichier CSV
 Tapez help <cmd> pour de l'aide sur une commande

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
