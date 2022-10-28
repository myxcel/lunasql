/*
 * Package *OBJ-TAFFY*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

use lib-taffy.js;

----------- Aide du package -----------

def -cr help-obj-taffy {
print "Module *OBJ-TAFFY* : fonctions de base de données en Javascript
 Fonctions ajoutées : bibliothèque js Taffy.js
 Taffy : http://www.taffydb.com/
 An opensouce library that brings database features into your applications.
 Exemples :
 // Create DB and fill it with records
 var friends = TAFFY([
   {'id':1,'gender':'M','first':'John','last':'Smith','city':'Seattle, WA'},
   {'id':2,'gender':'F','first':'Kelly','last':'Ruth','city':'Dallas, TX'},
   {'id':3,'gender':'M','first':'Jeff','last':'Stevenson','city':'Washington, D.C.'},
   {'id':4,'gender':'F','first':'Jennifer','last':'Gill','city':'Seattle, WA'}
 ]);
 // Find all the friends in Seattle
 friends({city:'Seattle, WA'});
 // Find John Smith, by ID
 friends({id:1});
 // Find John Smith, by Name
 friends({first:'John',last:'Smith'});
 // Kelly's record
 var kelly = friends({id:2}).first();
 // Kelly's last name
 var kellyslastname = kelly.last;
 // Get an array of record ids
 var cities = friends().select('id');
 // Get an array of distinct cities
 var cities = friends().distinct('city');
 // Apply a function to all the male friends
 friends({gender:'M'}).each(function (r) { print(r.name + '!'); });
 // Move John Smith to Las Vegas
 friends({first:'John',last:'Smith'}).update({city:'Las Vegas, NV:'});
 // Remove Jennifer Gill as a friend
 friends({id:4}).remove();
 // insert a new friend
 friends.insert({'id':5,'gender':'F','first':'Jennifer','last':'Gill','city':'Seattle, WA'});

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;

