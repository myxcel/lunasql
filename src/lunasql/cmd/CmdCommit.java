package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande COMMIT <br>
 * (SQL) Valide les transactions danns le SGBD
 * @author M.P.
 */
public class CmdCommit extends Instruction {

   public CmdCommit(Contexte cont){
      super(cont, TYPE_CMDSQL, "COMMIT", null);
   }

   @Override
   public int execute() {
      return executeUpdate("COMMIT", "transactions validées");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  commit             Valide les transactions effectuées\n";
   }
}// class

