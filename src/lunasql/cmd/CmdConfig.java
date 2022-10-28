package lunasql.cmd;

import java.io.File;
import java.io.IOException;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;
//import jline.console.ConsoleReader;

/**
 * Commande CONFIG <br>
 * (Interne) Sauvegarde de la configuration (constantes de config., variables et greffons)
 * @author M.P.
 */
public class CmdConfig extends Instruction {

   private final OptionParser parser;

   public CmdConfig(Contexte cont){
      super(cont, TYPE_CMDINT, "CONFIG", "CF");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("s", "sauvegarde des var. et options");
      parser.accepts("l", "chargement des var. et options");
      parser.accepts("o", "seulement les options");
      parser.nonOptions("fichier").ofType(String.class);
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
         List<?> lf = options.nonOptionArguments();
         if (lf.size() > 2)
            return cont.erreur("CONFIG", "au max. une cmd et un nom de fichier sont attendus", lng);

         Valeur vr = new ValeurDef(cont);
         String cmd = lf.isEmpty() ? "LOAD" : ((String) lf.get(0)).toUpperCase();
         if (options.has("s") || cmd.equals("SAVE")) { // sauvegarde
            try {
               int n;
               if (options.has("s")) n = saveVars(lf.isEmpty() ?
                     cont.getVar(Contexte.ENV_CONF_FILE) : (String) lf.get(0), vr, options.has("o"));
               else n = saveVars(lf.size() == 1 ?
                     cont.getVar(Contexte.ENV_CONF_FILE) : (String) lf.get(1), vr, options.has("o"));
               vr.setSubValue(Integer.toString(n));
            } catch (IOException ex) {
               return cont.exception("CONFIG", "ERREUR IOException : " + ex.getMessage(), lng, ex);
            }
         } else if (options.has("l") || cmd.equals("LOAD")) { // chargement
            File f;
            String v = null;
            if (options.has("l") && lf.size() == 1) v = (String) lf.get(0);
            else if (lf.size() == 2) v = (String) lf.get(1);
            if (v == null) {
               f = new File(cont.getVar(Contexte.ENV_CONF_FILE));
               if (!f.isFile())
                  return cont.erreur("CONFIG", "le fichier " + Contexte.ENV_CONF_FILE + " '" +
                        f.getName() + "' n'existe pas", lng);
            } else if ((f = new File(v)).isFile()) cont.setVar(Contexte.ENV_CONF_FILE, v);
            else return cont.erreur("CONFIG", "le fichier '" + v + "' n'existe pas", lng);

            int nbl = cont.loadConfigFile(options.has("o"));
            vr.setDispValue("Fichier de config. '" + f.getAbsolutePath() + "' chargé (" + nbl + " définitions)");
            vr.setSubValue("0");
         } else if (!options.has("l") && !options.has("s"))
            return cont.erreur("CONFIG", "une option LOAD ou SAVE est attendue", lng);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);

         vr.setRet();
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("CONFIG", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex){
         return cont.exception("CONFIG", "ERREUR IOException : " + ex.getMessage() , lng, ex);
      }
   }

   /**
    * Ecriture des variables utilisateur en fichier de configuration spécifié
    * @param cfgfile le nom du fichier de configuration de destination
    * @param sysonly si l'on doit sauver uniquement les var. systèmes
    * @throws IOException si c'est le cas
    */
   private int saveVars(String cfgfile, Valeur vr, boolean sysonly) throws IOException {
      File fcfg = new File(cfgfile);
      if (!cont.askWriteFile(fcfg)) {
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }

      // Ecriture
      int nbl = cont.dumpConfigFile(fcfg, sysonly);
      vr.setDispValue("Fichier de config. '" + fcfg.getCanonicalPath() + "' écrit (" + nbl + " définitions)",
            Contexte.VERB_MSG);
      return nbl;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  config, cf  Gère la configuration actuelle (variables et greffons)\n";
   }
}// class
