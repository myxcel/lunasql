package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande EXPLAIN <br>
 * (SQL) Détail de l'exécution d'une requête
 * @author M.P.
 */
public class CmdExplain extends Instruction {

   public CmdExplain(Contexte cont){
      super(cont, TYPE_CMDSQL, "EXPLAIN", null);
   }

   @Override
   public int execute() {
      return executeCall("EXPLAIN", "exécution terminée");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  explain (+instr)   Détaille l'exécution d'une requête\n";
   }
}// class

