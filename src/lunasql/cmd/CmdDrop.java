package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande DROP <br>
 * (SQL) Suppression d'un objet de base de données (table, index, vue...)
 * @author M.P.
 */
public class CmdDrop extends Instruction {

   public CmdDrop(Contexte cont){
      super(cont, TYPE_CMDSQL, "DROP", null);
   }

   @Override
   public int execute() {
      return executeUpdate("DROP", "objet supprimé");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  drop    (+instr)   Supprime un objet de la base\n";
   }
}// class

