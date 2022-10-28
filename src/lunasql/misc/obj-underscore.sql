/*
 * Package *OBJ-UNDERSCORE*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

use lib-underscore.js;

----------- Aide du package -----------

def -cr help-obj-underscore {
print "Module *OBJ-UNDERSCORE* : programmation fonctionnelle en js
 Fonctions ajoutées : bibliothèque js de programmation fonctionnelle :
   - underscore : http://underscorejs.org
  Underscore provides a whole mess of useful functional programming helpers
  without extending any built-in objects.
  Exemples :
  _.each([1, 2, 3], println);
  _.map([1, 2, 3], function(num){ return num * 3; }); // -> [3, 6, 9]

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
