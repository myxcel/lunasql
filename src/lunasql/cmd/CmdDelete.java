package lunasql.cmd;

import java.sql.SQLException;

import lunasql.lib.Contexte;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurExe;

/**
 * Commande DELETE <br>
 * (SQL) Suppression d'enregistrements dans une table de la base
 * @author M.P.
 */
public class CmdDelete extends Instruction {

   public CmdDelete(Contexte cont){
      super(cont, TYPE_CMDSQL, "DELETE", null);
   }

   @Override
   public int execute() {
      return executeUpdate("DELETE", " supprimée", true);
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  delete  (+instr)   Supprime des données de la base\n";
   }
}// class

