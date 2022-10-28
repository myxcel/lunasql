package lunasql.val;

import lunasql.lib.Contexte;

/**
 * Valeur de retour des commandes à exécution SQL
 * 
 * @author M.P.
 */
public class ValeurExe extends Valeur {

   private final int nblines; // nombre de lignes affectées

   /**
    * Constructor ValeurExe
    */
   public ValeurExe(Contexte cont, int nbl) {
      this.nblines = nbl;
      cont.setVar(Contexte.ENV_RET_VALUE, getSubValue());
      cont.setVar(Contexte.ENV_RET_NLINES, getNbLinesStr());
   }

   /**
    * Retourne la valeur de la commande à substituer (format minimal)
    * @return valeur String
    */
   public String getSubValue() {
      return Integer.toString(nblines);
   }

   /**
    * Retourne le nombre de lignes affectées par la requête
    * @return valeur int
    */
   public int getNbLines() {
      return nblines;
   }

   /**
    * Retourne le nombre de lignes affectées par la requête
    * @return valeur int
    */
   public String getNbLinesStr() {
      return Integer.toString(nblines);
   }
}
