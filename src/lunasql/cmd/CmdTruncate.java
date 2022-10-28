package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande TRUNCATE <br>
 * (SQL) Suppression du contenu d'une table
 * @author M.P.
 */
public class CmdTruncate extends Instruction {

   public CmdTruncate(Contexte cont){
      super(cont, TYPE_CMDSQL, "TRUNCATE", null);
   }

   @Override
   public int execute() {
      return executeUpdate("TRUNCATE", "table purgée");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  truncate (+instr)  Supprime un objet de la base\n";
   }
}// class

