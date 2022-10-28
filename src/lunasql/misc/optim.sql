/*
 * Package *OPTIM*
 *
 * Module de syntaxe optimale pour la majorité des usages
 */

---------- Corps du package -----------

if $[opt -d _DENY_OPT_CMD] {
  exit 9 "Module 'optim' impossible à charger car commande OPT interdite";
}

engine js;

opt :ALIAS_ARG    0;
opt :ALLOW_RECUR  1;
opt :ALLOW_REDEF  1;
opt :AUTOSAVE_CFG 0;
opt :END_CMD_NL   1;
opt :EXIT_ON_ERR  1;
opt :FILE_CONFIRM 0;
opt :ENCODING     ISO-8859-1;
opt :HISTORY_DBL  0;
opt :LIST_SUBSTIT 1;
opt :ON_ERROR     "";
opt :PROMPT       SQL;

print -c=7 "OpTimaL ModE aKtivaTeD";

----------- Aide du package -----------

def -cr help-optim {
print "Module *OPTIM* : syntaxe pour la majorité des usages
 Paramétrage des options :
  :ALIAS_ARG=0, :ALLOW_RECUR=1, :ALLOW_REDEF=1, :AUTOSAVE_CFG=0,
  :END_CMD_NL=1, :EXIT_ON_ERR=1, :FILE_CONFIRM=0, :ENCODING=ISO-8859-1,
  :HISTORY_DBL=0, :LIST_SUBSTIT=1
 Moteur d'évaluation : Javascript

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
