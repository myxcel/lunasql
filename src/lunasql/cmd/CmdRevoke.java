package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande REVOKE <br>
 * (SQL) Suppression de droits à un utilisateur de la base
 * @author M.P.
 */
public class CmdRevoke extends Instruction {

   public CmdRevoke(Contexte cont) {
      super(null, TYPE_CMDSQL, "REVOKE", null);
   }

   @Override
   public int execute() {
      return executeUpdate("REVOKE", "droits retirés");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  revoke  (+attr)    Retire des droits à un utilisateur\n";
   }
}// class

