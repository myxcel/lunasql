package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande END <br>
 * (Interne) Fin de commande conditionnelle IF
 * @author M.P.
 */
public class CmdEnd extends Instruction {

   public CmdEnd(Contexte cont){
      super(cont, TYPE_MOTC_WH, "END", null);
   }

   @Override
   public int execute() {
      if (getLength() == 1) {
         if (cont.hasIf()) {
            cont.endWhen();
            cont.setValeur(null);
            return RET_CONTINUE;
         }
         else return cont.erreur("END", "WHEN/END : aucun WHEN correspondant", lng);
      }
      else return cont.erreur("END", "aucun argument attendu", lng);
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return null;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getHelp(){
      return cont.getCommand("WHEN").getHelp();
   }
}// class

