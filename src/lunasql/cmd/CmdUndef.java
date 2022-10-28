package lunasql.cmd;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Tools;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande UNDEF <br>
 * (Interne) Suppression d'une variable de l'environnement d'exécution
 * @author M.P.
 */
public class CmdUndef extends Instruction {

   private final OptionParser parser;

   public CmdUndef(Contexte cont) {
      super(cont, TYPE_CMDINT, "UNDEF", "-");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("a", "supprime toutes variables");
      parser.accepts("f", "supprime les variables sur patron").withRequiredArg().ofType(String.class)
         .describedAs("pattern");
      parser.accepts("d", "supprime si déclarée");
      parser.nonOptions("noms_variables").ofType(String.class);
   }

   @Override
   public int execute() {
      OptionSet options;
      try {
         options = parser.parse(getCommandA1());
         // Aide sur les options
         if (options.has("?")) {
            parser.printHelpOn(cont.getWriterOrOut());
            cont.setValeur(null);
            return RET_CONTINUE;
         }

         // Exécution avec autres options
         Valeur vr = new ValeurDef(cont);
         int ret = RET_CONTINUE, nbundef = 0;
         boolean err = false;
         if (options.has("a")) { // suppression de toutes variables
            nbundef = cont.unsetAllVars();
            vr.setDispValue("Toutes variables supprimées", Contexte.VERB_BVR);
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         } else if (options.has("f")) { // suppression sur patron
            SortedSet<String> sort = new TreeSet<>(cont.getAllVars().keySet());
            Iterator<String> iter = sort.iterator();
            Pattern ptnb = Pattern.compile(Tools.removeBQuotes((String) options.valueOf("f")).value);
            StringBuilder sb = new StringBuilder();
            while (iter.hasNext()) {
               String key = iter.next();
               if (cont.isNonSys(key) && ptnb.matcher(key).matches() && cont.unsetVar(key)) {
                  nbundef++;
                  sb.append('\'').append(key).append("' supprimée").append('\n');
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
               }
            }
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
            vr.setDispValue(sb.toString(), Contexte.VERB_BVR);
         } else {
            List<?> lv = options.nonOptionArguments();
            if (lv.isEmpty())
               return cont.erreur("UNDEF", "un nom de variable au moins est attendu", lng);

            StringBuilder sb = new StringBuilder();
            for (Object o : lv) { // pour chaque var
               String key = (String) o;
               if (cont.isSys(key) || cont.isSysUser(key)) {
                  err = true;
                  ret = cont.erreur("UNDEF", "suppression d'option système interdite : " + key, lng);
               } else {
                  if (cont.unsetVar(key)) {
                     nbundef++;
                     sb.append('\'').append(key).append("' supprimée").append('\n');
                     cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
                  } else if (!options.has("d")) {
                     err = true;
                     ret = cont.erreur("UNDEF", "variable globale '" + key + "' non définie", lng);
                  }
               }
            }
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
            vr.setDispValue(sb.toString(), Contexte.VERB_BVR);
         }
         if (err) return ret;

         vr.setSubValue(Integer.toString(nbundef));
         vr.setRet();
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("UNDEF", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("UNDEF", "ERREUR IOException : " + ex.getMessage(), lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  undef, -    Supprime une variable de l'environnement de console\n";
   }
}// class

