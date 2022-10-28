package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande ELSE <br>
 * (Interne) Conditionnelle ELSE
 * @author M.P.
 */
public class CmdElse extends Instruction {

   public CmdElse(Contexte cont){
      super(cont, TYPE_MOTC_WH, "ELSE", null);
   }

   @Override
   public int execute() {
      if (getLength() == 1) {
         if (cont.hasIf()) {
            cont.invWhen();
            cont.setValeur(null);
            return RET_CONTINUE;
         }
         else return cont.erreur("ELSE", "aucun WHEN correspondant", lng);
      }
      else return cont.erreur("ELSE", "aucun argument n'est attendu", lng);
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

