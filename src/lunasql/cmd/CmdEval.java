package lunasql.cmd;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sun.corba.se.pept.transport.ContactInfo;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

import javax.naming.Context;

/**
 * Commande EVAL <br>
 * (Interne) Évaluation d'une expression arithmétique ou d'une commande
 * @author M.P.
 */
public class CmdEval extends Instruction {

   private final OptionParser parser;

   public CmdEval(Contexte cont){
      super(cont, TYPE_CMDINT, "EVAL", "=");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("t", "analyse du temps d'exécution du bloc");
      parser.accepts("n", "nom(s) de commande (si -c)").withRequiredArg().ofType(String.class)
         .describedAs("noms");
      parser.accepts("c", "code à exécuter si erreur").requiredIf("n")
         .withRequiredArg().ofType(String.class).describedAs("code");
      parser.accepts("f", "code à exécuter dans tous les cas (finally)")
         .withRequiredArg().ofType(String.class).describedAs("code");
      parser.accepts("v", "dictionnaire de var. locales").withRequiredArg().ofType(String.class)
         .describedAs("vars");
      parser.nonOptions("nom_var chaîne_à_évaluer").ofType(String.class);
   }

   @Override
   public int execute() {
      try {
         OptionSet options = parser.parse(getCommandA1());
         // Aide sur les options
         if (options.has("?")) {
            parser.printHelpOn(cont.getWriterOrOut());
            cont.setValeur(null);
            return RET_CONTINUE;
         }

         // Exécution avec autres options
         List<?> lc = options.nonOptionArguments();
         if (lc.isEmpty())
            return cont.erreur("EVAL", "une commande (ou un bloc de code) est attendue", lng);

         // Variables locales
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "eval");
         if (options.has("v")) { // dict de vars fourni
            Properties prop = Tools.getProp((String)options.valueOf("v"));
            if (prop == null) return cont.erreur("EVAL", "dictionnaire invalide" , lng);
            for (Map.Entry<Object, Object> me : prop.entrySet()) {
               String key = (String) me.getKey(), val = (String) me.getValue();
               if (cont.valideKey(key)) cont.setLecVar(key, val);
               else return cont.erreur("EVAL", "affectation de variable invalide : " + key, lng);
            }
         }
         if (options.has("c")) { // bloc code err fourni
            vars.put(Contexte.LEC_CATCH_CODE, (String)options.valueOf("c"));
            vars.put(Contexte.LEC_CATCH_LCMD, options.has("n") ? (String)options.valueOf("n") : "");
         }

         // Exécution sans retour (à charge de la commande)
         int ret;
         long tm = 0L;
         boolean chrono = options.has("t");
         if (chrono) tm = System.currentTimeMillis();
         cont.addSubMode();
         Lecteur lec = new Lecteur(cont, listToString(lc), vars);
         ret = lec.getCmdState() == RET_EV_CATCH ? RET_CONTINUE : lec.getCmdState();
         cont.remSubMode();
         if (chrono && cont.getVerbose() >= Contexte.VERB_AFF) {
            tm = System.currentTimeMillis() - tm;
            double nb = 5000.0 / tm;
            // Affichage sans valeur de retour
            String s = "-> temps d'exécution : " + SQLCnx.frmDur(tm) + " (" + String.format("%.2f", nb) +
                    " fois en 5 s)";
            Valeur vr = cont.getValeur();
            if (vr == null) vr = new ValeurDef(cont, s, null);
            else vr.setDispValue(s);
            cont.setValeur(vr);
         }

         // Exécution du bloc finally (dans tous les cas)
         if (options.has("f")) { // finally
            vars.put(Contexte.LEC_SUPER, "eval:finally");
            vars.remove(Contexte.LEC_CATCH_CODE);
            vars.remove(Contexte.LEC_CATCH_LCMD);
            cont.addSubMode();
            Lecteur lec2 = new Lecteur(cont, (String)options.valueOf("f"), vars);
            cont.remSubMode();
            if (ret == RET_CONTINUE) ret = lec2.getCmdState();
         }
         return ret;
      }
      catch (OptionException ex) {
         return cont.exception("EVAL", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex){
         return cont.exception("EVAL", "Erreur IOException : " + ex.getMessage() , lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  eval, =     Évalue une commande en espace confiné\n";
   }
}// class

