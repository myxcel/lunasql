package lunasql.cmd;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import lunasql.Config;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Security;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande EXEC <br> (Interne) Exécution d'un fichier de commandes
 * @author M.P.
 */
public class CmdExec extends Instruction {

   private static int deep = 0; // compteur d'appels sur la pile

   public CmdExec(Contexte cont) {
      super(cont, TYPE_CMDINT, "EXEC", "@");
   }

   public CmdExec(Contexte cont, String name, String alias) {
      super(cont, TYPE_CMDINT, name, alias);
   }

   @Override
   public int execute() {
      // Contrôle de la profondeur de la pile pour éviter StackOverflow
      if (deep > Config.CF_MAX_CALL_DEEP) {
         cont.erreur("EXEC", "Halte là ! Exécution circulaire irréfléchie dépassant " +
               Config.CF_MAX_CALL_DEEP + " appels\n" +
               "(sachez ce que vous faites quand vous jouez avec la récursivité !)", lng);
         return RET_EXIT_SCR; // sortie script obligatoire
      }

      if (getLength() < 2)
         return cont.erreur("EXEC", "un nom de fichier SQL ou ScriptEngine au moins est requis\n"
               + "\t\tou bien en ligne commandes : - (sql) ou + (SE)", lng);

      URL purl = null;
      boolean cry = false, zip = false, jar = false;
      String sname = getArg(1), pname = null, ext;

      if (sname.equals("-")) ext = "SQL"; // commande SQL en ligne
      else if (sname.startsWith("+")) {
         ext = sname.length() == 1 ? cont.getEvalEngineExtens() : sname.substring(1);
      } // commande SE en ligne
      else {
         // Extraction de l'extension ext
         ext = cont.getEvalEngineExtens(); // Javascript par défaut
         int id = sname.lastIndexOf('.');
         if (id > 0) {
            ext = sname.substring(id + 1).toUpperCase();
            if (ext.equals("GZ")) { // mise au format GZip
               sname = sname.substring(0, id);
               zip = true;
            }
            else if (ext.equals("CRY")) {
               sname = sname.substring(0, id);
               cry = true;
            }
         }
         id = sname.lastIndexOf('.');
         if (id > 0 && id < sname.length() - 1) ext = sname.substring(id + 1);
         if (zip) sname += ".gz";
         else if (cry) sname += ".cry";

         // Détermination du chemin absolu pname
         if (sname.matches("^jar:.*$")) {  // resource en jar
            sname = sname.substring(4);
            pname = sname;
            jar = true;
         } else if (sname.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$")) {
            pname = sname;
            try {
               purl = new URL(pname);
            } catch (MalformedURLException ex) {
               return cont.exception("EXEC", "ERREUR MalformedURLException : " + ex.getMessage(), lng, ex);
            }
         } else {
            File fname = new File(sname);
            if (fname.isFile()) pname = sname; // si script existe en rép., on le prend
            else { // sinon, on va à la pêche dans ENV_SCR_PATH
               String[] path = cont.getVar(Contexte.ENV_SCR_PATH).split(File.pathSeparator);
               // Parcours du path, on s'arrête au 1er fichier trouvé
               boolean hasf = false;
               for (int i = 0; i < path.length && !hasf; i++) {
                  if (!path[i].endsWith(File.separator)) path[i] += File.separator;
                  fname = new File(path[i] + sname);
                  if (fname.isFile()) { // Existe + est un fichier
                     pname = fname.getAbsolutePath();
                     hasf = true;
                  }
               }
               if (!hasf) { // aucun script trouvé
                  StringBuilder ss = new StringBuilder();
                  ss.append("\n  - ").append(new File(".").getAbsolutePath());
                  for (String p : path) ss.append("\n  - ").append(p); // Path suivant
                  return cont.erreur("EXEC", "aucun script '" + sname + "' trouvé dans les répertoires :"
                        + ss.toString(), lng);
               }
            }
            try {
               // Vérification de la signature du fichier
               if (!verifySign(fname, zip))
                  return cont.erreur("EXEC", "signature erronée du script '" + sname + "'", lng);
            } catch (IllegalArgumentException ex) {
               return cont.erreur("EXEC", "signature invalide du script '" + sname +
                     "' (" + ex.getMessage() + ")", lng);
            } catch (IOException|NoSuchAlgorithmException|InvalidKeyException|
                  SignatureException ex) {
               return cont.erreur("EXEC", "ERREUR" + ex.getClass().getSimpleName() + " : " + ex.getMessage(),
                     lng);
            }
         }
      }

      /*
       * Script LunaSQL (extension SQL)
       */
      if (ext.matches(Config.CT_SQL_EXT)) {
         deep++;
         Valeur vr = new ValeurDef(cont);
         Lecteur lec = null;
         int ret = RET_CONTINUE;
         if (sname.equals("-")) {         // en ligne de commande
            lec = new Lecteur(cont, getArg(2), setLecVars(3));
            Valeur vr2 = cont.getValeur();
            vr.setSubValue(vr2 == null ? null : vr2.getSubValue());
         }
         else {                         // en fichier
            InputStream inp = null;
            try {
               HashMap<String, String> vars = setLecVars(cry ? 3 : 2);
               vars.put(Contexte.LEC_SCR_NAME, sname);
               vars.put(Contexte.LEC_THIS, sname);
               vars.put(Contexte.LEC_SUPER, "");
               vars.put(Contexte.LEC_SCR_PATH, pname);

               if (purl == null) {
                  if (jar) {
                     inp = getClass().getResourceAsStream(sname);
                     if (inp == null)
                        return cont.erreur("EXEC", "Ressource jar '" + sname + "' non disponible", lng);
                  }
                  else inp = new FileInputStream(pname);
               }
               else inp = purl.openStream();

               if (cry) {
                  if (getLength() < 3)
                     return cont.erreur("EXEC", "avec script chiffré, clef attendue en 1er argument", lng);

                  byte[] iv = new byte[16], key = Security.hexdecode(getArg(2));
                  if (key == null)
                     return cont.erreur("EXEC", "clef de déchiffrement fournie nulle", lng);

                  inp.read(iv);
                  Cipher c = getCipher(key, iv);
                  inp = new CipherInputStream(inp, c);
               }
               if (zip || cry) inp = new GZIPInputStream(inp);

               long tm = System.currentTimeMillis();
               lec = new Lecteur(cont, new InputStreamReader(inp, cont.getVar(Contexte.ENV_FILE_ENC)), vars);
               tm = System.currentTimeMillis() - tm;
               vr.setDispValue("-> script '" + pname + "' exécuté en " + SQLCnx.frmDur(tm), Contexte.VERB_BVR);
               Valeur vr2 = cont.getValeur();
               vr.setSubValue(vr2 == null ? null : vr2.getSubValue());
            } catch (IOException|NoSuchPaddingException|NoSuchAlgorithmException|InvalidAlgorithmParameterException|
                     InvalidKeyException ex) {
               ret = cont.erreur("EXEC", "ERREUR" + ex.getClass().getSimpleName() + " : " + ex.getMessage(),
                     lng);
            }
            finally {
               try {
                  if (inp != null) inp.close();
               }
               catch (IOException e) {}
            }
         }
         vr.setRet();
         cont.setValeur(vr);
         deep--;
         return lec == null ? ret : lec.getCmdState();

         /*
          * Script ScriptEngine
          */
      }
      else {
         Valeur vr = new ValeurDef(cont);
         ScriptEngine se = cont.getEvalEngine(ext);
         if (se == null)
            return cont.erreur("EXEC", "Objet ScriptEngine '" + ext + "' inaccessible."
                  + "\nAvez-vous ajouté la bibliothèque correspondante au CLASSPATH ?", lng);

         se.put("sql_connex", cont.getConnex());
         InputStream inp = null;
         int ret = RET_CONTINUE;
         try {
            long tm = System.currentTimeMillis();
            Object o;
            if (sname.startsWith("+")) { // en ligne de commande
               se.put(Contexte.LEC_SCR_ARGS, getCommand(3));
               o = se.eval(getArg(2));
            }
            else {                 // en fichier
               se.put(Contexte.LEC_SCR_NAME, sname);
               se.put(Contexte.LEC_SCR_PATH, pname);
               se.put(Contexte.LEC_SCR_ARGS, getCommand(cry ? 3 : 2)); // si chiffré, clef en arg 2

               if (purl == null) {
                  if (jar) {
                     inp = getClass().getResourceAsStream(sname);
                     if (inp == null)
                        return cont.erreur("EXEC", "Ressource jar '" + sname + "' non disponible", lng);
                  }
                  else inp = new FileInputStream(pname);
               }
               else inp = purl.openStream();

               if (cry) {
                  if (getLength() < 3)
                     return cont.erreur("EXEC", "avec script chiffré, clef attendue en 1er argument", lng);

                  byte[] iv = new byte[16], key = Security.hexdecode(getArg(2));
                  if (key == null)
                     return cont.erreur("EXEC", "clef de déchiffrement fournie nulle", lng);

                  inp.read(iv);
                  Cipher c = getCipher(key, iv);
                  inp = new CipherInputStream(inp, c);
               }
               if (zip || cry) inp = new GZIPInputStream(inp);
               o = se.eval(new InputStreamReader(inp, cont.getVar(Contexte.ENV_FILE_ENC)));
            }
            if (o != null) {
               vr.setSubValue(o.toString());
               if (o instanceof Integer)
                  vr.setDispValue("Objet entier returné : " + o);
               else if (o instanceof Double)
                  vr.setDispValue("Objet double returné : " + o);
               else if (o instanceof String)
                  vr.setDispValue("Objet chaîne returné : " + o);
               else
                  vr.setDispValue("Objet returné (" + o.getClass().toString() + ") :\n" + o.toString());
            }
            tm = System.currentTimeMillis() - tm;
            vr.setDispValue("-> script '" + pname + "' exécuté en " + SQLCnx.frmDur(tm),
                  Contexte.VERB_BVR);
            vr.setRet();
            cont.setValeur(vr);
         }
         catch (ScriptException ex) {
            ret = cont.erreur("EXEC", "Erreur ScriptException :\n" +
                  ex.getMessage().replace(":", ":\n"), lng);
         }
         catch (IllegalArgumentException ex) {
            return cont.erreur("EXEC", ex.getMessage(), lng);
         }
         catch (IOException|NoSuchPaddingException|NoSuchAlgorithmException|InvalidAlgorithmParameterException|
               InvalidKeyException ex) {
            ret = cont.exception("EXEC", "ERREUR " + ex.getClass().getSimpleName() + " : " +
                  ex.getMessage(), lng, ex);
         }
         catch (Exception ex) {
            ret = cont.exception("EXEC", "ERREUR Runtime : " + ex.getMessage(), lng, ex);
         }
         finally {
            try { if (inp != null) inp.close(); }
            catch (IOException ex) {}
         }
         return ret;
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc() {
      return "  exec, @     Exécute le fichier de commandes SQL ou SE en paramètre\n";
   }

   /**
    * Affectation des paramètres en mode SQL
    */
   private HashMap<String, String> setLecVars(int n) {
      HashMap<String, String> vars = new HashMap<>();
      StringBuilder args = new StringBuilder();
      vars.put(Contexte.LEC_SCR_ARG_NB, Integer.toString(getLength() - n));
      for (int i = n; i < getLength(); i++) {
         vars.put(Contexte.LEC_SCR_ARG + (i - n + 1), getArg(i));
         args.append(getArg(i)).append(' ');
      }
      if (args.length() > 1) args.deleteCharAt(args.length() - 1);
      vars.put(Contexte.LEC_SCR_ARG_LS, args.toString());
      return vars;
   }

   /**
    * Préparation du chiffre
    */
   private Cipher getCipher(byte[] bkey, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
         InvalidAlgorithmParameterException, InvalidKeyException {
      Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
      c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(bkey, "AES"), new IvParameterSpec(iv));
      return c;
   }

   /**
    * Vérification de la signature du fichier
    * @param f le fichier
    * @param zip si le fichier est zippé (Gzip)
    * @return true si signature ok, false sinon
    */
   private boolean verifySign(File f, boolean zip) throws IOException, NoSuchAlgorithmException, InvalidKeyException,
         SignatureException {
      String policy = cont.getVar(Contexte.ENV_SIGN_POLICY);
      if (policy.equals("0")) return true;

      Security sec = new Security();
      String ms, pk;
      File fsg = new File(f.getAbsolutePath() + ".sig");
      if (fsg.exists()) { // external signature
         StringBuilder sbsg = new StringBuilder();
         for (String line : Files.readAllLines(fsg.toPath())) {
            if (!(line = line.trim()).startsWith("#")) sbsg.append(line);
         }
         byte[] totsg = Security.b64decode(sbsg.toString());
         if (totsg == null || totsg.length != 102) // 32 + 6 + 64
               throw new IllegalArgumentException("signature nulle ou invalide");

         byte[] bpk = Arrays.copyOfRange(totsg, 0, 32), bms = Arrays.copyOfRange(totsg, 32, 38),
                bsig = Arrays.copyOfRange(totsg, 38, 102);
         sec.setPublicKey(bpk);
         if (!sec.verify(f, bms, bsig)) return false;
         pk = Security.b64encode(bpk);
         ms = Security.hexencode(bms);
      }
      else { // embedded signature
         String[] pkarr = new String[2];
         if (zip) {
            try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(f));
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
               int nb;
               byte[] buffer = new byte[1024];
               while ((nb = in.read(buffer, 0, buffer.length)) > 0) out.write(buffer, 0, nb);
               byte[] content = out.toByteArray();
               if (!sec.verifyPk(content, pkarr)) return false;
            }
         }
         else if (!sec.verifyPk(f, pkarr)) return false;
         pk = pkarr[0];
         ms = pkarr[1];
      }
      cont.printSignInfos(pk, ms);

      // Confiance dans la clef
      String[] tkey = cont.getVar(Contexte.ENV_SIGN_KEY).split("\\|");
      if (tkey.length < 2 || !tkey[0].equals(pk)) {
         if (cont.getVar(Contexte.ENV_SIGN_TRUST).contains(pk)) return true;
         if (policy.equals("1")) cont.errprintln("Note : signature du script valide mais clef non fiable");
         else return false;
      }
      return true;
   }
}// class

