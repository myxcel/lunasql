package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande en erreur <br>
 * (Interne) Commande en erreur : inconnue
 * @author M.P.
 */
public class CmdErrInc extends CmdErr {

   public CmdErrInc(Contexte cont){
      super(cont, "ERRINC", null);
   }

   @Override
   public int execute() {
      return cont.erreur(getCommandName(), "commande inconnue ou expression invalide" +
              (msg != null ? " :\n" + msg : ""), lng);
   }
}
