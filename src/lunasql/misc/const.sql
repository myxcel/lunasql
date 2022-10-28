/*
 * Package *CONST*
 *
 * Module de définition de constantes utiles
 */

---------- Corps du package -----------

-- Couleurs pour PRINT
def NONE,BLACK,RED,GREEN,YELLOW,BLUE,MAGENTA,CYAN,WHITE 0 1 2 3 4 5 6 7 8;

-- Niveau de verbose pour VERB
def SILENCE,DISPLAY,MESSAGE,CHATTY,DEBUG 0 1 2 3 4

-- Formats de date pour formatDate() et TIME
def DATE_FRM,TIME_FRM       'yyyy-MM-dd' 'yyyy-MM-dd HH:mm:ss';
def DATE_FRM_FR,TIME_FRM_FR 'dd/MM/yyyy' 'dd/MM/yyyy HH:mm:ss';
def DATE_FRM_DE,TIME_FRM_DE 'dd.MM.yyyy' 'dd.MM.yyyy HH:mm:ss';
def DATE_FRM_US,TIME_FRM_US 'MM/dd/yyyy' 'MM/dd/yyyy HH:mm:ss';

-- Types de SGBD définis en TypesSGBD
def ODBC,ACCESS,ACCESSX,UCACCESS,HSQLDB,H2DB,MYSQL,ORACLE,DERBY,SQLSERVER 0 1 2 3 4 5 6 7 8 9

-- États de sortie (_CMD_STATE)
def NO_CHANGES,HAD_CHANGES,GOT_ERROR 0 1 E

-- Booléens
def TRUE,FALSE,YES,NO,VRAI,FAUX,OUI,NON,WAHR,FALSCH,JA,NEIN 1 0 1 0 1 0 1 0 1 0 1 0

----------- Aide du package -----------

def -cr help-const {
print "Module *CONST* : définition de constantes utiles
 Pour la commande PRINT :
   RED,GREEN,YELLOW,BLUE,MAGENTA,CYAN,WHITE
 Ex: print -c=^$GREEN ^"Traitement réussi^"

 Pour la commande VERB :
   SILENCE,DISPLAY,MESSAGE,CHATTY,DEBUG
 Ex: verb ^$CHATTY   -- ou:  verb chatty
     if [ ^$_NVERBOSE < ^$CHATTY] { verb chatty }

 Pour le formatage des dates et heures :
   DATE_FRM, TIME_FRM, DATE_FRM_FR, TIME_FRM_FR,
   DATE_FRM_DE, TIME_FRM_DE, DATE_FRM_US, TIME_FRM_US

 Types de SGBD pris en charge :
   ODBC,ACCESS,ACCESSX,UCACCESS,HSQLDB,H2DB,MYSQL,ORACLE,DERBY,
   SQLSERVER
 Ex: if [^$_DB_NTYPE >= ^$ACCESS && ^$_DB_NTYPE <= ^$UCACCESS] {
       print "Hello, Access!"
     }

 États de sortie de commandes (option système _CMD_STATE) :
   NO_CHANGES,HAD_CHANGES,GOT_ERROR

 Booléens pour les comparaisons avec les options 0/1 :
   TRUE FALSE YES NO VRAI FAUX OUI NON WAHR FALSCH JA NEIN
 Ex: def :ALIAS_ARG ^$OUI

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
