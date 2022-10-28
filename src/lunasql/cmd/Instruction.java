package lunasql.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lunasql.lib.Contexte;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurExe;
import lunasql.val.ValeurReq;

/**
 * Classe Instruction<br>
 * Modèle de commande pour les classes filles<br>
 * 
 * @author M.P.
 */
public abstract class Instruction {

   public static final int     RET_CONTINUE = 0;
   public static final int     RET_NEXT_LP = RET_CONTINUE + 1;
   public static final int     RET_BREAK_LP = RET_NEXT_LP + 1;
   public static final int     RET_EV_CATCH = RET_BREAK_LP + 1;
   public static final int     RET_RETR_SCR = RET_EV_CATCH + 1;
   public static final int     RET_EXIT_SCR = RET_RETR_SCR + 1;
   public static final int     RET_SHUTDOWN = RET_EXIT_SCR + 1;
   public static final int     TYPE_VIDE    = 0;
   public static final int     TYPE_ERR     = TYPE_VIDE + 1;
   public static final int     TYPE_MOTC_WH = TYPE_ERR + 1;
   public static final int     TYPE_INVIS   = TYPE_MOTC_WH + 1;
   public static final int     TYPE_CMDSQL  = TYPE_INVIS + 1;
   public static final int     TYPE_CMDINT  = TYPE_CMDSQL + 1;
   public static final int     TYPE_CMDPLG  = TYPE_CMDINT + 1;

   protected int           type;
   protected String        name;
   protected String        alias;
   protected int           lng;
   protected ArrayList<String> command;
   protected Contexte          cont;


   /**
    * Constructeur Instruction
    * Impossible d'instancier une commande par ce constructeur : il sert pour super
    * 
    * @param cont le contexte
    * @param type le type d'instruction
    * @param name le nom de l'instruction à reconnaître
    * @param alias l'alias de l'instruction à reconnaître
    */
   public Instruction(Contexte cont, int type, String name, String alias) {
      this.cont = cont;
      this.type = type;
      this.name = name;
      this.alias = alias;
   }

   /**
    * Retourne le nombre d'éléments de l'instruction complète
    * 
    * @return int nombre d'éléments (tous arguments compris)
    */
   public int getLength() {
      return this.command.size();
   }

   /**
    * Retourne l'instruction complète
    * 
    * @return instruction en ArrayList
    */
   public ArrayList<String> getCommand() {
      return this.command;
   }

   /**
    * Retourne l'instruction à partir du Nième arg
    * 
    * @param n l'indice dans la commande
    * @return instruction en ArrayList
    */
   public ArrayList<String> getCommand(int n) {
      if (n < 0 || n >= command.size()) return new ArrayList<>();
      ArrayList<String> a = new ArrayList<>();
      for (int i = n; i < this.command.size(); i++) a.add(command.get(i));
      return a;
   }

   /**
    * Retourne l'instruction en tableau de strings à partir du Nième arg
    * 
    * @param n l'indice dans la commande
    * @return instruction en String[]
    */
   public String[] getCommandA(int n){
      return getCommand(n).toArray(new String[0]);
   }

   /**
    * Retourne les arguments de l'instruction en tableau de strings
    * 
    * @return instruction en String[]
    */
   public String[] getCommandA1(){
      return getCommand(1).toArray(new String[0]);
   }

   /**
    * Retourne la commande entière
    * 
    * @return commande en String
    */
   public String getSCommand() {
      return getSCommand(0);
   }

   /**
    * Retourne la commande à partir du Nième arg
    * 
    * @param n l'indice dans la commande
    * @return commande en String
    */
   public String getSCommand(int n) {
      if (n < 0 || n >= command.size()) return "";
      StringBuilder args = new StringBuilder();
      for (int i = n; i < command.size(); i++) args.append(command.get(i)).append(' ');
      if (args.length() > 0) args.deleteCharAt(args.length() - 1);
      return args.toString();
   }

   /**
    * Retourne le nom de la commande exécutée (position 0)
    * 
    * @return nom en chaîne de caractères
    */
   public String getCommandName() {
      return getArg(0);
   }

   /**
    * Retourne l'argument de la commande de la position n
    * 
    * @param n l'indice dans la commande
    * @return arguments en chaîne de caractères
    */
   public String getArg(int n) {
      if (n < 0 || n >= command.size()) return "";
      return this.command.get(n);
   }

   /**
    * Retourne le type de commande
    * 
    * @return le numéro du type de commande
    */
   public int getType() {
      return this.type;
   }

   /**
    * Retourne le nom de commande
    * 
    * @return le nom de commande
    */
   public String getName() {
      return this.name;
   }

   /**
    * Retourne l'alias de commande
    * 
    * @return l'alias de commande
    */
   public String getAlias() {
      return this.alias;
   }

   /**
    * Numéro de ligne
    * 
    * @return numéro de ligne
    */
   public int getNoLine() {
      return this.lng;
   }

   /**
    * Fixe la commande entière
    * 
    * @param command une ArrayList de la commande
    */
   public void setCommand(ArrayList<String> command) {
      this.command = command;
   }

   /**
    * Fixe le type de commande
    * 
    * @param type le numéro de type
    */
   public void setType(int type) {
      this.type = type;
   }

   /**
    * Fixe le numéro de ligne
    * 
    * @param lng le numéro de ligne
    */
   public void setNoLine(int lng) {
      this.lng = lng;
   }

   /**
    * Obtention de la description courte de la commande<br>
    * 
    * @return l'aide
    */
   public String getDesc(){
      return null;
   }

   /**
    * Obtention de l'aide de la commande<br>
    * 
    * @return l'aide
    */
   public String getHelp(){
      String cl = getClass().getSimpleName();
      try {
         return getFContent(null, "/lunasql/doc/" + cl.toLowerCase() + ".txt");
      } catch (IOException ex) {
         return "\n  ERREUR : impossible d'accéder à l'aide pour " + cl +
                "\n  Cause : " + ex.getMessage();
      }
   }

   /**
    * Obtention du contenu d'un fichier ressource en console
    * 
    * @param nom le titre à afficher (ou null)
    * @param fich le nom du fichier en fichier jar
    * @return le contenu
    * @throws java.io.IOException si fichier inrouvable
    */
   public String getFContent (String nom, String fich) throws IOException {
      StringBuilder sb = new StringBuilder();
      InputStream is =  getClass().getResourceAsStream(fich);
      if (is == null) throw new IOException("ressource introuvable");

      BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      String read;
      if (nom == null) sb.append('\n');
      else sb.append("\n ").append(SQLCnx.frmI(" " + nom, 67, '-')).append("\n\n");
      while((read = br.readLine()) != null) sb.append(read).append('\n');
      sb.deleteCharAt(sb.length() - 1);
      br.close();
      return sb.toString();
   }

   /**
    * Exécution de la commande<br>
    * 
    * @return RET_CONTINUE, RET_EXIT_SCR ou RET_SHUTDOWN
    */
   public abstract int execute();

   /**
    * Transforme une liste en chaîne de caractères
    * 
    * @param list la liste à transformer
    * @param deb à partir de cet indice d'argument inclus
    * @param fin jusqu'à cet indice d'argument inclu. Si -1: dernier élément, -2:
    * @return la châine formée séparée par des espaces
    */
   public static String listToString(List<?> list, int deb, int fin) {
      if (list == null) return null;
      if (fin < 0) fin = list.size() + fin;
      else if (fin >= list.size()) fin = list.size() - 1;

      StringBuilder sb = new StringBuilder();
      for(int i = deb; i <= fin; i++) {
         if (sb.length() > 0) sb.append(' ');
         sb.append(list.get(i).toString());
      }
      return sb.toString();
   }

   /**
    * Transforme une liste en chaîne de caractères
    * 
    * @param list la liste à transformer
    * @param deb à partir de cet indice d'argument
    * @return la châine formée séparée par des espaces
    */
   public static String listToString(List<?> list, int deb) {
     return listToString(list, deb, -1);
   }

   /**
    * Transforme une liste en chaîne de caractères
    * 
    * @param list la liste à transformer
    * @return la châine formée séparée par des espaces
    */
   public static String listToString(List<?> list){
      return listToString(list, 0, -1);
   }

   // Méthodes d'exécution pour les classes filles (commandes SQL)

   /**
    * Exécute une commande SQL de modification
    *
    * @param cmd le nom de la commande
    * @param comm le commentaire à afficher
    * @return continue
    */
   protected int executeUpdate(String cmd, String comm) {
      return executeUpdate(cmd, comm, false);
   }

   /**
    * Exécute une commande SQL de modification
    *
    * @param cmd le nom de la commande
    * @param comm le commentaire à afficher
    * @param ligne si l'on affiche le résumé du nb de lignes affectées
    * @return continue
    */
   protected int executeUpdate(String cmd, String comm, boolean ligne) {
      if (!cont.getVar(Contexte.ENV_SQL_UPDATE).equals(Contexte.STATE_TRUE))
         return cont.erreur(cmd, "exécution d'une commande SQL de modification non autorisée\n" +
               "(console lancée avec l'option --deny-sql-update. Cf documentation de OPT)", lng);

      try {
         cont.showWheel();
         long tm = System.currentTimeMillis();
         ValeurExe vr = new ValeurExe(cont, cont.getConnex().execute(getSCommand()));
         tm = System.currentTimeMillis() - tm;
         cont.hideWheel();
         int n = vr.getNbLines();
         if (ligne) comm = (cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE) ?
               Contexte.COLORS[Contexte.BR_CYAN] + n + Contexte.COLORS[Contexte.NONE] : n)
               + " ligne" + (n > 1 ? "s" : "") + comm + (n > 1 ? "s" : "");
         vr.setDispValue("-> " + comm + " (" + SQLCnx.frmDur(tm) + ")");
         cont.setValeur(vr);
         cont.setVar(Contexte.ENV_EXEC_TIME, Long.toString(tm));
         cont.setVar(Contexte.ENV_CMD_STATE, n > 0 ? Contexte.STATE_TRUE : Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (SQLException ex) {
         cont.hideWheel();
         return cont.exception(cmd, "ERREUR SQLException : "+ ex.getMessage(), lng, ex);
      }
   }

   /**
    * 
    * @param cmd le nom de la commande
    * @param comm le commentaire à afficher
    * @return l'état d'exécution
    */
   protected int executeCall(String cmd, String comm) {
      try {
         cont.showWheel();
         long tm = System.currentTimeMillis();
         ValeurReq vr = new ValeurReq(cont, cont.getConnex().getResultString(getSCommand(),
               cont.getVar(Contexte.ENV_SELECT_ARR).equals(Contexte.STATE_TRUE),
               cont.getColMaxWidth(lng), cont.getRowMaxNumber(lng),
               cont.getVar(Contexte.ENV_ADD_ROW_NB).equals(Contexte.STATE_TRUE),
               cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE)));
         tm = System.currentTimeMillis() - tm;
         cont.hideWheel();

         if (cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE))
            vr.setDispValue("-> " + comm +
                  (vr.isNblTrunc() ? Contexte.COLORS[Contexte.BR_YELLOW] + " (d'autres lignes existent)" +
                        Contexte.COLORS[Contexte.NONE] : "") + " (" + SQLCnx.frmDur(tm) + ")");
         else
            vr.setDispValue("-> " + comm + (vr.isNblTrunc() ? " (d'autres lignes existent)" : "") +
                  " (" + SQLCnx.frmDur(tm) + ")");

         cont.setValeur(vr);
         cont.setVar(Contexte.ENV_EXEC_TIME, Long.toString(tm));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         return RET_CONTINUE;
      }
      catch (SQLException ex) {
         cont.hideWheel();
         return cont.exception(cmd, "ERREUR SQLException : "+ ex.getMessage(), lng, ex);
      }
   }
   
   @Override
   public int hashCode() {
      return this.getName().hashCode() * this.getLength();
   }
}// class
