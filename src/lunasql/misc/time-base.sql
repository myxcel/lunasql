/*
 * Package *TIME-BASE*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

def -r set-chrono {let var chrono=millis(); 0;};
def -r get-chrono {let typeof(chrono)==='undefined'? 0:millis()-chrono;};
def -r get-uptime {let I(millis()-$(_LOGIN_MS));};
def -r frm-duration {
  arg ms:int;
  Packages.lunasql.sql.SQLCnx.frmDur($(ms));
};

----------- Aide du package -----------

help -a set-chrono "Déclenche un chronomètre js";
help -a get-chrono "Retourne la valeur en ms du chronomètre js";
help -a get-uptime "Retourne le nombre de ms depuis connexion";
help -a frm-duration "Formate en lisible une durée en ms";

def -cr help-time-base {
print "Module *TIME-BASE* : fonctions de manipulation de dates
 Macros ajoutées
  - set-chrono : initialise un chrono js en ms
  - get-chrono : retourne la valeur du chrono
  - get-uptime : retourne le nombre de ms depuis connexion
  - frm-duration : prend une durée ms et la retourne en h min s ms
  Exemple :
    print ^$[frm-duration ^$[get-uptime]] --> 1 min 36 s 116 ms

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
