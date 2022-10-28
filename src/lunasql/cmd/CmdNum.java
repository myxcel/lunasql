package lunasql.cmd;

import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;

/**
 * Commande NUM <br>
 * (Interne) Commande à ne pas utiliser. Elle est appelée en cas d'alias expression (sortie de EVAL)
 * @author M.P.
 */
public class CmdNum extends Instruction {

   public CmdNum(Contexte cont) {
      super(cont, TYPE_INVIS, "NUM", null);
   }

   @Override
   public int execute() {
      cont.setValeur(new ValeurDef(cont, "", Contexte.VERB_BVR, Contexte.BR_BLUE,
            getCommandName()));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return null;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getHelp(){
      return null;
   }
}// class

