package lunasql.cmd;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.Config;
import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;

/**
 * Commande NEED <br>
 * (Interne) Contrôle la version du programme, fait penser à la fonction 'require' de Perl
 * @author M.P.
 */
public class CmdNeed extends Instruction {

   private final OptionParser parser; // pas besoin pour le moment
   public static final String VER_REG =
         "^([0-9]{1,3})(\\.([0-9]{1,3})(\\.([0-9]{1,3})(\\.([0-9]{1,3}))?)?)?$";

   public CmdNeed(Contexte cont) {
      super(cont, TYPE_CMDINT, "NEED", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("x", "version maximale requise").withRequiredArg().ofType(String.class)
         .describedAs("version");
      parser.nonOptions("no_version").ofType(String.class);
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
         List<?> ln = options.nonOptionArguments();
         if (ln.size() != 1)
            return cont.erreur("NEED", "un numéro de version minimale est attendu", lng);

         boolean bmax = options.has("x");
         String smin = (String) ln.get(0), smax = null;
         int[] v0 = frmVersion(Config.APP_VERSION_NUM), vmin = frmVersion(smin), vmax = null;
         if (bmax) {
            smax = (String) options.valueOf("x");
            vmax = frmVersion(smax);
         }

         // Contrôles
         if (v0 == null)
            throw new IllegalArgumentException("numéro de version APP_VERSION_NUM invalide : " + Config.APP_VERSION_NUM);
         if (vmin == null)
            return cont.erreur("NEED", "numéro de version min. invalide : " + smin, lng);
         if (bmax && vmax == null)
            return cont.erreur("NEED", "numéro de version max. invalide : " + smax, lng);

         long diffmin = 0, diffmax = 0;
         for (int i = 0; i < 4; i++) diffmin += Math.pow(10, 3 * i) * (v0[i] - vmin[i]);
         if (bmax) for (int i = 0; i < 4; i++) diffmax += Math.pow(10, 3 * i) * (vmax[i] - v0[i]);
         if (diffmin < 0) { // Mauvaise version minimale : on sort du script
            return cont.erreur("NEED", "version insuffisante : " + Config.APP_VERSION_NUM
                  + " (requise en appel : " + smin + ")", lng);
         }
         if (diffmax < 0) { // Mauvaise version maximale : on sort aussi du script
            return cont.erreur("NEED", "version excédante : " + Config.APP_VERSION_NUM
                  + " (requise en appel : " + smax + ")", lng);
         }
         if(vmin[3] < 3) {
            return cont.erreur("NEED", "version non supportée : " + Config.APP_VERSION_NUM
                  + " (requise en appel : " + smin + ")", lng);
         }

         // Retour
         cont.setValeur(new ValeurDef(cont, "Version requise OK : " + Config.APP_VERSION_NUM,
               Contexte.VERB_BVR, Long.toString(diffmin + diffmax)));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("NEED", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("NEED", "ERREUR IOException : " + ex.getMessage() , lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  need        Teste la version en cours par rapport à numéro donné\n";
   }

   /**
    * Formate un numéro de version en 000.000.000.000
    *
    * @param v la version
    * @return formaté
    */
   private int[] frmVersion(String v) {
      Matcher m = Pattern.compile(VER_REG).matcher(v);
      if (m.find()) {
         int mj = m.group(1) == null ? 0 : Integer.parseInt(m.group(1)),
               mn = m.group(3) == null ? 0 : Integer.parseInt(m.group(3)),
               dt = m.group(5) == null ? 0 : Integer.parseInt(m.group(5)),
               rv = m.group(7) == null ? 0 : Integer.parseInt(m.group(7));
         return new int[] { rv, dt, mn, mj };
      } else {
         return null;
      }
   }
}// class
