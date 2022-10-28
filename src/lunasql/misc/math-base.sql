/*
 * Package *MATH-BASE*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

use lib-math.js;

----------- Aide du package -----------

def -cr help-math-base {
print "Module *MATH-BASE* : fonctions de calcul mathématique
 Constantes ajoutées : E, PI
 Fonctions ajoutées : max, min, abs, fonctions de trigo et
  randn(n), intToBase(number,ob,nb), logb(x,base),
  roundp(x,nbdec), fractApprox(x,maxDenom),
  fractReduce(numer,denom)

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
