package lunasql.cmd;

import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;

/**
 * Commande NEXT <br>
 * (Interne) Passe à l'itération suivante d'une boucle for ou while en cours
 * @author M.P.
 */
public class CmdNext extends Instruction {

   public CmdNext(Contexte cont){
      super(cont, TYPE_CMDINT, "NEXT", null);
   }

   @Override
   public int execute() {
      if (cont.getLoopDeep() == 0)
         return cont.erreur("NEXT", "NEXT appelée hors d'une boucle FOR ou WHILE", lng);
      if (getLength() > 1)
         return cont.erreur("NEXT", "aucun argument attendu", lng);

      cont.setValeur(new ValeurDef(cont, null, "0"));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return RET_NEXT_LP;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  next        Saute à l'itération suivante (FOR ou WHILE)\n";
   }
}// class
