/*
 * Console SQL
 * @author M.P.
 */
package lunasql;

import java.io.File;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.FileNameCompletor;
import jline.History;
import jline.MultiCompletor;
import jline.SimpleCompletor;
import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
//TODO : passer donc en Jline 3 https://github.com/jline/jline3

public class Console {

   private Contexte cont;
   private ConsoleReader reader;

   /**
    * Constructeur Console
    * Pas d'affichage de la bannière. La faire en amont.
    *
    * @param cont le contexte d'exécution (connexion à la base et environnement)
    */
   public Console(Contexte cont) {
      // Contexte
      this.cont = cont;
      if (cont == null || !cont.hasSQLCnx()) 
         throw new IllegalArgumentException("Constructeur Console : contexte ou connexion SQL null");

      compute();
   }

   /**
    * Constructeur Console
    *
    * @param cont le contexte d'exécution (connexion à la base et environnement)
    * @param path le chemin de connexion
    * @param driver le driver de BDD
    * @param login le nom d'utilisateur
    * @param mdp le mot de passe de connexion
    * @param banner si on affiche la bannière d'accueil
    * @throws java.sql.SQLException si erreur SQL
    */
   public Console(Contexte cont, String path, String driver, String login, String mdp, boolean banner)
           throws SQLException {
      createConsole(false, cont, 0, path, driver, null, login, mdp, null, null, 0, banner);
   }

   /**
    * Constructeur Console
    *
    * @param cont le contexte d'exécution (connexion à la base et environnement)
    * @param type le type de BDD
    * @param base le nom de la base
    * @param login le nom d'utilisateur
    * @param mdp le mot de passe de connexion
    * @param opts les options des connexions (ex : crypt)
    * @param host le serveur hébergeant la base
    * @param port le port de connexion (0 si non renseigné)
    * @param banner si on affiche la bannière d'accueil
    * @throws SQLException si erreur SQL
    */
   public Console(Contexte cont, int type, String base, String login, String mdp, String opts, 
           String host, int port, boolean banner) throws SQLException {
      createConsole(true, cont, type, null, null, base, login, mdp, opts, host, port, banner);
   }

   /**
    * Création et lancement de la console
    * @param mode mode de connexion : simplifié (avec type) ou normal (avec chaîne de connexion)
    * @throws SQLException si erreur SQL
    */
   private void createConsole(boolean mode, Contexte cont, int type, String path, String driver,
           String base, String login, String mdp, String opts, String host, int port, boolean banner)
           throws SQLException {
      // Contexte
      if (cont == null) throw new IllegalArgumentException("Constructeur Console : contexte null");
      this.cont = cont;
      // Affichage
      if (banner && cont.getVerbose() >= Contexte.VERB_MSG) dispBanner(cont);

      // Création de la console et connexion
      if (!cont.hasSQLCnx()) {
         String l = login, p = mdp;
         if (l == null || p == null || (l.length() == 0 && p.length() == 0)) {
            String[] lp = askConnUserPswd();
            if (lp == null) System.exit(-1);
            l = lp[0]; p = lp[1];
         }
         // Connexion si pas de connexion déjà en contexte
         SQLCnx sqlx;
         if (mode) sqlx = new SQLCnx(type, base, l, p, opts, host, port); // mode simplifié
         else sqlx = new SQLCnx(path, driver, l, p); // mode normal
         cont.setSQLCnx(sqlx);
         cont.loadInitFile(); // Si présence d'un fichier INIT, exécution du fichier
      }
      compute();
   }

   /**
    * Affichage du message d'accueil
    *
    * @param cont le contexte
    */
   static void dispBanner(Contexte cont) {
      cont.printlnX(" " + SQLCnx.frm("", 64, '='), Contexte.YELLOW);
      cont.printlnX("   LunaSQL - Gestion d'une base de données en console et script", Contexte.GREEN);
      cont.printlnX("     Version : " + Config.APP_VERSION_NUM + " - " + Config.APP_VERSION_NAME +
            " - " + Config.APP_DT_REVISION, Contexte.GREEN);
      cont.printlnX(" " + SQLCnx.frm("", 64, '=') + "\n", Contexte.YELLOW);
   }

   /**
    * *** Mode de saisie SQL en boucle ***
    */
   private void compute() {
      try {
         reader = new ConsoleReader();
         cont.setConsole(this);
         cont.setConsoleReader(reader);
         reader.setBellEnabled(cont.getVar(Contexte.ENV_BEEP_ON).equals(Contexte.STATE_TRUE));

         // Completors
         setCompletors();

         // Historique facultatif
         History histo = null;
         //FileHistory histo = null;
         if (cont.getVar(Contexte.ENV_HIST_FILE).length() > 0) {
            histo = new History(new File(cont.getVar(Contexte.ENV_HIST_FILE)));
            //histo = new FileHistory(new File(cont.getVar(Contexte.ENV_HIST_FILE)));
            histo.setMaxSize(Config.CS_HISTORY_SIZE);
            cont.setHistory(histo);
            reader.setHistory(histo);
            reader.setUseHistory(false); // ajout manuel des commandes
         }

         // Prompt SQL
         boolean canCont = true;
         StringBuilder prompt = new StringBuilder();
         Lecteur lec;
         while (canCont) {
            boolean hasColors = cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE);
            String hdbl = cont.getVar(Contexte.ENV_HIST_DBL); // 0, 1 ou 2
            lec = new Lecteur(cont);
            lec.setLecVar(Contexte.LEC_SUPER, "");
            lec.setLecVar(Contexte.LEC_THIS, "(console)");
            lec.setConsoleMode(); // seule utilisation
            lec.setCheckWhile(false);
            boolean rtmp = false;
            prompt.setLength(0);
            prompt.append('\n');
            if (hasColors) prompt.append("\u001b[32m");
            prompt.append(lec.substituteExt(cont.getVar(Contexte.ENV_PROMPT))).append("> ");
            if (lec.getCmdState() != Instruction.RET_CONTINUE) {
               cont.errprintln("Réinitialisation de l'option " + Contexte.ENV_PROMPT);
               cont.setVar(Contexte.ENV_PROMPT, "");
               continue;
            }
            if (hasColors) prompt.append("\u001b[0m");

            String line;
            StringBuilder lines = new StringBuilder(); // doublage de l'historique
            int np = 0, na = 0, nc = 0, nl = 2;
            do { // boucle d'assemblage des multi-lignes
               try {
                  line = reader.readLine(prompt.toString());
               }
               catch (IndexOutOfBoundsException ex){
                  // FIXME: correction de BUG JLine sur pression sur <ESC> alors que curseur en commande
                  // Sera résolu (j'espère) avec JLine3
                  cont.errprintln("\nBUG JLine 1 : soyez en fin de ligne avant de presser <ESC>", false);
                  line = null; // pour sortir
               }
               if (line == null) {
                  if (nl > 2) {
                     cont.errprintln("\nInterrompu");
                     break;
                  } else {
                     if (cont.canExec()) cont.println("Bye!");
                     else cont.println("");
                     line = "QUIT;";
                     if (histo != null) histo = null; // ne pas inscrire la ligne QUIT
                  }
               }
               else if (line.length() == 0) continue;

               rtmp = lec.add(line);
               lines.append(line).append('\n');
               np = lec.getNP();    // nb de parenthèses ouvertes
               na = lec.getNA();    // nb d'accolades ouvertes
               nc = lec.getNC();    // nb de crochets ouverts
               prompt.setLength(0);
               if (hasColors) prompt.append("\u001b[33m");
               prompt.append(String.format("%02d ", nl++));
               prompt.append(np == 0 && na == 0 && nc == 0 ? lec.getMLineChar() :
                  (np > 0 ? String.format("%02d(%" + (np * 2) + "s", np, "") :
                     (na > 0 ? String.format("%02d{%" + (na * 2) + "s", na, "") :
                        (nc > 0 ? String.format("%02d[%" + (nc * 2) + "s", nc, "") : "#)"))));
               if (hasColors) prompt.append("\u001b[0m");
               prompt.append(' ');
            } while (!rtmp);

            if ((np != 0 || na != 0 || nc != 0) && cont.getVerbose() >= Contexte.VERB_MSG) 
               cont.errprintln("Parenthèses/accolades/crochets désappariés : " + (np + na + nc));
            canCont = (lec.getCmdState() != Instruction.RET_SHUTDOWN);

            if (histo != null) {
               if (hdbl.equals("1")) histo.addToHistory(line);
               else if (hdbl.equals("2")) histo.addToHistory(Tools.cleanSQLCode(cont, lines.toString(), -2));
               else if (hdbl.equals("3")) histo.addToHistory(Tools.cleanSQLCode(cont, lines.toString(), 2).trim());
            }

            // Affichage du retour de la commande
            if (cont.getVerbose() >= Contexte.VERB_MSG) {
               Valeur vr = cont.getValeur();
               String sub;
               if (vr != null && (sub = vr.getSubValue()) != null)
                  cont.printlnX("=> " + sub, Contexte.BR_BLUE);
            }
            lec.fin();
         }// while

         // Fermeture
         reader.flushConsole();
         //reader.flush();
         if (cont.getVar(Contexte.ENV_SAVE_CONF).equals(Contexte.STATE_TRUE)) cont.dumpConfigFile();
      }
      catch (IOException ex) {
         cont.exception("Console", "ERREUR IOException : " + ex.getMessage() + 
               "\nOuverture de la console interrompue", 1, ex);
      }
      finally {
         if (cont.fermerConnex()) System.exit(cont.getQuitStat());
         // en console, on sort de toutes façons
      }
   }

   /**
    * Fixe les différents moteurs de complètement
    */
   @SuppressWarnings("unchecked")
   public void setCompletors() {
      // Suppression des anciens
      Iterator<Completor> it = reader.getCompletors().iterator();
      while (it.hasNext()) reader.removeCompletor(it.next());

      // Création des nouveaux
      List<Completor> cpArg = new LinkedList<>();
      // Arg 1 : Compléter les commandes/macros/alias
      HashMap<String, Instruction> cmds = cont.getAllCommands();
      List<String> cpCmd = new ArrayList<>(cmds.size());
      for (Map.Entry<String, Instruction> me : cmds.entrySet())
         if (me.getKey() != null){ 
            cpCmd.add(me.getKey());
            cpCmd.add(me.getKey().toLowerCase());
         }
      cpArg.add(new MultiCompletor(new Completor[]{
            new SimpleCompletor(cpCmd.toArray(new String[]{})),
            new SimpleCompletor(getVariablesCol())
      }));
      // Arg 2 et plus : Compléter les fichiers et les tables
      cpArg.add(new MultiCompletor(new Completor[]{
            new FileNameCompletor(),
            new SimpleCompletor(getTables()),
            new SimpleCompletor(getVariablesDol())
      }));
      reader.addCompletor(new ArgumentCompletor(cpArg));
   }

   /**
    * Liste des tables en majuscule et en minuscule
    * Types listés : "TABLE", "VIEW", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"
    *
    * @return String[] de tables non system
    */
   private String[] getTables() {
      String[] ret = null;
      try {
         DatabaseMetaData dMeta = cont.getConnex().getMetaData();
         String[] typestbl =
            {"TABLE", "VIEW", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"};
         ArrayList<String> tables = new ArrayList<>();
         ResultSet result = dMeta.getTables(null, null, null, typestbl);
         while (result.next()) {
            tables.add(result.getString(3));
         }
         ret = new String[tables.size() * 2];
         for (int i = 0; i < tables.size(); i++) {
            ret[i * 2] = tables.get(i).toUpperCase();
            ret[i * 2 + 1] = tables.get(i).toLowerCase();
         }
         result.close();
      }
      catch (SQLException ex) {
         cont.exception("Console", "ERREUR SQLException : " + ex.getMessage(), 1, ex);
      }
      return ret;
   }

   /**
    * Liste des variables et constantes définies
    *
    * @return String[] des variables
    */
   private String[] getVariablesDol() {
      Object[] o = cont.getAllVars().keySet().toArray();
      int l = o.length;
      String[] r = new String[l * 2];
      for (int i = 0; i < l; i++) {
         r[i] = (String) o[i];
         r[i + l] = "$(" + o[i] + ")";
      }
      return r;
   }
   private String[] getVariablesCol() {
      Object[] o = cont.getAllVars().keySet().toArray();
      int l = o.length;
      String[] r = new String[l * 2];
      for (int i = 0; i < l; i++) {
         r[i] = (String) o[i];
         r[i + l] = cont.isNonSys(r[i]) ? ":" + o[i] : "";
      }
      return r;
   }

   /**
    * Demande des identifiants de connexion
    * 
    * @return String[] tableau de deux éléments : le nom de connexion et le mot de passe
    */
   static String[] askConnUserPswd() {
      java.io.Console csl = System.console();
      if (csl != null) {
         String l, p;
         l = csl.readLine("Nom d'utilisateur : ");
         if (l == null) { // saisie de null (Ctrl-D), on ->[]
            System.out.println("\n\nBye");
            return null;
         }
         char [] cp = csl.readPassword("Mot de passe : ");
         if (cp == null) { // saisie de null (Ctrl-D), on ->[]
            System.out.println("\nBye");
            return null;
         }
         p = new String(cp);
         return new String[]{l, p};
      }
      return null;
   }
}// class

