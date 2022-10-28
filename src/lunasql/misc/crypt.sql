/*
 * Package *CRYPT*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 0;

plugin lunasql.misc.CmdHash;
if '$(_CMD_STATE)'=='E' {exit 9 "Chargement de la classe Hash impossible";};
plugin lunasql.misc.CmdCrypt;
if '$(_CMD_STATE)'=='E' {exit 9 "Chargement de la classe Crypt impossible";};
plugin lunasql.misc.CmdDcrypt;
if '$(_CMD_STATE)'=='E' {exit 9 "Chargement de la classe Dcrypt impossible";};
plugin lunasql.misc.CmdSign;
if '$(_CMD_STATE)'=='E' {exit 9 "Chargement de la classe Sign impossible";};
plugin lunasql.misc.CmdVerify;
if '$(_CMD_STATE)'=='E' {exit 9 "Chargement de la classe Verify impossible";};

def -r list-trust-keys {
  arg [search];
  if $[str nempty? $(:SIGN_KEY);] {
    def -l mykey $[str substr $(:SIGN_KEY) 0:$[str index $(:SIGN_KEY) |;];];
    if $[str starts? $(mykey) $search;] {
        print -c=4 "my: $(mykey) ($[str substr $(mykey) 0:10;])";
    };
  };
  def i 0;
  if $[str nempty? $(:SIGN_TRUST);] {
    for pk $[str split $(:SIGN_TRUST) ,;] {
      if $[str starts? $(pk) $(search);] {
        print -c=5 "$[str lpad $(i#inc!) 2 0;]: $(pk) ($[str substr $(pk) 0:10;])";
      };
    };
  };
  undef i;
  put;
};

def add-trust-key {
  arg key;
  if !$[list has? [43 44] $[str len $(key);];] {
    error "La clef (base64) doit faire 43 (ou 44 si présence d'un '=' final)";
  };
  if $[str len $(key);]==44 {def -u1 key $[str chop $(key);];};
  if $[str has? $(:SIGN_TRUST) $(key);] {return 0;};
  opt :SIGN_TRUST $(:SIGN_TRUST),$(key);
  print "Clef ajoutée. Pensez à mettre à jour le fichier $[file name $(_CONFIG_FILE);]!";
  return 1;
};

def del-trust-key {
  arg key;
  if !$[list has? [43 44] $[str len $(key);];] {
    error "La clef (base64) doit faire 43 (ou 44 si présence d'un '=' final)";
  };
  if $[str len $(key);]==44 {def -u1 key $[str chop $(key);];};
  def -l keylist $[str split $(:SIGN_TRUST) ,;];
  def -l i $[list index $(keylist) $(key);];
  if $(i=-1) {return 0;};
  opt :SIGN_TRUST $[list join $[list remove $(keylist) $(i);] ,;];
  print "Clef supprimée. Pensez à mettre à jour le fichier $[file name $(_CONFIG_FILE);]!";
  return 1;
};

----------- Aide du package -----------

help -a list-trust-keys "Liste les clefs publiques de confiance
Usage :  list-trust-keys [<start>]";
help -a add-trust-key "Ajoute une clef publique de confiance
Usage :  add-trust-key <key>
Note : le fichier de config. est réécrit (options uniquement)";
help -a del-trust-key "Supprime une clef publique de confiance
Usage :  del-trust-key <key>
Note : le fichier de config. est réécrit (options uniquement)";

def -cr help-crypt {
print "Module *CRYPT* : outils de cryptographie
 Commandes ajoutées
  - HASH   : calcule l'empreinte d'un message
  - CRYPT  : chiffre un message en AES 128/256 bits
  - DCRYPT : déchiffre un message chiffré
  - SIGN   : signe un message par EdDSA (Ed25519)
  - VERIFY : verifie une signature numérique
 Fonctions ajoutées
  - list-trust-keys : liste les clefs de confiance
  - add-trust-key : ajoute une clef de confiance
  - del-trust-key : supprime une clef de confiance
 Tapez help <cmd> pour de l'aide sur une commande

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
