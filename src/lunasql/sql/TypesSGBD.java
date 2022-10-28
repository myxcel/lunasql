package lunasql.sql;

import lunasql.Config;

/**
 * Types de SGBD<br>
 * TODO : réorganiser cette classe pour éviter les redondances (utiliser tableau)
 * @author M.P.
 */
public class TypesSGBD {

   /** Nom du type de SGBD : ODBC */
   public static final String S_TYPE_ODBC = "ODBC";
   /** Nom du type de SGBD : MS Access *.mdb */
   public static final String S_TYPE_ACCESS = "ACCESS";
   /** Nom du type de SGBD : MS Access 2010 *.accdb */
   public static final String S_TYPE_ACCESSX = "ACCESSX";
   /** Nom du type de SGBD : UCanAccess http://ucanaccess.sourceforge.net */
   public static final String S_TYPE_UCACCESS = "UCACCESS";
   /** Nom du type de SGBD : Hypersonic SQL http://hsqldb.org/ */
   public static final String S_TYPE_HSQLDB = "HSQLDB";
   /** Nom du type de SGBD : H2 DB http://www.h2database.com/html/main.html */
   public static final String S_TYPE_H2DB = "H2DB";
   /** Nom du type de SGBD : MySQL */
   public static final String S_TYPE_MYSQL = "MYSQL";
   /** Nom du type de SGBD : Oracle */
   public static final String S_TYPE_ORACLE = "ORACLE";
   /** Nom du type de SGBD : Derby http://db.apache.org/derby/ */
   public static final String S_TYPE_DERBY = "DERBY";
   /** Nom du type de SGBD : MS SQL Server */
   public static final String S_TYPE_SQLSERV = "SQLSERVER";

   /** Type SGBD : ODBC */
   public static final int TYPE_ODBC = 0;
   /** Type SGBD : MS Access */
   public static final int TYPE_ACCESS = TYPE_ODBC + 1;
   /** Type SGBD : MS Access 2010 */
   public static final int TYPE_ACCESSX = TYPE_ACCESS + 1;
   /** Nom du type de SGBD : UCanAccess */
   public static final int TYPE_UCACCESS = TYPE_ACCESSX + 1;
   /** Type SGBD : Hypersonic SQL http://hsqldb.org/ */
   public static final int TYPE_HSQLDB = TYPE_UCACCESS + 1;
   /** Type SGBD : H2 DB http://www.h2database.com/html/main.html */
   public static final int TYPE_H2DB = TYPE_HSQLDB + 1;
   /** Type SGBD : MySQL */
   public static final int TYPE_MYSQL = TYPE_H2DB + 1;
   /** Type SGBD : Oracle */
   public static final int TYPE_ORACLE = TYPE_MYSQL + 1;
   /** Type SGBD : Derby http://db.apache.org/derby/ */
   public static final int TYPE_DERBY = TYPE_ORACLE + 1;
   /** Type SGBD : MS SQL Server */
   public static final int TYPE_SQLSERV = TYPE_DERBY + 1;
   
   /** Limite maximale de bornes des types (max = TYPE_SQLSERV) */
   public static final int TYPE_MAX = TYPE_SQLSERV;
   
   /** Tableau des drivers */
   public static final String[] DRIVERS = {
      "sun.jdbc.odbc.JdbcOdbcDriver", // ODBC
      "sun.jdbc.odbc.JdbcOdbcDriver", // ACCESS
      "sun.jdbc.odbc.JdbcOdbcDriver", // ACCESSX
      "net.ucanaccess.jdbc.UcanaccessDriver", // UCACCESS
      "org.hsqldb.jdbcDriver", // HSQLDB
      "org.h2.Driver", // H2DB
      "com.mysql.jdbc.Driver", // MYSQL
      "oracle.jdbc.driver.OracleDriver", // ORACLE
      "org.apache.derby.jdbc.ClientDriver", // DERBY
      "com.microsoft.sqlserver.jdbc.SQLServerDriver" // SQLSERVER
   };
   public static final String[] CHAINES = {
      "jdbc:odbc:<dtb>", // ODBC
      "jdbc:odbc:DRIVER=Microsoft Access Driver (*.mdb); DBQ=<dtb>", // ACCESS
      "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb, *.accdb)}; DBQ=<dtb>", // ACCESSX
      "jdbc:ucanaccess://<dtb>", // UCACCESS
      "jdbc:hsqldb:file:<dtb><opt>", // HSQLDB
      "jdbc:h2:<dtb><opt>", // H2DB
      "jdbc:mysql://<host><port>/<dtb>", // MYSQL
      "jdbc:oracle:thin:@<host><port>:<dtb>", // ORACLE
      "jdbc:derby:<host><port>:<dtb>", // DERBY
      "jdbc:sqlserver://<host><port>;database=<dtb>" // SQLSERVER
      // ou "jdbc:sqlserver://<host><port>;database=<dtb>;user=<user>;password=<pswd>"
   };
   // Variables d'instance
   private final int type;
   private final String driver;
   private String chaine;
   
   /**
    * Constructeur TypeSGBD
    * @param type le type de SGBD : TYPE_ODBC, TYPE_ACCESS, TYPE_ACCESSX, TYPE_UCACCESS,
    *    TYPE_HSQLDB, TYPE_H2DB, TYPE_MYSQL, TYPE_ORACLE, TYPE_DERBY, TYPE_SQLSERV
    * @param params le tableau des paramètres éventuels à passer dans le cas des bases 
    *    à serveur distant (cas de TYPE_ORACLE, TYPE_DERBY, TYPE_SQLSERV)<br>
    *    params[0] : host name
    *    params[1] : port (facultatif: "")
    *    params[2] : database name
    *    params[3] : options
    */
   public TypesSGBD(int type, String[] params) {
      if (!existsType(type)) throw new IllegalArgumentException("Type invalide : " + type);
      this.type = type;
      this.driver = DRIVERS[type];
      this.chaine = CHAINES[type];
      
      // Retouches pour les chaines à compléter
      if (params == null || params.length != 4)
         throw new IllegalArgumentException("Paramètres de connexion invalides");
      if(params[0] != null) this.chaine = this.chaine.replace("<host>", params[0]);
      if(params[1] != null) {
         if (params[1].length() > 0) params[1] = ":" + params[1];
         this.chaine = this.chaine.replace("<port>", params[1]);
      }
      if(params[2] != null) this.chaine = this.chaine.replace("<dtb>", params[2]);
      if(params[3] != null) this.chaine = this.chaine.replace("<opt>", params[3]);
   }

   /**
    * Chiffrement de la base de données (si compatible)
    * @param key la clef symétrique de chiffrement
    * @param algo l'algorithme de chiffrement
    * @return une chaîne complétant la chaîne de connexion
    * TODO: étendre la comptatibilité de chiffrement à d'autres SGBD
    */
   public String getEncryption(String key, String algo) {
      if (this.type == TYPE_HSQLDB) return ";crypt_key=" + key + ";crypt_type=" + algo;
      if (this.type == TYPE_H2DB) return ";CIPHER=" + algo;
      // autres SGBD
      else throw new IllegalArgumentException("Chiffrement non supporté par le SGBD " 
                 + getSTypeFromArg(this.type));
   }

   /**
    * Chiffrement de la base de données (si compatible) avec algo=AES128
    * @param key la clef symétrique de chiffrement
    * @return une chaîne complétant la chaîne de connexion
    */
   public String getEncryption(String key) {
      return getEncryption(key, "AES");
   }

   /**
    * Retourne le type en fonction du type saisi en console
    * @param arg le type : ODBC, ACCESS, ACCESSX, UCACCESS, HSQLDB, H2DB, ORACLE, JAVADB, SQLSERVER
    * @return int le type de SGBD : TYPE_ODBC, TYPE_ACCESS, TYPE_ACCESSX, TYPE_UCACCESS,
    *    TYPE_HSQLDB, TYPE_H2DB, TYPE_MYSQL, TYPE_ORACLE, TYPE_DERBY, TYPE_SQLSERV
    */
   public static int getTypeFromArg(String arg) {
      if (arg == null || arg.length() == 0)     return Config.DB_DEFAUT_TYPE;
      if (arg.equalsIgnoreCase(S_TYPE_ODBC))    return TYPE_ODBC;
      if (arg.equalsIgnoreCase(S_TYPE_ACCESS))  return TYPE_ACCESS;
      if (arg.equalsIgnoreCase(S_TYPE_ACCESSX)) return TYPE_ACCESSX;
      if (arg.equalsIgnoreCase(S_TYPE_UCACCESS)) return TYPE_UCACCESS;
      if (arg.equalsIgnoreCase(S_TYPE_HSQLDB))  return TYPE_HSQLDB;
      if (arg.equalsIgnoreCase(S_TYPE_H2DB))    return TYPE_H2DB;
      if (arg.equalsIgnoreCase(S_TYPE_MYSQL))   return TYPE_MYSQL;
      if (arg.equalsIgnoreCase(S_TYPE_ORACLE))  return TYPE_ORACLE;
      if (arg.equalsIgnoreCase(S_TYPE_DERBY))   return TYPE_DERBY;
      if (arg.equalsIgnoreCase(S_TYPE_SQLSERV)) return TYPE_SQLSERV;
      return -1; // type invalide
   }

   /**
    * Retourne le type en fonction du type numérique
    * @param  arg le type : 0, 1, 2 ou 3
    * @return String le type de SGBD : ODBC, ACCESS, ACCESSX, UCACCESS, HSQLDB, H2DB, ORACLE,
    *    JAVADB, SQLSERVER
    */
   public static String getSTypeFromArg(int arg) {
      switch (arg) {
         case TYPE_ODBC:    return S_TYPE_ODBC;
         case TYPE_ACCESS:  return S_TYPE_ACCESS;
         case TYPE_ACCESSX: return S_TYPE_ACCESSX;
         case TYPE_UCACCESS: return S_TYPE_UCACCESS;
         case TYPE_HSQLDB:  return S_TYPE_HSQLDB;
         case TYPE_H2DB:    return S_TYPE_H2DB;
         case TYPE_MYSQL:   return S_TYPE_MYSQL;
         case TYPE_ORACLE:  return S_TYPE_ORACLE;
         case TYPE_DERBY:   return S_TYPE_DERBY;
         case TYPE_SQLSERV: return S_TYPE_SQLSERV;
         default:           return null;
      }
   }

   /**
    * Teste si un type donné existe
    * @param type le num du type
    * @return true si ok, false sinon
    */
   public static boolean existsType(int type) {
      return type >= 0 && type <= TYPE_MAX;
   }

   public int getType() {
      return this.type;
   }

   public String getDriver() {
      return this.driver;
   }

   public String getChaine() {
      return this.chaine;
   }
}
