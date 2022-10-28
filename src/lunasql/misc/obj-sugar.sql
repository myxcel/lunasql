/*
 * Package *OBJ-SUGAR*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

use lib-sugar.js;

----------- Aide du package -----------

def -cr help-obj-sugar {
print "Module *FUNJS-SUGAR* : fonctions de manipulation des objets JS
 Fonctions ajoutées : bibliothèque js Sugarjs.js
 Sugar.js : https://sugarjs.com/
 Getting started: https://sugarjs.com/quickstart/
 Documentation complète : https://sugarjs.com/docs/
 Sugar is a Javascript utility library for working with native objects.
 Exemples :
    Sugar.Number.random(1, 100);
    Sugar.Date.create('next Friday');
    Sugar.Array.unique([1,2,2,3]); //->[1, 2, 3]
    Date.create('last week Friday'); //->September 1, 2017 12:00 AM
    Sugar.Date.format(new Date(), '%Y-%m-%d'); //-> '2020-11-08'

    var arr = new Sugar.Array([1,2]);
    arr.concat([2,3]).unique().raw;  //->[1, 2, 3]

    Sugar.extend();
    [1,2,2,3].unique(); //->[1, 2, 3]
    (3.1415).round(1).toFixed(2); //->'3.10'
    new Date().format('%Y-%m-%d'); //-> '2020-11-08'

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;

