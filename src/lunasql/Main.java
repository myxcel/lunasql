package lunasql;

import static java.util.Arrays.asList;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.cmd.CmdHelp;
import lunasql.cmd.CmdNeed;
import lunasql.http.HttpConsole;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.sql.SQLCnx;
import lunasql.sql.TypesSGBD;
import lunasql.ui.FrmEditScript;

/**
 * Lancement général LunaSQL <br>
 * Created on 10 octobre 2010, 11:05
 *
 * @author M.P.
 */
public class Main {

   private static Contexte contex;
   private static boolean  hasBase, hasContex, banner;
   private static int      port;
   private static String   user, mdp, base, dbtype, path, driver, host, opts, login;
   private static File     basefile;

   /**
    * Main de lancement
    *
    * @param args the command line arguments
    */
   public static void main(String[] args) {
      // Rattrapage des exceptions imprévues
      Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
      System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());

      try { readArgs(args); }
      catch(OptionException ex) {
        printErr("ERREUR OptionException : " + ex.getMessage());
        System.exit(-1);
      }
   }

   /*
    * Lecture des arguments
    */
   private static void readArgs(String[] args) throws OptionException {
      OptionParser parser = new OptionParser();
      // Options
      parser.acceptsAll(asList("u", "username"), "nom d'utilisateur de la base de données")
         .withRequiredArg().ofType(String.class).describedAs("user");
      parser.acceptsAll(asList("p", "password"), "mot de passe de la base de données")
         .withOptionalArg().ofType(String.class).defaultsTo("").describedAs("pswd");
      parser.acceptsAll(asList("t", "type"), "type de base de données")
         .withRequiredArg().ofType(String.class).describedAs("type");
      parser.acceptsAll(asList("n", "name"), "nom de la base de données")
         .withRequiredArg().ofType(String.class).describedAs("name");
      parser.acceptsAll(asList("H", "host"), "machine hôte de la base de données")
         .withRequiredArg().ofType(String.class).describedAs("host");
      parser.acceptsAll(asList("R", "port"), "port de connexion à la base de données")
         .withRequiredArg().ofType(Integer.class).describedAs("port");
      parser.acceptsAll(asList("D", "driver"), "driver de connexion à la base de données")
         .withRequiredArg().ofType(String.class).describedAs("driver");
      parser.acceptsAll(asList("P", "path"), "chemin de connexion à la base de données")
         .withRequiredArg().ofType(String.class).describedAs("path");
      parser.acceptsAll(asList("l", "login"), "connexion à une base sur alias de fichier")
         .withRequiredArg().ofType(String.class).describedAs("path");
      parser.acceptsAll(asList("o", "db-options"), "options de connexion à la base de données")
         .withRequiredArg().ofType(String.class).describedAs("db-options");
      parser.acceptsAll(asList("b", "verb-level"), "niveau de bavardage")
         .withRequiredArg().ofType(String.class).describedAs("num|lib");
      parser.acceptsAll(asList("V", "need-version"), "numéro de version requise")
            .withRequiredArg().ofType(String.class).describedAs("version");
      parser.accepts("exit-on-error", "interruption du script sur erreur")
         .withOptionalArg().ofType(Integer.class).defaultsTo(1).describedAs("0|1");
      parser.accepts("defs", "variables d'initialisation de la console")
         .withRequiredArg().ofType(String.class).describedAs("key1=val1,key2=val2")
         .withValuesSeparatedBy(",");
      parser.accepts("opts", "options d'initialisation de la console")
         .withRequiredArg().ofType(String.class).describedAs("key1=val1,key2=val2")
         .withValuesSeparatedBy(",");
      parser.accepts("uses", "bibliothèques à charger au démarrage")
         .withRequiredArg().ofType(String.class).describedAs("lib1,lib2")
         .withValuesSeparatedBy(",");
      parser.accepts("scripts-path", "chemins des répertoires de scripts")
         .withRequiredArg().ofType(File.class).describedAs("rep1" + File.pathSeparator + "rep2")
         .withValuesSeparatedBy(File.pathSeparator);
      parser.accepts("plugins-path", "chemins des répertoires de greffons")
         .withRequiredArg().ofType(File.class).describedAs("rep1" + File.pathSeparator + "rep2")
         .withValuesSeparatedBy(File.pathSeparator);
      parser.accepts("config-file", "fichier de configuration de la console")
         .withRequiredArg().ofType(File.class).describedAs("file");
      parser.accepts("init-file", "fichier SQL d'initialisation de la console")
         .withRequiredArg().ofType(File.class).describedAs("file");
      parser.accepts("history-file", "fichier de l'historique de la console")
         .withRequiredArg().ofType(File.class).describedAs("file|-");
      parser.accepts("bases-file", "fichier XML des bases de connexion")
         .withRequiredArg().ofType(File.class).describedAs("file");
      parser.accepts("log-dir", "répertoire des journaux d'erreur")
         .withRequiredArg().ofType(File.class).describedAs("dir");
      parser.accepts("exec-args", "arguments de la commande 'exec'")
         .withRequiredArg().ofType(String.class).describedAs("arg1;arg2");
      parser.accepts("http-port", "port d'écoute du serveur HTTP")
            .withRequiredArg().ofType(Integer.class).describedAs("port");
      parser.acceptsAll(asList("A", "no-colors"), "sans coloration de la console");
      parser.acceptsAll(asList("B", "no-banner"), "sans la bannière d'accueil en console");
      parser.acceptsAll(asList("O", "deny-opt-command"), "interdit la commande OPT");
      parser.acceptsAll(asList("U", "deny-sql-update"), "interdit les commandes SQL de modification");

      // commandes
      parser.accepts("?", "aide sur les options");
      parser.acceptsAll(asList("h", "help"), "aide succinte sur le lancement");
      parser.acceptsAll(asList("a", "apropos"), "infos sur la version de l'application");
      parser.acceptsAll(asList("v", "version"), "numéro de version de l'application");
      parser.acceptsAll(asList("r", "run-sql"), "commande(s) SQL à exécuter puis sortie")
         .withRequiredArg().ofType(String.class).describedAs("cmd1;cmd2;");
      parser.accepts("init-sql", "commande(s) SQL à exécuter avant lancement")
         .withRequiredArg().ofType(String.class).describedAs("cmd1;cmd2;");
      parser.acceptsAll(asList("x", "exec"), "fichier SQL à exécuter puis sortie")
         .withRequiredArg().ofType(String.class).describedAs("file|-:cmds|+:cmds");
      parser.acceptsAll(asList("c", "console"), "lancement de la console SQL");
      parser.acceptsAll(asList("i", "stdin"), "lecture de commandes depuis l'entrée standard");
      parser.accepts("editor", "ouverture de l'éditeur de texte")
         .withOptionalArg().ofType(File.class).defaultsTo(new File("-")).describedAs("file");
      parser.acceptsAll(asList("e", "list-engines"), "liste des moteurs SE puis sortie");
      parser.accepts("http-console", "démarre le serveur HTTP");
      parser.accepts("test-jline3", "classe de test de JLine3");

      //... autres options
      OptionSet options = null;
      try { options = parser.parse(args); }
      catch (OptionException ex) {
         printErr("ERREUR OptionException : " + ex.getMessage());
         try { parser.printHelpOn(System.err); }
         catch(IOException ex2) {}
         System.exit(-1);
      }

      // *** Simples commandes hors contexte ***
      // Aide générale
      if (options.has("help")) {
         System.out.println("Aide générale de la console LunaSQL\nUsage :");
         System.out.println(CmdHelp.getLaunching());
         System.out.println("\nPour toute interrogation, se rapprocher de");
         System.out.println("\t" + Config.APP_AUTHOR_NAME + "  " + Config.APP_AUTHOR_EMAIL);
         System.exit(0);

      // Commandes en ligne
      } else if (options.has("?")) {
         System.out.println("Aide générale de la console LunaSQL\nOptions :\n");
         try { parser.printHelpOn(System.out); }
         catch (IOException ex) {}
         System.exit(0);

         // Affichage des infos sur la version
      } else if (options.has("apropos")) {
         StringBuilder sb = new StringBuilder();
         sb.append("Console LunaSQL - Gestion d'une base de données SQL en console\n");
         sb.append("Version : ").append(Config.APP_VERSION_NUM);
         sb.append(" (Nom de code : ").append(Config.APP_VERSION_NAME).append(")\n");
         sb.append("Auteur  : ").append(Config.APP_AUTHOR_NAME);
         sb.append("\n\tContact  : ").append(Config.APP_AUTHOR_EMAIL);
         sb.append("\nTourne sur la JVM version ").append(System.getProperty("java.version"))
            .append(", ").append(System.getProperty("java.vm.info"));
         System.out.println(sb.toString());
         System.exit(0);

         // Affichage du numéro de version seul
      } else if (options.has("version")) {
         System.out.println(Config.APP_VERSION_NUM);
         System.exit(0);
      }

      /*else if (options.has("test-jline3")) {
         new TestJLine3();
      }*/

      // *** Options dépendant du contexte ***
      // Préparation des arguments par défaut
      user = "";
      mdp = "";
      base = "";
      path = "";
      driver = "";
      host = "";
      port = 0;
      login = "";
      basefile = null;
      opts = null;
      dbtype = TypesSGBD.getSTypeFromArg(Config.DB_DEFAUT_TYPE);
      hasBase = false;
      banner = true;

      // *** Options ***
      // option : exit-on-error
      if (options.has("exit-on-error")) {
         creationContexte();
         contex.setVar(Contexte.ENV_EXIT_ERR, ((Integer)options.valueOf("exit-on-error"))==1 ? "1" : "0");
      }

      // option : need-version
      if (options.has("need-version")) {
         creationContexte();
         String v = (String)options.valueOf("need-version");
         if (v.matches(CmdNeed.VER_REG)) contex.executeCmd("NEED", v);
         else {
            printErr("numéro de version fourni invalide : " + v);
            System.exit(-2);
         }
      }

      // option : bases-file
      if (options.has("bases-file")) {
         File f = (File)options.valueOf("bases-file");
         if (f.isFile() && f.canRead()) basefile = f;
         else {
            printErr("impossible de lire le fichier : " + f.getAbsolutePath());
            System.exit(-5);
         }
      }

      // option : connexion à une base
      if (options.has("login")) {
         login = (String)options.valueOf("login");
         setLogin(true);
      }

      // option : username
      if (options.has("username")) {
         user = (String)options.valueOf("username");
         if (options.has("password")) mdp = (String)options.valueOf("password");
         else mdp = "";
      }

      // option : type
      if (options.has("type")) {
         if (!options.has("name")) {
            printErr("l'option 'type' nécessite l'option 'name'");
            System.exit(-5);
         }
         dbtype = (String)options.valueOf("type");
      }

      // option : name
      if (options.has("name")) {
         if (!options.has("type")) {
            printErr("l'option 'name' nécessite l'option 'type'");
            System.exit(-5);
         }
         base = (String)options.valueOf("name");
         if (!base.equals("-")) hasBase = true;
      }

      // option : host et port
      if (options.has("host")) {
         if (!options.has("type")) {
            printErr("l'option 'host' nécessite l'option 'type'");
            System.exit(-5);
         }
         host = (String)options.valueOf("host");
         if (options.has("port")) port = ((Integer)options.valueOf("port"));
      }

      // option : driver
      if (options.has("driver")) {
         if (!options.has("path")) {
            printErr("l'option 'driver' nécessite l'option 'path'");
            System.exit(-5);
         }
         if (options.has("type")) {
            printErr("l'option 'type' est incompatible avec l'option 'driver'");
            System.exit(-5);
         }
         driver = (String)options.valueOf("driver");
      }

      // option : path et driver
      if (options.has("path")) {
         if (!options.has("driver")) {
            printErr("l'option 'path' nécessite l'option 'driver'");
            System.exit(-5);
         }
         path = (String)options.valueOf("path");
         if (path.length() > 0) hasBase = true;
      }

      // option : no-colors
      if (options.has("no-colors")) {
         creationContexte(false);
      }

      // option : banner
      if (options.has("no-banner")) {
         banner = false;
      }

      // option : options de connexion (selon compatibilité)
      if (options.has("db-options")) {
         creationContexte();
         opts = (String)options.valueOf("db-options");
      }

      // option : defs
      if (options.has("defs")) {
         creationContexte();
         List<?> vars = options.valuesOf("defs");
         for (Object o : vars) {
            String kv = o.toString();
            int i = kv.indexOf('=');
            if (i < 1) continue;
            String k = kv.substring(0, i), v = kv.substring(i + 1);
            if (contex.valideKey(k)) contex.setVar(k, v);
            else contex.errprintln(" * defs : affectation de variable invalide ou non autorisé : " + k);
         }
      }

      // option : opts
      if (options.has("opts")) {
         creationContexte();
         List<?> vars = options.valuesOf("opts");
         for (Object o : vars) {
            String kv = o.toString();
            int i = kv.indexOf('=');
            if (i < 1) continue;
            String k = kv.substring(0, i), v = kv.substring(i + 1);
            if (contex.isSysUser(k)) contex.setVar(k, v);
            else contex.errprintln(" * opts : non définie ou édition non autorisée : " + k);
         }
      }

      // option : uses
      if (options.has("uses")) {
         creationContexte();
         List<?> vars = options.valuesOf("uses");
         String[] lcmd = new String[vars.size() + 1];
         lcmd[0] = "USE";
         for (int i = 0; i < vars.size(); i++) lcmd[i + 1] = vars.get(i).toString();
         contex.executeCmd(lcmd);
      }

      // option : scripts-path
      if (options.has("scripts-path")) {
         creationContexte();
         List<?> p = options.valuesOf("scripts-path");
         StringBuilder ss = new StringBuilder();
         for (Object o : p) {
            File f = (File) o;
            if (f.isDirectory()) ss.append(f.getAbsolutePath()).append(File.pathSeparator);
         }
         contex.setVar(Contexte.ENV_SCR_PATH, ss.toString());
      }

      // option : plugins-path
      if (options.has("plugins-path")) {
         creationContexte();
         List<?> p = options.valuesOf("plugins-path");
         StringBuilder ss = new StringBuilder();
         for (Object o : p) {
            File f = (File)o;
            if (f.isDirectory()) ss.append(f.getAbsolutePath()).append(File.pathSeparator);
         }
         contex.setVar(Contexte.ENV_PLG_PATH, ss.toString());
      }

      // option : config-file
      if (options.has("config-file")) {
         creationContexte();
         File f = (File)options.valueOf("config-file");
         if (f.isFile() && f.canRead()) contex.setVar(Contexte.ENV_CONF_FILE, f.getAbsolutePath());
      }

      // option : init-file
      if (options.has("init-file")) {
         creationContexte();
         File f = (File)options.valueOf("init-file");
         if (f.isFile() && f.canRead()) contex.setVar(Contexte.ENV_INIT_FILE, f.getAbsolutePath());
      }

      // option : history-file
      if (options.has("history-file")) {
         creationContexte();
         File f = (File)options.valueOf("history-file");
         if (f.getName().equals("-")) // pas d'historisation des commandes
            contex.setVar(Contexte.ENV_HIST_FILE, "");
         else { // fichier histo renseigné
            if (!f.exists() || (f.isFile() && f.canRead()))
               contex.setVar(Contexte.ENV_HIST_FILE, f.getAbsolutePath());
         }
      }

      // option : init-sql
      if (options.has("init-sql")) {
         if (!hasBase) {
            printErr("commande CONSOLE invalide si aucune base fournie");
            System.exit(-4);
         }
         execScript((String)options.valueOf("init-sql"), false);
         execConsole();
      }

      // option : verb-level
      if (options.has("verb-level")) {
         creationContexte();
         contex.setVerbose((String)options.valueOf("verb-level"));
      }

      // option : log-dir
      if (options.has("log-dir")) {
         System.setProperty(Contexte.PROP_LOG_DIR,
               ((File) options.valueOf("log-dir")).getAbsolutePath());
      }

      // option : deny-opt-command
      if (options.has("deny-opt-command")) {
         creationContexte();
         contex.setVar(Contexte.ENV_CONST_EDIT, "1"); // 0: aucune, 1: locales, 2: toutes
      }

      // option : deny-modify-sql
      if (options.has("deny-sql-update")) {
         creationContexte();
         contex.setVar(Contexte.ENV_SQL_UPDATE, Contexte.STATE_FALSE); // 0: non, 1: oui
      }

      // *** Autres commandes dépendant du contexte ***
      // Les commandes sont exclusives : une seule est permise

      // Aucune base fournie : recherche de login (sucre syntaxique à option --login)
      if (!hasBase) {
         List<?> arglist = options.nonOptionArguments();
         if (!arglist.isEmpty()) login = (String)arglist.get(0);
         setLogin(false);
      }

        // Lancement simple de la console
      if (options.has("console")) {
         if (!hasBase) {
            printErr("commande CONSOLE invalide si aucune base fournie");
            System.exit(-4);
         }
         execConsole();

         // Exécution d'une commande SQL
      } else if (options.has("run-sql")) {
         if (!hasBase) {
            printErr("commande SQL invalide si aucune base fournie");
            System.exit(-4);
         }
         System.exit(execScript((String)options.valueOf("run-sql")));

         // Exécution d'un fichier LunaSQL ou ScriptEngine
      } else if (options.has("exec")) {
         if (!hasBase) {
            printErr("commande EXEC invalide si aucune base fournie");
            System.exit(-4);
         }
         String f = (String)options.valueOf("exec"), exargs = "";
         if (options.has("exec-args")) exargs = (String)options.valueOf("exec-args");
         if (f.startsWith("-:") || f.startsWith("+:")) {// exec +:cmds arg1 args2
            System.exit(execScript("EXEC " + f.charAt(0) + " \"" + f.substring(2) + "\" "
                        + exargs.replace(';', ' ')));
         }
         else System.exit(execScript("EXEC \"" + f + "\" " + exargs.replace(';', ' ')));

      // Lecture des commandes depuis l'entrée standard
      } else if (options.has("stdin")) {
         if (!hasBase) {
            printErr("commande STDIN invalide si aucune base fournie");
            System.exit(-4);
         }
         if (!hasContex) {
            creationContexte();
            contex.loadConfigFile();
            hasContex = true;
         }
         int stat = 0;
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            setsql();
            contex.loadInitFile();
            // Lecture du script
            String ln;
            Lecteur lec = new Lecteur(contex);
            lec.setLecVar(Contexte.LEC_THIS, "(stdin)");
            while ((ln = reader.readLine()) != null) lec.add(ln);
            stat = Math.min(lec.getCmdState(), contex.getQuitStat());
         }
         catch (IOException ex) {
            printErr("ERREUR IOException : " + ex.getMessage());
            stat = -5;
         }
         finally {
            contex.fermerConnex();
            System.exit(stat);
         }

         // Ouverture de l'éditeur Swing
      } else if (options.has("editor")) {
         if (!hasBase) {
            printErr("commande EDITOR invalide si aucune base fournie");
            System.exit(-4);
         }
         creationContexte();
         contex.loadConfigFile();
         banner();
         setsql();
         contex.loadInitFile();
         try {
            FrmEditScript fed = new FrmEditScript(contex, true);
            fed.setVisible(true);
            File f = (File) options.valueOf("editor");
            if (!f.getName().isEmpty() && !f.getName().equals("-")) fed.openFile(f);
         } catch (HeadlessException ex) {
            printErr("ERREUR HeadlessException : " + ex.getMessage());
         }

         // Listage des moteurs de scripts diponibles depuis le classpath
      } else if (options.has("list-engines")) {
         creationContexte();
         contex.loadConfigFile();
         contex.executeCmd("ENGINE", "-l");
         System.exit(contex.getQuitStat());

         // Lancement de la console HTTP (serveur)
      } else if (options.has("http-console")) {
         if (!hasBase) {
            printErr("commande HTTP-CONSOLE invalide si aucune base fournie");
            System.exit(-4);
         }
         creationContexte(false);
         contex.setHttpMode(true);
         contex.loadConfigFile();
         banner();

         if (user.length() == 0) {
            String[] lp = Console.askConnUserPswd();
            if (lp == null) System.exit(-1);
            user = lp[0];
            mdp = lp[1];
         }
         int svport = options.has("http-port") ? (Integer) options.valueOf("http-port")
               : Config.SR_HTTP_PORT;
         try {
            new HttpConsole(contex, TypesSGBD.getTypeFromArg(dbtype), base, user, mdp, opts, host, port, svport);
         }
         catch (IOException e) {
            printErr("ERREUR IOException : " + e.getMessage());
            System.exit(-3);
         }
         catch (SQLException e) {
            printErr("ERREUR SQLException : " + e.getMessage());
            System.exit(-3);
         }
         System.out.println("En écoute sur http://localhost:" + svport + " et http://localhost/api:" + svport
               + ".\nTapez <Entrée> pour terminer le service.\n");
         // Ouverture du browser
         Desktop desk = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
         if (desk != null && desk.isSupported(Desktop.Action.BROWSE)) {
             try { desk.browse(new URL("http://localhost:" + svport + "/").toURI()); }
             catch (Exception ex) { System.err.println("Erreur navigation : " + ex.getMessage()); }
         } else System.err.println("Attention : ouverture du navigateur non supportée");
         // Sortie si touche "entrée" tappé
         try { System.in.read(); }
         catch (Throwable t) { }
         finally {
            contex.fermerConnex();
            System.exit(0);
         }
      }
      // autres commandes...

      else { // aucune commande spécifiée : lancement de la console par défaut
         setLogin(false);
         if (!hasBase) {
            printErr("commande CONSOLE invalide si aucune base fournie");
            System.exit(-4);
         }
         execConsole();
      }
   } // readArgs

   /**
    * Lancement par option Login
    * Options par entrée : dbpath, driver, schema, passwd
    *
    * @param doFile si le fichier INI doit obligatoirement être présent
    */
   private static void setLogin(boolean doFile) {
      creationContexte();
      try {
         if (basefile == null) basefile = new File(Config.CT_BASES_FILE);
         if (!basefile.isFile()) basefile = new File(Config.CT_BASES_PATH);
         if (!basefile.isFile()) {
            if (!doFile) return;
            printErr("fichier de définition de bases " + basefile.getCanonicalPath() + " introuvable"
                  + "\nIdentifiant de login demandé : " + (login.isEmpty() ? "(vide)" : login));
            System.exit(-5);
         }

         contex.setVar(Contexte.ENV_BASES_FILE, basefile.getCanonicalPath());
         Properties prop = new Properties();
         prop.load(new FileReader(basefile));
         if (login.isEmpty()) login = prop.getProperty("default", "");
         path = prop.getProperty(login + ".dbpath", "");
         driver = prop.getProperty(login + ".driver", "");
         if (path.isEmpty() || driver.isEmpty()) {
            printErr("entrée de fichier de bases invalide : " + (login.isEmpty() ? "(vide)" : login));
            System.exit(-5);
         }
         user = prop.getProperty(login + ".schema", "");
         mdp = prop.getProperty(login + ".passwd", "");
         hasBase = true;
      } catch (IOException ex) {
         printErr("ERREUR IOException : " + ex.getMessage());
         System.exit(-5);
      }
   }

   /**
    * Création du contexte d'exécution et de connexion
    */
   private static void creationContexte(boolean color) {
      if (contex != null) return;
      try { contex = new Contexte(color); }
      catch (Exception ex) {
         printErr("impossible de créer le contexte : " + ex.getMessage());
      }
   }
   private static void creationContexte() {
      creationContexte(true);
   }

   /**
    * Exécution de code
    */
   private static int execScript(String cmd){
      return execScript(cmd, true);
   }

   private static int execScript(String cmd, boolean close) {
      if (!hasContex) {
         creationContexte();
         contex.loadConfigFile();
         hasContex = true;
      }

      setsql();
      contex.loadInitFile();
      // Lecture du script
      Lecteur lec = new Lecteur(contex, cmd + ';',
            new HashMap<String, String>() {{ put(Contexte.LEC_THIS, "(run-sql)"); }});
      if (close) contex.fermerConnex();
      return Math.min(lec.getCmdState(), contex.getQuitStat());
   }

   /**
    * Exécution de console
    */
   private static void execConsole() {
      if (!hasContex) {
         creationContexte();
         contex.loadConfigFile();
         hasContex = true;
      }
      // Lancement console
      if (banner && contex.getVerbose() >= Contexte.VERB_MSG) Console.dispBanner(contex);
      setsql();
      if (banner) contex.printlnX("\nPour de l'aide sur les commandes, tapez 'help'", Contexte.BR_WHITE);
      contex.loadInitFile();
      new Console(contex);
   }

   /**
    * Création de la connexion
    * Contexte ne doit pas être null
    */
   private static void setsql() {
      if (contex.hasSQLCnx()) return;

      // Test user/mdp
      if (user.length() == 0) {
         String[] lp = Console.askConnUserPswd();
         if (lp == null) System.exit(-1);
         user = lp[0]; mdp = lp[1];
      }

      try {
         if (banner) {
            String bs = base.length() > 0 ? base : path;
            bs = bs.substring(bs.lastIndexOf(File.separatorChar) + 1);
            contex.print("Établissement de la connexion à " + bs + "... ");
         }
         SQLCnx sqlx = base.length() > 0 ? new SQLCnx(
               TypesSGBD.getTypeFromArg(dbtype), base, user, mdp, opts, host, port)
               : new SQLCnx(path, driver, user, mdp);
         if (banner) contex.println("OK");
         contex.setSQLCnx(sqlx);
      } catch (Exception e) {
         printErr("impossible de créer la connexion : " + e.getMessage());
         System.exit(-1);
      }
   }

   /**
    * Affichage d'un message d'erreur le cas échéant en rouge
    */
   private static void printErr(String msg) {
      if (contex != null && contex.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE)) {
         System.err.println(Contexte.COLORS[Contexte.BR_RED] + "LunaSQL : " + msg + Contexte.COLORS[Contexte.NONE]);
      }
      else System.err.println("LunaSQL : " + msg);
   }

   /**
    * Affichage d'une banière
    */
   private static void banner() {
      if (contex.getVerbose() >= Contexte.VERB_AFF) {
         contex.println("LunaSQL - Gestion d'une base de données en console et script");
         contex.println("Version : " + Config.APP_VERSION_NUM + " - " + Config.APP_VERSION_NAME + '\n');
      }
   }

   /**
    * Handles an exception thrown in the event-dispatch thread. Pratique pour ne pas planter le
    * programme sur une exception de type Event
    */
   public static class ExceptionHandler implements Thread.UncaughtExceptionHandler {

      public void handle(Throwable thrown) {
         // for EDT exceptions
         handleException(Thread.currentThread().getName(), thrown);
      }

      @Override
      public void uncaughtException(Thread thread, Throwable thrown) {
         // for other uncaught exceptions
         handleException(thread.getName(), thrown);
      }

      void handleException(String tname, Throwable thrown) {
         System.err.println("ERROR : Severe exception on thread : " + tname + " : "
               + thrown.getMessage());
         System.err.println(getNStackString(thrown));

         // Ecriture du fichier log des traces d'exception à l'exécution
         File  p = new File(System.getProperty(Contexte.PROP_LOG_DIR, Config.CT_LOG_DIR)),
               f = new File(System.getProperty(Contexte.PROP_LOG_DIR, Config.CT_LOG_DIR) + File.separator +
                     Config.CT_LOG_FILE);
         try {
            if (!p.exists()) System.err.println("\nÉcriture du rapport en " + f.getParent() + " impossible !");
            else if (f.exists() || f.createNewFile()) {
               // Ajout de l'information au fichier crashes
               BufferedWriter out = new BufferedWriter(new FileWriter(f, true));
               out.write("==================== "
                     + new SimpleDateFormat("E dd/MM/yyyy HH:mm:ss").format(new Date())
                     + " ====================\n");

               out.write("--- Propriétés système ---");
               out.write("\nos.version      : " + System.getProperty("os.name", "")
                     + " (version " + System.getProperty("os.version", "") + ") - "
                     + System.getProperty("os.arch", ""));
               out.write("\njava.version    : " + System.getProperty("java.version", ""));
               out.write("\njava.home       : " + System.getProperty("java.home", ""));

               out.write("\n\n--- Application LunaSQL ---");
               String cp = System.getProperty("java.class.path", "");
               if(cp.endsWith(File.pathSeparator)) cp = cp.substring(0, cp.length() - 2);
               out.write("\nlunasql version : " + Config.APP_VERSION_NUM);
               out.write("\njava.class.path :\n  - " + cp.replaceAll(File.pathSeparator, "\n  - "));

               out.write("\n\n--- Trace de l'exception ---\n");
               thrown.printStackTrace(new PrintWriter(out));
               out.write("\n");
               out.close();
               // Affichage
               System.out.println("\nRapport sauvé sous " + f.getCanonicalPath());
            }
         }
         catch (IOException ex) {
            ex.printStackTrace();
         }
      }

      private String getNStackString(Throwable e) {
         StringBuilder sb = new StringBuilder();
         sb.append("Origine : ").append(e.toString()).append('\n');
         int i = 1;
         sb.append("Trace de la pile :");
         for (StackTraceElement element : e.getStackTrace()) {
             sb.append("\n  ").append(element.toString());
             if (i++ >= 25) { // stop à 25 lignes
                sb.append("...\n[stop à ").append(25).append(" lignes]\n");
                break;
             }
         }
         return sb.toString();
     }
   }// ExceptionHandler
} // class

