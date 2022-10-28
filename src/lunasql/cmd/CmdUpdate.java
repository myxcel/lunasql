package lunasql.cmd;

import java.sql.SQLException;

import lunasql.lib.Contexte;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurExe;

/**
 * Commande UPDATE <br>
 * (SQL) Mise-à-jour des données d'une table de la base
 * @author M.P.
 */
public class CmdUpdate extends Instruction {

   public CmdUpdate(Contexte cont){
      super(cont, TYPE_CMDSQL, "UPDATE", null);
   }

   @Override
   public int execute() {
      return executeUpdate("UPDATE", " modifiée", true);
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  update  (+instr)   Modifie des données de la base\n";
   }
}// class

