package lunasql.cmd;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.Config;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.sql.SQLCnx;
import lunasql.sql.TypesSGBD;

/**
 * Commande HELP <br>
 * (Interne) Affichage de l'aide de la console ou d'une commande
 * @author M.P.
 */
public class CmdHelp extends Instruction {

   private final OptionParser parser;

   public CmdHelp(Contexte cont) {
      super(cont, TYPE_CMDINT, "HELP", "?");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("a", "ajoute une aide à un alias (variable)");
      parser.accepts("t", "avec -a, l'aide n'est pas formatée en paragraphes");
      parser.accepts("d", "suppression d'une aide");
      parser.accepts("f", "exporte en fichier f toutes les aides").withRequiredArg().ofType(File.class)
         .describedAs("file");
      parser.nonOptions("rubrique").ofType(String.class);
   }

   @Override
   public int execute() {
      if (getLength() == 1) {
         if (cont.getVerbose() >= Contexte.VERB_AFF) cont.printlnX(getIntro(), Contexte.BR_WHITE);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

      } else if (getLength() >= 2) {
         StringBuilder sb = new StringBuilder();
         try {
            OptionSet options = parser.parse(getCommandA1());
            // Aide sur les options
            if (options.has("?")) {
               parser.printHelpOn(cont.getWriterOrOut());
               cont.setValeur(null);
               return RET_CONTINUE;
            }
            // Exportation de toutes les aides
            if (options.has("f")) {
               sb.append("spool \"").append(((File) options.valueOf("f")).getAbsolutePath()).append("\"; help; ");
               String[] libs = {"launching", "syntax", "delimiters", "commands", "substitutes", "expressions", "variables",
                                "catching", "js-funct", "packages", "libraries", "etikette", "bonus", "licenses", "changelog"};
               for (String l : libs) sb.append("help ").append(l).append("; ");
               sb.append("print; print \" -------------------------------- Bibliothèques chargées par défaut\";");
               sb.append("print; help-base; print; print; ");
               sb.append("print \" ------------------------------------- Liste des commandes internes\";");
               // Parcours de toutes les aides des commandes
               TreeMap<String, Instruction> cmds = cont.getAllCommandsTree();
               for (Map.Entry<String, Instruction> me : cmds.entrySet()) {
                  Instruction ins = me.getValue();
                  if ((ins.getType() == Instruction.TYPE_CMDSQL ||
                     ins.getType() == Instruction.TYPE_CMDINT ||
                     ins.getType() == Instruction.TYPE_MOTC_WH) && ins.getDesc() != null)
                     sb.append("help ").append(ins.getName()).append("; print; ");
               }
               sb.append("spool off;");
               new Lecteur(cont, sb.toString());
               cont.setValeur(null);
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
               return RET_CONTINUE;
            }

            // Exécution avec autres options
            List<?> lc = options.nonOptionArguments();

            if (options.has("a")) {
               if (lc.size() >= 2){
                  String c = (String)lc.get(0), h = listToString(lc, 1);
                  if (cont.isSet(c) && cont.isNonSys(c)) {
                     cont.addVarHelp(c, h, !options.has("t"));
                     if (cont.getVerbose() >= Contexte.VERB_BVR)
                        cont.println("-> aide fixée pour la commande : " + c);
                     cont.setVar(Contexte.ENV_RET_VALUE, h);
                  } else
                     return cont.erreur("HELP", "affectation d'aide à variable non définie", lng);
               } else
                  return cont.erreur("HELP", "avec l'option a, nom de variable et chaîne attendus", lng);
            } else if (options.has("d")) {
               if (lc.size() == 1) {
                  cont.removeVarHelp((String)lc.get(0));
               } else
                  return cont.erreur("HELP", "avec l'option d, nom de variable attendu", lng);
            } else for (int i = 0; i < lc.size(); i++) { // pour chaque cmd
               if (cont.getVerbose() == Contexte.VERB_SIL) {
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
                  return RET_CONTINUE;
               }

               sb.setLength(0);
               String cmdname = ((String)lc.get(i)).toUpperCase();

               // On commence par rechercher les COMMANDES et les VARIABLES/MACRO
               Instruction cmd = cont.getCommand(cmdname);
               String vh = cont.getVarHelp((String)lc.get(i));
               if (cmd != null) {
                  // Message si macro du même nom
                  if (vh != null && cont.getVerbose() >= Contexte.VERB_MSG) {
                     cont.errprintln("Note : nom de macro identique à une commande, la macro est ignorée");
                  }
                  // Affichage de l'aide de la commande interne/plugin
                  String help = cmd.getHelp();
                  sb.append("\n ").append(SQLCnx.frmI(" Commande " + cmd.getName(), 67, '-'));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  if (help == null)
                     cont.errprintln("  Commande à usage interne à l'application uniquement !");
                  else cont.printlnX(help, Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
               } else if (vh != null) {
                  // Affichage de l'aide de la fonction utilisateur
                  sb.append("\n ").append(SQLCnx.frmI(" Variable/Macro " + cmdname, 67, '-'))
                     .append('\n');
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.printlnX(vh, Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
               }

               // puis les rubriques d'aide
               else if ("COMMANDS".startsWith(cmdname)) {
                  sb.append("\n ").append(SQLCnx.frmI(" Liste des commandes", 67, '-')).append("\n\n");
                  TreeMap<String, Instruction> cmds = cont.getAllCommandsTree();
                  sb.append("Commandes SQL :\n");
                  for (Map.Entry<String, Instruction> me : cmds.entrySet()) {
                     Instruction ins = me.getValue();
                     if (ins.getType() == Instruction.TYPE_CMDSQL && ins.getDesc() != null)
                        sb.append(ins.getDesc());
                  }
                  sb.append("\nCommandes LunaSQL (et greffons éventuels) :\n");
                  for (Map.Entry<String, Instruction> me : cmds.entrySet()) {
                     Instruction ins = me.getValue();
                     int type = ins.getType();
                     if ((type == Instruction.TYPE_CMDINT || type == Instruction.TYPE_CMDPLG ||
                          type == Instruction.TYPE_MOTC_WH) && ins.getDesc() != null)
                        sb.append(ins.getDesc());
                  }
                  TreeMap<String, String> helps = cont.getAllVarHelpTree();
                  if (helps.size()> 0) sb.append("\nVariables utilisateur :\n");
                  for (Map.Entry<String, String> me : helps.entrySet()) {
                     sb.append("  ").append(SQLCnx.frm(me.getKey(), 10, ' ')).append("  ")
                        .append(me.getValue()).append('\n');
                  }
                  // autres aides...
                  sb.append("\nPour obtenir la syntaxe d'une commande : help nom_commande\n");
                  sb.append("Les alias et macros se complètent par la touche <TAB>. Pour\n");
                  sb.append("ajouter un alias ou une macro en complétion : DEF -c");
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("LAUNCHING".startsWith(cmdname)) {
                  sb.append("\n ").append(SQLCnx.frmI(" Lancement de LunaSQL", 67, '-')).append('\n');
                  sb.append(getLaunching());
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("SYNTAX".startsWith(cmdname)) {
                  sb.append(getFContent("Utilisation générale", "/lunasql/doc/syntax.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("DELIMITERS".startsWith(cmdname)) {
                  sb.append(getFContent("Délimitation des chaînes", "/lunasql/doc/delimiters.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("SUBSTITUTES".startsWith(cmdname)) {
                  sb.append(getFContent("Usage des substitutions", "/lunasql/doc/substitutes.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("EXPRESSIONS".startsWith(cmdname)) {
                  sb.append(getFContent("Expressions substituées", "/lunasql/doc/expressions.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("VARIABLES".startsWith(cmdname)) {
                  sb.append(getFContent("Propriétés des variables", "/lunasql/doc/variables.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("JS-FUNCT".startsWith(cmdname)) {
                  sb.append(getFContent("Liste des fonctions Javascript", "/lunasql/doc/js-funct.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("CATCHING".startsWith(cmdname)) {
                  sb.append(getFContent("Gestion à chaud des erreurs", "/lunasql/doc/catching.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("PACKAGES".startsWith(cmdname)) {
                  sb.append(getFContent("Modules disponibles à charger", "/lunasql/doc/packages.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("LIBRARIES".startsWith(cmdname)) {
                  sb.append(getFContent("À propos des bibliothèques Java", "/lunasql/doc/libraries.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("ETIKETTE".startsWith(cmdname)) {
                  sb.append(getFContent("Les bonnes pratiques en LunaSQL", "/lunasql/doc/etikette.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("BONUS".startsWith(cmdname)){
                  sb.append(getFContent("Quelques bonus pour jouer", "/lunasql/doc/bonus.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if ("LICENSES".startsWith(cmdname)) {
                  // Affichage de la licence pour LunaSQL
                  sb.append(getFContent("Licence LunaSQL", "/lunasql/LICENSE.txt")).append('\n');
                  // Affichage de la licence BSD pour JLine
                  sb.append('\n').append(getFContent("Licence JLine", "/jline/LICENSE.txt"));
                  sb.append("\nhttp://opensource.org/licenses/bsd-license.php");
                  // Affichage de la licence MIT pour JOptsimple
                  sb.append('\n').append(getFContent("Licence JOptsimple", "/joptsimple/LICENSE.txt"));
                  sb.append("\nhttp://opensource.org/licenses/mit-license.php");
                  // Affichage de la licence Apache 2 pour OpenCSV
                  sb.append('\n').append(getFContent("Licence OpenCSV", "/opencsv/LICENSE.txt"));
                  sb.append("\nhttp://opensource.org/licenses/Apache-2.0");
                  // Affichage de la licence LGPL pour JTableView
                  sb.append("\n\n ").append(SQLCnx.frmI(" Licence JTableView", 67, '-')).append('\n');
                  sb.append("http://opensource.org/licenses/LGPL-2.1");
                  // Affichage de la licence pour NanoHTTPD
                  sb.append('\n').append(getFContent("Licence NanoHTTPD", "/lunasql/http/NanoHTTPD-LICENSE.txt"));
                  // Affichage de la licence pour Highlight.js
                  sb.append('\n').append(getFContent("Licence Highlight.js", "/lunasql/http/res/Highlightjs-LICENSE.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               } else if("CHANGELOG".startsWith(cmdname)) {
                  sb.append(getFContent("Nouveautés des dernières versions", "/lunasql/doc/changelog.txt"));
                  cont.printlnX(sb.toString(), Contexte.BR_WHITE);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

               // autres : erreur
               } else return cont.erreur("HELP", "aucune rubrique d'aide pour la variable/macro '" +
                              cmdname + "'", lng);
            }// for
         } catch (OptionException ex) {
            return cont.exception("HELP", "option incorrecte : " + ex.getMessage() , lng, ex);
         } catch (IOException ex) {
            return cont.exception("HELP", "ERREUR IOException : " + ex.getMessage(), lng, ex);
         }
      }
      else return cont.erreur("HELP", "un nom de commande au maximum est attendu", lng);
      cont.setValeur(null);
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc() {
      return "  help, ?     Affiche différentes rubriques d'aide\n";
   }

   public static String getIntro() {
      StringBuilder sb = new StringBuilder();
      sb.append("\n--------------------------------------------------------------\n");
      sb.append("\t\tAIDE DE LA CONSOLE LunaSQL");
      sb.append("\n--------------------------------------------------------------\n\n");
      sb.append("LunaSQL version ").append(Config.APP_VERSION_NUM).append(" - ").append(Config.APP_VERSION_NAME);
      sb.append("\nClient SQL JDBC pour la gestion cosmique d'une base de données\n");
      sb.append("  26950 lignes de code, 4041 lignes de doc interne\n\n");
      sb.append("*  -- Liste des numéros d'employées depuis fichier\n");
      sb.append("*  file eachln employees.dat { arg idemp\n");
      sb.append("*    -- Recherche du nom (et du salaire) de l'employée en base\n");
      sb.append("*    for -qn [SELECT NAME, SAL FROM EMPLOYEES WHERE ID=$idemp] {\n");
      sb.append("*      if [$SAL >= 1000] {\n");
      sb.append("*        print \"$NAME est riche, elle gagne $SAL !\"\n");
      sb.append("*      }\n*    }\n*  }\n\n");
      sb.append("Rubriques d'aide disponibles (taper help <rubrique>) :\n");
      sb.append("  launching practice substitutes commands catching js-funct packages\n");
      sb.append("  libraries bonus license changelog\n");
      sb.append("\n - Options de lancement                 : help launching\n");
      sb.append(" - Informations sur l'utilisation       : help syntax\n");
      sb.append(" - Fonctionnement des chaînes de car.   : help delimiters\n");
      sb.append(" - Liste complète des commandes         : help commands\n");
      sb.append(" - Guide d'usage des substitutions      : help substitutes\n");
      sb.append(" - Guide des expressions substituées    : help expressions\n");
      sb.append(" - Quelques propriétés des variables    : help variables\n");
      sb.append(" - Aide sur la commande <c>             : help <c>\n");
      sb.append(" - Guide de gestion des erreurs         : help catching\n");
      sb.append(" - Liste complète des fonctions js      : help js-funct\n");
      sb.append(" - Liste complète des modules           : help packages\n");
      sb.append(" - À propos des bibliothèques Java      : help libraries\n");
      sb.append(" - Les bonnes pratiques en LunaSQL      : help etikette\n");
      sb.append(" - Quelques bonus et exemples           : help bonus\n");
      sb.append(" - Licence de LunaSQL et bibliothèques  : help licenses\n");
      sb.append(" - Nouveautés des dernières versions    : help changelog\n");
      sb.append(" - Aide sur le module de base           : help-base\n");
      sb.append("\nLunaSQL fut créée pour répondre à un besoin d'interprète multi-SGBD\n");
      sb.append("de scripts SQL, puis fut poursuivie à visée pédagogique. Elle n'a pas\n");
      sb.append("la prétention d'être un « vrai » langage de programmation, mais...\n");
      sb.append("elle est amusante, élégante et bien utile tout de même !\n");
      sb.append("« Les cieux proclament la gloire de Dieu [...].\n");
      sb.append("  Un jour après l'autre fait jaillir le langage » - Ps. 19:1,2 \n");
      sb.append("\nUsage, modification et redistribution placés sous licence CeCILL v1\n");
      sb.append(" http://cecill.info/licences/Licence_CeCILL_V2.1-fr.html\n");
      sb.append("\nBugs et suggestions à rapporter à ").append(Config.APP_AUTHOR_NAME);
      sb.append("\n(le maniaque derrière LunaSQL qui rédige ses chèques en héxadécimal)");
      sb.append("\nCourriel : ").append(Config.APP_AUTHOR_EMAIL);
      sb.append("\nOpenPGP  : ").append(Config.APP_AUTHOR_PGP);
      // Proverbe sur la lune
      sb.append("\n\n« Qui est plus utile, le soleil ou la lune ?");
      sb.append("\nLa lune, bien entendu. Elle brille quand il fait noir, alors que le");
      sb.append("\nsoleil brille uniquement quand il fait clair. »");
      sb.append("\n - Physicien et écrivain allemand Georg Christoph Lichtenberg");
      return sb.toString();
   }

   /* Aide de la console */
   public static String getLaunching() {
      StringBuilder sb = new StringBuilder();
      sb.append("\n  java -jar lunasql-x.x.x.jar <options> <commande>");
      sb.append("\n  ou");
      sb.append("\n  java lunasql.Main <options> <commande>\n");
      sb.append("\n Avec un seul argument, équivaut à : --login=<arg> --console\n");
      sb.append("\n Options (plusieurs possibles, elles peuvent être abrégées) :");
      sb.append("\n  Pour la connexion :");
      sb.append("\n  --type=<type>  (type : ACCESS ACCESSX UCACCESS HSQLDB H2DB MYSQL\n   DERBY ORACLE SQLSERVER)");
      sb.append("\n    pour UCanAccess cf. http://ucanaccess.sourceforge.net");
      sb.append("\n    LunaSQL recommande la base H2, cf. la bibliothèque doc-h2");
      sb.append("\n  --name=<base name>  ('-' pour ne pas spécifier de base)");
      sb.append("\n  --host=<host name>\n  --port=<port>");
      sb.append("\n  ou ");
      sb.append("\n  --driver=<driver.de.connexion>");
      sb.append("\n    Exemples de drivers (tout driver JDBC convient) :");
      sb.append("\n     ODBC      : ").append(TypesSGBD.DRIVERS[TypesSGBD.TYPE_ODBC]);
      sb.append("\n     ACCESS(X) : ").append(TypesSGBD.DRIVERS[TypesSGBD.TYPE_ACCESS]);
      sb.append("\n     UCACCESS  : ").append(TypesSGBD.DRIVERS[TypesSGBD.TYPE_UCACCESS]);
      sb.append("\n     HSQLDB    : ").append(TypesSGBD.DRIVERS[TypesSGBD.TYPE_HSQLDB]);
      sb.append("\n     H2DB      : ").append(TypesSGBD.DRIVERS[TypesSGBD.TYPE_H2DB]);
      sb.append("\n     MYSQL     : ").append(TypesSGBD.DRIVERS[TypesSGBD.TYPE_MYSQL]);
      sb.append("\n     DERBY     : ").append(TypesSGBD.DRIVERS[TypesSGBD.TYPE_DERBY]);
      sb.append("\n     ORACLE    : ").append(TypesSGBD.DRIVERS[TypesSGBD.TYPE_ORACLE]);
      sb.append("\n     SQLSERVER : ").append(TypesSGBD.DRIVERS[TypesSGBD.TYPE_SQLSERV]);
      sb.append("\n  --path <chemin:de:connexion>");
      sb.append("\n    Exemples de paths (tout SGBD convient si driver dispo.) :");
      sb.append("\n    (Note : les ports peuvent être préfixés par un ':')");
      sb.append("\n     ODBC      : ").append(TypesSGBD.CHAINES[TypesSGBD.TYPE_ODBC]);
      sb.append("\n     ACCESS    : ").append(TypesSGBD.CHAINES[TypesSGBD.TYPE_ACCESS]);
      sb.append("\n     ACCESSX   : ").append(TypesSGBD.CHAINES[TypesSGBD.TYPE_ACCESSX]);
      sb.append("\n     UCACCESS  : ").append(TypesSGBD.CHAINES[TypesSGBD.TYPE_UCACCESS]);
      sb.append("\n     HSQLDB    : ").append(TypesSGBD.CHAINES[TypesSGBD.TYPE_HSQLDB]);
      sb.append("\n     H2DB      : ").append(TypesSGBD.CHAINES[TypesSGBD.TYPE_H2DB]);
      sb.append("\n     MYSQL     : ").append(TypesSGBD.CHAINES[TypesSGBD.TYPE_MYSQL]);
      sb.append("\n     DERBY     : ").append(TypesSGBD.CHAINES[TypesSGBD.TYPE_DERBY]);
      sb.append("\n     ORACLE    : ").append(TypesSGBD.CHAINES[TypesSGBD.TYPE_ORACLE]);
      sb.append("\n     SQLSERVER : ").append(TypesSGBD.CHAINES[TypesSGBD.TYPE_SQLSERV]);
      sb.append("\n  ou ");
      sb.append("\n  --login=<alias de base défini en fichier de bases>");
      sb.append("\n    avec pour chaque [base] les clefs : dbpath, driver, schema, passwd");
      sb.append("\n    Sans base fournie, le login est le premier argument en commande.");
      sb.append("\n    Exemple à appeler par --login=foo :\n      default=foo");
      sb.append("\n      foo.dbpath=jdbc:h2:base/MyHDB\n      foo.driver=org.h2.Driver\n      foo.schema=sa");

      sb.append("\n\n Options de connexion (selon compatibilité avec la base) :");
      sb.append("\n  --username=<user>\n  --password=<pswd>");
      sb.append("\n  --db-options=<opt1=val1;opt2=val2;...> (selon compatibilité)");
      sb.append("\n\n Configuration de la console :");
      sb.append("\n  --defs=<key1=val1,key2=val2...> (plusieurs --defs possibles)");
      sb.append("\n  --opts=<key1=val1,key2=val2...> (plusieurs --opts possibles)");
      sb.append("\n    (les options en fichier de config. écrasent ces définitions)");
      sb.append("\n  --uses=<lib1,lib2...> (plusieurs --uses possibles)");
      sb.append("\n  --need-version=num-version-min (erreur mais ne quitte pas)");
      sb.append("\n  --exit-on-error[=<0 / 1 (defaut)>]\n  --init-sql=<commande LunaSQL>");
      sb.append("\n  --scripts-path=<chemin des répertoires des scripts, sép. '").append(File.pathSeparator).append("'>");
      sb.append("\n    défaut : ").append(Config.CT_SCRIPTS_PATH);
      sb.append("\n  --plugins-path=<chemin des répertoires des plugins, sép. '").append(File.pathSeparator).append("'>");
      sb.append("\n    défaut : ").append(Config.CT_PLUGINS_PATH);
      sb.append("\n  --config-file=<nom fichier config, key:=val, sauts de ligne '\\n'>");
      sb.append("\n    défaut : ").append(Config.CT_CONFIG_FILE);
      sb.append("\n  --init-file=<nom fichier d'initialisation de la console>");
      sb.append("\n    défaut : ").append(Config.CT_INIT_FILE);
      sb.append("\n  --bases-file=<nom fichier de définition des bases>");
      sb.append("\n    défaut : ").append(Config.CT_BASES_FILE).append(" en rép. courant");
      sb.append("\n    ou bien : ").append(Config.CT_BASES_PATH);
      sb.append("\n  --history-file=<nom fichier hist. console, ou '-' pour aucun>");
      sb.append("\n    défaut : ").append(Config.CS_HISTORY_FILE);
      sb.append("\n  --verb-level=<valeur ou nom de verbose>");
      sb.append("\n    défaut : ").append(Contexte.VERBLIB[Config.CT_VERBOSE]);
      sb.append("\n  --log-dir=<répertoire des fichiers log d'erreur>");
      sb.append("\n    défaut : ").append(Config.CT_LOG_DIR);
      sb.append("\n  --http-port=<port du serveur HTTP si --http-console>");
      sb.append("\n    défaut : ").append(Config.SR_HTTP_PORT);
      sb.append("\n  --no-colors (pas de couleurs, utile sur certains systèmes en mousse");
      sb.append("\n    ignorant l'ANSI, activé par défaut avec --http-console)");
      sb.append("\n  --deny-opt-command (modification d'option par OPT interdite)");
      sb.append("\n  --deny-sql-update (exécution de SQL de modification interdite)");
      sb.append("\n\n Commandes (une seule commande obligatoire) :");
      sb.append("\n  --help\n    Affiche cette aide et quitte");
      sb.append("\n  --console\n    Lance LunaSQL en mode interactif dans un terminal");
      sb.append("\n  --run-sql=<commande LunaSQL>\n    Lance LunaSQL pour exécuter la commande fournie, et quitte");
      sb.append("\n  --stdin\n    Lance LunaSQL pour exécuter le contenu en entrée standard (pipe)");
      sb.append("\n  --exec=<chemin fichier script SQL ou SE> | -:cmds | +:cmds");
      sb.append("\n    Lance LunaSQL pour exécuter le contenu du fichier (cf. EXEC)");
      sb.append("\n  --list-engines\n    Liste les moteurs d'exécution et quitte");
      sb.append("\n  --editor[=<fichier_script>]\n    Ouvre le fichier scrupt par l'éditeur EDIT");
      sb.append("\n  --http-console\n    Lance un serveur HTTP pour exécuter du code LunaSQL à distance");
      sb.append("\n    (port par défaut: ").append(Config.SR_HTTP_PORT).append(", peut être changé avec --http-port)");
      sb.append("\n    http://localhost:").append(Config.SR_HTTP_PORT).append(" : application Web interactive");
      sb.append("\n    http://localhost:").append(Config.SR_HTTP_PORT).append("/api : API GET|POST pour exécuter du ");
      sb.append("code\n    avec param|body: sqlquery=<code> header: 'content-type: text/plain'");
      sb.append("\n    Ex.: http://localhost:5862/api?sqlquery=select%20*%20from%20test");
      sb.append("\n  --apropos\n    Affiche des informations sur LunaSQL et quitte");
      sb.append("\n  --version\n    Affiche le numéro de la version courante et quitte");
      sb.append("\n\nBiblothèques optionnelles à ajouter au CLASSPATH :");
      sb.append("\n  javamail.jar (commande MAIL), jsqlparser.jar (commande FRM),");
      sb.append("\n  csvjdbc.jar (commande CSV)");

      sb.append("\n\nOptions de lancement de la JVM :");
      sb.append("\n  Il est possible d'adapter la mémoire et la pile par les options de");
      sb.append("\n  lancement de la JVM -Xms (tas) et -Xss (pile). Exemple :");
      sb.append("\n    java -Xms64m -Xss1m lunasql.Main [options de LunaSQL]");
      sb.append("\n\nNotes :\n - sous la console DOS de Windows, pour corriger le problème");
      sb.append("\n   d'affichage des caractères accentués, utilisez en batch la commande");
      sb.append("\n   CHCP. ex : 'chcp 28591 > nul' (code page ISO-8859-1)");
      sb.append("\n   ou bien codez directement le fichier DOS en IBM850");
      sb.append("\n - pour certains SGBD, il peut être sage d'utiliser la console fournie");
      sb.append("\n   par le SGBD, si elle existe (meilleure compatibilité).");
      sb.append("\n   Exemple ORACLE : voir l'utilisation de SQLPlus*");

      return sb.toString();
   }// affiche
}
