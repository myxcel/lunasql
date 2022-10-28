package lunasql.cmd;

import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;

/**
 * Commande EXIT <br>
 * (Interne) Interruption de l'exécution d'un fichier de commandes
 * @author M.P.
 */
public class CmdExit extends Instruction {

   public CmdExit(Contexte cont) {
      super(cont, TYPE_CMDINT, "EXIT", null);
   }

   @Override
   public int execute() {
      int errno = 0;
      if (getLength() >= 2) {
         try {
            errno = Integer.parseInt(getArg(1));
         }
         catch (NumberFormatException ex) {// erreur prévisible > cont.erreur
            cont.erreur("EXIT", "valeur de sortie non numérique : " + getArg(1), lng);
         }
      }
      if (cont.getVerbose() >= Contexte.VERB_AFF && getLength() >= 3) {
         if (errno == 0) cont.println(getSCommand(2));
         else cont.errprintln(getSCommand(2)); // errprintln et non erreur
      }
      cont.setValeur(new ValeurDef(cont, null, Integer.toString(errno)));
      cont.setVar(Contexte.ENV_CMD_STATE, errno == 0 ? Contexte.STATE_FALSE : Contexte.STATE_ERROR);
      return RET_EXIT_SCR;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  exit        Sort du fichier de commandes SQL en cours\n";
   }
}// class

