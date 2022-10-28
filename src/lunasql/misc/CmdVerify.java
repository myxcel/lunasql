package lunasql.misc;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.lib.Security;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;

/**
 * Commande VERIFY <br>
 * (Interne) Vérifie la signature du message donné
 * @author M.P.
 */
public class CmdVerify extends Instruction {

   private final OptionParser parser;

   public CmdVerify(Contexte cont) {
      super(cont, TYPE_CMDPLG, "VERIFY", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("f", "vérifier un fichier").withRequiredArg().ofType(File.class)
            .describedAs("file");
      parser.accepts("d", "signature détachée");
      parser.nonOptions("nom_var chaine|fichier").ofType(String.class);
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
         return cont.exception("VERIFY", "erreur d'option : " + ex.getMessage(), lng, ex);
      }

      // Exécution avec autres option
      try {
         Valeur vr = new ValeurDef(cont);
         Security sec = new Security();
         String pk, ms;
         if (options.has("f")) { // fichier
            File f = (File) options.valueOf("f");
            if (!f.isFile() || !f.canRead())
                  return cont.erreur("VERIFY", "fichier '" + f.getAbsolutePath() + "' inaccessible", lng);

            if (options.has("d")) { // detached
               File fsg = new File(f.getAbsolutePath() + ".sig");
               if (!fsg.canRead())
                  return cont.erreur("VERIFY", "fichier de signature détachée inaccessible", lng);

               StringBuilder sbsg = new StringBuilder();
               for (String line : Files.readAllLines(fsg.toPath())) {
                  if (!(line = line.trim()).startsWith("#")) sbsg.append(line);
               }
               byte[] totsg = Security.b64decode(sbsg.toString());
               if (totsg == null || totsg.length != 102) // 32 + 6 + 64
                  throw new IllegalArgumentException("signature invalide ou incomplète");

               byte[] bpk = Arrays.copyOfRange(totsg, 0, 32), bms = Arrays.copyOfRange(totsg, 32, 38),
                      bsig = Arrays.copyOfRange(totsg, 38, 102);
               pk = Security.b64encode(bpk);
               ms = Security.hexencode(bms);

               sec.setPublicKey(bpk);
               if (!sec.verify(f, bms, bsig))
                  return cont.erreur("VERIFY", "signature numérique erronée", lng);
            }
            else {
               String[] pkarr = new String[2];
               if (!sec.verifyPk(f, pkarr))
                  return cont.erreur("VERIFY", "signature numérique erronée", lng);
               pk = pkarr[0];
               ms = pkarr[1];
            }
         }
         else { // texte
            List<?> lc = options.nonOptionArguments();
            if (lc.size() < 2)
               return cont.erreur("VERIFY", "une option, ou un message et une signature sont attendus", lng);

            String msg = (String) lc.get(0), ssig = (String) lc.get(1);
            byte[] totsg = Security.b64decode(ssig);
            if (totsg == null || totsg.length != 102) // 32 + 6 + 64
               throw new IllegalArgumentException("signature nulle ou invalide");

            byte[] bpk = Arrays.copyOfRange(totsg, 0, 32), bms = Arrays.copyOfRange(totsg, 32, 38),
                   bsig = Arrays.copyOfRange(totsg, 38, 102);
            pk = Security.b64encode(bpk);
            ms = Security.hexencode(bms);

            sec.setPublicKey(bpk);
            if (!sec.verify(msg, bms, bsig))
               return cont.erreur("VERIFY", "signature numérique erronée", lng);
         }
         cont.printSignInfos(pk, ms);

         // Confiance dans la clef
         String[] tkey = cont.getVar(Contexte.ENV_SIGN_KEY).split("\\|");
         if ((tkey.length < 2 || !tkey[0].equals(pk)) && !cont.getVar(Contexte.ENV_SIGN_TRUST).contains(pk)) {
            String errmsg = "signature numérique valide mais clef non fiable (" + pk.substring(0,10)+ ")";
            switch (cont.getVar(Contexte.ENV_SIGN_POLICY)) {
               case "0":
                  break;
               case "1":
                  cont.errprintln("Note : " + errmsg);
                  break;
               default:
                  return cont.erreur("VERIFY", errmsg, lng);
            }
         }

         // Retour
         vr.setSubValue(Long.toString(Long.parseLong(ms,16)));
         vr.setRet();

         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (IllegalArgumentException ex) {
         return cont.erreur("VERIFY", ex.getMessage(), lng);
      }
      catch (NoSuchAlgorithmException|InvalidKeyException|SignatureException|IOException ex) {
         return cont.erreur("VERIFY", "ERREUR " + ex.getClass().getSimpleName() + " : " +
               ex.getMessage(), lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  verify      Vérifie la signature numérique d'un message\n";
   }
}// class
