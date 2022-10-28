/*
 * Package *GEEK*
 *
 * Outils d'utilité encore inconnue
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;
engine JavaScript;

-- Affichage d'un message d'une ligne dit par une vache
-- liste des accents : http://www.pjb.com.au/comp/diacritics.html
let {function cowsay(txt){
  var dash='';
  for(var i=0;i<txt.length;i++)dash=dash + '-';
  print(' -'+dash+'- ');
  print('( '+txt+' )');
  print(' -'+dash+'- ');
  print('    \\  ^__^ ');
  print('     \\ (oo)|_______');
  print('       (__)|       )/\\/');
  print('           ||----w |');
  print('           /|     ||');
};};
def cowsay { cowsay('$(_l)'); return; };

if $(_NVERBOSE)>=2 {
  let {cowsay('Bienvenue aux codeurs invertébrés !');};
}
-- TODO: citation du jour
--void $[var s,br=new Packages.java.io.BufferedReader(new Packages.java.io.InputStreamReader(
--      new Packages.java.net.URL('http://www...').openStream()));
--   while((s=br.readLine())!=null)print(s);br.close();];

----------- Aide du package -----------

help -a cowsay  "Affichage d'un pov' message par une vache";

def -cr help-geek {
  print "Module *GEEK* : module aussi inutile qu'indispensable !
 Fonctions ajoutées
  - cowsay : affichage d'un message d'une ligne par une vache
             requiert le moteur ScriptEngine Javascript

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
