package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande VOID <br>
 * (Interne) Commande qui ne fait absolument rien
 * @author M.P.
 */
public class CmdVoid extends Instruction {

   public CmdVoid(Contexte cont){
      super(cont, TYPE_CMDINT, "VOID", ".");
   }

   @Override
   public int execute() {
      cont.setValeur(null);
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  void, .     Ne fait absolument rien\n";
   }
}// class
