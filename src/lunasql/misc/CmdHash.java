package lunasql.misc;

import static joptsimple.util.RegexMatcher.regex;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.lib.Security;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande HASH <br>
 * (Interne) Calcul le Hash MD5 du message fourni
 * @author M.P.
 */
public class CmdHash extends Instruction {

   private final OptionParser parser;

   public CmdHash(Contexte cont) {
      super(cont, TYPE_CMDPLG, "HASH", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("f", "empreinte de fichier");
      parser.accepts("r", "format de l'empreinte").withRequiredArg().ofType(String.class)
         .defaultsTo("hex").describedAs("hex|b64|utf").withValuesConvertedBy(regex("hex|b64|utf"));
      parser.accepts("a", "algorithme de hashage").withRequiredArg().ofType(String.class)
         .defaultsTo("SHA-256").describedAs("MD2|MD5|SHA-1|SHA-256|SHA-384|SHA-512")
         .withValuesConvertedBy(regex("MD2|MD5|SHA-1|SHA-256|SHA-384|SHA-512"));
      parser.accepts("p", "mot de passe");
      parser.nonOptions("chaîne").ofType(String.class);
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
         return cont.exception("HASH", "erreur d'option : " + ex.getMessage(), lng, ex);
      }

         // Exécution avec autres options
      try {
         Valeur vr = new ValeurDef(cont);
         List<?> lc = options.nonOptionArguments();
         if (lc.isEmpty()) return cont.erreur("HASH", "un message ou un fichier est attendu", lng);

         String val;
         Security sec = new Security();
         sec.setHashAlgo((String) options.valueOf("a"));
         String frm = (String) options.valueOf("r");
         if (options.has("f")) { // hash de fichier
            File f = new File((String) lc.get(0));
            if (!f.isFile() || !f.canRead())
               return cont.erreur("HASH", "fichier '"+ f.getAbsolutePath() +"' inaccessible", lng);
            val = sec.getHashFile(f, frm);
         }
         else if (options.has("p")) { // dérivation de mot de passe
            sec.deriveString(listToString(lc));
            val = sec.getHash(frm);
         }
         else { // hash de chaîne en commande
            val = sec.getHashStr(listToString(lc), frm);
         }
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

         // Retour
         vr.setSubValue(val);
         vr.setRet();
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (IOException|NoSuchAlgorithmException|InvalidKeySpecException ex){
         return cont.erreur("HASH", "ERREUR " + ex.getClass().getSimpleName() + " : " +
               ex.getMessage() , lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  hash        Calcule l'empreinte cryptographique d'un message\n";
   }
}// class
