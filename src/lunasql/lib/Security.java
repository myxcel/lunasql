 /*
 * SecurityTools
 * @Created on 1 févr. 2004<br>
 * @author : MP
 */
package lunasql.lib;

import lunasql.Config;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.Utils;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Security {

   public static final char[] HEXCHARSET = "0123456789ABCDEF".toCharArray();
   public static final String HCHARSET =
         "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~#$%&@+-*/,.:;|_<=>'\"`^[]{}!? "; // Taille 92
   public static final String SIG_EMBED_BEGIN = "\n\n/* $$ BEGIN SIGNATURE $$\n",
                              SIG_EMBED_END   = "\n * $$ END SIGNATURE $$ */\n";


   private String hashAlgo = "SHA-1"; // MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
   private String cipherAlgo = "AES"; // AES, ARCFOUR/RC4, Blowfish, DES, DESede, PBEWith<digest>And<encryption>
   private MessageDigest msgDigest;
   private byte[] hash;
   private Key secretKey;
   private EdDSAPrivateKey privateKey;
   private EdDSAPublicKey publicKey;

   /*
    * Fixation de la clef à partir d'une chaîne (dérivation de clef)
    */
   public void deriveString(String pswd) throws NoSuchAlgorithmException, InvalidKeySpecException {
      if (pswd == null) throw new IllegalArgumentException("mot de passe de chiffrement null");

      PBEKeySpec spec = new PBEKeySpec(pswd.toCharArray(), "Ft91HNnx".getBytes(), 10000, 160);
      SecretKeyFactory fac = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      hash = fac.generateSecret(spec).getEncoded();
   }
   public void setSecretString(String pswd) throws NoSuchAlgorithmException, InvalidKeySpecException {
      deriveString(pswd);
      setSecretKey(hash);
   }

   /*
    * Méthode de hachage d'un fichier
    */
   public String getHashFile(File file) throws IOException {
      return getHashFile(file, "hex");
   }

   public String getHashFile(File file, String frm) throws IOException {
      InputStream is = null;
      boolean err = false;
      try {
         msgDigest = MessageDigest.getInstance(hashAlgo);
         is = new DigestInputStream(new FileInputStream(file), msgDigest);
         while(is.read() >= 0) {}
         is.close();
         hash = msgDigest.digest();
      }
      catch (NoSuchAlgorithmException ex) {
         err = true;
      }
      finally {
         if (is != null) is.close();
      }
      if (err) return null;
      return getHash(frm);
   }

   /*
    * Methode de hachage d'une chaîne
    */
   public String getHashStr(String msg, String frm) {
      return getHashStr(msg.getBytes(), frm);
   }

   public String getHashStr(byte[] msg, String frm) {
      try {
         msgDigest = MessageDigest.getInstance(hashAlgo);
         msgDigest.update(msg);
         hash = msgDigest.digest();
      }
      catch (NoSuchAlgorithmException ex) {}
      return getHash(frm);
   }

   /**
    * Méthode de chiffrement à partir de la clef en chaîne hex
    * @param plaintext le texte
    * @param iv le vecteur d'initialisation
    * @return la chaîne hex
    * @throws InvalidKeyException si c'est le cas
    * @throws InvalidAlgorithmParameterException si c'est le cas
    */
   public String crypt(String plaintext, byte[] iv)
         throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
         InvalidAlgorithmParameterException, IOException {
      if (plaintext == null) return null;
      return hexencode(crypt(plaintext.getBytes(), iv));
   }

   /**
    * Méthode de chiffrement à partir de la clef en chaîne hex
    * @param plaintext le texte
    * @param iv le vecteur d'initialisation
    * @param fiv le format de l'IV
    * @param frm le format de sortie (b64 ou hex)
    * @param nbc le nombre de caractères
    * @return la chaîne hex
    * @throws InvalidKeyException si c'est le cas
    * @throws InvalidAlgorithmParameterException si c'est le cas
    * @throws IOException si c'est le cas
    */
   public String crypt(String plaintext, String iv, String fiv, String frm, int nbc)
         throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
         InvalidAlgorithmParameterException, IOException {
      if (plaintext == null) return null;
      byte[] biv = "b64".equals(fiv) ? b64decode(iv) : ("hex".equals(fiv) ? hexdecode(iv) : iv.getBytes());
      byte[] c = crypt(plaintext.getBytes(), biv);
      if ("b64".equals(frm)) return b64encode(c, nbc);
      if ("hex".equals(frm)) return hexencode(c, nbc);
      return new String(c);
   }

   /**
    * Méthode de chiffrement en mode CBC à partir de la clef en byte array
    * Le message est d'abord compressé par GZip
    * @param plaintext le texte
    * @param iv le vecteur d'initialisation
    * @return le message chiffré en byte array
    * @throws InvalidKeyException si c'est le cas
    * @throws InvalidAlgorithmParameterException si c'est le cas
    * @throws IOException si c'est le cas
    */
   public byte[] crypt(byte[] plaintext, byte[] iv)
         throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
         InvalidAlgorithmParameterException, IOException {
      // Préparation
      Cipher cipher = Cipher.getInstance(cipherAlgo + "/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

      // Compression et chiffrement
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
           CipherOutputStream cpos = new CipherOutputStream(baos, cipher);
           GZIPOutputStream gzos = new GZIPOutputStream(cpos)) {
         gzos.write(plaintext);
         gzos.flush();
         gzos.close(); // must close before toByteArray()
         return baos.toByteArray();
      }
   }

   /**
    * Méthode de chiffrement d'une clef en mode ECB à partir de la clef en byte array
    * @param plainkey le texte
    * @return la clef chiffrée en string
    * @throws InvalidKeyException si c'est le cas
    */
   public String crypt(byte[] plainkey)
         throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
         IllegalBlockSizeException {
      // Préparation
      Cipher cipher = Cipher.getInstance(cipherAlgo + "/ECB/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      return b64encode(cipher.doFinal(plainkey));
   }

   /**
    * Méthode de chiffrement de fichier en mode CBC à partir de la clef
    * Le contenu est d'abord compressé par GZip
    * @param fpla le fichier source
    * @param fcry le fichier chiffré (sortie)
    * @return le nombre d'octets lus
    * @throws InvalidKeyException si c'est le cas
    * @throws InvalidAlgorithmParameterException si c'est le cas
    * @throws IOException si c'est le cas
    */
   public long crypt(File fpla, File fcry)
         throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
         InvalidKeyException, IOException {
      if (!fpla.isFile() || !fpla.canRead())
         throw new IllegalArgumentException("fichier '" + fpla.getAbsolutePath() + "' inaccessible");

      long nbtot = 0;
      // Préparation
      byte[] iv = new byte[16];
      new SecureRandom().nextBytes(iv);
      Cipher cipher = Cipher.getInstance(cipherAlgo + "/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

      // Compression et chiffrement
      try (FileInputStream in = new FileInputStream(fpla);
           FileOutputStream out = new FileOutputStream(fcry);
           CipherOutputStream cpos = new CipherOutputStream(out, cipher);
           GZIPOutputStream gzos = new GZIPOutputStream(cpos)) {
         byte[] buffer = new byte[1024];
         int nb;
         out.write(iv);
         while ((nb = in.read(buffer)) != -1) {
            nbtot += nb;
            gzos.write(buffer, 0, nb);
         }
         gzos.flush();
      }
      return nbtot;
   }

   /**
    * Déchiffrement du message chiffré
    * @param ciphertext le message chiffré
    * @param iv le vecteur d'initialisation
    * @return le message clair
    * @throws InvalidKeyException si c'est le cas
    * @throws InvalidAlgorithmParameterException si c'est le cas
    */
   public String decrypt(String ciphertext, byte[] iv)
         throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
         InvalidAlgorithmParameterException {
      byte[] b = decrypt(hexdecode(ciphertext), iv);
      return b == null ? null : new String(b);
   }

   /**
    * Déchiffrement du message chiffré
    * @param ciphertext le message chiffré
    * @param iv le vecteur d'initialisation
    * @param frm le format chiffré
    * @return le message clair
    * @throws InvalidKeyException si c'est le cas
    * @throws InvalidAlgorithmParameterException si c'est le cas
    */
   public String decrypt(String ciphertext, byte[] iv, String frm)
         throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
         InvalidAlgorithmParameterException {
      byte[] b ;
      if ("b64".equals(frm)) b = decrypt(b64decode(ciphertext), iv);
      else if ("hex".equals(frm)) b = decrypt(hexdecode(ciphertext), iv);
      else b = ciphertext.getBytes();
      return b == null ? null : new String(b);
   }

   /**
    * Déchiffrement du message chiffré
    * @param ciphertext le message chiffré
    * @param iv le vecteur d'initialisation
    * @return le message clair en tableau de bytes
    * @throws InvalidKeyException si c'est le cas
    * @throws InvalidAlgorithmParameterException si c'est le cas
    */
   public byte[] decrypt(byte[] ciphertext, byte[] iv)
         throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
         InvalidAlgorithmParameterException  {
      // Préparation
      Cipher cipher = Cipher.getInstance(cipherAlgo + "/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

      // Déchiffrement et décompression
      try {
         ByteArrayInputStream bais = new ByteArrayInputStream(ciphertext);
         CipherInputStream cpis = new CipherInputStream(bais, cipher);
         GZIPInputStream gzis = new GZIPInputStream(cpis);

         // Écriture en byte[] pas aisée (Guava, Common IO ?)
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         int n;
         byte[] tmp = new byte[1024];
         while ((n = gzis.read(tmp, 0, tmp.length)) != -1) baos.write(tmp, 0, n);
         baos.flush();
         return baos.toByteArray();
      }
      catch (IOException e) {
         return null;
      }
   }

   /**
    * Déchiffrement de la clef chiffrée
    * @param cipherkey la clef déchiffrée
    * @return le message clair en base64
    * @throws InvalidKeyException si c'est le cas
    */
   public byte[] decrypt(String cipherkey)
         throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException,
         IllegalBlockSizeException {
      // Préparation
      Cipher cipher = Cipher.getInstance(cipherAlgo + "/ECB/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      return cipher.doFinal(b64decode(cipherkey));
   }

   /**
    * Déchiffrement du fichier chiffré
    * @param fcry le fichier chiffré
    * @param fpla le fichier clair (sortie)
    * @return le nombre d'octets lus et déchiffrés
    * @throws InvalidAlgorithmParameterException si c'est le cas
    * @throws InvalidKeyException si c'est le cas
    * @throws IOException si c'est le cas
    */
   public long decrypt(File fcry, File fpla) throws NoSuchPaddingException, NoSuchAlgorithmException,
         InvalidAlgorithmParameterException, InvalidKeyException, IOException {
      if (!fcry.isFile() || !fcry.canRead())
         throw new IllegalArgumentException("fichier '" + fcry.getAbsolutePath() + "' inaccessible");

      long nbtot = 0;
      // Préparation
      byte[] iv = new byte[16];
      FileInputStream in = new FileInputStream(fcry);
      nbtot += in.read(iv);
      Cipher cipher = Cipher.getInstance(cipherAlgo + "/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

      // Compression et chiffrement
      try (CipherInputStream cpis = new CipherInputStream(in, cipher);
           GZIPInputStream gzis = new GZIPInputStream(cpis);
           FileOutputStream out = new FileOutputStream(fpla);
           ) {
         byte[] buffer = new byte[1024];
         int nb;
         while ((nb = gzis.read(buffer)) != -1) {
            nbtot += nb;
            out.write(buffer, 0, nb);
         }
         out.flush();
      }
      in.close();
      return nbtot;
   }

   /**
    * Génération d'une paire de clefs pour signature
    */
   public void genKeyPair() {
      KeyPair pair = new net.i2p.crypto.eddsa.KeyPairGenerator().generateKeyPair();
      privateKey = (EdDSAPrivateKey) pair.getPrivate();  // get: getSeed()
      publicKey = (EdDSAPublicKey) pair.getPublic();     // get: getAbyte()
   }

   /**
    * Signature d'un message text
    * @param plainText le texte à signer
    * @param ms le timestamp de signature (6 octets)
    * @return un objet Property de la signature
    * @throws NoSuchAlgorithmException si c'est le cas
    * @throws InvalidKeyException si c'est le cas
    * @throws SignatureException si c'est le cas
    */
   public byte[] sign(String plainText, byte[] ms)
         throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
      Signature sig = prepareSign(true);
      sig.update(ms);
      sig.update(plainText.getBytes(StandardCharsets.UTF_8));
      return sig.sign();
   }

   public byte[] sign(File f, byte[] ms)
         throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
      Signature sig = prepareSign(true);
      sig.update(ms);

      byte[] tb = new byte[1024];
      InputStream in = new FileInputStream(f);
      int nbb;
      while ((nbb = in.read(tb)) != -1) sig.update(tb, 0, nbb);
      in.close();
      return sig.sign();
   }

   /**
    * Vérifie la signature d'un message texte
    * @param plainText le texte à vérifier
    * @param ms le timestamp de signature (6 octets)
    * @param signature la signature
    * @return true si la signature est ok, false sinon
    * @throws InvalidKeyException si c'est le cas
    * @throws SignatureException si c'est le cas
    */
   public boolean verify(String plainText, byte[] ms, byte[] signature)
         throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
      Signature sig = prepareSign(false);
      sig.update(ms);
      sig.update(plainText.getBytes(StandardCharsets.UTF_8));
      if (signature == null) throw new IllegalArgumentException("signature nulle ou invalide");
      return sig.verify(signature);
   }

   /**
    * Vérifie la signature externe d'un fichier
    * @param f le fichier
    * @param ms le timestamp
    * @param signature la signature externe
    * @return true si la signature est ok, false sinon
    * @throws InvalidKeyException si c'est le cas
    * @throws SignatureException si c'est le cas
    * @throws IOException si c'est le cas
    */
   public boolean verify(File f, byte[] ms, byte[] signature)
         throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
      Signature sig = prepareSign(false);
      sig.update(ms);

      byte[] tb = new byte[1024];
      InputStream in = new FileInputStream(f);
      int nbb;
      while ((nbb = in.read(tb)) != -1) sig.update(tb, 0, nbb);
      in.close();

      //byte[] sgdata = b64decode(signature);
      if (signature == null) throw new IllegalArgumentException("signature nulle ou invalide");
      return sig.verify(signature);
   }

   /**
    * Vérifie la signature interne d'un tableau d'octets
    * @param data le tableau
    * @param pkarr un tableau de retour [pk, ms]
    * @return true si la signature est ok, false sinon
    * @throws InvalidKeyException si c'est le cas
    * @throws SignatureException si c'est le cas
    * @throws IllegalArgumentException si autre erreur
    */
   public boolean verifyPk(byte[] data, String[] pkarr)
         throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
      byte[] content = Arrays.copyOfRange(data, 0, data.length - 191),
             footer = Arrays.copyOfRange(data, content.length, data.length);

      String sfooter = new String(footer);
      if (!sfooter.startsWith("\n\n/* $$ BEGIN SIGNATURE $$\n") || !sfooter.endsWith("\n * $$ END SIGNATURE $$ */\n"))
         throw new IllegalArgumentException("cartouche de signature non reconnu");

      byte[] totsg = b64decode(sfooter.substring(27, 165)); // 27 + (32 + 6 + 64) * 4 / 3 + 2
      if (totsg == null || totsg.length != 102) // 32 + 6 + 64
         throw new IllegalArgumentException("signature nulle ou invalide");

      byte[] pk = Arrays.copyOfRange(totsg, 0, 32), ms = Arrays.copyOfRange(totsg, 32, 38),
             bsig = Arrays.copyOfRange(totsg, 38, 102);
      setPublicKey(pk);
      if (pkarr != null && pkarr.length == 2) {
         pkarr[0] = b64encode(pk);
         pkarr[1] = hexencode(ms);
      }
      Signature sig = prepareSign(false);
      sig.update(ms);
      sig.update(content);
      return sig.verify(bsig);
   }

   /**
    * Vérifie la signature interne d'un fichier
    * @param f le fichier
    * @param pkarr un tableau de retour [pk, ms]
    * @return true si la signature est ok, false sinon
    * @throws InvalidKeyException si c'est le cas
    * @throws SignatureException si c'est le cas
    * @throws IOException si c'est le cas
    * @throws IllegalArgumentException si autre erreur
    */
   public boolean verifyPk(File f, String[] pkarr)
         throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException,
         IllegalArgumentException {
      if (f.length() > Math.pow(2,21))  // max 2 Mio
         throw new IOException("le fichier " + f.getName() + " est trop volumineux (> 2 Mio)");
      if (f.length() < 230) throw new IllegalArgumentException("cartouche de signature non reconnu");

      return verifyPk(Files.readAllBytes(f.toPath()), pkarr);
   }

   private Signature prepareSign(boolean sign) throws InvalidKeyException, NoSuchAlgorithmException {
      EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
      Signature sig = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
      if (sign) sig.initSign(privateKey);
      else sig.initVerify(publicKey);
      return sig;
   }

   /**
    * Compose the signature base64 string
    * @param pk the public key bytes
    * @param ms the timestanp bytes
    * @param sig the signature bytes
    * @return the full 2 lines signature string
    */
   public static String getSignatureStr(byte[] pk, byte[] ms, byte[] sig) {
      ByteBuffer bbuf = ByteBuffer.allocate(102).put(pk).put(ms).put(sig); // 32 + 6 + 64
      return b64encode(bbuf.array(), 68); // 68 char (base64) par ligne
   }

   /*
    * Accesseurs et mutateurs
    */
   public String getHashAlgo() {
      return hashAlgo;
   }

   public void setHashAlgo(String algo) {
      hashAlgo = algo;
   }

   public String getCipherAlgo() {
      return cipherAlgo;
   }

   public void setCipherAlgo(String algo) {
      cipherAlgo = algo;
   }

   public byte[] getHash() {
      return hash;
   }

   public String getHash(String frm) {
      if ("b64".equals(frm)) return b64encode(hash);
      if ("hex".equals(frm)) return hexencode(hash);
      return new String(hash);
   }

   public void setHash(byte[] bs) {
      hash = bs;
   }

   public byte[] getSecretKey() {
      return secretKey == null ? null : secretKey.getEncoded();
   }

   public void setSecretKey(byte[] keyData) {
      if (keyData.length > 32) {
         byte[] tmp = keyData;
         keyData = new byte[32];
         System.arraycopy(tmp, 0, keyData, 0, 32);
      }
      else if (keyData.length < 32 && keyData.length > 16) {
         byte[] tmp = keyData;
         keyData = new byte[16];
         System.arraycopy(tmp, 0, keyData, 0, 16);
      }

      secretKey = new SecretKeySpec(keyData, cipherAlgo);
   }

   /**
    * Get the private seed
    * @return the seed
    */
   public byte[] getPrivateKey() {
      return privateKey == null ? null : privateKey.getSeed();
   }
   public String getPrivateKeyS() {
      return b64encode(getPrivateKey());
   }

   public void setPrivateKey(byte[] keyData) {
      if (keyData == null) throw new IllegalArgumentException("clef privée nulle ou invalide");

      EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
      privateKey = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(keyData, spec));

      // Vérification de l'appairement avec la clef publique
      if (!Arrays.equals(privateKey.getAbyte(), getPublicKey()))
         throw new IllegalArgumentException("clef privée non appairée (mot de passe incorrect ?)");
   }

   public void setPrivateKey(String keyData) {
      setPrivateKey(b64decode(keyData));
   }

   /**
    * Get the public key
    * @return the public key
    */
   public byte[] getPublicKey() {
      return publicKey == null ? null : publicKey.getAbyte();
   }
   public String getPublicKeyS() {
      return b64encode(getPublicKey());
   }

   public void setPublicKey(byte[] keyData) {
      if (keyData == null) throw new IllegalArgumentException("clef publique nulle ou invalide");

      EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
      publicKey = new EdDSAPublicKey(new EdDSAPublicKeySpec(keyData, spec));
   }

   public void setPublicKey(String keyData) {
      setPublicKey(b64decode(keyData));
   }


   /**
    * Transforme une chaine pouvant contenir des accents dans une version sans accent
    *  @param chaine Chaine a convertir sans accent
    *  @return Chaîne dont les accents ont été supprimés
    */
   public static String sansAccent(String chaine) {
      return Normalizer.normalize(chaine, Normalizer.Form.NFKD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
   }

   /**
    * Brouillage d'une chaîne par Vigenère à 3 caractères aléatoires. Les accents sont supprimés
    * @param txt le texte à brouiller
    * @return texte brouillé
    */
   public static String brouille(String txt) {
      if (txt == null || txt.length() == 0) return null;

      txt = sansAccent(txt);
      StringBuilder stb = new StringBuilder(txt.length() + 3);
      int n = (int) (Math.random() * HCHARSET.length()),
          m = (int) (Math.random() * HCHARSET.length()),
          o = (int) (Math.random() * HCHARSET.length());
      stb.append(HCHARSET.charAt(n)).append(HCHARSET.charAt(m)).append(HCHARSET.charAt(o));

      for (int i = 0; i < txt.length(); i++) {
         int p = HCHARSET.indexOf(txt.charAt(i)), a = (i % 3 == 0 ? n : (i % 3 == 1 ? m : o)),
             c = ((p < 0 ? HCHARSET.length() - 1 : p) + a) % HCHARSET.length();
         stb.append(HCHARSET.charAt(c));
      } // Chaîne retournée
      return stb.toString();
   }

   /**
    * Débrouillage d'une chaîne brouillée par Vigenère
    * @param txt le texte à débrouiller
    * @return texte débrouillé
    */
   public static String debrouille(String txt) {
      if (txt == null || txt.length() < 3) return null;

      StringBuilder stb = new StringBuilder(txt.length() - 3);
      int n = HCHARSET.indexOf(txt.charAt(0)),
              m = HCHARSET.indexOf(txt.charAt(1)),
              o = HCHARSET.indexOf(txt.charAt(2));
      for (int i = 3; i < txt.length(); i++) {
         int a = (i % 3 == 0 ? n : (i % 3 == 1 ? m : o)),
                 c = (HCHARSET.indexOf(txt.charAt(i)) + HCHARSET.length() - a) % HCHARSET.length();
         stb.append(HCHARSET.charAt(c));
      } // Chaîne retournée (espaces supprimés)
      return stb.toString().trim();
   }

   /*
    * Outils d'écriture des tableaux de bytes sous format Base64
    */

   /**
    * Encode a byte array to a base64 string
    * @param tbb byte array
    * @param nbc number of characters per row
    * @return base64 string
    */
   public static String b64encode(byte[] tbb, int nbc) {
      String b64code = b64encode(tbb);
      if (nbc < 4 || b64code == null) return b64code;

      StringBuilder stb = new StringBuilder();
      for (int i = 0; i < b64code.length(); i++) {
         if (i != 0 && i % nbc == 0) stb.append("\n");
         stb.append(b64code.charAt(i));
      }
      return stb.toString();
   }

   /**
    * Encode a byte array to a base64 string
    * @param tbb byte array
    * @return base64 string
    */
   public static String b64encode(byte[] tbb) {
      if (tbb == null || tbb.length == 0) return null;
      return Base64.getEncoder().withoutPadding().encodeToString(tbb);

      /*StringBuilder rtb = new StringBuilder();
      int[] tmpCoded = new int[3];
      int offset = 0;
      // Parcours de la chaîne
      for (int b : tbb) {
         tmpCoded[offset++] = b;
         if (offset == 3) {
            rtb.append(B64CHARSET.charAt(((tmpCoded[0] & 0xFC) >> 2)))
                  .append(B64CHARSET.charAt(((tmpCoded[0] & 0x03) << 4) | ((tmpCoded[1] & 0xF0) >> 4)))
                  .append(B64CHARSET.charAt(((tmpCoded[1] & 0x0F) << 2) | ((tmpCoded[2] & 0xC0) >> 6)))
                  .append(B64CHARSET.charAt((tmpCoded[2] & 0x3F)));
            offset = 0;
         }
      }
      // Flush : chaîne finale
      if (offset == 1) {
         rtb.append(B64CHARSET.charAt(((tmpCoded[0] & 0xFC) >> 2)))
            .append(B64CHARSET.charAt(((tmpCoded[0] & 0x03) << 4)))
            .append("==");
      }
      else if (offset == 2) {
         rtb.append(B64CHARSET.charAt(((tmpCoded[0] & 0xFC) >> 2)))
            .append(B64CHARSET.charAt(((tmpCoded[0] & 0x03) << 4) | ((tmpCoded[1] & 0xF0) >> 4)))
            .append(B64CHARSET.charAt(((tmpCoded[1] & 0x0F) << 2)))
            .append("=");
      }
      return rtb.toString();*/
   }

   /**
    * Decode a base64 string to byte array
    * @param se base64 string
    * @return byte array
    */
   public static byte[] b64decode(String se) {
      if (se == null || se.length() == 0) return null;
      return Base64.getDecoder().decode(se.replaceAll("\\s",""));

      /*// Nettoyage des espaces, retours à la ligne...
      StringBuilder stb = new StringBuilder();
      for (int i = 0; i < se.length(); i++) {
         char c = se.charAt(i);
         if (!Character.isWhitespace(c)) stb.append(c);
      }

      int slg, lg;
      se = stb.toString();
      if ((slg = se.length()) % 4 != 0) return null;
      // Détermination de la longueur du tableau de retour
      if (se.endsWith("==")) {
         lg = 3 * slg / 4 - 2;
      }
      else if (se.endsWith("=")) {
         lg = 3 * slg / 4 - 1;
      }
      else {
         lg = 3 * slg / 4;
      }

      // Création du tableau
      byte[] rtb = new byte[lg];
      int[] coded = new int[4];
      int offset = 0;

      // Parcours de la chaîne
      for (int i = 0, j = 0; i < slg; i++, j++) {
         int b = -1;
         switch (offset) {
            case 0:
               offset++;
               coded[0] = se.charAt(i);
               coded[1] = se.charAt(++i);
               b = (((B64CHARSET.indexOf(coded[0])) & 0x3F) << 2)
                       | (((B64CHARSET.indexOf(coded[1])) & 0x30) >> 4);
               break;
            case 1:
               offset++;
               coded[2] = se.charAt(i);
               if (coded[2] == '=') return rtb;
               b = (((B64CHARSET.indexOf(coded[1])) & 0x0F) << 4)
                       | (((B64CHARSET.indexOf(coded[2])) & 0x3C) >> 2);
               break;
            case 2:
               offset = 0;
               coded[3] = se.charAt(i);
               if (coded[3] == '=') return rtb;
               b = (((B64CHARSET.indexOf(coded[2])) & 0x03) << 6)
                       | (((B64CHARSET.indexOf(coded[3])) & 0x3F));
               break;
         }
         rtb[j] = (byte) b;
      }
      return rtb;*/
   }

   /**
    * Encode a byte array to a hex string
    * @param tbb byte array
    * @return hex string
    */
   public static String hexencode(byte[] tbb) {
      if (tbb == null || tbb.length == 0) return null;
      try {
         return Utils.bytesToHex(tbb);
      } catch (IllegalArgumentException ex) {
         return null;
      }

      /*char[] hex = new char[tbb.length * 2];
      for (int j = 0; j < tbb.length; j++) {
          int v = tbb[j] & 0xFF;
          hex[j * 2] = HEXCHARSET[v >>> 4];
          hex[j * 2 + 1] = HEXCHARSET[v & 0x0F];
      }
      return new String(hex);*/
   }

   /**
    * Encode a byte array to a hex string
    * @param tbb byte array
    * @param nbc number of characters per row
    * @return hex string
    */
   public static String hexencode(byte[] tbb, int nbc) {
      String hexcode = hexencode(tbb);
      if (nbc < 2 || hexcode == null) return hexcode;
      StringBuilder stb = new StringBuilder();
      for (int i = 0; i < hexcode.length(); i++) {
         if (i != 0 && i % nbc == 0) stb.append('\n');
         stb.append(hexcode.charAt(i));
      }
      return stb.toString();
   }

   /**
    * Decode a hex string to byte array
    * @param se hex string
    * @return byte array
    */
   public static byte[] hexdecode(String se) {
      if (se == null || se.length() == 0) return null;
      if (se.length() % 2 != 0) throw new IllegalArgumentException("taille de chaîne hexadécimale incorrecte");
      return Utils.hexToBytes(se.replaceAll("\\s",""));

      /*
      int slg;
      // Nettoyage des espaces, retours à la ligne...
      StringBuilder stb = new StringBuilder();
      for (int i = 0; i < se.length(); i++) {
         char c = se.charAt(i);
         if (!Character.isWhitespace(c)) stb.append(c);
      }

      se = stb.toString();
      if ((slg = se.length()) % 2 != 0) return null;
      byte[] data = new byte[slg / 2];
      for (int i = 0; i < slg; i += 2) {
          data[i / 2] = (byte) ((Character.digit(se.charAt(i), 16) << 4)
                               + Character.digit(se.charAt(i+1), 16));
      }
      return data; */
   }

   /**
    * Calcule la valeur currentTimeMillis en 6 octets
    * @return tab. d'octets
    */
   public static byte[] getTimeMs() {
      long ms = System.currentTimeMillis();
      return new byte[] {
            (byte) (ms >> 40),
            (byte) (ms >> 32),
            (byte) (ms >> 24),
            (byte) (ms >> 16),
            (byte) (ms >> 8),
            (byte) ms
      };
   }
}//fin de la classe
