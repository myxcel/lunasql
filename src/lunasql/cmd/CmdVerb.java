package lunasql.cmd;

import lunasql.lib.Contexte;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande VERB <br>
 * (Interne) Positionnement de l'état verbeux de la console
 * @author M.P.
 */
public class CmdVerb extends Instruction {

   public CmdVerb(Contexte cont) {
      super(cont, TYPE_CMDINT, "VERB", ",");
   }

   @Override
   public int execute() {
      Valeur vr = new ValeurDef(cont);

      // Affichage de la valeur de verbose
      switch (getLength()) {
      case 1:
         int nv = cont.getVerbose();
         vr.setDispValue("VERB = " + nv + " (" + Contexte.VERBLIB[nv] + ")", Contexte.VERB_AFF);
         vr.setSubValue(Integer.toString(nv));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         break;

      case 2:
         // Modification de la valeur de verbose
         String sval;
         if ((sval = cont.setVerbose(getArg(1))) != null) {
            vr.setDispValue("Nouveau VERB = " + sval, Contexte.VERB_BVR);
            vr.setSubValue(sval);
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         } else {
            return cont.erreur("VERB", "valeur ou libellé de verbose invalide : " + getArg(1), lng);
         }
         break;

      default:
         return cont.erreur("VERB", "une valeur ou un libellé de verbose est attendu(e)", lng);
      }
      vr.setRet();
      cont.setValeur(vr);
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  verb, ,     Attribue au système une valeur de verbose\n";
   }
}// class
