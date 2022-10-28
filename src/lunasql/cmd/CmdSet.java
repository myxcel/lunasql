package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande SET <br>
 * (SQL) Affecte un paramètre interne de la base SQL
 * @author M.P.
 */
public class CmdSet extends Instruction {

   public CmdSet(Contexte cont) {
      super(cont, TYPE_CMDSQL, "SET", null);
   }

   @Override
   public int execute() {
      return executeUpdate("SET", "paramètre fixé");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  set     (+param)   Fixe un paramètre de la base du SGBD\n";
   }
}// class

