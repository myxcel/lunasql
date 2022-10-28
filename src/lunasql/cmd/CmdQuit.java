package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande QUIT <br>
 * (Interne) Fermeture de la console
 * @author M.P.
 */
public class CmdQuit extends Instruction {

   public CmdQuit(Contexte cont) {
      super(cont, TYPE_CMDINT, "QUIT", null);
   }

   @Override
   public int execute() {
      if (getLength() == 1) {
         cont.setQuitStat(0);
         cont.setValeur(null);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (getLength() >= 2) {
         String st = getArg(1);
         int ist;
         try {
            ist = Integer.parseInt(st);
         }
         catch(NumberFormatException ex){
            cont.exception("QUIT", "valeur de sortie non numérique", lng, ex);
            ist = -1;
         }
         cont.setQuitStat(ist);
         if (getLength() >= 3 && cont.getVerbose() >= Contexte.VERB_AFF) cont.println(getSCommand(2));
         cont.setValeur(null);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_ERROR);
      }
      else {
         cont.erreur("QUIT", "maximum une valeur de sortie est attendue", lng);
      }
      return RET_SHUTDOWN;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  quit        Ferme de la connexion à la base et quitte (Ctrl+D)\n";
   }
}// class
