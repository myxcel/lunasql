package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande CALL <br>
 * (SQL) Appelle une procédure ou toute expression SQL
 * @author M.P.
 */
public class CmdCall extends Instruction {

   public CmdCall(Contexte cont){
      super(cont, TYPE_CMDSQL, "CALL", null);
   }

   @Override
   public int execute() {
      return executeCall("CALL", "évaluation terminée");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc() {
      return "  call    (+proc)    Appelle une procédure ou toute expression SQL\n";
   }
}// class

