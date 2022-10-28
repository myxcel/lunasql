package lunasql.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lunasql.lib.Contexte;
import lunasql.lib.Tools;

/**
 * Connexion à une base JDBC
 * @author M.P.
 */
public class SQLCnx {

   private int type;
   private String login, // nom de l'utilisateur qui se connecte
           driver, // pilote de connexion à la base
           base, // base de données
           path;        // chaîne de connexion
   private Connection connex;
   private Statement statmt;

   /**
    * Constructeur SQLConnexion simple (pas de type...)
    * @param path la chaine entière de connexion
    * @param driver le chemin de la classe du driver
    * @param login nom d'utilisateur
    * @param pswd mot de passe de connexion
    * @throws SQLException si erreur SQL
    */
   public SQLCnx(String path, String driver, String login, String pswd)
           throws IllegalArgumentException, SQLException {

      if (path == null || path.length() == 0)
         throw new IllegalArgumentException("Chemin de base de données null");
      if (driver == null || driver.length() == 0)
         throw new IllegalArgumentException("Chaîne de driver de connexion null");
      if (login == null || pswd == null)
         throw new IllegalArgumentException("Indentifiant ou mot de passe null");

      // Extraction du type de SGBD
      Matcher mat = Pattern.compile("^jdbc:([a-z0-9]+?):").matcher(path);
      if (mat.find()) {
         String t = mat.group(1);
         if (t.equals("odbc")) t = "access";
         this.type = TypesSGBD.getTypeFromArg(t);
      }
      this.login = login;
      try {
         Class.forName(driver).newInstance();

         this.path = path;
         this.base = path;
         this.driver = driver;
         this.connex = DriverManager.getConnection(path, login, pswd);
         this.statmt = this.connex.createStatement();
      } catch (InstantiationException ex) {
         System.err.println("\nERREUR InstantiationException : " + ex.getMessage() + "\n");
         System.exit(-5);
      } catch (IllegalAccessException ex) {
         System.err.println("\nERREUR IllegalAccessException : " + ex.getMessage() + "\n");
         System.exit(-5);
      } catch (ClassNotFoundException ex) {
         System.err.println("\nERREUR ClassNotFoundException : " + ex.getMessage() +
                 "\nAvez-vous ajouté la bibliothèque de votre driver de connexion au CLASSPATH ?\n");
         System.exit(-5);
      }
   }

   /**
    * Constructeur SQLConnexion pour les bases locales (fichier)
    * @param type type de la base : TYPE_ACCESS, TYPE_HSQLDB, TYPE_H2DB
    * @param base nom ou chemin de la base de données
    * @param login nom d'utilisateur
    * @param pswd mot de passe de connexion
    * @throws SQLException si erreur SQL
    */
   public SQLCnx(int type, String base, String login, String pswd) throws SQLException {
      this(type, base, login, pswd, null, null, 0);
   }

   /**
    * Constructeur SQLConnexion pour les bases locales (fichier)
    * @param type type de la base : TYPE_ACCESS, TYPE_HSQLDB, TYPE_H2DB
    * @param base nom ou chemin de la base de données
    * @param login nom d'utilisateur
    * @param pswd mot de passe de connexion
    * @param opts les options des connexions (ex : crypt)
    * @throws SQLException si erreur SQL
    */
   public SQLCnx(int type, String base, String login, String pswd, String opts) throws SQLException {
      this(type, base, login, pswd, opts, null, 0);
   }

   /**
    * Constructeur SQLConnexion pour les bases distantes (serveur)
    * @param type type de la base : TYPE_ORACLE, TYPE_DERBY, TYPE_SQLSERV
    * @param base nom ou chemin de la base de données
    * @param login nom d'utilisateur
    * @param pswd mot de passe de connexion
    * @param opts les options des connexions (ex : crypt)
    * @param host le cas échéant, le nom du serveur portant la base
    * @param port le cas échéant, le port pour la connexion
    * @throws SQLException si erreur SQL
    */
   public SQLCnx(int type, String base, String login, String pswd, String opts, String host, int port)
           throws SQLException {
      if (!TypesSGBD.existsType(type))
         throw new IllegalArgumentException("Type invalide : " + type);
      if (base == null || base.length() == 0)
         throw new IllegalArgumentException("Base de données non renseignée");
      if (login == null || pswd == null)
         throw new IllegalArgumentException("Indentifiant ou mot de passe null");

      this.type = type;
      this.base = base;
      this.login = login;
      try {
         TypesSGBD tsgbd = new TypesSGBD(type, new String[]{
               host, port == 0 ? "" : Integer.toString(port), base, opts == null ? "" : ";" + opts
         });
         String ch = tsgbd.getChaine();
         Class.forName(tsgbd.getDriver()).newInstance();

         this.path = ch;
         this.driver = tsgbd.getDriver();
         this.connex = DriverManager.getConnection(ch, login, pswd);
         this.statmt = this.connex.createStatement();
      } catch (IllegalArgumentException ex) {
         System.err.println("\nERREUR IllegalArgumentException : " + ex.getMessage() + "\n");
         System.exit(-5);
      } catch (InstantiationException ex) {
         System.err.println("\nERREUR InstantiationException : " + ex.getMessage() + "\n");
         System.exit(-5);
      } catch (IllegalAccessException ex) {
         System.err.println("\nERREUR IllegalAccessException : " + ex.getMessage() + "\n");
         System.exit(-5);
      } catch (ClassNotFoundException ex) {
         System.err.println("\nERREUR ClassNotFoundException : " + ex.getMessage() +
                 "\n\tAvez-vous ajouté la bibliothèque de votre driver de connexion au CLASSPATH ?\n");
         System.exit(-5);
      } 
   }

   /**
    * Constructeur SQLConnexion
    * @param connex la connexion
    * @throws SQLException si erreur SQL
    * @throws IllegalArgumentException si connexion nulle
    */
   public SQLCnx(Connection connex) throws IllegalArgumentException, SQLException {
      if (connex == null) throw new IllegalArgumentException("Connexion nulle");
      this.connex = connex;
      creerStmt();
   }

   /**
    * Création du statement (ou renvoi de celui en cours si non fermé / non null)
    * @throws SQLException si erreur SQL
    */
   private void creerStmt() throws SQLException {
      if (statmt == null) statmt = connex.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
   }

   /**
    * Fermeture du statement en cours et de la connexion
    * @throws SQLException si erreur SQL
    */
   public void fermer() throws SQLException {
      if (type == TypesSGBD.TYPE_HSQLDB) execute("SHUTDOWN");
      if (statmt != null) statmt.close();
      if (connex != null) connex.close();
   }

   /**
    * getResultset : objet de parcours des lignes de la requête
    * Usage :
    * object rs=sqlselect("SELECT * FROM TABLE1")
    * while(rs.next())
    *    print(rs.getString(1)) # getString getInt...
    * endwhile
    * rs.close()
    * @param sqlcmd la commande SQL
    * @return le ResultSet
    * @throws SQLException si erreur SQL
    */
   public synchronized ResultSet getResultset(String sqlcmd) throws SQLException {
      if (connex == null || statmt == null) throw new SQLException("Connexion non instanciée");
      if (sqlcmd == null || sqlcmd.length() == 0) throw new IllegalArgumentException("Requête vide");
      return statmt.executeQuery(sqlcmd);
   }

   /**
    * getResultArray : résultat de la commande sous forme de string
    * @param sqlcmd la commande SQL
    * @return un ArrayList d'ArrayList
    * @throws SQLException si erreur SQL
    */
   public synchronized ArrayList<ArrayList<Object>> getResultArray(String sqlcmd)
         throws SQLException {
      ArrayList<ArrayList<Object>> lignes = new ArrayList<>();
      ResultSet rs = getResultset(sqlcmd);
      ResultSetMetaData rsSchema = rs.getMetaData();
      int nbCol = rsSchema.getColumnCount();

      while (rs.next()) {
         ArrayList<Object> ll = new ArrayList<>(nbCol);
         Object o;
         for (int i = 1; i <= nbCol; i++) { // Calcul de la longueur des données
            o = rs.getObject(i);
            ll.add(o == null ? "" : o);
         }
         lignes.add(ll);
      }
      rs.close();
      return lignes;
   }

   /**
    * getResultString : résultat de la commande sous forme de string
    * TODO : retourner donc un objet ValeurReq !
    *
    * @param sqlcmd la commande SQL
    * @param mode le mode d'affichage : tabulaire (true) ou linéaire (false)
    * @param maxcl la largeur maximale des colonnes (si &lt; 5 : limité à 1000)
    * @param maxlg le nombre maximal de ligne à retourner (si &lt; 0 ou nul : limité à 1000)
    * @param rowno si l'on affiche le numéro de ligne
    * @param colors si l'on affiche les couleurs
    * @return un objet QueryResult (sans Contexte)
    * @throws SQLException si erreur SQL
    */
   public synchronized QueryResult getResultString(String sqlcmd, boolean mode, int maxcl, int maxlg,
                                                 boolean rowno, boolean colors) throws SQLException {
      if (maxcl < 5) maxcl = 1000;

      // Recherche des données
      String valPrem = null;
      ResultSet rs = getResultset(sqlcmd);
      ResultSetMetaData rsSchema = rs.getMetaData();
      int no = (rowno ? 1 : 0), nbCol = rsSchema.getColumnCount(), nbLig = 0;
      ArrayList<String> cols = new ArrayList<>();
      if (rowno) cols.add("#");
      ArrayList<String[]> lignes = new ArrayList<>();
      int[] tMax = new int[nbCol + no];
      if (rowno) tMax[0] = 1;
      for (int i = 0; i < nbCol; i++) { // Calcul de la longueur des noms de champs
         String c = rsSchema.getColumnName(i + 1);
         cols.add(c);
         int l = c.length();
         tMax[i + no] = l==0 ? 6 : l; // longueur minimale en cas de champ null : 'ExprXX'
      }

      // Récupération des données
      boolean overrow = false;
      while (rs.next()) {
         // Interruption si nb lignes max est atteint
         if (++nbLig > maxlg) {
            overrow = true;
            nbLig = maxlg;
            break;
         }
         String s;
         String[] t = new String[nbCol + no];
         if (rowno) {
            t[0] = Integer.toString(nbLig);
            tMax[0] = Math.max(tMax[0], t[0].length());
         }
         for (int i = 0; i < nbCol; i++) { // Calcul de la longueur des données
            s = rs.getString(i + 1);
            if (nbLig == 1 && i == 0) valPrem = s; // récupération de la valeur [1,1] pour retour
            int i2 = i + no;
            if (s == null) t[i2] = "%<dbnull>";
            else if (s.length() > maxcl) t[i2] = s.substring(0, maxcl - 3) + "...";
            else t[i2] = s;
            tMax[i2] = Math.min(Math.max(tMax[i2], t[i2].length()), maxcl);
         }
         lignes.add(t);
      }

      // Mise en forme des données 
      StringBuilder ret = new StringBuilder();
      if (mode) { // format tabulaire
         ret.append('+');
         // Ajout de la 1ère ligne de séparateurs
         for (int i = 0; i < nbCol + no; i++) ret.append(frm2("", tMax[i], '-')).append('+');
         ret.append("\n|");
         // Ajout des noms de champs
         int nbExpr = 0;
         for (int i = 0; i < nbCol + no; i++) {
            String c = cols.get(i);
            if (colors)
               ret.append(Contexte.COLORS[Contexte.BR_YELLOW])
                     .append(frmT(c == null || c.isEmpty() ? "Expr" + String.format("%02d", ++nbExpr) : c, tMax[i]))
                     .append(Contexte.COLORS[Contexte.NONE]).append('|');
            else
               ret.append(frmT(c == null || c.isEmpty() ? "Expr" + String.format("%02d", ++nbExpr) : c, tMax[i])).append('|');
         }
         ret.append("\n+");
         // Ajout de la 2ème ligne de séparateurs
         for (int i = 0; i < nbCol + no; i++) ret.append(frm2("", tMax[i], '-')).append('+');
         ret.append("\n|");
         // Ajout des données (avec alignement à droite des nombres et mise en couleur)
         String[] t;
         Pattern ptnb = Pattern.compile("[+-]?\\d+([.,]\\d+)?([E|e][+-]?\\d+)?");
         for (int i = 0; i < nbLig; i++) { // chq ligne de données
            t = lignes.get(i);
            for (int j = 0; j < nbCol + no; j++) { // chq colonne
               if (t[j].equals("%<dbnull>")) {
                  if (colors)
                     ret.append(Contexte.COLORS[Contexte.MAGENTA]).append(frm2("NULL", tMax[j], ' '))
                           .append(Contexte.COLORS[Contexte.NONE]).append('|');
                  else ret.append(frm2("NULL", tMax[j], ' ')).append('|');
               } else {
                  if (colors && rowno && j == 0)
                     ret.append(Contexte.COLORS[Contexte.YELLOW]).append(frmI2(t[j], tMax[j]))
                           .append(Contexte.COLORS[Contexte.NONE]).append('|');
                  else ret.append(ptnb.matcher(t[j]).matches() ? frmI2(t[j], tMax[j]) :
                        frm2(t[j].replace('\n', ' '), tMax[j], ' ')).append('|');
               }
            }
            ret.append("\n|");
         }
         // Ajout de la ligne inférieure
         ret.deleteCharAt(ret.length() - 1).append('+');
         for (int i = 0; i < nbCol + no; i++) ret.append(frm2("", tMax[i], '-')).append('+'); // 1 ligne de sép.
      } else { // format linéaire
         String[] t;
         for (int i = 0; i < nbLig; i++) { // chq ligne de données
            ret.append("-- ligne ").append(i + 1).append('\n');
            t = lignes.get(i);
            for (int j = 0; j < nbCol + no; j++) // chq colonne
               ret.append(cols.get(j)).append(" : ").append(t[j]).append('\n');
         }
      }
      rs.close();
      //return new String[]{ret.toString(), valPrem, Integer.toString(nbLig), overrow ? "1" : "0"};
      return new QueryResult(ret.toString(), valPrem, nbLig, overrow);
   }

   /**
    * Recherche d'une valeur retournée par commande SELECT
    * Si le résultat de la requête fait plusieurs colonnes, une propriété (dict) est retournée
    * Si en plus le résulat fait plusieurs lignes, c'est un tableau (list) de propriétés (dict)
    * 
    * @param sqlcmd la commande SQL
    * @param enc l'encodage pour la sortie Properties
    * @param rowmax le nombre max de lignes à retourner
    * @return valeur unique
    * @throws SQLException si erreur SQL
    */
   public synchronized String seek(String sqlcmd, String enc, int rowmax) throws SQLException {
      ResultSet rs = getResultset(sqlcmd);
      ResultSetMetaData rsm = rs.getMetaData();
      StringBuilder sb = new StringBuilder();
      int nbcol = rsm.getColumnCount(), norow = 0;
      while (rs.next()) {
         if (nbcol == 1) {
            String v = rs.getString(1);
            sb.append(Tools.putBraces(v == null ? "" : v)).append('\n');
         }
         else {
            Properties prop = new Properties();
            int nbExpr = 0;
            for (int i = 1; i <= nbcol; i++) {
               String v = rs.getString(i), ncol = rsm.getColumnName(i);
               prop.put(ncol == null || ncol.isEmpty() ?
                     "Expr" + String.format("%02d", ++nbExpr) : ncol, v == null ? "" : v); // NULL
            }
            try {
               ByteArrayOutputStream os = new ByteArrayOutputStream();
               prop.store(os, null);
               sb.append("{\n").append(os.toString(enc)).append("}\n");
            } catch (IOException ex) { }
         }
         if (++norow >= rowmax) break;
      }
      if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
      rs.close();
      return sb.toString();
   }

   /**
    * Recherche d'un retour de nombre de ligne
    * @param sqlcmd table + where éventuel
    * @return valeur int unique
    * @throws SQLException si erreur SQL
    */
   public synchronized int seekNbl(String sqlcmd) throws SQLException {
      ResultSet rs = getResultset("SELECT COUNT(*) FROM " + sqlcmd);
      int nbl = 0;
      if (rs.next()) nbl = rs.getInt(1);
      rs.close();
      return nbl;
   }

   /**
    * Execution de la requete de mise à jour INSERT, UPDATE, DELETE  (pas de risque d'exception)
    * @param sqlcmd la requête SQL
    * @return le nombre de lignes modifiées ou exception
    * @throws SQLException si erreur SQL
    */
   public synchronized int execute(String sqlcmd) throws SQLException {
      int result;
      result = statmt.executeUpdate(sqlcmd);
      return result;
   }// int execUpadte(String)

   /**
    * Liste des méta-datas
    * @return les méta-datas
    */
   public DatabaseMetaData getMetaData() {
      DatabaseMetaData dbmd = null;
      try {
         dbmd = connex.getMetaData();
      } catch (SQLException e) {
         System.err.println("ERREUR SQLException : " + e.getMessage());
      }
      return dbmd;
   }

   /* Accesseurs : sers-t'en ou t'es un perdant */
   public String getLogin() {
      return login;
   }

   public String getDriver() {
      return driver;
   }

   public String getPath() {
      return path;
   }

   public String getBase() {
      return base;
   }

   public int getType() {
      return type;
   }

   public Connection getConnex() {
      return this.connex;
   }

   public String getCatalog() throws SQLException {
      return this.connex.getCatalog();
   }

   /**
    * Formatage des données sur 20 car. avec espace
    *
    * @param txt le texte à formater
    * @return texte formaté
    */
   public static String frm(String txt) {
      return frm(txt, 20, ' ');
   }// frm

   /**
    * Formatage des données avec espace
    *
    * @param txt le texte à formater
    * @param lg la longueur à obtenir
    * @return texte formaté
    */
    public static String frm(String txt, int lg) {
      return frm(txt, lg, ' ');
   }// frm

   /**
    * Formatage des données
    *
    * @param txt le texte à formater
    * @param lg  la longueur à obtenir
    * @param c   le caractère de remplissage
    * @return texte formaté
    *
    */
   public static String frm(String txt, int lg, char c) {
      StringBuilder ret = new StringBuilder();
      if (txt != null) {
         ret.append(txt);
         for (int i = txt.length(); i < lg; i++) ret.append(c);
      }
      return ret.toString();
   }// frm

   /**
    * Formatage des données, et en ajoutant un caractère c au début
    * @param txt le texte à formater
    * @param lg  la longueur à obtenir
    * @param c   le caractère de remplissage
    * @return texte formaté
    */
   private static String frm2(String txt, int lg, char c) {
      StringBuilder ret = new StringBuilder();
      ret.append(c);
      if (txt != null) {
         ret.append(txt);
         for (int i = txt.length(); i <= lg; i++) ret.append(c);
      }
      return ret.toString();
   }// frm

   /**
    * Formatage des données en sens inverse avec espace
    *
    * @param txt le texte à formater
    * @param lg  la longueur à obtenir
    * @return texte formaté
    */
   public static String frmI(String txt, int lg) {
      return frmI(txt, lg, ' ');
   }// frmI

   /**
    * Formatage des données en sens inverse
    * 
    * @param txt le texte à formater
    * @param lg  la longueur à obtenir
    * @param c   le caractère de remplissage
    * @return texte formaté
    */
   public static String frmI(String txt, int lg, char c) {
      StringBuilder ret = new StringBuilder();
      if (txt != null) {
         for (int i = txt.length(); i < lg; i++) ret.append(c);
         ret.append(txt);
      }
      return ret.toString();
   }// frmI

   /**
    * Formatage des données en sens inverse (pour les nombres, avec espace + 1 espace à la fin)
    * @param txt le texte à formater
    * @param lg  la longueur à obtenir
    * @return texte formaté
    */
   private static String frmI2(String txt, int lg) {
      StringBuilder ret = new StringBuilder();
      if (txt != null) {
         for (int i = txt.length(); i <= lg; i++) ret.append(' ');
         ret.append(txt);
      }
      ret.append(' ');
      return ret.toString();
   }// frm

   /**
    * Formatage de tableau pour requête SELECT : centrage des entêtes de colonnes
    *
    * @param txt le texte à formater
    * @param lg  la longueur à obtenir
    * @return texte formaté
    */
   private static String frmT(String txt, int lg) {
      StringBuilder ret = new StringBuilder();
      ret.append(' ');
      int deb = (lg - txt.length()) / 2, fin = lg + 1 - txt.length() - deb;
      for (int i = 0; i < deb; i++) ret.append(' ');
      ret.append(txt);
      for (int i = 0; i < fin; i++) ret.append(' ');
      return ret.toString();
   }// frm
   
   /**
    * Formatage d'une durée en quelques chose de lisible
    * 
    * @param millis la durée en millisecondes
    * @return la chaine formatée du type '1 h 25 min 43 s 590 ms'
    */
   public static String frmDur(long millis){
      /* TODO return String.format("%d min, %d sec", 
            TimeUnit.MILLISECONDS.toMinutes(millis),
            TimeUnit.MILLISECONDS.toSeconds(millis) - 
            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));*/
      int sec = (int) ((millis / 1000) % 60);
      int min = (int) ((millis / 60000) % 60);
      int hrs = (int) (millis / 3600000); // pas de limite d'heures
      int mls = (int) (millis - (sec * 1000 + min * 60000 + hrs * 3600000));
      return (hrs > 0 ? hrs + " h " : "")
           + (hrs > 0 || min > 0 ? min + " min " : "")
           + (hrs > 0 || min > 0 || sec > 0 ? sec + " s " : "")
           + mls + " ms";
   }

   /**
    * Classe de valeur de retour du résultat
    */
   public class QueryResult {
      public String result;
      public String premVal;
      public int nbLng;
      public boolean istrunc;

      public QueryResult(String result, String premVal, int nbLng, boolean istrunc) {
         this.result = result;
         this.premVal = premVal;
         this.nbLng = nbLng;
         this.istrunc = istrunc;
      }
   }
}// class

