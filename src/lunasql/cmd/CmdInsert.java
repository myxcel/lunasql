package lunasql.cmd;

import java.sql.SQLException;

import lunasql.lib.Contexte;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurExe;

/**
 * Commande INSERT <br>
 * (SQL) Insertion d'enregistrements dans une table de la base de données
 * @author M.P.
 */
public class CmdInsert extends Instruction {

   public CmdInsert(Contexte cont) {
      super(cont, TYPE_CMDSQL, "INSERT", null);
   }

   @Override
   public int execute() {
      return executeUpdate("INSERT", " insérée", true);
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  insert  (+instr)   Ajoute des données dans la base\n";
   }
}// class

