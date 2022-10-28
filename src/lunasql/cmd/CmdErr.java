package lunasql.cmd;

import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;

/**
 * Commande en erreur <br>
 * (Interne) Commande en erreur
 * @author M.P.
 */
public class CmdErr extends Instruction {

   protected String msg;

   public CmdErr(Contexte cont){
      super(cont, TYPE_ERR, "ERR", null);
   }
   public CmdErr(Contexte cont, String name, String alias){
      super(cont, TYPE_ERR, name, alias);
   }

   public void setMessage(String msg){
      this.msg = msg;
   }

   @Override
   public int execute() {
      return cont.getVar(Contexte.ENV_EXIT_ERR).equals(Contexte.STATE_TRUE) ? RET_EXIT_SCR : RET_CONTINUE;
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
}
