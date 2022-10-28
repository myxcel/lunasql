package lunasql.cmd;

import java.io.IOException;
import javax.script.ScriptException;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.val.Valeur;

/**
 * Commande IF <br>
 * (Interne) Conditionnel IF (ELSEIF ELSE)
 * @author M.P.
 */
public class CmdIf extends Instruction {

   private static final String SYNERR = "syntaxe : IF expr bloc [ELSEIF expr bloc]+ [ELSE bloc]";

   public CmdIf(Contexte cont){
      super(cont, TYPE_CMDINT, "IF", null);
   }

   @Override
   public int execute() {
      try {
         int lg = getLength();
         if (lg < 3) return cont.erreur("IF", SYNERR, lng);

         // Test des conditions booléennes
         int ret = RET_CONTINUE;
         boolean exec;
         Lecteur lec = new Lecteur(cont);
         lec.setLecVar(Contexte.LEC_SUPER, "if");
         String iter = getArg(1);
         if (cont.evaluerBool(iter)) {
            ret = cont.evaluerBlockIf(lec, getArg(2));
            exec = true;
         }
         else { // false
            exec = false;
            int i = 3;

            // Test des elseif
            while (!exec && i < lg && getArg(i).equalsIgnoreCase("ELSEIF")) {
               if (i + 2 >= lg) return cont.erreur("IF", SYNERR, lng);
               if (cont.evaluerBool(getArg(i + 1))) {
                  ret = cont.evaluerBlockIf(lec, getArg(i + 2));
                  exec = true;
               }
               i += 3;
            }

            // Aucun elseif valide > else
            if (!exec && i < lg && getArg(i).equalsIgnoreCase("ELSE")) { // exécution else
               if (i + 1 >= lg) return cont.erreur("IF", SYNERR, lng);
               ret = cont.evaluerBlockIf(lec, getArg(i + 1));
               exec = true;
            }
         }
         lec.fin();

         if (exec) {
            Valeur vr = cont.getValeur();
            if (vr != null) vr.setDispValue(null, Contexte.VERB_SIL);
         }
         else cont.setValeur(null);  // retour sans exécution
         return ret;
      }
      catch (IllegalArgumentException e) {// erreur déjà traitée (cf. cont.evaluerBool)
         return cont.erreur("IF", "Expression invalide :\n" + e.getMessage(), lng);
      }
      catch (ScriptException e) {// erreur prévisible > cont.erreur
         return cont.erreur("IF", "Impossible d'évaluer l'expression :\n" + e.getMessage(), lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  if          Teste une expression et exécute les arguments\n";
   }
}// class

