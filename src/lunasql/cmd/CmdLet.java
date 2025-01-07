package lunasql.cmd;

import lunasql.lib.Contexte;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande LET <br>
 * (Interne) Évalue par SE une expression arithmétique
 * @author M.P.
 */
public class CmdLet extends Instruction {

   public CmdLet(Contexte cont){
      super(cont, TYPE_CMDINT, "LET", "%");
   }

   @Override
   public int execute() {
      try {
         Object r = cont.evaluerExpr("objectToJson(" + getSCommand(1) + ",1)");
         Valeur vr = r == null ? null :
            new ValeurDef(cont, r.toString(), Contexte.VERB_BVR, Contexte.BR_BLUE, r.toString());
         cont.setValeur(vr);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (Exception ex) {
         return cont.erreur("LET", "expression incorrecte : " + ex.getMessage() , lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  let, %      Évalue une expression par SE\n";
   }
}// class
