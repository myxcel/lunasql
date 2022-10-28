package lunasql.cmd;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.val.EvalEngine;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande ENGINE <br>
 * (Interne) Fixation du moteur d'évaluation de la console
 * @author M.P.
 */
public class CmdEngine extends Instruction {

   private final OptionParser parser;

   public CmdEngine(Contexte cont){
      super(cont, TYPE_CMDINT, "ENGINE", "SE");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("l", "liste des engines disponibles");
      parser.nonOptions("engine").ofType(String.class);
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
         Valeur vr = new ValeurDef(cont);
         // Listage de tous les moteurs disponibles
         if (options.has("l")) {
            int nb = 0;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, EvalEngine> entry : cont.getAllEngines().entrySet()) {
               nb++;
               ScriptEngineFactory factory = entry.getValue().getEngine().getFactory();
               sb.append("\n  Code : ").append(entry.getKey());
               sb.append("\n  Nom : ").append(factory.getEngineName())
                     .append(" (").append(factory.getEngineVersion()).append(")\n");
               StringBuilder aliases = new StringBuilder();
               for (String n : factory.getNames()) aliases.append(n).append(", ");
               aliases.delete(aliases.length() - 2, aliases.length());
               sb.append("  Alias : ").append(aliases.toString()).append('\n');
               sb.append("  Langage : ").append(factory.getLanguageName()).append(" (")
                     .append(factory.getLanguageVersion()).append(")\n");
            }
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
            vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
            vr.setSubValue(Integer.toString(nb));
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         // Sans argument: affichage de l'engine courant
         else if (options.nonOptionArguments().isEmpty()) {
            vr = new ValeurDef(cont, "Moteur d'évaluation actuel : " + cont.getEvalEngineName(),
                  Contexte.VERB_MSG, cont.getEvalEngineAlias());
            cont.setValeur(vr);
            return RET_CONTINUE;
         }
         // Avec un argument: affectation d'un nouveau
         else {
            List<?> lc = options.nonOptionArguments();
            if (lc.size() > 1)
               return cont.erreur("ENGINE", "au maximum un nom de moteur SE est attendu", lng);

            String id = (String) lc.get(0);
            ScriptEngine se;
            try {
               se = cont.getEvalEngine(Integer.parseInt(id));
            } catch (NumberFormatException ex) {
               se = cont.getEvalEngine(id);
            }
            if (se == null)
               return cont.erreur("ENGINE", "objet ScriptEngine '" + getArg(1) + "' inaccessible."
                     + "\nAvez-vous ajouté la bibliothèque correspondante au CLASSPATH ?", lng);

            cont.setEvalEngine(se);
            vr.setDispValue("Nouveau moteur EVAL = " + cont.getEvalEngineName(), Contexte.VERB_BVR);
            vr.setSubValue(cont.getEvalEngineAlias());
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         vr.setRet();
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("ENGINE", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex){
         return cont.exception("ENGINE", "ERREUR IOException : " + ex.getMessage() , lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc() {
      return "  engine, se  Attribue au système un nouveau moteur d'évaluation\n";
   }
}// class

