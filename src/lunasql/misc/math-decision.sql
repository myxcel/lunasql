/*
 * Package *MATH-DECISION*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

use lib-underscore.js;
use lib-decision.js;

----------- Aide du package -----------

def -cr help-math-decision {
print "Module *MATH-DECISION* : module de fonctions mathématiques
 Fonctions ajoutées : bibliothèque js de prise de décision :

 Decision : https://github.com/serendipious/nodejs-decision-tree-id3
  (http://en.wikipedia.org/wiki/ID3_algorithm)
  Contains the NodeJS Implementation of Decision Tree using ID3 Algorithm
  Constructeur : Decision(training_data, class_name, features)
  Exemples :
  var training_data = [{'color':'blue', 'shape':'square', 'liked':false}];
  var test_data = []; // ...
  var class_name = 'liked', features = ['color', 'shape'];
  var dt = new Decision(training_data, class_name, features);
  var predicted_class = dt.predict({color: 'blue', shape: 'hexagon'});
  var accuracy = dt.evaluate(test_data);

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
