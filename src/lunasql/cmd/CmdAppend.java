package lunasql.cmd;

import lunasql.lib.Contexte;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande APPEND <br>
 * (Interne) Affecte à la valeur de retour _RET_VALUE les arguments de la commande en mode ajout
 * @author M.P.
 */
public class CmdAppend extends Instruction {

   public CmdAppend(Contexte cont){
      super(cont, TYPE_CMDINT, "APPEND", null);
   }

   @Override
   public int execute() {
      String ret = (getLength() == 1 ? null : getSCommand(1)), ancv;
      Valeur anc = cont.getValeur();
      if (anc == null || (ancv = anc.getSubValue()) == null) {
         if (ret == null) cont.setValeur(null);
         else cont.setValeur(new ValeurDef(cont, null, ret));
      }
      else if (ret != null) cont.setValeur(new ValeurDef(cont, null, ancv + ret));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  append      Affecte en mode ajout la valeur de retour\n";
   }
}// class
