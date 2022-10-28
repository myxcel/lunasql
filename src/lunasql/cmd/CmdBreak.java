package lunasql.cmd;

import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;

/**
 * Commande BREAK <br>
 * (Interne) Sort d'une boucle for ou while en cours
 * @author M.P.
 */
public class CmdBreak extends Instruction {

   public CmdBreak(Contexte cont){
      super(cont, TYPE_CMDINT, "BREAK", null);
   }

   @Override
   public int execute() {
      if (cont.getLoopDeep() == 0)
         return cont.erreur("BREAK", "BREAK appelée hors d'une boucle FOR ou WHILE", lng);

      // Profondeur fournie
      int lo = 0;
      if (getLength() == 2) {
         try { lo = Integer.parseInt(getArg(1)); }
         catch (NumberFormatException ex) {// erreur prévisible > cont.erreur
            return cont.erreur("BREAK", "profondeur non numérique : " + getArg(1), lng);
         }
      }
      // Programmation de la sortie sur profondeurs > 0
      if (lo > 0) {
         cont.setLecVar(Contexte.LEC_LOOP_BREAK, "1"); // positionnement non null
         cont.setLinkVar(Contexte.LEC_LOOP_BREAK, lo);
      }
      cont.setValeur(new ValeurDef(cont, null, Integer.toString(lo)));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return RET_BREAK_LP;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  break       Sort d'une boucle FOR ou WHILE\n";
   }
}// class
