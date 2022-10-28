package lunasql.lib;

import lunasql.val.Valeur;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Classe d'encapsulation du contexte
 */
public class Coquille {

   private final Contexte cont;

   /**
    * Constructeur
    * @param cont le contexte
    */
   public Coquille(Contexte cont) {
      this.cont = cont;
   }

   /**
    * Affichage
    * @param s la chaîne
    */
   public void println(String s) {
      cont.println(s);
   }

   /**
    * Affichage d'erreur
    * @param s la chaîne
    */
   public void errprintln(String s) {
      cont.errprintln(s);
   }

   /**
    * Évaluation d'une chaîne de code LunaSQL
    * @param s la châine à évaluer
    */
   public String eval(String s) {
      new Lecteur(cont, s);
      Valeur v = cont.getValeur();
      return v == null ? null : v.getSubValue();
   }

   /**
    * Obtient la valeur de la variable globale ou de l'option
    * @param key le nom de la variable
    * @return sa valeur
    */
   public String getVarOpt(String key) {
      return cont.getGlbVar(key);
   }

   /**
    * Fixe la valeur de la variable globale
    * @param key le nom de la variable
    * @param val sa valeur
    * @param docirc si la var. est exempte de contrôle de ref. circ.
    * @throws IllegalArgumentException si le nom n'est pas valide
    */
   public void setVar(String key, String val, boolean docirc) {
      if (!cont.valideKey(key)) throw new IllegalArgumentException("nom de variable non valide : '" + key + "'");
      cont.setVar2(key, val, docirc);
   }

   /**
    * Fixe la valeur de l'option
    * @param key le nom de l'option
    * @param val sa valeur
    * @throws IllegalArgumentException si le nom n'est pas valide
    */
   public void setOpt(String key, String val) {
      if (!cont.isSysUser(key)) throw new IllegalArgumentException("Nom d'option non valide : " + key);
      if (!cont.getVar(Contexte.ENV_CONST_EDIT).equals("2")) throw new IllegalArgumentException("édition d'option non autorisée");
      cont.setVar2(key, val);
   }

   /**
    * Teste si la variable existe dans l'environnement global (option ou variable)
    * @param key le nom de la variable
    * @return true si existe
    */
   public boolean isVarOptSet(String key) {
      return cont.isSet(key);
   }

   /**
    * Obtient la l'aide de la variable globale
    * @param key le nom de la variable
    * @return son aide
    */
   public String getVarHelp(String key) {
      return cont.getVarHelp(key);
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
   public ResultSet getResultset(String sqlcmd) throws SQLException {
      return cont.getConnex().getResultset(sqlcmd);
   }

   /**
    * getResultMap : résultat de la commande sous forme de string
    * @param sqlcmd la commande SQL
    * @return un ArrayList d'ArrayList
    * @throws SQLException si erreur SQL
    */
   public synchronized ArrayList<ArrayList<Object>> getResultArray(String sqlcmd)
         throws SQLException {
      return cont.getConnex().getResultArray(sqlcmd);
   }

   /**
    * Recherche d'une valeur retournée par commande SELECT
    * Si le résultat de la requête fait plusieurs colonnes, une propriété (dict) est retournée
    * Si en plus le résulat fait plusieurs lignes, c'est un tableau (list) de propriétés (dict)
    *
    * @param sqlcmd la commande SQL
    * @param enc l'encodage pour la sortie Properties
    * @param nbmax le nombre de lignes max. à retourner
    * @return valeur unique
    * @throws SQLException si erreur SQL
    */
   public String seek(String sqlcmd, String enc, int nbmax) throws SQLException {
      return cont.getConnex().seek(sqlcmd, enc, nbmax);
   }

   /**
    * Recherche d'un retour de nombre de ligne
    * @param sqlcmd table + where éventuel
    * @return valeur int unique
    * @throws SQLException si erreur SQL
    */
   public int seekNbl(String sqlcmd) throws SQLException {
      return cont.getConnex().seekNbl(sqlcmd);
   }

   /**
    * Execution de la requête de mise à jour INSERT, UPDATE, DELETE...
    * @param sqlcmd la requête SQL
    * @return le nombre de lignes modifiées ou exception
    * @throws SQLException si erreur SQL
    */
   public int execute(String sqlcmd) throws SQLException {
      if (!cont.getVar(Contexte.ENV_SQL_UPDATE).equals(Contexte.STATE_TRUE))
         throw new IllegalArgumentException("exécution d'une commande SQL de modification non autorisée");
      return cont.getConnex().execute(sqlcmd);
   }
}
