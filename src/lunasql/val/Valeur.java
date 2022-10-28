package lunasql.val;

import java.util.Arrays;

import static lunasql.lib.Contexte.VERBLIB;
import static lunasql.lib.Contexte.VERB_AFF;
import static lunasql.lib.Contexte.VERB_BVR;
import static lunasql.lib.Contexte.VERB_MSG;
import static lunasql.lib.Contexte.VERB_SIL;

/**
 * Interface pour les valeurs de retour des commandes
 * 
 * @author M.P.
 */
public abstract class Valeur {

   // Commentaire selon 3 niveaux de verbose : VERB_AFF, VERB_MSG, VERB_BVR
   private final String[] tbcomm;

   /**
    * Constructor implicite ValeurExe
    */
   public Valeur() {
      tbcomm = new String[VERBLIB.length - 2];
      Arrays.fill(tbcomm, "");
   }

   /**
    * Retourne la valeur de la commande à substituer (format minimal)
    * Doit être redéfinie
    * @return valeur String
    */
   public abstract String getSubValue();

   /**
    * Fixe la valeur de la commande à substituer (format minimal)
    * Par défaut, ne faiixe rien
    * @param sub la valeur entrant en substitution
    */
   public void setSubValue(String sub) {}

   /**
    * Ajout du commentaire en mode verbose VERB_MSG
    * @param comm le commentaire à ajouter
    */
   public void setDispValue(String comm) {
      setDispValue(comm, VERB_MSG);
   }

   /**
    * Ajout du commentaire selon le niveau de Verbose. Si verb == VERB_SIL, les messages de tous
    * les niveaux sont supprimés.
    * @param comm le commentaire à ajouter
    * @param verb le niveau de verbose
    */
   public void setDispValue(String comm, int verb) {
      if (verb == VERB_SIL) Arrays.fill(tbcomm, null);
      else if (verb < VERB_AFF || verb > VERB_BVR)
         throw new IllegalArgumentException("Valeur.setDispValue : niveau de verb invalide : " + verb);
      else tbcomm[verb - 1] = comm;
   }

   /**
    * Ajout du commentaire sans nouvelle ligne
    * @param comm le commentaire à ajouter
    * @param verb le niveau de verbose
    */
   public void appendDispValue(String comm, int verb) {
      appendDispValue(comm, verb, false);
   }

   /**
    * Ajout du commentaire
    * @param comm le commentaire à ajouter
    * @param verb le niveau de verbose
    * @param nl si l'ajout se fait en nouvelle ligne
    */
   public void appendDispValue(String comm, int verb, boolean nl) {
      if (verb < VERB_AFF || verb > VERB_BVR)
         throw new IllegalArgumentException("Valeur.appendDispValue : niveau de verb invalide : " + verb);
      tbcomm[verb - 1] += (nl && tbcomm[verb - 1].length() > 0 ? "\n" : "") + comm;
   }

   /**
    * Retourne le commentaire de fin d'exécution de la commande
    * @return valeur String
    */
   public String getDispValue() {
      return getDispValue(VERB_MSG);
   }

   /**
    * Retourne le commentaire de fin d'exécution de la commande
    * @param verb le niveau de verbose
    * @return valeur String
    */
   public String getDispValue(int verb) {
      if (verb < VERB_AFF || verb > VERB_BVR)
         throw new IllegalArgumentException("Valeur.getDispValue : verb invalide");
      return tbcomm[verb - 1];
   }

   /**
    * Fixe la valeur de la commande à substituer (format minimal)
    * @param color la couleur d'affichage
    */
   public void setColor(int color) {}

   /**
    * Fixe la couleur du texte à afficher
    * @return color int
    */
   public int getColor() {
      return -1;
   }

   /**
    * Finalisation des constantes de retour.
    * Par défaut ne fait rien...
    */
   public void setRet() {}
   public void setRet(String nbl) {}
}
