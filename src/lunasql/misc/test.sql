/*
 * Package *TEST*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 0;

plugin lunasql.misc.CmdTest;
if '$(_CMD_STATE)'=='E' {exit 9 "Chargement de la classe Test impossible";};

def -r if-def {
  arg key:idopt codeY [codeN];
  if $[def -d $(key);] {eval $(codeY);} elseif {!$(codeN#empty?)} {eval $(codeN);};
};
def -r if-ndef {
  arg key:idopt codeY [codeN];
  if !$[def -d $(key);] {eval $(codeY);} elseif {!$(codeN#empty?)} {eval $(codeN);};
};
def -r check-eq {
  arg key:idopt val;
  if ['$($(key))'!='$(val)'] {error "$(key) (= '$($(key))') est différent de '$(val)' !";};
};
def -r check-neq {
  arg key:idopt val;
  if ['$($(key))'=='$(val)'] {error "$(key) est égal à '$(val)' !";};
};
def -r equal {
  arg val1 val2;
  if ['$(val1)'!='$(val2)'] {error "'$(val1)' est différent de '$(val2)' !";};
};
def -r differ {
  arg val1 val2;
  if ['$(val1)'=='$(val2)'] {error "'$(val1)' est égual à '$(val2)' !";};
};
def -r if-eq {
  arg key:idopt val codeY [codeN];
  if $[str eq? $($(key)) $(val);] {eval $(codeY);} elseif {!$(codeN#empty?)} {eval $(codeN);};
};
def -r if-neq {
  arg key:idopt val codeY [codeN];
  if $[str neq? $($(key)) $(val);] {eval $(codeY);} elseif {!$(codeN#empty?)} {eval $(codeN);};
};
def -r if-sup {
  arg key:idopt val codeY [codeN];
  if ['$($(key))'>'$(val)'] {eval $(codeY);} elseif {!$(codeN#empty?)} {eval $(codeN);};
};
def -r if-inf {
  arg key:idopt val codeY [codeN];
  if ['$($(key))'<'$(val)'] {eval $(codeY);} elseif {!$(codeN#empty?)} {eval $(codeN);};
};
def -r if-supeq {
  arg key:idopt val codeY [codeN];
  if ['$($(key))'>='$(val)'] {eval $(codeY);} elseif {!$(codeN#empty?)} {eval $(codeN);};
};
def -r if-infeq {
  arg key:idopt val codeY [codeN];
  if ['$($(key))'<='$(val)'] {eval $(codeY);} elseif {!$(codeN#empty?)} {eval $(codeN);};
};
def -r if-is {
  arg key:idopt op:`==|<=?|>=?` val:num codeY [codeN];
  if ['$($(key))'$(op)'$(val)'] {eval $(codeY);} elseif {!$(codeN#empty?)} {eval $(codeN);};
};

def -r run-tests {
  print "Lancement des tests";
  time init;
  def nbtot,nberr 0 0;
  for v $[list filter $[info globals;] {str starts? $(arg1) test-;};] {
    print "$[str lpad $(nbtot#inc!) 3 0;] - $v";
    eval {
      $v;
      print -c=3 "  OK";
    } -c {
      void $(nberr#inc!);
      print -c=2 "  $(err_msg)";
    };
  };
  if [$(nberr) == 0] {print -c=3 "$<n>Tests réussis";} ^
  else {print -c=2 "$<n>Tests échoués : $(nberr)";};
  print "Total : $(nbtot) ($[time chron;])";
  undef nbtot nberr;
  put;
};

----------- Aide du package -----------

help -a if-def "Teste l'existence d'une variable
Usage :  if-def <var> { code si définie } [{ code sinon }]";
help -a if-ndef "Teste la non existence d'une variable
Usage :  if-ndef <var> { code si non définie } [{ code sinon }]";
help -a check-eq  "Vérifie l'égalité d'une variable à une valeur
Usage :  check-eq <var> <val>";
help -a check-neq "Vérifie la valeur d'une variable (différence)
Usage :  check-neq <var> <val>";
help -a equal  "Vérifie que 2 chaînes sont identiques
Usage :  equal <val1> <val2>";
help -a differ "Vérifie que 2 chaînes sont différentes
Usage :  differ <val1> <val2>";
help -a if-eq  "Teste si une variable égale à une valeur
Usage :  if-eq <var> <val> { code si éguale } [{ code sinon }]";
help -a if-neq "Teste si une variable diffère d'une valeur
Usage :  if-neq <var> val> { code si différente } [{ code sinon }]";
help -a if-sup "Teste si une variable est supérieure à une valeur
Usage :  if-sup <var> <val> { code si supérieure } [{ code sinon }]";
help -a if-inf "Teste si une variable est inférieure à une valeur
Usage :  if-inf <var> <val> { code si inférieure } [{ code sinon }]";
help -a if-supeq "Teste si une variable est supérieure ou égale à une valeur
Usage :  if-supeq <var> <val> { code si >= } [{ code sinon }]";
help -a if-infeq "Teste si une variable est inférieure ou égale à une valeur
Usage :  if-infeq <var> <val> { code si <= } [{ code sinon }]";
help -a if-is  "Teste une variable par rapport à une valeur
Usage :  if-is <var> <op> <val> { code si ok } [{ code sinon }]
<op> doit être ==, <, <=, > ou >=";
help -a run-tests "Lance les tests définis par macros
Toutes les macros à tester doivent s'appeler test-*. Elles peuvent
contenir la commande TEST, ou toute autre commande susceptible de
générer une erreur.";

def -cr help-test {
print "Module *TEST* : utilitaires de tests unitaires
 Commandes ajoutées
  - TEST : lancement du test unitaire
 Fonctions ajoutées
  - if-def : teste si une variable est déclarée
  - if-ndef : teste si une variable n'est pas déclarée
  - check-eq  : vérifie l'égalité d'une variable à une valeur
                usage : check-eq <varname> <value>
  - check-neq : idem mais vérifie que différent
  - equal  : vérifie que deux chaînes sont identiques
  - differ : vérifie que deux chaînes sont différentes
  - if-eq  : teste si une variable égale à une valeur
  - if-neq : teste si une variable diffère d'une valeur
  - if-inf : teste si une variable est inférieure à une valeur
  - if-sup : teste si une variable est supérieure à une valeur
  - if-infeq : teste si une variable est inf. ou égale à une valeur
  - if-supeq : teste si une variable est sup. ou égale à une valeur
  - if-is  : teste une variable par rapport à une valeur
  - run-tests : lance les tests définis en macros test-*
 Ces fonctions requièrent le moteur ScriptEngine JavaScript
 Tapez  help <cmd> pour de l aide sur une commande

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
