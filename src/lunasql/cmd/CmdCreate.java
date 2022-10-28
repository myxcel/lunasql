package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande CREATE <br>
 * (SQL) Création d'un objet de base de données (table, index, vue...)
 * @author M.P.
 */
public class CmdCreate extends Instruction {

   public CmdCreate(Contexte cont){
      super(cont, TYPE_CMDSQL, "CREATE", null); 
   }

   @Override
   public int execute() {
      return executeUpdate("CREATE", "objet créé");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  create  (+instr)   Crée un objet dans la base\n";
   }
}// class

