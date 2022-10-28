/*
 * Package *TIME-XDATE*
 *
 * Outils de gestion et de formatage de date/heure
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

use lib-xdate.js;

----------- Aide du package -----------

def -cr help-time-xdate {
print "Module *TIME-XDATE* : module de date
 Variables ajoutées : bibliothèque js de manipulation de dates :
 XDate : http://arshaw.com/xdate/
 XDate is a thin wrapper around JavaScript's native Date object that
 provides enhanced functionality for parsing, formatting, and
 manipulating dates.

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;

