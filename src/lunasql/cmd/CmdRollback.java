package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande ROLLBACK <br>
 * (SQL) Annule les transactions danns le SGBD
 * @author M.P.
 */
public class CmdRollback extends Instruction {

   public CmdRollback(Contexte cont) {
      super(cont, TYPE_CMDSQL, "ROLLBACK", null);
   }

   @Override
   public int execute() {
      return executeUpdate("ROLLBACK", "transactions annulées");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  rollback           Annule les transactions effectuées\n";
   }
}// class

