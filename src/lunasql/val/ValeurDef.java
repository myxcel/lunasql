package lunasql.val;

import lunasql.lib.Contexte;

/**
 * Valeur de retour des commandes normales (retour par défaut)
 * 
 * FIXME: beaucoup de commandes utilisent un StringBuilder et ajoutent :
 * if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
 * On supprime le caractère \n pour l'ajouter plus tard? Optimiser en proposant
 * en ValeurDef 3 methodes: addDispValue(String) + print()/println() (appelées par Contexte#executeCmd())
 * 
 * @author M.P.
 */
public class ValeurDef extends Valeur {

   private String sub;
   private final Contexte cont;
   private int color;
   //Object obj; // TODO: gérer des objets et non des chaînes

   /**
    * Constructor ValeurDef par défaut
    * Utiliser setRet() à la fin des appels à setDispValue
    * @param cont le contexte
    */
   public ValeurDef(Contexte cont) {
      this.cont = cont;
      this.sub = null;
      this.color = -1;
      //this.obj = null;
   }

   /**
    * Constructor ValeurDef avec spécification du message VERB_MSG
    * @param cont le contexte
    * @param aff le texte à afficher
    * @param sub la valeur de retour
    */
   public ValeurDef(Contexte cont, String aff, String sub) {
      this(cont, aff, Contexte.VERB_MSG, -1, sub, true);
   }

   /**
    * Constructor ValeurDef avec spécification du message VERB_MSG
    * @param cont le contexte
    * @param aff le texte à afficher
    * @param sub la valeur de retour
    * @param setnbl si on doit mettre à 0 la constante _RET_NLINES
    */
   public ValeurDef(Contexte cont, String aff, String sub, boolean setnbl) {
      this(cont, aff, Contexte.VERB_MSG, -1, sub, setnbl);
   }

   /**
    * Constructor ValeurDef avec spécification du message VERB_AFF
    * @param cont le contexte
    * @param aff le texte à afficher
    * @param verb le niveau de verbose
    * @param sub la valeur de retour
    */
   public ValeurDef(Contexte cont, String aff, int verb, String sub) {
      this(cont, aff, verb, -1, sub, true);
   }

   /**
    * Constructor ValeurDef avec spécification du message VERB_AFF
    * @param cont le contexte
    * @param aff le texte à afficher
    * @param verb le niveau de verbose
    * @param color la couleur d'affichage
    * @param sub la valeur de retour
    */
   public ValeurDef(Contexte cont, String aff, int verb, int color, String sub) {
      this(cont, aff, verb, color, sub, true);
   }

   /**
    * Constructor ValeurDef avec spécification du message VERB_AFF
    * @param cont le contexte
    * @param aff le texte à afficher
    * @param verb le niveau de verbose
    * @param color la couleur d'affichage
    * @param sub la valeur de retour
    * @param setnbl si on doit mettre à 0 la constante _RET_NLINES
    */
   public ValeurDef(Contexte cont, String aff, int verb, int color, String sub, boolean setnbl) {
      this.cont = cont;
      setDispValue(aff, verb);
      this.sub = sub;
      this.color = color;
      if (setnbl) setRet(); else setRetV();
   }

   /**
    * Retourne la valeur de la commande à substituer (format minimal)
    * @return valeur String
    */
   @Override
   public String getSubValue() {
      return sub;
   }

   /**
    * Fixe la valeur de la commande à substituer (format minimal)
    * @param sub la valeur de retour
    */
   @Override
   public void setSubValue(String sub) {
      this.sub = sub;
   }

   /**
    * Fixe la valeur de la commande à substituer (format minimal)
    * @param color la couleur d'affichage
    */
   @Override
   public void setColor(int color) {
      this.color = color;
   }

   /**
    * Fixe la couleur du texte à afficher
    * @return color int
    */
   @Override
   public int getColor() {
      return color;
   }

   /**
    * À utiliser en interne à ValeurDef
    * Modifie seulement _RET_VALUE
    */
   private void setRetV() {
      cont.setVar(Contexte.ENV_RET_VALUE, sub == null ? "" : sub);
   }

   /**
    * À utiliser avec le constructeur ValeurDef(Contexte cont)
    * pour finaliser les constantes de retour
    */
   @Override
   public void setRet() {
      cont.setVar(Contexte.ENV_RET_VALUE, sub == null ? "" : sub);
      cont.setVar(Contexte.ENV_RET_NLINES, "0");
   }

   /**
    * À utiliser avec le constructeur ValeurDef(Contexte cont)
    * pour finaliser les constantes de retour. Utilisée par SIZE.
    * @param nbl le nombre de lignes string affectées
    */
   @Override
   public void setRet(String nbl) {
      cont.setVar(Contexte.ENV_RET_VALUE, sub == null ? "" : sub);
      cont.setVar(Contexte.ENV_RET_NLINES, nbl);
   }
}
