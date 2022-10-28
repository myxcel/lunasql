/*
 * Package *OBJ-BASE*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

def -r defrecord {
  arg name:id keys;
  -- Macro de création new
  def -l defm-new "defmacro new-$(name) {$(keys)} {^
put ^"^$[dict new _struct $(name);]";
for k $(keys) {
  def -u1 defm-new "$(defm-new)$(k:!:.*=)=^$($(k))$<n>";
};
def -l defm-new "$(defm-new)^";$<n>} {Création d'un objet '$(name)'} 0 1;";
  eval defm-new;
  -- Macro de lecture en BDD read
  def -l defm-read "defmacro read-$(name) {*sql} {^
def -l $(name)-keys ^"";
for k $(keys) {
  def -u1 defm-read "$(defm-read)$(k:!:.*=) ";
};
def -l defm-read "$(defm-read)^";
def -l tmpbdd ^"^$[list get ^$[seek ^$(sql);] 0;]";
def -u1 defm-read "$(defm-read)^";
def -l errobj ^$[list minus ^$($(name)-keys) ^$[dict keys ^$(tmpbdd);];];  -- pas en obj
if ^$(errobj#len)>0 {
  error ^"clefs absentes en objet '$(name)' : ^$(errobj)^";
};
def -l errbdd ^$[list minus ^$[dict keys ^$(tmpbdd);] ^$($(name)-keys);];  -- pas en bdd
if ^$(errbdd#len)>0 {
  error ^"clefs inconnues pour '$(name)' : ^$(errbdd)^";
};
put ^"^$(tmpbdd)_struct $(name)$<n>^";$<n>} ^
{Lecture d'un objet '$(name)' depuis une requête} 0 1;";
  eval defm-read;
  -- Macro d'accès get (par référence)
  def -l defm-get "defmacro $(name)-get {obj key} {^
if ^$[str neq? ^$[dict get ^$(^$(obj)) _struct;] $(name);] {
  error ^"ceci n'est pas un objet '$(name)'^";
};
if ^$[dict has-key? ^$(^$(obj)) ^$(key);] {
  dict get ^$(^$(obj)) ^$(key);
} else {
  error ^"clef '^$(key)' inconnue pour '$(name)'^";
};$<n>} {Lecture d'un attribut de l'objet '$(name)'} 0 1;";
  eval defm-get;
  -- Macro d'accès set (par référence)
  def -l defm-set "defmacro $(name)-set {obj key val} {^
if ^$[str neq? ^$[dict get ^$(^$(obj)) _struct;] $(name);] {
  error ^"ceci n'est pas un objet '$(name)'^";
};
if ^$[dict has-key? ^$(^$(obj)) ^$(key);] {
  def ^$(obj) ^"^$[dict put ^$(^$(obj)) ^$(key) ^$(val);]^";
  put ^$(val);
} else {
  error ^"clef '^$(key)' inconnue pour '$(name)'^";
};$<n>} {Modification d'un attribut de l'objet '$(name)'} 0 1;";
  eval defm-set;
  -- Macro de suppression de l'objet
  defmacro del-$(name) {obj} {undef $(obj);} "Suppression d'un objet '$(name)'" 0 1;
};
def -r defr defrecord;

def -r undefrecord {
  arg name:id;
  -- Macro de suppression undef
  undef new-$(name) read-$(name) $(name)-get $(name)-set del-$(name);
  put;
};
def -r undefr undefrecord;

let {
  /*
   * Defining the base constructor for all classes, which will execute the final class prototype's initialize method if exists
   * Merci à http://blog.xebia.fr/2013/06/10/javascript-retour-aux-bases-constructeur-prototype-et-heritage/
   */
  var Class = function() {
      this.initialize && this.initialize.apply(this, arguments);
  };
  Class.extend = function(childPrototype) {
      var parent = this;
      var child = function() { return parent.apply(this, arguments); };
      child.extend = parent.extend;
      var Surrogate = function() {};
      Surrogate.prototype = parent.prototype;
      child.prototype = new Surrogate;
      for (var key in childPrototype) child.prototype[key] = childPrototype[key];
      return child;
  };
};

----------- Aide du package -----------

help -a defr "Cf. aide de defrecord";
help -a defrecord "Définition d'un patron de structure de données$<n>
Exemple d'utilisation :
  defrecord personne {NOM:`[A-Z][a-z]+` AGE:int SEX:`M|F`}
  def j ^$[new-personne Jean 28 M]
  def m ^$[new-personne Marie 42 F]
  def z ^$[read-personne $<c>
           select NOM, AGE, SEX from AMIS where NOM='Zoé']
  personne-get m SEX --> F
  personne-get j AGE --> 28
  personne-set j AGE 29
  del-personne j
  undefrecord personne
Note : requiert le moteur ScriptEngine Javascript";
help -a undefrecord "Suppression d'un patron d'objet";
help -a undefr "Cf. aide de undefrecord";

def -cr help-obj-base {
print "Module *obj-base* : manipulation d'objets de base
 Fonctions ajoutées :
  - defrecord : définition d'un patron de structure de données
    (raccourci : defr)
  - undefrecord : suppression du patron (raccourci : undefr)
 Tapez help <cmd> pour de l'aide sur une commande
 Fonctions Javascript :
  - Class.extend() usage :
    var Dog = Class.extend({
       initialize : function() { this.numberOfLegs = 4; },
       bark : function() { print('wouf wouf'); }
    });
    var Doberman = Dog.extend({ growl : function() { print('aouwww'); } });
    var rufus = new Doberman(); // now play with it !

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;

