package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande MERGE <br>
 * (SQL) Fusion des données de deux sources
 * @author M.P.
 */
public class CmdMerge extends Instruction {

   public CmdMerge(Contexte cont){
      super(cont, TYPE_CMDSQL, "MERGE", null);
   }

   @Override
   public int execute() {
      return executeUpdate("MERGE", "fusion réalisée");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  merge (+instr)     Fusionne les données de deux tables\n";
   }
}// class

