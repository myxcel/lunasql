package lunasql.cmd;

import java.io.IOException;

import javax.script.ScriptException;
import static lunasql.cmd.Instruction.RET_CONTINUE;

import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;

/**
 * Commande WHEN <br>
 * (Interne) Conditionnelle WHEN par contexte d'exécution
 * @author M.P.
 */
public class CmdWhen extends Instruction {

   public CmdWhen(Contexte cont){
      super(cont, TYPE_MOTC_WH, "WHEN", null);
   }

   @Override
   public int execute() {
      if (getLength() >= 2) {
         String s = getSCommand(1);
         try {
            boolean r = cont.canExec();
            if (r) r = (cont.evaluerBool(s));
            cont.addWhen(r);
            cont.setValeur(new ValeurDef(cont, null, r ? "1" : "0"));
            return RET_CONTINUE;
         }
         catch (IllegalArgumentException e) {// erreur déjà traitée (cf. cont.evaluerBool)
            return cont.erreur("WHEN", "Expression invalide :\n" + e.getMessage(), lng);
         }
         catch (ScriptException e) {// erreur prévisible > cont.erreur
            return cont.erreur("WHEN", "Impossible d'évaluer l'expression :\n" + e.getMessage(), lng);
         }
      }
      else return cont.erreur("WHEN", "une expression est attendue", lng);
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc() {
      return "  when        Teste une expression et fixe le contexte d'exécution\n";
   }
}// class

