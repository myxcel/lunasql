/*
 * Package *OBJ*
 */

---------- Corps du package -----------

use obj-base;
use obj-sugar;
use obj-taffy;
use obj-underscore;

----------- Aide du package -----------

def -cr help-obj {
print "Module *OBJ* : manipulation d'objets
 Bibliothèques chargées :
  - obj-base : functions de base
  - obj-sugar : gestion d'objets natifs js
      https://sugarjs.com/
  - obj-taffy : moteur de BDD en objet js
      https://github.com/typicaljoe/taffydb
  - obj-underscore : prog. fonctionnelle en js
      http://underscorejs.org

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
