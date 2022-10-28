package lunasql.cmd;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.ret.RetPlugin;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande PLUGIN <br>
 * (Interne) Gestion des plugins de la console
 * @author M.P.
 */
public class CmdPlugin extends Instruction {

   private final OptionParser parser;

   public CmdPlugin(Contexte cont) {
      super(cont, TYPE_CMDINT, "PLUGIN", "PG");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("l", "liste les greffons enregistrés");
      parser.accepts("n", "affiche le nombre de greffons");
      parser.accepts("c", "supprime tous les greffons");
      parser.accepts("a", "affiche la classe du greffon").withRequiredArg().ofType(String.class)
         .describedAs("nom");
      parser.accepts("r", "supprime le greffons").withRequiredArg().ofType(String.class)
         .describedAs("nom");
      parser.nonOptions("nom_classe").ofType(String.class);
   }

   @SuppressWarnings("unchecked")
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
         int ret = RET_CONTINUE;
         if (options.has("l")) {
            int nb = 0;
            StringBuilder sb = new StringBuilder();
            sb.append("Liste des greffons inscrits :\n");
            Set<Map.Entry<String, Class<Instruction>>> lplug = cont.getAllPlugins();
            for (Map.Entry<String, Class<Instruction>> entry : lplug){
               nb++;
               sb.append(" - ").append(entry.getKey()).append(" : ").append(entry.getValue()).append('\n');
            }
            if (nb == 0) sb.append(" (aucun)\n");
            sb.append("Total : ").append(nb).append(" greffon(s)");
            vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
            vr.setSubValue(Integer.toString(nb));
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         else if (options.has("n")){
            String lg = Integer.toString(cont.getPluginsNb());
            vr.setDispValue("Nombre de greffons : " + lg, Contexte.VERB_AFF);
            vr.setSubValue(lg);
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         else if (options.has("c")){
            cont.clearAllPlugins();
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         else if (options.has("r")){
            String pname = (String) options.valueOf("r");
            Class<Instruction> cl = cont.getPlugin(pname);
            if(cl == null) return cont.erreur("PLUGIN", "le greffon " + pname + " n'existe pas", lng);
            cont.removePlugin(pname);
            vr.setSubValue(pname);
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         else if (options.has("a")){
            String pname = (String) options.valueOf("a");
            Class<Instruction> cl = cont.getPlugin(pname);
            if(cl == null) return cont.erreur("PLUGIN", "le greffon " + pname + " n'existe pas", lng);
            vr.setDispValue(pname + " : " + cl.toString(), Contexte.VERB_AFF);
            vr.setSubValue(cl.getCanonicalName());
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         else {
            List<?> lc = options.nonOptionArguments();
            if (lc.size() != 1)
               return cont.erreur("PLUGIN", "une classe de greffon est attendue", lng);
            String cmd = (String) lc.get(0);
            try {
               Class<?> cl = Class.forName(cmd);
               if (Instruction.class.isAssignableFrom(cl)) {
                  RetPlugin r = cont.addPlugin((Class<Instruction>) cl, lng);
                  ret = r.ret; // valeur retour
                  if (r.success == 1) { // succes ajout par cont.addPlugin
                     vr.setDispValue("-> greffon ajouté : " + cl.getCanonicalName()
                           + " en commande " + r.cmdName, Contexte.VERB_BVR);
                     vr.setSubValue(r.cmdName);
                     cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
                  }
               }
               else return cont.erreur("PLUGIN", "la classe " + cl +
                     " n'est pas sous-classe d'Instruction", lng);
            }
            catch (ClassNotFoundException ex) {// erreur prévisible > cont.erreur
               return cont.erreur("PLUGIN", "classe " + cmd + " inaccessible", lng);
            }
            catch (NoClassDefFoundError er) { // ce n'est pas beau, mais ça rattrape d'autres erreurs
               return cont.erreur("PLUGIN", "classe " + er.getMessage() + " inaccessible", lng);
            }
         }

         vr.setRet();
         cont.setValeur(vr);
         return ret;
      }
      catch (OptionException ex) {
         return cont.exception("PLUGIN", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("PLUGIN", "ERREUR IOException : " + ex.getMessage() , lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  plugin, pg  Attribue au système un nouveau greffon\n";
   }
}// class
