package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande COMMENT <br>
 * (SQL) Ajout d'un commentaire
 * @author M.P.
 */
public class CmdComment extends Instruction {

   public CmdComment(Contexte cont){
      super(cont, TYPE_CMDSQL, "COMMENT", null);
   }

   @Override
   public int execute() {
      return executeUpdate("COMMENT", "commentaire ajouté");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  comment (+instr)   Supprime un objet de la base\n";
   }
}// class

