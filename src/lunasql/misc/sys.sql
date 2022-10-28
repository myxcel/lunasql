/*
 * Package *SYS*
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 1;

-- Commandes Shell pour Windows et *Nix
engine js;  -- pour JV, OS, OSWin, WD, ENV
let {var
  JV=Packages.java.lang.System.getProperty('java.version')||'',
  OS=Packages.java.lang.System.getProperty('os.name')||'',
  OSWin=OS.startsWith('Windows'),
  WD=Packages.java.lang.System.getProperty('user.dir')||'',
  ENV=Packages.java.lang.System.getenv;
};
def -r fc   {shell "$[OSWin ? 'fc' : 'diff'] $(arg_ls)";};
def -r ls   {shell "$[OSWin ? 'dir' : 'ls'] $(arg_ls)";};
def -r cp   {shell "$[OSWin ? 'xcopy' : 'cp'] $(arg_ls)";};
def -r md   {shell "$[OSWin ? 'md' : 'mkdir'] $(arg_ls)";};
def -r mv   {shell "$[OSWin ? 'move' : 'mv'] $(arg_ls)";};
def -r rm   {shell "$[OSWin ? 'del' : 'rm'] $(arg_ls)";};
def -r cat  {shell "$[OSWin ? 'type' : 'cat'] $(arg_ls)";};
def -r gc   {let java.lang.System.gc();};
def -r pwd  {print $[WD];};
def -r halt {
  print "Extinction de l'ordinateur...";
  shell "$[OSWin ? 'shutdown -s -t 0' : 'shutdown -h now';]";
  quit;
};
def -r env {return $[$(arg_nb)==0 ? ENV() : ENV('$(arg1)');];};

-- Commandes spécifiques *Nix
when !OSWin;
  def -r find  {shell "find $(arg_ls)";};
  def -r tar   {shell "tar $(arg_ls)";};
  def -r gzip  {shell "gzip $(arg_ls)";};
  def -r grep  {shell "grep $(arg_ls)";};
  def -r ps    {shell "ps $(arg_ls)";};
  def -r kill  {shell "kill $(arg_ls)";};
  def -r date  {shell "date $(arg_ls)";};
  def -r uptime  {shell "uptime $(arg_ls)";};
  def -r locate  {shell "locate $(arg_ls)";};
  def -r which {shell "which $(arg_ls)";};
  def -r whoami {shell "whoami";};
end;

def -r list-dir {
  print "$[str rpad File 30;]  Dir?         Size";
  print $<'-'*49>;
  for f $[file glob . $(*arg1?.*);] {
    print "$[str rpad $f 30;]  $[file dir? $f;]    $[str lpad $[file size $f;] 12;]";
  };
};

def -r log-new {
  if $[def -d logfile;] {exit 1 "fichier log déjà ouvert";};
  def logfile $[file open $(*arg1?$[time compact;].log) a;];
  file writeln $logfile "+ LunaSQL : $(_VERSION) - $(_VERS_NAME)";
  file writeln $logfile "+ Chemin  : $(_CNX_PATH)";
  file writeln $logfile "+ Session : $(_SESSION_ID)";
  file writeln $logfile "$<'-'*18> Début du traitement : $[time datetime;] $<'-'*18>";
};
def -r log {
  file writeln $logfile "$[time datetime;] : $(*arg1)";
};
def -r log-end {
  file writeln $logfile "$<'-'*20> Fin du traitement : $[time datetime;] $<'-'*18>$<n>";
  file close $logfile;
  undef logfile;
};

----------- Aide du package -----------

help -a list-dir "Liste les fichiers du répertoire courant et leur taille
   Usage : list-dir [pattern]
   <pattern> : patron regexp de fichiers, par défaut : .*
   Exemple : liste des fichiers ne commençant pas par '.' :
     list-dir ^"^[^.].*^"";
help -a log-new "Ouverture d'un flux de sortie de log
   Usage : log-new [file]
   Sans argument, le fichier en sortie est nommé à la date du jour.";
help -a log "Écriture d'une ligne de log
   Usage : log [ligne]
   Nécessite d'avoir ouvert le flux par log-new";
help -a log-end "Fermeture du flux de sortie de log
   Usage : log-end
   Nécessite d'avoir ouvert le flux par log-new";

def -cr help-sys {
print "Module *SYS* : utilitaires de commandes système
 Fonctions ajoutées
  - log-new      : ouverture d'un flux de log
  - log          : écriture dans le flux de log
  - log-end      : fermeture du flux de log
 Tapez  help <cmd> pour de l'aide sur une commande
 Commandes shell
 Note: sous Windows, pour certaines commandes comme ls ou cat,
       il faudra créer et appeler un fichier .bat
  - env [var] : retourne une variable d'environnement
  - ls     : liste les fichiers du rép. courant
  - cat    : liste le contenu d'un fichier texte
  - cp     : copie un ou plusieurs fichiers
  - fc     : compare deux fichiers entre eux
  - md     : crée un nouveau répertoire
  - mv     : déplace ou renomme un ou plusieurs fichiers
  - rm     : supprime un ou plusieurs fichiers
  - pwd    : affiche le rép. courant
  - halt   : arrête l'ordinateur
  - find   : recherche un fichier (Unix seulement)
  - tar    : archive des fichiers (Unix seulement)
  - gzip   : compresse des fichiers (Unix seulement)
  - ps     : liste les processus actifs (Unix seulement)
  - kill   : envoie un signal à un process (Unix seulement)
  - which  : affiche le chemin d'une commande (Unix seulement)
  - gc     : appelle le ramasse-miettes (System.gc())
 Valeurs/fonctions Javascript
  - JV     : version du runtime Java
  - OS     : nom du système d'exploitation
  - OSWin  : true si OS==Windows, false sinon
  - WD     : répertoire de travail
  - ENV()  : environnement. Retourne Map<String,String>
             ex. ENV(), ENV('PATH'), ENV().keySet()
 Macro internes :
   - list-dir [f]: liste les fichiers du rép. courant (macro)
            selon patron de nom de fichier regexp

 Tapez help-<lib> pour de l'aide sur une bibliothèque";
};

---------------------------------------
return 1;
