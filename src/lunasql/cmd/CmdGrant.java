package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande GRANT <br>
 * (SQL) Attribution de droits à un utilisateur de la base
 * @author M.P.
 */
public class CmdGrant extends Instruction {

   public CmdGrant(Contexte cont) {
      super(cont, TYPE_CMDSQL, "GRANT", null);
   }

   @Override
   public int execute() {
      return executeUpdate("GRANT", "droits attribués");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  grant   (+attr)    Attribue des droits à un utilisateur\n";
   }
}// class

