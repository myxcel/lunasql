package lunasql.cmd;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Security;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.ValeurDef;
import opencsv.CSVReader;

/**
 * Commande IMPORT <br>
 * (Interne) Importation d'un fichier formaté CSV ou TXT
 * @author M.P.
 */
public class CmdImport extends Instruction {

   private final OptionParser parser;

   public CmdImport(Contexte cont) {
      super(cont, TYPE_CMDINT, "IMPORT", "IM");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("f", "recherche de fichiers sur patron").withRequiredArg().ofType(String.class)
         .describedAs("pattern");
      parser.accepts("k", "clef de chiffrement (si ext. cry)").withRequiredArg().ofType(String.class)
         .describedAs("key");
      parser.accepts("o", "options d'import CSV").withRequiredArg().ofType(String.class)
         .describedAs("lng,qut,esc");
      parser.accepts("t", "format de date personnalisé").withRequiredArg().ofType(String.class)
         .describedAs("frm1,frm2").withValuesSeparatedBy(',');
      parser.accepts("l", "avec -t, locale").withRequiredArg().ofType(String.class)
         .describedAs("locale");
      parser.accepts("p", "propriétés du driver (avec -q)").withRequiredArg().ofType(String.class)
         .describedAs("prop");
      parser.accepts("q", "requête SELECT de selection").requiredIf("p");
      parser.accepts("d", "pas de ligne d'entêtes");
      parser.accepts("h", "entêtes de colonnes").withRequiredArg().ofType(String.class)
         .describedAs("headers");
      parser.accepts("c", "importe depuis le presse-papier");
      parser.nonOptions("fichier table").ofType(String.class);
   }

   @Override
   public int execute() { // TODO: à réduire en utilisant des méthodes
      try {
         OptionSet options = parser.parse(getCommandA1());
         // Aide sur les options
         if (options.has("?")) {
            parser.printHelpOn(cont.getWriterOrOut());
            cont.setValeur(null);
            return RET_CONTINUE;
         }

         // Exécution avec autres options
         List<?> lf = options.nonOptionArguments();

         // Cas de l'import sur requeste SQL par la bibliothèque CsvJdbc
         if (options.has("q")) {
            try {
               Class.forName("org.relique.jdbc.csv.CsvDriver");
               String path = (String) lf.get(0), table = (String) lf.get(1),
                     sql = listToString(lf, 2), zip = path.endsWith(".zip") ? "zip:" : "";
               Properties prop = new Properties();
               prop.put("separator", ";"); // options par défaut
               if (options.has("p")) prop.putAll(Tools.getProp((String) options.valueOf("p")));

               long tm = System.currentTimeMillis();
               Connection conn = DriverManager.getConnection("jdbc:relique:csv:" + zip + path, prop);
               ResultSet rs = conn.createStatement().executeQuery(sql);
               ResultSetMetaData rsSchema = rs.getMetaData();
               int nbCol = rsSchema.getColumnCount(), nbLig = 0;

               // Préparation de la requête d'insertion
               StringBuilder sb = new StringBuilder();
               sb.append("INSERT INTO ").append(table).append(" VALUES (");
               for (int i = 1; i <= nbCol; i++) sb.append("?,");
               sb.deleteCharAt(sb.length() - 1).append(')');
               PreparedStatement stmt = cont.getConnex().getConnex().prepareStatement(sb.toString());

               // Boucle sur les données CSV
               while (rs.next()) {
                  nbLig++;
                  for (int i = 1; i <= nbCol; i++) stmt.setString(i, rs.getString(i));
                  stmt.executeUpdate();
               }
               stmt.close();
               tm = System.currentTimeMillis() - tm;

               // Conclusion
               String ccl = new StringBuilder()
                  .append("-> ").append(nbLig).append(" ligne").append(nbLig > 1 ? "s":"")
                  .append(" insérée").append(nbLig > 1 ? "s":"")
                  .append(" (").append(SQLCnx.frmDur(tm)).append(")").toString();

               cont.setValeur(new ValeurDef(cont, ccl, Integer.toString(nbLig), false));
               cont.setVar(Contexte.ENV_CMD_STATE, nbLig > 0 ?
                     Contexte.STATE_TRUE : Contexte.STATE_FALSE);
               return RET_CONTINUE;

            }
            catch (ClassNotFoundException ex) {
               return cont.erreur("IMPORT", "impossible de charger le driver : " + ex.getMessage() +
                     "\n\tVérifiez que la bibliothèque csvjdbc.jar est disponible", lng);
            }
            catch (SQLException ex) {
               return cont.erreur("CSV", "ERREUR SQLException : " + ex.getMessage() , lng);
            }
         }

         String fname, ext, fbs;
         String[] fnames;
         boolean fltmod = false; // mode filtre de fichier

         // Option -f : modèle regexp de nom de fichier
         if (options.has("f")) {
            fltmod = true;
            String f = (String) options.valueOf("f");
            fname = Tools.removeBQuotes(f).value;
            String path;
            int id = fname.lastIndexOf(File.separator);
            if (id >= 0) {
               path = fname.substring(0, id);
               fname = fname.substring(id + 1);
            }
            else path = ".";

            File dir = new File(path);
            final String fname0 = fname;
            try {
               fnames = dir.list((directory, fileName) -> fileName.matches(fname0));
            }
            catch(PatternSyntaxException ex){
               return cont.erreur("IMPORT", "syntaxe regexp incorrecte : "+ ex.getMessage(), lng);
            }
            if (fnames == null || fnames.length == 0) // si pas de correspondance, erreur
               return cont.erreur("IMPORT", "aucun fichier ne correspond au filtre : "+ f, lng);

            for (int i = 0; i < fnames.length; i++)
               fnames[i] = (path.equals(".") ? fnames[i] : path + File.separator + fnames[i]);
         }
         else {
            if (lf.isEmpty() || lf.size() > 2)
               return cont.erreur("IMPORT", "un nom de fichier et une table de destination sont attendus", lng);
            fnames = new String[]{ (String) lf.get(0) };
         }

         // Boucle de parcours des fichiers sélectionnés
         int nbltot = 0, nberrtot = 0;
         int ret = RET_CONTINUE;
         StringBuilder ccl = new StringBuilder();
         for (String fnm : fnames) {
            fname = fnm;

            boolean zip = false, cry = false, clip = options.has("c");
            if (clip) {
               fbs = fname;
               ext = "";
            }
            else {
               int id = fname.lastIndexOf('.');
               if (id > 0) {
                  ext = fname.substring(id + 1).toUpperCase();
                  if (ext.equals("GZ")){ // mise au format GZip
                     fname = fname.substring(0, id);
                     zip = true;
                  }
                  else if (ext.equals("CRY")) { // chiffrement des données
                     fname = fname.substring(0, id);
                     cry = true;
                  }
               }
               id = fname.lastIndexOf('.');
               if (id > 0 && id < fname.length() - 1) {
                  fbs = fname.substring(0, id).toUpperCase();
                  ext = fname.substring(id + 1).toUpperCase();
               } else {
                  fbs = fname;
                  ext = "";
               }
               id = fname.lastIndexOf(File.separator);
               if (id > 0) fbs = fbs.substring(id + 1);

               // Si extension sql > renvoi à EXEC
               if (ext.equals("SQL")) return cont.executeCmd("EXEC", fname);
               if (!ext.equals("CSV") && !ext.equals("TXT")) {
                  return cont.erreur("IMPORT", "format de fichier non supporté : "
                        + (ext.length() == 0 ? "(ext. vide)" : ext), lng);
               }
            }

            // Vérification de la clef de chiffrement en mode cry
            byte[] bkey = null;
            if (cry) {
               if (!options.has("k"))
                  return cont.erreur("IMPORT", "option k (clef) requise si format cry", lng);

               try {
                  bkey = Security.hexdecode((String) options.valueOf("k"));
                  if (bkey == null)
                     return cont.erreur("IMPORT", "clef de déchiffrement fournie nulle", lng);
               } catch (IllegalArgumentException ex) {
                  return cont.erreur("IMPORT", ex.getMessage(), lng);
               }
            }

            String table = fltmod || lf.size() == 1 ? fbs : ((String) lf.get(1)).toUpperCase();
            char sep = (clip || ext.equals("TXT") ? '\t' : ';');

            // Ouverture du fichier ou de l'url
            URL purl = null;
            if(!clip) {
               if (fname.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")){
                  try {
                     purl = new URL(fname);
                  }
                  catch (MalformedURLException ex) {
                     return cont.erreur("IMPORT", "ERREUR MalformedURLException : " + ex.getMessage(), lng);
                  }
               }
               else {
                  File file = new File(fname + (zip ? ".gz" : (cry ? ".cry" : "")));
                  if (file.isFile() && file.canRead()) fname = file.getAbsolutePath();
                  else return cont.erreur("IMPORT", "le fichier '"+ file.getAbsolutePath()
                           + "' est inaccessible", lng);
               }
            }

            String[] line;
            StringBuilder s = new StringBuilder();
            CSVReader reader = null;
            try {
               // Options d'import
               String[] csvopt;     // 0:nol 1:qut 2:esc
               if (options.has("o")) {
                  csvopt = ((String) options.valueOf("o")).split(",");
                  if (csvopt.length != 3 || !csvopt[0].matches("^[0-9]+$")
                        || csvopt[1].length() != 1 || csvopt[2].length() != 1){
                     return cont.erreur("IMPORT", "format des options d'import incorrect", lng);
                  }
               }
               else csvopt = new String[]{"0", "\"", "\\"};

               // Ouverture du fichier
               InputStream inp;
               if (clip) { // lecture du presse-papier
                   byte[] data = ((String) Toolkit.getDefaultToolkit()
                         .getSystemClipboard().getData(DataFlavor.stringFlavor)).getBytes();
                   inp = new ByteArrayInputStream(data) ;
               }
               else {
                  inp = (purl == null ? new FileInputStream(fname) : purl.openStream());
                  if (cry) {
                     byte[] iv = new byte[16];
                     inp.read(iv);
                     Cipher c = getCipher(bkey, iv);
                     inp = new CipherInputStream(inp, c);
                  }
                  if (zip || cry) inp = new GZIPInputStream(inp);
               }

               boolean given = options.has("h"), fields = !given && !options.has("d"), first = true;
               reader = new CSVReader(new InputStreamReader(inp, cont.getVar(Contexte.ENV_FILE_ENC)),
                     sep, csvopt[1].charAt(0), csvopt[2].charAt(0), Integer.parseInt(csvopt[0]),
                     false, true);
               List<?> dateexpr = null;
               String sloc = null;
               if (options.has("t")) {
                  dateexpr = options.valuesOf("t");
                  if (options.has("l")) sloc = (String) options.valueOf("l");
               }

               // Préparation de la requête d'insertion
               long tm = System.currentTimeMillis();
               PreparedStatement stmt = null;
               s.append("INSERT INTO ").append(table);

               int nbfields = 0, nbl = 0, nberr = 0, csvlg = 0;
               int[] types = null;
               while ((line = reader.readNext()) != null) {
                  csvlg++;
                  if (line.length == 1 && line[0].length() == 0) continue;  // ignore empty lines
                  if (line[0].trim().startsWith("#")) continue; // ignore comments

                  try {
                     if (first) { // Création
                        nbfields = line.length;
                        types = new int[nbfields];
                        StringBuilder styp = new StringBuilder();
                        if (fields) {
                           s.append(" (");
                           for (String ln : line) styp.append(ln).append(','); // Noms de champs
                           styp.deleteCharAt(styp.length() - 1);
                           s.append(styp).append(") VALUES (");

                           // Lecture des types de champs
                           styp.insert(0, "SELECT ").append(" FROM ").append(table);
                           ResultSet rs = cont.getConnex().getResultset(styp.toString());
                           ResultSetMetaData rsmd = rs.getMetaData();
                           for (int i=0; i<types.length; i++) types[i] = rsmd.getColumnType(i + 1);
                           rs.close();
                        }
                        else if (given) {
                           s.append(" (").append((String) options.valueOf("h")).append(") VALUES (");
                        }
                        else s.append(" VALUES (");
                        for (int i=0; i<nbfields; i++) s.append("?,");
                        s.deleteCharAt(s.length() - 1).append(')');

                        // Statement
                        if (cont.getVerbose() >= Contexte.VERB_DBG) {
                           cont.println("Requête : " + s.toString());
                           cont.println("Types : ");
                           for (int i = 0; i < types.length; i++) cont.print((i + 1) + " : " + types[i] + ", ");
                           cont.println();
                        }
                        stmt = cont.getConnex().getConnex().prepareStatement(s.toString());
                        first = false;
                        if (fields) continue;
                     }
                     // Pas première ligne d'entêtes en fichier
                     int i = 0;
                     for (int j = 0; j < line.length; j++) {
                        // Traitement des NULL : champs numériques vides, et chaînes "NULL"
                        if (types != null && j < types.length && line[j].isEmpty()) {
                           switch (types[j]) {
                              case Types.BOOLEAN:
                              case Types.BIT:
                              case Types.DECIMAL:
                              case Types.NUMERIC:
                              case Types.INTEGER:
                              case Types.BIGINT:
                              case Types.FLOAT:
                              case Types.DOUBLE:
                              case Types.REAL:
                              case Types.SMALLINT:
                              case Types.TINYINT:
                              case Types.DATE:
                              case Types.TIME:
                              case Types.TIME_WITH_TIMEZONE:
                              case Types.TIMESTAMP:
                              case Types.TIMESTAMP_WITH_TIMEZONE:
                              case Types.NULL:
                                 stmt.setNull(j + 1, types[j]);
                                 break;
                              default: stmt.setString(j + 1, "");
                           }
                        }
                        else if (line[j].equals("<NULL>")) {
                           if (types != null && j < types.length) stmt.setNull(j + 1, types[j]);
                           else stmt.setNull(j + 1, Types.NULL);
                        }
                        else {
                           if (dateexpr != null && types != null && j < types.length) {
                              switch (types[j]) {
                                 case Types.TIME:
                                 case Types.TIME_WITH_TIMEZONE:
                                    stmt.setTime(j + 1, new Time(CmdTime.parseDate(line[j],
                                          (String) dateexpr.get(Math.min(i++, dateexpr.size() - 1)), sloc)));
                                    break;
                                 case Types.DATE:
                                    stmt.setDate(j + 1, new java.sql.Date(CmdTime.parseDate(line[j],
                                          (String) dateexpr.get(Math.min(i++, dateexpr.size() - 1)), sloc)));
                                    break;
                                 case Types.TIMESTAMP:
                                 case Types.TIMESTAMP_WITH_TIMEZONE:
                                    stmt.setTimestamp(j + 1, new Timestamp(CmdTime.parseDate(line[j],
                                          (String) dateexpr.get(Math.min(i++, dateexpr.size() - 1)), sloc)));
                                    break;
                                 default: stmt.setString(j + 1, line[j]);
                              }
                           }
                           else stmt.setString(j + 1, line[j]);
                        }
                     }
                     for (int j = line.length; j < nbfields; j++) stmt.setString(j + 1, "");
                     if (nbfields != line.length) {
                        if (cont.getVerbose() >= Contexte.VERB_BVR)
                           cont.println("Attention (" + fbs + (ext.length() > 0 ? "." : "") + ext + " ligne "
                                   + csvlg + ") : " + nbfields + " entêtes pour " + line.length + " champs trouvés");
                     }
                     nbl += stmt.executeUpdate();
                  }
                  catch (SQLException ex) {     // pas de retour
                     nberr++;
                     cont.exception("IMPORT", "ERREUR SQLException (" + fbs +
                           (ext.length() > 0 ? "." : "") + ext + " ligne " + csvlg + ") : " + ex.getMessage(),
                           lng, ex);
                     if (cont.getVar(Contexte.ENV_EXIT_ERR).equals("1")) break;
                  }
                  catch (ParseException ex) {     // pas de retour
                     nberr++;
                     cont.exception("IMPORT", "Format de date incorrect (" + fbs +
                           (ext.length() > 0 ? "." : "") + ext + " ligne " + csvlg + ") : " + ex.getMessage(),
                           lng, ex);
                     if (cont.getVar(Contexte.ENV_EXIT_ERR).equals("1")) break;
                  }
               }// while

               // Fermeture
               if (stmt != null) try { stmt.close(); }
               catch (SQLException ex){}
               tm = System.currentTimeMillis() - tm;
               reader.close();
               inp.close();

               // Message de rapport
               if (clip) ccl.append("Presse-papier ");
               else ccl.append("Fichier '").append(fname).append("' ");
               if (nbl == 0 && nberr == 0)  ccl.append("vide\n");
               else ccl.append(nbl == 0 ? "non " : (nberr > 0 ? "partiellement " : "")).append("importé\n");
               ccl.append("-> ").append((cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE) ?
                     Contexte.COLORS[Contexte.BR_CYAN] + nbl + Contexte.COLORS[Contexte.NONE] : nbl))
                     .append(" ligne").append(nbl > 1 ? "s" : "")
                     .append(" insérée").append(nbl > 1 ? "s" : "")
                     .append(zip ? ", format zippé" : (cry ? ", format chiffré" : ""))
                     .append(" (").append(SQLCnx.frmDur(tm)).append(")");
               if (nberr > 0)
                  ccl.append(" et ")
                     .append((cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE) ?
                        Contexte.COLORS[Contexte.BR_RED] + nberr + Contexte.COLORS[Contexte.NONE] : nberr))
                     .append(" erreur").append(nberr > 1 ? "s" : "");

               cont.setVar(Contexte.ENV_RET_NLINES, Integer.toString(nbl));
               nbltot += nbl;
               nberrtot += nberr;
            }
            catch (UnsupportedFlavorException|NoSuchPaddingException|NoSuchAlgorithmException|
                   InvalidAlgorithmParameterException|InvalidKeyException ex) {
               ret = cont.exception("IMPORT", "ERREUR " + ex.getClass().getSimpleName() + " : " +
                     ex.getMessage(), lng, ex);
            }
            finally {
               if (reader != null) reader.close();
            }
         }// for fnames

         cont.setValeur(new ValeurDef(cont, ccl.toString(), Integer.toString(nbltot), false));
         cont.setVar(Contexte.ENV_CMD_STATE, nberrtot > 0 ? Contexte.STATE_ERROR :
                     (nbltot > 0 ? Contexte.STATE_TRUE : Contexte.STATE_FALSE));
         return ret;
      }
      catch (OptionException ex) {
         return cont.exception("IMPORT", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.erreur("IMPORT", "ERREUR IOException : " + ex.getMessage() , lng);
      }
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

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  import, im  Importe le fichier de données en paramètre selon format\n";
   }
}// class

