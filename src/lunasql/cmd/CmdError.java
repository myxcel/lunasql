package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande ERROR <br>
 * (SQL) Levée d'une erreur
 * @author M.P.
 */
public class CmdError extends Instruction {

   public CmdError(Contexte cont){
      super(cont, TYPE_CMDINT, "ERROR", null);
   }

   @Override
   public int execute() {
      return cont.erreur("ERROR", getSCommand(1), lng);
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  error       Lève une erreur d'exécution avec message\n";
   }
}// class

