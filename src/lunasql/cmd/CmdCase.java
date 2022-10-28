package lunasql.cmd;

import java.util.regex.PatternSyntaxException;

import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.val.Valeur;

/**
 * Commande CASE <br>
 * (Interne) Conditionnel SWITCH CASE
 * @author M.P.
 */
public class CmdCase extends Instruction {

   private static final String SYNERR = "syntaxe : CASE val [str|regexp bloc]* [ELSE bloc]";

   public CmdCase(Contexte cont){
      super(cont, TYPE_CMDINT, "CASE", null);
   }

   @Override
   public int execute() {
      try {
         int lg = getLength();
         if (lg < 2 || lg % 2 != 0) return cont.erreur("CASE", SYNERR, lng);

         // Test des égalités regexp
         int ret = RET_CONTINUE;
         boolean exec = false;
         Lecteur lec = new Lecteur(cont);
         lec.setLecVar(Contexte.LEC_SUPER, "case");
         int i = 2;
         String val = getArg(1);
         while (!exec && i < lg - 1) {
            String reg = getArg(i);
            Tools.BQRet rbq = Tools.removeBQuotes(reg);
            if (rbq.hadBQ) { // avec quotes > regexp
               if (val.matches(rbq.value)) {
                  ret = cont.evaluerBlockIf(lec, getArg(i + 1));
                  exec = true;
               }
            }
            else { // sans quotes > exacte
               if (val.equals(reg)) {
                  ret = cont.evaluerBlockIf(lec, getArg(i + 1));
                  exec = true;
               }
            }
            i += 2;
         }
         // Aucun match valide > else
         if (!exec && lg > 3 && getArg(lg - 2).equalsIgnoreCase("ELSE")) { // exécution else
            ret = cont.evaluerBlockIf(lec, getArg(lg - 1));
            exec = true;
         }
         lec.fin();
         
         if (exec) {
            Valeur vr = cont.getValeur();
            if (vr != null) vr.setDispValue(null, Contexte.VERB_SIL);
         }
         else cont.setValeur(null);  // retour sans exécution
         return ret;
      }
      catch (PatternSyntaxException ex) {
         return cont.erreur("CASE", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  case        Teste en chaîne une exp. reg. et exécute les arguments\n";
   }
}// class

