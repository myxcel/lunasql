package lunasql.misc;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.Config;
import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.lib.Security;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

/**
 * Commande SIGN <br>
 * (Interne) Signe le message donné (clef EdDSA de 256 bits / hash de 512 bits)
 * @author M.P.
 */
public class CmdSign extends Instruction {

   private final OptionParser parser;

   public CmdSign(Contexte cont) {
      super(cont, TYPE_CMDPLG, "SIGN", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("g", "génération d'une clef");
      parser.accepts("p", "changement de mot de passe");
      parser.accepts("f", "signer un fichier").withRequiredArg().ofType(File.class).describedAs("file");
      parser.accepts("d", "signature détachée");
      parser.nonOptions("texte").ofType(String.class);
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
         return cont.exception("SIGN", "erreur d'option : " + ex.getMessage(), lng, ex);
      }

      // Exécution avec autres options
      Valeur vr = new ValeurDef(cont);
      try {
         Security sec = new Security();
         String val = null;

         if (options.has("g")) { // génération
            if (!cont.getVar(Contexte.ENV_SIGN_KEY).isEmpty())
               return cont.erreur("SIGN", "clef privée déjà définie en " + Contexte.ENV_SIGN_KEY, lng);

            String psw1 = cont.getInput("Mot de passe de la clef : ", '*'),
                   psw2 = cont.getInput("Saisissez-le à nouveau  : ", '*');
            if (psw1 == null) return cont.erreur("SIGN", "aucun mot de passe fourni", lng);
            if (!psw1.equals(psw2)) return cont.erreur("SIGN", "les mots de passe sont différents", lng);

            sec.genKeyPair();
            sec.setSecretString(psw1);
            String pri = sec.crypt(sec.getPrivateKey()), pub = sec.getPublicKeyS();
            cont.setVar(Contexte.ENV_SIGN_KEY,pub + "|" + pri);
            val = pub;
         }
         else {
            String[] tkey = cont.getVar(Contexte.ENV_SIGN_KEY).split("\\|");
            if (tkey.length != 2)
               return cont.erreur("SIGN", "aucune clef privée définie en " + Contexte.ENV_SIGN_KEY, lng);

            String psw = cont.getInput("Mot de passe de la clef : ", '*');
            if (psw == null) return cont.erreur("SIGN", "aucun mot de passe fourni", lng);

            sec.setSecretString(psw);
            byte[] pk = Security.b64decode(tkey[0]), ms = Security.getTimeMs();
            sec.setPublicKey(pk);
            sec.setPrivateKey(sec.decrypt(tkey[1]));

            // Changement de mot de passe
            if (options.has("p")) { // changement de mot de passe
               String psw1 = cont.getInput("Nouveau mot de passe    : ", '*'),
                      psw2 = cont.getInput("Saisissez-le à nouveau  : ", '*');
               if (psw1 == null) return cont.erreur("SIGN", "aucun mot de passe fourni", lng);
               if (!psw1.equals(psw2)) return cont.erreur("SIGN", "les mots de passe sont différents", lng);

               sec.setSecretString(psw1);
               String pri = sec.crypt(sec.getPrivateKey()), pub = sec.getPublicKeyS();
               cont.setVar(Contexte.ENV_SIGN_KEY,pub + "|" + pri);
               if (cont.getVerbose() >= Contexte.VERB_MSG)
                  cont.println("Mot de passe de la clef de signature modifié (pensez à mettre à jour le fichier de config.)");
            }

            // Signature de fichier/string
            else {
               if (cont.getVerbose() >= Contexte.VERB_BVR) cont.println("Clef de signature : " + tkey[0]);

               File f = null;
               boolean tofile = options.has("f");
               if (tofile) {
                  f = (File) options.valueOf("f");
                  if (!f.isFile() || !f.canRead())
                     return cont.erreur("SIGN", "fichier '" + f.getAbsolutePath() + "' inaccessible", lng);
               }

               // Signature
               boolean detach = options.has("d");
               byte[] sig = tofile ? sec.sign(f, ms) : sec.sign(listToString(options.nonOptionArguments()), ms);
               String sbsig = Security.getSignatureStr(pk, ms, sig);

               if (tofile) {
                  FileWriter writer = detach ?
                        new FileWriter(f.getAbsolutePath() + ".sig") : new FileWriter(f, true);
                  if (detach) {
                     writer.append("# Signature numérique détachée de LunaSQL ").append(Config.APP_VERSION_NUM).append("\n");
                     writer.append("# Cette signature garantit l'authenticité et l'intégrité du fichier\n");
                     writer.append("# Vérifier par :  verify -f ").append(f.getName()).append(" -d\n");
                  }
                  else writer.append(Security.SIG_EMBED_BEGIN);
                  writer.append(sbsig);
                  if (!detach) writer.append(Security.SIG_EMBED_END);
                  writer.close();
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
               }
               else {
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
               }
               val = sbsig;
            }
         }

         // Retour
         vr.setSubValue(val);
         vr.setRet();
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (IllegalArgumentException ex) {
         return cont.erreur("SIGN", ex.getMessage(), lng);
      }
      catch (NoSuchAlgorithmException|InvalidKeyException|InvalidKeySpecException|SignatureException|
             NoSuchPaddingException|BadPaddingException|IllegalBlockSizeException|IOException ex) {
         return cont.erreur("SIGN", "ERREUR " + ex.getClass().getSimpleName() + " : " +
               ex.getMessage(), lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  sign        Signe numériquement un message par EdDSA\n";
   }
}// class
