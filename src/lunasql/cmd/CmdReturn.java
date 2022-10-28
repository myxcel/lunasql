package lunasql.cmd;

import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;

/**
 * Commande RETURN <br>
 * (Interne) Interruption de l'exécution d'un fichier de commandes et retour d'un résultat
 * @author M.P.
 */
public class CmdReturn extends Instruction {

   public CmdReturn(Contexte cont) {
      super(cont, TYPE_CMDINT, "RETURN", "^");
   }

   @Override
   public int execute() {
      String ret = (getLength() == 1 ? null : getSCommand(1));
      cont.setValeur(new ValeurDef(cont, ret, Contexte.VERB_BVR, ret));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return RET_RETR_SCR;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  return, ^   Sort d'un bloc de code en renvoyant une valeur\n";
   }
}// class

