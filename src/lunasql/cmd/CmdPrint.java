package lunasql.cmd;

import java.io.IOException;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande PRINT <br>
 * (Interne) Affichage d'un message sur la sortie standard de la console
 * @author M.P.
 */
public class CmdPrint extends Instruction {

   private final OptionParser parser;

   public CmdPrint(Contexte cont) {
      super(cont, TYPE_CMDINT, "PRINT", "<");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("n", "sans nouvelle ligne");
      parser.accepts("v", "selon niveau de verbose").withRequiredArg().ofType(Integer.class)
         .describedAs("verb");
      parser.accepts("e", "écrit en mode erreur");
      parser.accepts("b", "émet un biip énervant");
      parser.accepts("s", "pas d'affichage en mode évaluation");
      parser.accepts("c", "couleur du texte").withRequiredArg().ofType(Integer.class)
               .describedAs("couleur");
      parser.accepts("f", "efface la console");
      parser.nonOptions("texte_à_afficher").ofType(String.class);
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
         if (options.has("f")) cont.clearConsole();
         if (options.has("b")) cont.playBeep();
         int verb = options.has("v") ? ((Integer)options.valueOf("v")) : Contexte.VERB_AFF;
         if (verb < Contexte.VERB_SIL || verb >= Contexte.VERB_NUMBER)
            return cont.erreur("PRINT", "numéro de verbose invalide : " + verb, lng);
         int coul = options.has("c") ? ((Integer)options.valueOf("c")) : 0;
         if (coul < 0 || coul >= Contexte.COLORS.length)
            return cont.erreur("PRINT", "numéro de couleur invalide : " + coul, lng);

         // Impression
         Valeur vr = null;
         if (options.has("s")) {
            vr = new ValeurDef(cont);
            vr.setDispValue(listToString(options.nonOptionArguments()), verb);
         }
         else {
            if (options.has("e")) {
               if (cont.getVerbose() >= verb)
                  cont.errprintln(listToString(options.nonOptionArguments()), !options.has("n"));
            }
            else {
               if (cont.getVerbose() >= verb)
                  cont.printlnX(listToString(options.nonOptionArguments()), coul, !options.has("n"));
            }
         }
         cont.setValeur(vr);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("PRINT", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("PRINT", "ERREUR IOException : " + ex.getMessage() , lng, ex);
      }
   }


   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  print, <    Affiche le message en paramètre sur la sortie standard\n";
   }
}// class

