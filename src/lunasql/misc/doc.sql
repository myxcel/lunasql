/*
 * Package *DOC*
 */

---------- Corps du package -----------

use doc-h2;
use doc-sql;

----------- Aide du package -----------

def -cr help-doc {
print "Module *DOC* : documentation
 Bibliothèques chargées :
  - doc-h2 : documentation sur le SGBD H2
  - doc-sql : documentation sur le langage SQL
 Tapez help-<lib> pour de l'aide sur une bibliothèque

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;

