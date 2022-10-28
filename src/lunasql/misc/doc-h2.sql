/*
 * Package *DOC-H2*
 *
 * Documentation sur la base H2 depuis la console SQL
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

def -r BACKUP    {spec BACKUP $(arg_ls);};
def -r MERGE     {spec MERGE $(arg_ls);};
def -r RUNSCRIPT {spec RUNSCRIPT $(arg_ls);};
def -r SCRIPT    {spec SCRIPT $(arg_ls);};
def -r CHECKPOINT {spec CHECKPOINT $(arg_ls);};
def -r SAVEPOINT {spec SAVEPOINT $(arg_ls);};
def -r h2-show   {spec -s SHOW $(arg_ls);};

def H2HOME https://www.h2database.com;

def -r h2-home     {shell -d $(H2HOME);};
def -r h2-cheat    {shell -d $(H2HOME)/html/cheatSheet.html;};
def -r h2-start    {shell -d $(H2HOME)/html/quickstart.html;};
def -r h2-install  {shell -d $(H2HOME)/html/installation.html;};
def -r h2-perfs    {shell -d $(H2HOME)/html/performance.html;};
def -r h2-features {shell -d $(H2HOME)/html/features.html;};
def -r h2-advanced {shell -d $(H2HOME)/html/advanced.html;};

def -r h2-command {
  if [$(arg_nb)<1] {exit 1 "usage: h2-command <cmd_name>";};
  shell -d $(H2HOME)/html/commands.html#$(arg1);
};
def -r h2-function {
  if [$(arg_nb)<1] {exit 1 "usage: h2-function <func_name>";};
  shell -d $(H2HOME)/html/functions.html#$(arg1);
};
def -r h2-grammar {
  if [$(arg_nb)<1] {exit 1 "usage: h2-grammar <sql_keyword>";};
  shell -d $(H2HOME)/html/grammar.html#$(arg1);
};
def -r h2-datatype {
  if [$(arg_nb)<1] {exit 1 "usage: h2-datatype <data_type>";};
  shell -d $(H2HOME)/html/datatypes.html#$(arg1)_type;
};

def -r h2-help {
  if [$(arg_nb)<1] {exit 1 "usage: h2-help <sql_keyword>";};
  print -c8 $[select TOPIC||CHAR(10)||TEXT||CHAR(10)||CHAR(10)||SYNTAX ^
              from INFORMATION_SCHEMA.HELP where upper(TOPIC) = upper('$(arg_ls)');];
};
def -r h2-search {
  if [$(arg_nb)<1] {exit 1 "usage: h2-search <sql_keyword>";};
  opt -l :SELECT_ARRAY 0; opt -l :COL_MAX_WIDTH 512; opt -l :ADD_ROW_NB 0;
  spec -s help $(arg_ls);
};
def -r h2-webconsole {
  start - {shell "javaw org.h2.tools.Console -user ^"$(*arg1?sa)^" -password ^"$(*arg2)^" -url ^"$(_CNX_PATH)^" -driver $(_CNX_DRIVER)";};
};
def -r h2-import {
  if [$(arg_nb)<2] {exit -1 "usage : h2-import table fichier [cols] [where]";};
  INSERT INTO $(arg1) SELECT $(*arg3?*) FROM CSVREAD('$(arg2)') WHERE $(*arg4?TRUE);
};

-- Fonctions utilitaires
def -r exists {
  if [$(arg_nb)<1] {exit 1 "usage: exists <table>";};
  return  $[select count(*) from INFORMATION_SCHEMA.TABLES ^
            where TABLE_SCHEMA='PUBLIC' and upper(TABLE_NAME) like upper('$(arg1)');];
}
help -a exists "  Retourne 1 si la table t existe
  Supporte le caractère joker SQL '%' en nom de table";

----------- Aide du package -----------

def -cr help-doc-h2 {
print "Module *DOC-H2* : documentation du moteur de BDD H2
 Constante ajoutée :
  - H2HOME         : $(H2HOME)
 Fonctions ajoutées :
  - h2-help <w>    : affiche l'aide sur le mot-clef w
  - h2-search <w>  : recherche les aides sur le mot-clef w
  Les fonctions suivantes ouvrent un navigateur
  (nécessitent donc une connexion à Internet) :
  - h2-home        : ouvre $(H2HOME)
  - h2-cheat, h2-start, h2-install, h2-features
    h2-perfs, h2-advanced
  - h2-command <c>   : aide sur la commande SQL c
  - h2-function <f>  : aide sur la fonction f
  - h2-grammar <w>   : aide sur le mot-clef SQL w
  - h2-datatype <t>  : aide sur le type de données t
  - h2-show <prm>    : commande SHOW de H2
  - commandes BACKUP, MERGE, RUNSCRIPT, SCRIPT, COMMENT,
      TRUNCATE, CHECKPOINT, SAVEPOINT
  - h2-webconsole <u> <p> : ouverture de la console H2 web
  - h2-import <t> <f> [c] [w] : import d'un fichier CSV pour H2
      table fichier colonnes where
  - exists <t>   : retourne le nb de tables correspondant à t

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
