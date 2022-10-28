package lunasql.misc;

import java.io.IOException;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;

/**
 * Commande TEST <br>
 * (Interne) Teste si les chaînes données en argument sont non nulles
 * @author M.P.
 */
public class CmdTest extends Instruction {

   private final OptionParser parser;

   public CmdTest(Contexte cont) {
      super(cont, TYPE_CMDPLG, "TEST", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("n", "numéro de test").withRequiredArg().ofType(Integer.class)
         .describedAs("numéro");
      parser.accepts("d", "description du test").withRequiredArg().ofType(String.class)
               .describedAs("description");
      parser.accepts("e", "valeur attendue").withRequiredArg().ofType(String.class)
         .describedAs("valeur");
      parser.nonOptions("tests").ofType(String.class);
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
      }
      catch (OptionException|IOException ex) {
         return cont.exception("TEST", "erreur d'option : " + ex.getMessage(), lng, ex);
      }

      // Exécution avec autres options
      if (options.nonOptionArguments().isEmpty())
         return cont.erreur("TEST", "une chaîne au moins est requise", lng);

      String nb = options.has("n") ? ((Integer)options.valueOf("n")).toString() + " ": "";
      String desc = options.has("d") ? "\"" + options.valueOf("d") + "\" " : "";
      List<?> lt = options.nonOptionArguments();
      for (int i = 0; i < lt.size(); i++) { // pour chaque objet
         String arg = lt.get(i).toString();
         boolean echec = false;
         if (options.has("e")) {
            if (!arg.equals(options.valueOf("e"))) echec = true;
         } else if (arg.length() == 0 || arg.matches("0(.0)?") || arg.equalsIgnoreCase("false"))
            echec = true;
         if (echec) return cont.erreur("TEST", "test " + nb + desc +
                 "position " + (i + 1) + " : échec (" + arg + ")", lng);
      }
      cont.setValeur(new ValeurDef(cont, "Test " + nb + desc + "réussi", Contexte.VERB_BVR, "1"));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  test        Commande de test de nullité des arguments\n";
   }
}// class
