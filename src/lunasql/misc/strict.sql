/*
 * Package *STRICT*
 * 
 * Module de garantie de syntaxe stricte
 */

---------- Corps du package -----------

if $[opt -d _DENY_OPT_CMD] {
  exit 9 "Module 'strict' impossible à charger car commande OPT interdite";
}

engine js;
let 'use strict';  -- mode strict JS (ECMAScript 5.0)

opt :ALIAS_ARG    0;
opt :ALLOW_RECUR  0;
opt :ALLOW_REDEF  0;
opt :AUTOSAVE_CFG 0;
opt :END_CMD_NL   0;
opt :EXIT_ON_ERR  1;
opt :ENCODING     ISO-8859-1;
opt :FILE_CONFIRM 1;
opt :HISTORY_DBL  0;
opt :LIST_SUBSTIT 1;
opt :ON_ERROR     "";
opt :PROMPT       SQL*;

print -c=4 "StriKt ModE aKtivaTeD";

----------- Aide du package -----------

def -cr help-strict {
print "Module *STRICT* : garantie de comportement strict
 Paramétrage des options :
  :ALIAS_ARG=0, :ALLOW_RECUR=0, ALLOW_RECUR=0, :AUTOSAVE_CFG=0,
  :END_CMD_NL=0, :EXIT_ON_ERR=1, :FILE_CONFIRM=1, :ENCODING=ISO-8859-1,
  :HISTORY_DBL=0, :LIST_SUBSTIT=1
 Moteur d'évaluation : Javascript

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
