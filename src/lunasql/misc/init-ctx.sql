/*
 * Script d'initialisation du contexte, exécuté dès la création du contexte
 * Version : 31/01/2018, révision 05/10/2020
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

def -r reinit {exec "$(_INIT_FILE)";};
def -r status {'$(_CMD_STATE)'=='E' ? 'ERREUR':'OK';};
def -r retcmd {print "return: $(_RET_VALUE)";};
def -r tutorial {exec jar:/lunasql/misc/tutorial.sql;};
def -r loaded {print "loaded: $(_LOADED_LIBS)";};
def -r is-loaded {
  arg lib;
  list has? $[str split $(_LOADED_LIBS) $[file pathsep;];] $(lib);
};

def -r shift {
  def -l tmp $(arg_ls\1);
  def -u1 arg_ls $(tmp#shift);
};

def -r with {
  arg vars body;
  if $(_CMD_STATE=E) {
    error "Arguments incorrects, taper 'help with'";
  };
  eval -v $(vars) $(body);
};

def -r defmacro {
  arg name:id sign body [doc] [net 1] [ref 0];
  if $(_CMD_STATE=E) {
    error "Arguments incorrects, taper 'help defmacro'";
  };
  -- Construction du corps
  def -l sign1,body1 "" "arg;$<n>";
  if [$(sign#len)>0] {
    opt -l :END_CMD_NL 0; -- cleansql
    def -u1 sign1 $[str cleansql $(sign) 0;];
    if [$(sign1#len)>0] {
      def -u2 body1 "arg $(sign1)$<n>if ^$(_CMD_STATE=E) { exit 9 ^"Sortie de $(name)^"; };$<n>";
    }
  };
  if $(net=1) {
    -- Nettoyage
    if $(ref=1) {
      def -r $(name) "$(body1)$<n>$[str cleansql $(body) 2;]";
    } else {
      def $(name) "$(body1)$<n>$[str cleansql $(body) 2;]";
    };
  } else {
    if $(ref=1) {
      def -r $(name) "$(body1)$<n>$(body)";
    } else {
      def $(name) "$(body1)$<n>$(body)";
    };
  };
  -- Ajout de la documentation formatée
  if $(sign1#empty?) {
    def -u1 sign1 ";";
  } else {
    def -u1 sign1 "  $(sign1)";
  };
  help -at $(name) "$[str wrap ^"$(name)$(sign1)^" 70 $<e*4> -1;]$<n*2>$[str wrap $(doc) 72 $<e*2>;]";
};
def -r defm defmacro;
def -r undefmacro {
  arg name:id;
  -- Macro de suppression undef
  undef $(name);
};
def -r undefm undefmacro;

def -r backup {
  arg nom:id;
  if !$[def -d $(nom)] {
    error "La variable '$(nom)' n'existe pas !";
  };
  def $(nom)-bck$[time now HHmmssSS;] $($(nom));
  print "Variable '$(nom)' sauvée";
};

def -r help-init {
  print -e "Aucune aide de fichier d'initialisation n'est définie";
  print "Définissez donc la macro d'aide 'help-init' en fichier
  $(_INIT_FILE)";
};
def -r hi help-init;

----------- Aide du package -----------

help -a reinit "Recharge le fichier init.sql";
help -a status "Affiche l'état de la dernière cmd";
help -a loaded "Affiche les bibliothèques chargées (modules)";
help -a is-loaded "Teste si une bibliothèque est chargée";
help -a retcmd "Affiche le retour de la dernière commande";
help -a tutorial "Lance le super didacticiel de LunaSQL";
help -a shift "Décalage des arguments en macro ou fichier
Usage : shift
Exemple :
   def f {print ^$arg_ls; shift; print ^$arg_ls}
   f do ré mi  --> affiche 'do ré mi' puis 'ré mi'";
help -a with "Exécute un bloc de code avec variables locales.
Utile pour les fonctions à arguments nommés, ou pour simplifier les
appels répétés à ^$(dict,key), cf aide de la commande ARG.

Usage : with <vars> <code>
Les variables sont de la forme k = v, séparées par une fin de ligne
ou par // (cf. aide de la commande DICT).
Exemple :
   with {a = 42
         b = 62} {
     print ^"a=^$a, b=^$b^"
   }";
help -a defm "Cf. aide de defmacro"
help -a defmacro "Définition d'une macro avec nommage des arguments
Usage : defmacro <name> <args> <body> [doc] [net 1] [ref 0]$<n>
Supporte la définition des arguments comme la commande ARG
avec en plus le support des sauts de lignes et des commentaires.
Alimente la documentation par help <name> par <doc>.
Si net = 1, le code est nettoyé des espaces et des commentaires
Si ref = 1, la macro sera exempte de contrôle de ref. circulaire
Notes : - defmacro ne crée pas de macro locale
        - utilise cleansql, qui analyse le bloc selon :END_CMD_NL
        - utilise tolines, qui formate l'aide en paragraphes
        - si une stucture est délimitée par {} (ex. dict), les
          données peuvent être suffixées par un ';'
        - requiert le moteur ScriptEngine JavaScript
$<n>Exemples :
   defmacro f ^" a b ^" { print ^$a-^$b } ^"Fonction de test^"
   defmacro f {a b [c 3]} { print ^$a-^$b-^$c} -- optionnel []
   defmacro f {a:int b:num *c} { print ^$a-^$b-^$c}  -- liste d'arg
   defmacro f {} {/* macro f */ print ^"nettoyé^"} ^"^" 1
   undefmacro f  -- suppression de f
Cas réel : macro d'incrémentation de variable par SE Javascript
   defmacro inc {var [n:int 1]} {def ^$var ^$[^$(^$var)+^$n]}
   def a 1; inc a; inc a 2; def a --> 4";
help -a undefmacro "Suppression d'une macro";
help -a undefm "Cf. aide de undefmacro";

help -a backup "Sauvegarde d'une variable/macro
La var. est copiée dans une nouvelle nommée <nom>-bck<HHMMSSMS>
Usage : backup <name>";

help -a help-init "Liste des définitions en fichier d'initialisation";
def -r help-base {
print "Module *BASE* : utilitaires chargés au démarrage
 Fonctions disponibles par défaut
  - reinit    : recharge le fichier init.sql
  - status    : affiche l'état de la dernière cmd
  - loaded    : affiche les bibliothèques chargées
  - is-loaded : teste le chargement d'une bibliothèque
  - retcmd    : affiche le retour de la dernière commande
  - defmacro  : définition de macro avec arguments et aide
  - shift     : décalage des arguments d'une macro
  - with      : exécution de code avec dict. de var. locales
  - backup    : sauvegarde d'une variable/macro
  - tutorial  : lance le didacticiel de LunaSQL
 Certaines requièrent le moteur ScriptEngine JavaScript
 Fonction JS chargées par défaut (cf. help js-funct) :
 String :
   trim(s), ltrim(s), rtrim(s), fulltrim(s), truncate(s,len),
   onlyLetters(s), onlyLettersNums(s), ends(s,r), starts(s,r),
   upper(s), lower(s), lpad(s,pad,len), rpad(s,pad,len),
   stripHtml(s), encodeHtml(s)
 Date :
   millis(), formatDate(format, date)
   Formats: yyyy-MM-dd HH:mm:ss:S q (quarter)
   Exemples:
      print ^$[formatDate()] -- default 'dd/MM/yyyy HH:mm:ss'
      print ^$[formatDate('dd/MM/yyyy')]
      print ^$[formatDate('HH h mm min ss sec S ms')]
      print ^$[formatDate('yyyy-MM-dd', new Date()]

 Array :
   sortn(arr) find(arr,s) map(arr,f) forEach(arr,f) indexOf(arr,n)
   lastIndexOf(arr,n) insert(arr,i,v) shuffle(arr) unique(arr)
   concat(arr,a) copy(arr,a) pop(arr) push(arr) shift(arr)
   slice(arr,a,c) splice(arr,a,c) unshift(arr,a) remove(arr,v)
   contains(arr,v) sortObjects(field,reverse,primer)
   sortObjectsByProperty(arr,field,reverse,primer)
   Documentation : http://4umi.com/web/javascript/array.php
 Divers :
  include(file) isString(obj) isArray(obj) isInt(obj) isFloat(obj)
  isEmail(obj) isUrl(obj) I(n)
  objectToJson(obj,sp) jsonToObject(str) printObject(obj)
  messageInfo(m, t) messageWarn(m, t) messageError(m, t)
  + objets Java :
  script_engine (alias engine, obj javax.script.ScriptEngine 'js')
  db_connection (lunasql.sql.SQLCnx)

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
