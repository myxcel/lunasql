package lunasql.cmd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.PatternSyntaxException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Security;
import lunasql.lib.Tools;
import lunasql.val.ValeurDef;

/**
 * Commande FILE <br>
 * (Interne) Commande d'utilitaires d'ouverture/fermeture de fichiers
 * @author M.P.
 */
public class CmdFile extends Instruction {

   private String fil;
   private int nbl;

   public CmdFile(Contexte cont){
      super(cont, TYPE_CMDINT, "FILE", null);
   }

   @Override
   public int execute() {

      if (getLength() < 2)
         return cont.erreur("FILE", "une commande de fichier est requise", lng);

      int r;
      String cmd = getArg(1).toUpperCase(), aff = null;
      int ret = RET_CONTINUE;
      fil = ""; nbl = 0;

      if (cmd.equals("EXISTS?")) {
         if (getLength() != 3) return cont.erreur("FILE", "EXISTS? : 1 nom de fichier attendu", lng);
         fil = new File(getArg(2)).exists() ? "1" : "0";
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("FILE?")) {
         if (getLength() != 3) return cont.erreur("FILE", "FILE? : 1 nom de fichier attendu", lng);
         fil = new File(getArg(2)).isFile() ? "1" : "0";
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("DIR?")) {
         if (getLength() != 3) return cont.erreur("FILE", "DIR? : 1 nom de fichier attendu", lng);
         fil = new File(getArg(2)).isDirectory() ? "1" : "0";
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("ABSOL?")) {
         if (getLength() != 3) return cont.erreur("FILE", "ABSOL? : 1 nom de fichier attendu", lng);
         fil = new File(getArg(2)).isAbsolute() ? "1" : "0";
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("ABSOL")) {
         if (getLength() != 3) return cont.erreur("FILE", "ABSOL : 1 nom de fichier attendu", lng);
         try {
            fil = new File(getArg(2)).getCanonicalPath();
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "ABSOL : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("NAME")) {
         if (getLength() != 3) return cont.erreur("FILE", "NAME : 1 nom de fichier attendu", lng);
         fil = new File(getArg(2)).getName();
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("PWD")) {
         try {
            fil = new File(".").getCanonicalPath();
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "PWD : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("PARENT")) {
         if (getLength() != 3) return cont.erreur("FILE", "PARENT : 1 nom de fichier attendu", lng);
         fil = new File(getArg(2)).getParent();
         if (fil == null) fil = "";
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("SIZE")) {
         if (getLength() != 3) return cont.erreur("FILE", "SIZE : 1 nom de fichier attendu", lng);
         fil = Long.toString(new File(getArg(2)).length());
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("COUNT")) {
         if (getLength() < 3) return cont.erreur("FILE", "COUNT : au moins 1 nom de fichier attendu", lng);
         if ((r = fileContent("LIST", 0)) >= 0) return r;
         fil = Integer.toString(nbl);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("SEP")) {
         fil = File.separator;
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("PATHSEP")) {
         fil = File.pathSeparator;
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("LINESEP")) {
         fil = Contexte.END_LINE;
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("EXT")) {
         if (getLength() != 3) return cont.erreur("FILE", "EXT : 1 nom de fichier attendu", lng);
         String s = new File(getArg(2)).getName();
         int i = s.lastIndexOf('.');
         if (i >= 1) fil = s.substring(i + 1);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("CREATE")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("FILE", "CREATE : 1 nom de fichier, [1 chaîne] attendu", lng);
         try {
            File f;
            if(!(f = new File(getArg(2))).createNewFile())
               return cont.erreur("FILE", "CREATE : création impossible de '" + f.getCanonicalPath() + "'", lng);
            fil = f.getCanonicalPath();
            if (l == 4) { // Remplissage du fichier
               BufferedWriter wr = new BufferedWriter(new FileWriter(f));
               wr.write(Tools.removeBracketsIfAny(getArg(3)));
               wr.close();
            }
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "CREATE : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("REMOVE")) {
         if (getLength() != 3) return cont.erreur("FILE", "REMOVE : 1 nom de fichier attendu", lng);
         File f;
         if(!(f = new File(getArg(2))).delete())
            return cont.erreur("FILE", "REMOVE : suppression impossible de '" + f.getAbsolutePath()
                    + "'. Fichier introuvable ? Répertoire non vide ?", lng);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
      }
      else if (cmd.equals("MKDIR")) {
         if (getLength() != 3) return cont.erreur("FILE", "MKDIR : 1 nom de fichier attendu", lng);
         File f;
         if(!(f = new File(getArg(2))).mkdirs())
            return cont.erreur("FILE", "MKDIR : création impossible de '" + f.getAbsolutePath() + "'", lng);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
      }
      else if (cmd.equals("MOVE")) {
         if (getLength() != 4) return cont.erreur("FILE", "MOVE : 2 noms de fichier attendus", lng);
         File f;
         if(!(f = new File(getArg(2))).renameTo(new File(getArg(3))))
            return cont.erreur("FILE", "MOVE : déplacement impossible de '" + f.getAbsolutePath() + "'", lng);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
      }
      else if (cmd.equals("COPY")) {
         if (getLength() != 4) return cont.erreur("FILE", "COPY : 2 noms de fichier attendus", lng);
         // Merci à http://www.roseindia.net/java/beginners/CopyFile.shtml
         File f = new File(getArg(2));
         try {
            InputStream is = new FileInputStream(f);
            OutputStream os = new FileOutputStream(new File(getArg(3))); // pour append mettre true
            byte[] buf = new byte[1024];
            int l;
            while ((l = is.read(buf)) > 0) os.write(buf, 0, l);
            is.close();
            os.close();
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         catch (FileNotFoundException ex) {
            return cont.erreur("FILE", "COPY : le fichier " + f.getAbsolutePath() + " n'existe pas", lng);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "COPY : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("GLOB")) {
         int l = getLength();
         if (l < 2 || l > 4)
            return cont.erreur("FILE", "GLOB : 1 nom de fichier, [1 rép., 1 regexp] attendus", lng);
         final String rep = (l >= 3 ? getArg(2) : "."),
                     find = (l == 4 ? Tools.removeBQuotes(getArg(3)).value : ".*");
         String[] files = new File(rep).list((dir, fname) -> fname.matches(find));
         fil = files == null ? "" : Tools.arrayToString(Arrays.asList(files));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("LIST")) {
         int l = getLength();
         if (l < 3) return cont.erreur("FILE", "LIST : au moins 1 nom de fichier attendu", lng);

         if ((r = fileContent("LIST", 0)) >= 0) return r;
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("LINES")) {
         if (getLength() < 3) return cont.erreur("FILE", "LINES : au moins 1 nom de fichier attendu", lng);
         if ((r = fileContent("LINES", 1)) >= 0) return r;
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("VIEW")) {
         if (getLength() < 3) return cont.erreur("FILE", "VIEW : au moins 1 nom de fichier attendu", lng);
         if ((r = fileContent("VIEW", 2)) >= 0) return r;
         aff = fil;
         fil = null;
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("SCAN")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("FILE", "SCAN : 1 nom de fichier, [1 séq.] attendus", lng);
         try {
            String[] seq = (l == 4 ? getArg(3) : "1:-1").split(":");
            int deb = 1, fin;
            switch (seq.length) {
            case 1:
               fin = Integer.parseInt(seq[0]);
               if (fin < 0) {
                  deb = fin;
                  fin = -1;
               }
               break;
            case 2:
               deb = Integer.parseInt(seq[0]);
               fin = Integer.parseInt(seq[1]);
               break;
            default:
               return cont.erreur("FILE", "séquence attendue : début:fin", lng);
            }
            BufferedReader read;
            if (fin < 0) {  // Comptage du nb de ligne si fin<0
               int n = 0;
               read = new BufferedReader(new FileReader(getArg(2)));
               while (read.readLine() != null) n++;
               read.close();
               fin = fin + n + 1;
               if (deb < 0) deb = deb + n + 1;
            }
            String lu; int i = 1;
            StringBuilder sb = new StringBuilder();
            read = new BufferedReader(new FileReader(getArg(2)));
            while ((lu = read.readLine()) != null) {
               if (i >= deb && i <= fin) sb.append(lu).append('\n');
               i++;
            }
            read.close();
            fil = sb.toString();
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "SCAN : ERREUR IOException : " + ex.getMessage(), lng);
         }
         catch (NumberFormatException ex) {
            return cont.erreur("FILE", "paramètre de séquence invalide : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("TEMP")) {
         if (getLength() < 2 || getLength() > 3)
            return cont.erreur("FILE", "TEMP : [1 chaîne] attendue", lng);
         try {
            File f = File.createTempFile(getLength() == 3 ? getArg(2) : "lunasql-", null);
            f.deleteOnExit();
            fil = f.getAbsolutePath();
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "TEMP : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("OPEN")) {
         if (getLength() < 3 || getLength() > 4)
            return cont.erreur("FILE", "OPEN : 1 nom de fichier, [1 mode] attendus", lng);
         String f = getArg(2), m = getLength() == 4 && !getArg(3).isEmpty() ? getArg(3) : "r";
         try { fil = Integer.toString(cont.addFile(new File(f), m.charAt(0))); }
         catch (IOException ex) {
            return cont.erreur("FILE", "OPEN : ERREUR IOException : " + ex.getMessage(), lng);
         }
         cont.setVar(Contexte.ENV_CMD_STATE, m.equals("r") ?
               Contexte.STATE_FALSE : Contexte.STATE_TRUE);
      }
      else if (cmd.equals("CLOSE")) {
         if (getLength() != 3) return cont.erreur("FILE", "CLOSE : 1 index attendu", lng);
         try {
            int n = Integer.parseInt(getArg(2));
            cont.delFile(n);
            fil = getArg(2);
         }
         catch (NumberFormatException ex) {
            return cont.erreur("FILE", "CLOSE : index incorrect : " + getArg(2), lng);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "CLOSE : ERREUR IOException : " + ex.getMessage(), lng);
         }
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      }
      else if (cmd.equals("READ")) {
         if (getLength() < 3 || getLength() > 4)
            return cont.erreur("FILE", "READ : 1 index, [1 nombre] attendu", lng);
         try {
            FileReader fr = cont.getFileR(Integer.parseInt(getArg(2)));
            int nbCar = getLength() == 4 ? Integer.parseInt(getArg(3)) : 1;
            if (nbCar == 1) {
               if ((nbCar = fr.read()) < 0) fil = "";
               else fil = Character.toString((char)nbCar);
            }
            else {
               char[] buf = new char[nbCar];
               fr.read(buf, 0, nbCar);
               fil = new String(buf);
            }
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         catch (NumberFormatException ex) {
            return cont.erreur("FILE", "READ : index incorrect : " + getArg(2), lng);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "READ : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("READLN")) {
         if (getLength() != 3) return cont.erreur("FILE", "READLN : 1 index attendu", lng);
         try {
            FileReader fr = cont.getFileR(Integer.parseInt(getArg(2)));
            int n;
            StringBuilder sb = new StringBuilder();
            while ((n = fr.read()) > -1 && n != '\n') {
               if (n != '\r') sb.append((char)n); // TODO: séparation des lignes pas super
            }
            if (n == -1 && sb.length() == 0) sb.append("<EOF>");
            fil = sb.toString();
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         catch (NumberFormatException ex) {
            return cont.erreur("FILE", "READLN : index incorrect : " + getArg(2), lng);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "READLN : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("WRITE")) {
         try {
            if (getLength() != 4) return cont.erreur("FILE", "WRITE : 1 index, 1 chaîne attendus", lng);
            FileWriter w = cont.getFileW(Integer.parseInt(getArg(2)));
            String s = getArg(3);
            w.write(s);
            fil = Integer.toString(s.length());
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         catch (NumberFormatException ex) {
            return cont.erreur("FILE", "WRITE : index incorrect : " + getArg(2), lng);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "WRITE : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("WRITELN")) {
         try {
            if (getLength() != 4) return cont.erreur("FILE", "WRITELN : 1 index, 1 chaîne attendus", lng);
            FileWriter w = cont.getFileW(Integer.parseInt(getArg(2)));
            String s = getArg(3);
            w.write(s + Contexte.END_LINE);
            fil = Integer.toString(s.length() + Contexte.END_LINE.length());
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         catch (NumberFormatException ex) {
            return cont.erreur("FILE", "WRITELN : index incorrect : " + getArg(2), lng);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "WRITELN : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("EACH")) {
         if (getLength() != 4)
            return cont.erreur("FILE", "EACH : 1 nom de fichier, 1 bloc attendus", lng);

         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "file each");
         try {
            BufferedReader read = new BufferedReader(new FileReader(getArg(2)));
            Lecteur lec = new Lecteur(cont);
            int lu;
            int nbi = 0;
            while ((lu = read.read()) != -1 && ret == RET_CONTINUE) {
               nbi++;
               vars.put("arg1", Character.toString((char)lu));
               cont.addSubMode();
               lec.add(cmds, vars);
               lec.doCheckWhen();
               cont.remSubMode();
               ret = lec.getCmdState();
            }
            read.close();
            lec.fin();
            fil = Integer.toString(nbi);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "EACHLN : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("EACHLN")) {
         if (getLength() != 4)
            return cont.erreur("FILE", "EACHLN : 1 nom de fichier, 1 bloc attendus", lng);

         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "file eachln");
         try {
            BufferedReader read = new BufferedReader(new FileReader(getArg(2)));
            Lecteur lec = new Lecteur(cont);
            String lu;
            int nbi = 0;
            while ((lu = read.readLine()) != null && ret == RET_CONTINUE) {
               nbi++;
               vars.put("arg1", lu);
               cont.addSubMode();
               lec.add(cmds, vars);
               lec.doCheckWhen();
               cont.remSubMode();
               ret = lec.getCmdState();
            }
            read.close();
            lec.fin();
            fil = Integer.toString(nbi);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "EACHLN : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("BYTES")) {
         if (getLength() < 3 || getLength() > 4)
            return cont.erreur("FILE", "BYTES : 1 nom de fichier, [1 nombre] attendu", lng);

         try {
            File f = new File(getArg(2));
            if (f.length() > Math.pow(2,21))  // max 2 Mio
               return cont.erreur("FILE",
                     "BYTES : le fichier " + getArg(2) + " est trop volumineux (> 2 Mio)", lng);

            fil = Security.hexencode(Files.readAllBytes(f.toPath()));
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         catch (NumberFormatException ex) {
            return cont.erreur("FILE", "BYTES : nombre incorrect : " + getArg(3), lng);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "BYTES : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("DUMP")) {
         if (getLength() != 4) return cont.erreur("FILE", "DUMP : 1 nom de fichier, 1 chaîne attendus", lng);

         r = -1;
         try {
            File f = new File(getArg(2));
            if (cont.askWriteFile(f)) {
               OutputStream os = new FileOutputStream(f);
               if (f.getName().toUpperCase().endsWith(".GZ")) os = new GZIPOutputStream(os, 4096);
               BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                  os, cont.getVar(Contexte.ENV_FILE_ENC)));
               out.write(getArg(3));
               out.close();
               fil = Long.toString(f.length());
            }
            else fil = "0";
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         catch (IOException ex) {
            r = cont.erreur("FILE", "DUMP : ERREUR IOException : " + ex.getMessage(), lng);
         }
         if (r >= 0) return r;
      }
      else if (cmd.equals("INFO")) {
         if (getLength() != 3) return cont.erreur("FILE", "INFO : 1 nom de fichier attendu", lng);

         File f = new File(getArg(2)), f2;
         if (!f.exists()) return cont.erreur("FILE", "INFO : le fichier " + f.getAbsolutePath() +
               " n'existe pas", lng);
         // Chargement des attributs
         SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
         try {
            Properties prop = new Properties();
            prop.put("name", f.getName());
            prop.put("path", f.getAbsolutePath());
            prop.put("canon", f.getCanonicalPath());
            prop.put("size", Long.toString(f.length()));
            prop.put("modified_f", df.format(new Date(f.lastModified())));
            prop.put("modified", Long.toString(f.lastModified()));
            prop.put("parent", f.getParent() == null ? "null" : f.getParent());
            prop.put("parent2", (f2 = f.getParentFile()) == null ? "null" : f2.getCanonicalPath());
            prop.put("is-dir", f.isDirectory() ? "1" : "0");
            prop.put("is-file", f.isFile() ? "1" : "0");
            prop.put("is-absol", f.isAbsolute() ? "1" : "0");
            prop.put("is-hidden", f.isHidden() ? "1" : "0");
            prop.put("can-read", f.canRead() ? "1" : "0");
            prop.put("can-write", f.canWrite() ? "1" : "0");
            prop.put("can-exec", f.canExecute() ? "1" : "0");
            prop.put("URI", f.toURI().toASCIIString());
            prop.put("free-sp", Long.toString(f.getFreeSpace()));
            prop.put("total-sp", Long.toString(f.getTotalSpace()));
            prop.put("hash-sha1", new Security().getHashFile(f));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            prop.store(os, "File : " + f.getCanonicalPath());
            fil = os.toString(cont.getVar(Contexte.ENV_FILE_ENC));
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "INFO : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("HASH")) {
         if (getLength() != 3) return cont.erreur("FILE", "HASH : 1 nom de fichier attendu", lng);

         File f = new File(getArg(2));
         if (!f.canRead()) return cont.erreur("FILE", "HASH : le fichier " + f.getAbsolutePath() +
               " est inaccessible", lng);
         Checksum ck = new CRC32();
         byte[] tb = new byte[1024];
         int nbb;
         r = -1;
         try (InputStream in = new FileInputStream(f)) {
            while ((nbb = in.read(tb)) != -1) ck.update(tb, 0, nbb);
            fil = Long.toHexString(ck.getValue());
         }
         catch (IOException ex) {
            r = cont.erreur("FILE", "HASH : ERREUR IOException : " + ex.getMessage(), lng);
         }
         if (r >= 0) return r;
      }
      else if (cmd.equals("DIGEST")) {
         if (getLength() < 3 || getLength() > 4)
            return cont.erreur("FILE", "DIGEST : 1 nom de fichier attendu", lng);

         File f = new File(getArg(2));
         if (!f.canRead()) return cont.erreur("FILE", "DIGEST : le fichier " + f.getAbsolutePath() +
               " est inaccessible", lng);
         try {
            fil = new Security().getHashFile(f, getLength() == 3 ? "hex" : getArg(3));
         }
         catch (IOException ex) {
            return cont.erreur("FILE", "DIGEST : ERREUR IOException : " + ex.getMessage(), lng);
         }
      }
      else return cont.erreur("FILE", cmd + " : commande inconnue", lng);

      cont.setValeur(new ValeurDef(cont, aff, fil));
      return RET_CONTINUE;
   }

   /**
    * Lit le contenu intégral de la liste de fichiers getCommandA, selon le mode donné
    *
    * @param cmd le nom de la sous-commande
    * @param mode le mode de retour : 0: retour simple, 1 : retour liste, 2 : affichage console
    * @return -1 si tou va bien, code retour de erreur sinon
    */
   private int fileContent(String cmd, int mode) {
      String[] files = getCommandA(2);
      StringBuilder sb = new StringBuilder();

      for (String sf : files) {
         String path;
         String[] fnames;
         try {
            Tools.BQRet bqr = Tools.removeBQuotes(sf);
            String fname = bqr.value;
            if (bqr.hadBQ) {
               final String fname0 = fname;
               int id = fname.lastIndexOf(File.separator);
               if (id >= 0) path = fname.substring(0, id);
               else path = ".";
               File dir = new File(path);
               fnames = dir.list((directory, fileName) -> fileName.matches(fname0));
            }
            else {
               File f = new File(fname);
               if (!f.exists()) return cont.erreur("FILE", "le fichier "+ sf +" n'existe pas", lng);
               else if (f.length() > Math.pow(2,21))  // max 2 Mio
                  return cont.erreur("FILE", "le fichier " + sf + " est trop volumineux (> 2 Mio)", lng);
               else {
                  fnames = new String[]{fname};
                  path = ".";
               }
            }

            if (fnames == null || fnames.length == 0) // si pas de correspondance, erreur
               return cont.erreur("FILE", "aucun fichier ne correspond au filtre : "+ sf, lng);
            else
               for (int j = 0; j < fnames.length; j++) {
                  fnames[j] = (path.equals(".") ? fnames[j] : path + File.separator + fnames[j]);
                  File file = new File(fnames[j]);
                  if (!file.isFile() || !file.canRead())
                     return cont.erreur("FILE", "le fichier '"+ file.getCanonicalPath() + "' est inaccessible", lng);
                  else {
                     if (mode == 2) {
                        if (sb.length() > 0) sb.append('\n');
                        sb.append("Fichier : ").append(file.getName()).append('\n');
                     }
                     InputStream is = new FileInputStream(file);
                     if (fnames[j].toUpperCase().endsWith(".GZ")) is = new GZIPInputStream(is, 4096);
                     BufferedReader read = new BufferedReader(new InputStreamReader(is));
                     String line;
                     int l = 0;
                     while ((line = read.readLine()) != null) {
                        l++;
                        switch (mode) {
                        case 0:
                           sb.append(line).append(Contexte.END_LINE);
                           break;
                        case 1:
                           sb.append('{')
                             .append(line.replace("{", "^{").replace("}", "^}"))
                             .append('}').append("\n");
                           break;
                        case 2 :
                           sb.append(String.format("%04d", l)).append(" | ").append(line).append('\n');
                           break;
                        }
                     }// Parcours du fichier
                     nbl += l;
                     if (mode == 2) sb.append("-> ").append(l).append(" ligne").append(l > 1 ? "s" : "");
                     read.close();
                  }
               }
         }
         catch (IOException ex) {
            return cont.exception("FILE", cmd + " : ERREUR IOException : " + ex.getMessage(), lng, ex);
         }
         catch (PatternSyntaxException ex) {
            return cont.erreur("FILE", cmd + " : syntaxe regexp incorrecte : " + ex.getMessage(), lng);
         }
      }// for
      fil = sb.toString();
      return -1;
   }


   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  file        Outils de gestion de fichier\n";
   }
}// class
