/*
 * Package *DOC-SQL*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

-- Documentation SQL.sh
def SQLSH https://sql.sh;

def -r sqlsh-home {shell -d $(SQLSH);};
def -r sqlsh-word {
  if [$(arg_nb)<1] {exit 1 "usage: sqlsh-word <keyword>";};
  shell -d $(SQLSH)/cours/$(arg1);
};
def -r sqlsh-func {
  if [$(arg_nb)<1] {exit 1 "usage: sqlsh-word <function>";};
  shell -d $(SQLSH)/fonctions/$(arg1);
};
def -r sqlsh-cours-pdf {shell -d $(SQLSH)/ressources/cours-sql-sh-.pdf;};
def -r sqlsh-jointures {shell -d $(SQLSH)/2401-sql-join-infographie;};

-- Documentation W3C
def W3SHOME http://www.w3schools.com;

def -r w3s-home {shell -d $(W3SHOME);};
def -r w3s-sql  {shell -d $(W3SHOME)/sql/default.asp;};
def -r w3s-sql-ref  {shell -d $(W3SHOME)/sql/sql_ref_keywords.asp;};
def -r w3s-js   {shell -d $(W3SHOME)/js/default.asp;};

def -r w3s-sql-word {
  if [$(arg_nb)<1] {exit 1 "usage: w3s-sql-word <keyword>";};
  shell -d $(W3SHOME)/sql/sql_$(arg1).asp;
};
def -r w3s-sql-func {
  if [$(arg_nb)<1] {exit 1 "usage: w3s-sql-func <function>";};
  shell -d $(W3SHOME)/sql/sql_func_$(arg1).asp;
};
def -r w3s-js-topic {
  if [$(arg_nb)<1] {exit 1 "usage: w3s-js-topic <topic>";};
  shell -d $(W3SHOME)/js/js_$(arg1).asp;
};
def -r w3s-js-object {
  if [$(arg_nb)<1] {exit 1 "usage: w3s-js-object <object>";};
  shell -d $(W3SHOME)/jsref/jsref_obj_$(arg1).asp;
};

----------- Aide du package -----------

help -a sqlsh-home "Ouvre un navigateur vers $(SQLSH)
 Commandes associées : sqlsh-word, sqlsh-func,
  sqlsh-cours-pdf, sqlsh-jointures";
help -a w3s-home "Ouvre un navigateur vers $(W3SHOME)
 Commandes associées : w3s-sql, w3s-sql-ref, w3s-js,
  w3s-sql-word, w3s-sql-func, w3s-js-topic, w3s-js-object";

def -cr help-doc-sql {
print "Module *DOC-SQL* : documentations SQL
 Fonctions ajoutées (ouvrent un navigateur,
 donc nécessitent une connexion à Internet):
  - sqlsh-home    : navigue vers $(SQLSH)
  - sqlsh-word    : navigue vers SQL.sh mot-clef SQL
  - sqlsh-func    : navigue vers SQL.sh function SQL
  - sqlsh-cours-pdf : ouvre un cours SQL pdf en français
  - sqlsh-jointures : ouvre un mémento sur les jointures
  - w3s-home      : navigue vers $(W3SHOME)
  - w3s-sql       : navigue vers W3 SQL
  - w3s-sql-ref   : navigue vers W3 SQL
  - w3s-js        : navigue vers W3 JS
  - w3s-sql-word  : navigue vers W3 mot-clef SQL
  - w3s-sql-func  : navigue vers W3 fonction SQL
  - w3s-js-topic  : navigue vers W3 sujet JS
  - w3s-js-object : navigue vers W3 objet JS
 Tapez  help <cmd> pour de l'aide sur une commande

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
