/*
 * Package *MAIL*
 * 
 * Outil de d'envoi de messages par email
 * Nécessite la bibliothèque JavaMail
 */

---------- Corps du package -----------

opt -l :EXIT_ON_ERR 0;

plugin lunasql.misc.CmdMail;
if '$(_CMD_STATE)'=='E' {
  exit 9 "Classes JavaMail d'envoi de courriels inaccessible.
Avez-vous ajouté la bibliothèque correspondante au CLASSPATH ?
Lien : http://www.oracle.com/technetwork/java/javamail/index.html";
};

----------- Aide du package -----------

def -cr help-mail {
print "Module *MAIL* : outil d envoi de messages par mail
 Commandes ajoutées
  - MAIL   : envoi de message
 Tapez help <cmd> pour de l'aide sur une commande";
};

---------------------------------------
return 1;
