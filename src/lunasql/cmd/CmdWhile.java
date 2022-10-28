package lunasql.cmd;

import java.io.IOException;
import javax.script.ScriptException;
import static lunasql.cmd.Instruction.RET_CONTINUE;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.val.Valeur;

/**
 * Commande WHILE <br>
 * (Interne) Boucle WHILE
 * @author M.P.
 */
public class CmdWhile extends Instruction {

   private static final String SYNERR = "syntaxe : WHILE expr bloc [ELSE bloc]";

   public CmdWhile(Contexte cont){
      super(cont, TYPE_CMDINT, "WHILE", null);
   }

   @Override
   public int execute() {
      int lg = getLength();
      if (lg < 3) return cont.erreur("WHILE", SYNERR, lng);

      try {
         int ret = RET_CONTINUE;
         boolean exec = false;      
         Lecteur lec = new Lecteur(cont);
         lec.setLecVar(Contexte.LEC_SUPER, "while");
         lec.setLecVar(Contexte.LEC_LOOP_DEEP, cont.incrLoopDeep()); // profondeur de boucle
         String iter = getArg(1), code = getArg(2); // copie necessaire pour imbrication de while
         while (ret == RET_CONTINUE && cont.evaluerBool(iter)) {
            ret = cont.evaluerBlockFor(lec, code, null);
            exec = true;
         }

         // Condition jamais valide > clause ELSE
         if (!exec && lg > 3 && getArg(3).equalsIgnoreCase("ELSE")) { // exécution else
            if (lg == 4) return cont.erreur("WHILE", SYNERR, lng);
            ret = cont.evaluerBlockFor(lec, getArg(4), null);
            exec = true;
         }
         lec.fin();

         if (exec) {
            Valeur vr = cont.getValeur();
            if (vr != null) vr.setDispValue(null, Contexte.VERB_SIL);
         }
         else cont.setValeur(null);  // retour sans exécution

         if (ret == RET_BREAK_LP) {
            String lb = cont.getLecVar(Contexte.LEC_LOOP_BREAK); // si _LOOP_BREAK positionnée
            return lb == null ? RET_CONTINUE : RET_BREAK_LP;
         }
         else return ret;
      }
      catch (IllegalArgumentException e) {// erreur déjà traitée (cf. cont.evaluerBool)
         return cont.erreur("WHILE", "Expression invalide :\n" + e.getMessage(), lng);
      }
      catch (ScriptException e) {// erreur prévisible > cont.erreur
         return cont.erreur("WHILE", "Impossible d'évaluer l'expression :\n" + e.getMessage(), lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  while       Teste une expression et boucle sur les arguments\n";
   }
}// class

