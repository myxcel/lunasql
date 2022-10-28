package lunasql.cmd;

import lunasql.lib.Contexte;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

import java.io.IOException;
import java.util.List;

/**
 * Commande PUT <br>
 * (Interne) Affecte à la valeur de retour _RET_VALUE les arguments de la commande
 * @author M.P.
 */
public class CmdPut extends Instruction {

   public CmdPut(Contexte cont){
      super(cont, TYPE_CMDINT, "PUT", null);
   }

   @Override
   public int execute() {
      String ret = (getLength() == 1 ? null : getSCommand(1));
      cont.setValeur(new ValeurDef(cont, null, ret));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  put         Affecte simplement la valeur de retour\n";
   }
}// class
