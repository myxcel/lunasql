package lunasql.cmd;

import lunasql.lib.Contexte;

/**
 * Commande en erreur <br>
 * (Interne) Commande en erreur : syntaxe incorrecte
 * @author M.P.
 */
public class CmdErrSyn extends CmdErr {

   public CmdErrSyn(Contexte cont){
      super(cont, "ERRSYN", null);
   }

   @Override
   public int execute() {
      String s = getCommandName();
      return cont.erreur("Lecteur", "erreur de syntaxe (chaîne, parenthèse ou commentaire)" +
              (s.length() > 0 ? " : " + getCommandName() : "") +
              (msg != null ? " :\n" + msg : ""), lng);
   }
}
