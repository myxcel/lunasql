package lunasql.cmd;

import static joptsimple.util.RegexMatcher.regex;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande RAND <br>
 * (Interne) Calcule un nombre aléatoire
 * @author M.P.
 */
public class CmdRand extends Instruction {

   private final OptionParser parser;
   private SecureRandom secrnd;
   public static String CHAR_MAJ = "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
         CHAR_MIN = "abcdefghijklmnopqrstuvwxyz",
         CHAR_NB  = "1234567890",
         CHAR_SPE = "&#@$%|+-*/\\()[]<>~^.,;!?";

   public CmdRand(Contexte cont) {
      super(cont, TYPE_CMDINT, "RAND", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("m", "mode de génération").withRequiredArg().ofType(String.class)
         .defaultsTo("string").describedAs("number|string")
         .withValuesConvertedBy(regex("number|nbr|string|str|secure|sec|UUID|uuid"));
      parser.accepts("d", "graine").withRequiredArg().ofType(Long.class)
         .describedAs("seed");
      parser.accepts("i", "borne inf. (mode number)").withRequiredArg().ofType(Long.class)
         .defaultsTo(0l).describedAs("borne");
      parser.accepts("s", "borne sup. (mode number)").withRequiredArg().ofType(Long.class)
         .defaultsTo(1000000000l).describedAs("borne");
      parser.accepts("t", "taille (mode string ou secure)").withRequiredArg().ofType(Integer.class)
         .defaultsTo(16).describedAs("taille");
      parser.accepts("c", "famille de caractères (mode string)").withRequiredArg().ofType(String.class)
         .defaultsTo("a").describedAs("charset").withValuesConvertedBy(regex("[Aa0%]+"));
      parser.accepts("r", "base (mode secure)").withRequiredArg().ofType(Integer.class)
         .defaultsTo(16).describedAs("borne");
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
         String mode = options.has("m") ? (String) options.valueOf("m") : "number", // par défaut
                val = "";

         if (mode.matches("number|nbr")) {            // mode NOMBRE
            long bMin = ((Long)options.valueOf("i")),
                 bMax = ((Long)options.valueOf("s"));
            if (bMin >= bMax || bMin < 0)
               return cont.erreur("RAND",
                     "mode number : borne inf. négative ou  borne sup. inférieure à borne inf.", lng);
            // Calucul du nombre aléatoire
            val = Long.toString((long) (bMin + Math.random() * (bMax - bMin)));
         }
         else if (mode.matches("string|str")) {     // mode CHAINE (défaut)
            int size = ((Integer)options.valueOf("t")); // taille de la chaîne
            String cset = (String)options.valueOf("c"); // famille de caractères
            // Calucul de la chaîne de caractère aléatoire

            if(cset.indexOf('A') >= 0) val += CHAR_MAJ;
            if(cset.indexOf('a') >= 0) val += CHAR_MIN;
            if(cset.indexOf('0') >= 0) val += CHAR_NB;
            if(cset.indexOf('%') >= 0) val += CHAR_SPE;
            if(size < 1 || val.length() < 1)
               return cont.erreur("RAND",
                     "mode string : taille de chaîne négative ou charset invalide", lng);

            StringBuilder sb = new StringBuilder(size);
            for (int i = 0; i < size; i++)
               sb.append(val.charAt((int) (Math.random() * val.length())));
            val = sb.toString();
         }
         else if (mode.matches("secure|sec")) {     // mode SECURE
            if (secrnd == null) secrnd = new SecureRandom();
            if (options.has("d")) secrnd.setSeed(((Long)options.valueOf("d")));
            byte[] brnd = new byte[((Integer)options.valueOf("t"))];
            secrnd.nextBytes(brnd);
            val = new BigInteger(brnd).abs().toString(((Integer)options.valueOf("r")));
         }
         else if (mode.matches("UUID|uuid")) {      // mode UUID
            val = UUID.randomUUID().toString();
         }
         // else... autres modes de génération
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

         // Retour
         vr.setSubValue(val);
         vr.setRet();
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("RAND", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("RAND", "ERREUR IOException : " + ex.getMessage(), lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  rand        Génère un nombre ou une chaîne aléatoire\n";
   }
}// class

