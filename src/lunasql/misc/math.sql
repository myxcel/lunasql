/*
 * Package *MATH*
 */

---------- Corps du package -----------

use math-base;
use math-decision;
use math-jstat;

----------- Aide du package -----------

def -cr help-math {
print "Module *MATH* : outils mathématiques
 Bibliothèques chargées :
  - math-base : trigonométrie et base
  - math-decision : algorithme de décision par ID3
      https://github.com/serendipious/nodejs-decision-tree-id3
  - math-jstat : outils statistiques
      https://github.com/jstat/jstat

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
