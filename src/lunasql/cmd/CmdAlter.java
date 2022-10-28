package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande ALTER <br>
 * (SQL) Modification de la structure d'un élément de base de données
 * @author M.P.
 */
public class CmdAlter extends Instruction {

   public CmdAlter(Contexte cont){
      super(cont, TYPE_CMDSQL, "ALTER", null);
   }

   @Override
   public int execute() {
      return executeUpdate("ALTER", "objet modifié");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  alter   (+instr)   Modifie la structure d'un objet de la base\n";
   }
}// class

