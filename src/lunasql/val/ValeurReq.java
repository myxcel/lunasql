package lunasql.val;

import lunasql.lib.Contexte;
import lunasql.sql.SQLCnx;

/**
 * Valeur de retour des commandes à ResultSet
 * 
 * @author M.P.
 */
public class ValeurReq extends Valeur {

   private final String result;
   private final String premVal;
   private final int nbLng;
   private final boolean istrunc;

   /**
    * Constructor ValeurReq
    *
    * @param cont le contexte
    * @param result le résultat complet
    * @param premVal la première valeur
    * @param nbLng le nb de lignes retourné
    * @param istrunc si nb de ligne tronqué
    */
   public ValeurReq(Contexte cont, String result, String premVal, int nbLng, boolean istrunc) {
      this.result = result;
      this.premVal = premVal;
      this.nbLng = nbLng;
      this.istrunc = istrunc;
      setDispValue(result, Contexte.VERB_AFF);
      setSubValue(premVal);
      cont.setVar(Contexte.ENV_RET_VALUE, getSubValue());
      cont.setVar(Contexte.ENV_RET_NLINES, getNbLinesStr());
   }

   public ValeurReq(Contexte cont, SQLCnx.QueryResult qr) {
      this(cont, qr.result, qr.premVal, qr.nbLng, qr.istrunc);
   }


   /**
    * Retourne le tableau entier de résultats
    * @return valeur String
    */
   public String getResult() {return result; }

   /**
    * Retourne la première valeur du tableau
    * @return valeur String
    */
   public String getPremVal() {return premVal; }

   /**
    * Retourne la valeur de la commande à substituer (format minimal)
    * @return valeur String
    */
   public String getSubValue() {return getPremVal(); }

   /**
    * Retourne le nombre de lignes affectées par la requête
    * @return valeur String
    */
   public String getNbLinesStr() { return Integer.toString(nbLng); }

   /**
    * Retourne le nombre de lignes affectées par la requête
    * @return valeur int
    */
   public int getNbLines() { return nbLng; }

   /**
    * Retourne si le nombre de lignes est troqué
    * @return true si c'est la cas
    */
   public boolean isNblTrunc() { return istrunc; }
}
