package lunasql.cmd;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Security;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.ValeurDef;
import opencsv.CSVWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
//import jline.console.ConsoleReader;

/**
 * Commande EXPORT <br>
 * (Interne) Exportation des données d'une table en fichier SQL, CSV, TXT, XML ou HTML
 * @author M.P.
 */
public class CmdExport extends Instruction {

   private final OptionParser parser;

   public CmdExport(Contexte cont) {
      super(cont, TYPE_CMDINT, "EXPORT", "EX");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("e", "extension (avec -f)").withRequiredArg().ofType(String.class)
         .describedAs("ext");
      parser.accepts("f", "recherche de tables sur patron").requiredIf("e")
         .withRequiredArg().ofType(String.class)
         .describedAs("pattern");
      parser.accepts("k", "clef de chiffrement (si ext. cry)").withRequiredArg().ofType(String.class)
         .describedAs("key");
      parser.accepts("o", "options d'export CSV").withRequiredArg().ofType(String.class)
         .describedAs("qut,esc");
      parser.accepts("c", "exporte dans le presse-papier");
      parser.accepts("s", "exporte aussi la structure");
      parser.accepts("n", "valeurs nulles représentées par <NULL>");
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
         boolean fltmod; // mode filtre de fichier
         String fname, fbs, ext, ext1 = null;
         String[] tables;
         List<?> lf = options.nonOptionArguments();
         // Option -find : modèle de nom de fichier
         if (options.has("f")) {
            fltmod = true;
            String ftb = (String) options.valueOf("f");
            fname = Tools.removeBQuotes(ftb).value;
            ext1 = options.has("e") ? (String) options.valueOf("e") : "csv";
            List<String> ltbs = new ArrayList<>();
            try {
               String[] usertables = {"TABLE", "GLOBAL TEMPORARY", "VIEW"};
               DatabaseMetaData dMeta = cont.getConnex().getMetaData();
               ResultSet result = dMeta.getTables(null, null, null, usertables);
               String table;
               while (result.next()) {
                  table = result.getString(3);
                  if (table.matches("(?i)" + fname)) ltbs.add(table);
               }
               result.close();
            }
            catch (SQLException ex) {
               return cont.exception("EXPORT", "ERREUR SQLException : " + ex.getMessage(), lng, ex);
            }
            catch (PatternSyntaxException ex) {
               return cont.erreur("EXPORT", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
            }
            if (ltbs.isEmpty())
               return cont.erreur("EXPORT", "aucune table ne correspond au filtre : " + ftb, lng);
            tables = ltbs.toArray(new String[]{});
         }
         else {
            if (lf.size() < 1)
               return cont.erreur("EXPORT",
                     "un nom de fichier et une table/requête à exporter sont attendus", lng);

            fltmod = false;
            tables = new String[]{ (String) lf.get(0) };
         }

         // Boucle de parcours des tables sélectionnées
         int nbltot = 0;
         StringBuilder ccl = new StringBuilder();
         for (String tb : tables) {
            fname = fltmod ? tb + "." + ext1 : tb;

            boolean zip = false, cry = false, clip = options.has("c");
            if (clip) {
               fbs = fname;
               ext = "";
            }
            else {
               int id = fname.lastIndexOf('.');
               if (id > 0) {
                  ext = fname.substring(id + 1).toUpperCase();
                  if (ext.equals("GZ")) { // mise au format GZip
                     fname = fname.substring(0, id);
                     zip = true;
                  }
                  else if (ext.equals("CRY")) { // chiffrement des données
                     fname = fname.substring(0, id);
                     cry = true;
                  }
               }
               else fname += ".csv";

               // Extension normale
               id = fname.lastIndexOf('.');
               if (id > 0 && id < fname.length() - 1) {
                  fbs = fname.substring(0, id).toUpperCase();
                  ext = fname.substring(id + 1).toUpperCase();
               }
               else {
                  fbs = fname;
                  ext = "";
               }
               // Path separator
               id = fbs.lastIndexOf(File.separatorChar);
               if (id >= 0 && id < fbs.length() - 1) fbs = fbs.substring(id + 1);

               if (!ext.equals("SQL") && !ext.equals("CSV") && !ext.equals("TXT")
                        && !ext.equals("XML") && !ext.equals("HTML")) {
                  return cont.erreur("EXPORT", "format de fichier non supporté : "
                        + (ext.length() == 0 ? "(ext. vide)" : ext), lng);
               }
            }

            // Vérification de la clef de chiffrement en mode cry
            byte[] bkey = null;
            if (cry) {
               if (!options.has("k"))
                  return cont.erreur("EXPORT", "option -k (clef) requise si format .cry", lng);

               try {
                  bkey = Security.hexdecode((String) options.valueOf("k"));
                  if (bkey == null)
                     return cont.erreur("EXPORT", "clef de déchiffrement fournie nulle", lng);
               } catch (IllegalArgumentException ex) {
                  return cont.erreur("EXPORT", ex.getMessage(), lng);
               }
            }

            File file = null;
            if (!clip) {
               file = new File(fname + (zip ? ".gz" : (cry ? ".cry" : "")));
               if (!cont.askWriteFile(file)) {
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
                  return RET_CONTINUE;
               }
            }

            // Préparation de la table et requête d'insertion
            long tm = System.currentTimeMillis();
            String req1, table;
            if (fltmod || lf.size() == 1) {
               table = fbs;
               req1 = "SELECT * FROM " + table;
            }
            else {
               req1 = listToString(lf, clip ? 0 : 1).trim();
               int j = req1.indexOf(" ");
               table = (j > 1 ? req1.substring(0, j) : req1);
               // TODO: faire une vraie analyse grâce à JSQLFarceur (si présent en CLASSPATH)
               // problème: JSQLFarceur ne prend pas en charge CALL <procédure> ou SELECT <expr>.
               if (table.equalsIgnoreCase("SELECT")) {
                  j = req1.toUpperCase().indexOf(" FROM ");
                  if (j < 7) return cont.erreur("EXPORT", "format de requête d'export incorrect", lng);
                  int k = req1.toUpperCase().indexOf(" WHERE ");
                  table = (k > j ? req1.substring(j + 6, k) : req1.substring(j + 6));
               }
               else {
                  req1 = "SELECT * FROM " + table;
               }
            }

            // Exécution de la requête
            OutputStream outp = null;
            int ret = RET_CONTINUE;
            boolean err = false;
            try {
               if (cont.getVerbose() >= Contexte.VERB_DBG) cont.println("Requête : " + req1);
               ResultSet rs = cont.getConnex().getResultset(req1);
               ResultSetMetaData rsm = rs.getMetaData();
               int nbCol = rsm.getColumnCount(), nbl = 0;
               boolean shownulls = options.has("n");

               // Export format SQL
               if (ext.equals("SQL")) {
                  if (!table.matches("[A-Za-z_][A-Za-z0-9._]*")) // nom de table unique ou composé
                     return cont.erreur("EXPORT", "nom de table incorrect pour un export SQL", lng);
                  outp = new FileOutputStream(file);
                  if (cry) {
                     byte[] iv = new byte[16];
                     Cipher c = getCipher(bkey, iv);
                     outp.write(iv);
                     outp = new CipherOutputStream(outp, c);
                  }
                  if (zip || cry) outp = new GZIPOutputStream(outp, 4096); // chiffré est aussi zippé
                  PrintStream writer = new PrintStream(outp, false, cont.getVar(Contexte.ENV_FILE_ENC));
                  StringBuilder s = new StringBuilder();
                  s.append("/* ***************************************************************************** *");
                  s.append("\n *  Table exportée : ").append(table);
                  s.append("\n *  Requête d'exportation : ").append(req1);
                  s.append("\n *  Nom de base de données : ").append(cont.getConnex().getBase());
                  s.append("\n *  Date d'exportation : ").append(new java.util.Date().toString());
                  s.append("\n * ***************************************************************************** */\n\n");
                  writer.print(s.toString());
                  s.setLength(0);

                  // Construction de la requête avec noms de colonnes
                  s.append("INSERT INTO ").append(table).append(" (");
                  for (int j = 1; j <= nbCol; j++) s.append(rsm.getColumnName(j)).append(',');
                  s.deleteCharAt(s.length() - 1).append(") VALUES (");
                  String c = s.toString();
                  s.setLength(0);

                  // Exportation
                  if (options.has("s")) {
                     // Suppression de la table
                     s.append("DROP TABLE ").append(table).append(";\n\n");
                     // Création de la table
                     s.append("CREATE TABLE ").append(table).append(" (\n");
                     for (int j = 1; j <= nbCol; j++) {
                        s.append("  ").append(rsm.getColumnName(j)); // Nom de colonne
                        s.append(' ').append(rsm.getColumnTypeName(j)); // Type
                        s.append('(').append(rsm.getPrecision(j)).append(')'); // Taille
                        if (j < nbCol) s.append(',');
                        s.append('\n');
                     }
                     s.append(");\n\n");
                  }
                  // Suppression des données
                  s.append("DELETE FROM ").append(table).append(";\n\n");
                  // Parcours des lignes de la table
                  while (rs.next()) { // pour chaque ligne
                     s.append(c);
                     for (int j = 1; j <= nbCol; j++) { // pour chaque colonne
                        String r = rs.getString(j);
                        if (r == null) {
                           s.append("NULL");
                        }
                        else {
                           switch (rsm.getColumnType(j)) {
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
                           case Types.NULL:
                              s.append(r);
                              break;
                           default:
                              s.append('\'');
                              s.append(r.replace("'", "''").replace("\n", "$<n>"));
                              s.append('\'');
                              break;
                           }
                        }
                        s.append(',');
                     }
                     s.deleteCharAt(s.length() - 1).append(");\n");
                     nbl++;
                  }
                  s.append("-- Nombre de lignes : ").append(nbl).append("\n\n");
                  writer.print(s.toString());
                  writer.close();
               } // SQL

               else if(ext.equals("HTML")) {  // Export format : HTML
                  outp = new FileOutputStream(file);
                  if (cry) {
                     byte[] iv = new byte[16];
                     Cipher c = getCipher(bkey, iv);
                     outp.write(iv);
                     outp = new CipherOutputStream(outp, c);
                  }
                  if (zip || cry) outp = new GZIPOutputStream(outp, 4096);
                  PrintStream writer = new PrintStream(outp, false, cont.getVar(Contexte.ENV_FILE_ENC));
                  // introduction
                  writer.print("<!DOCTYPE html>\n<html>\n");
                  writer.print("<head>\n<title>Exportation requête LunaSQL</title>\n</head>\n<body>\n");
                  writer.print("<p align='center'>\n<table border=0>\n");
                  writer.print("<tr><td>Table exportée :</td><td>" + table + "</td></tr>\n");
                  writer.print("<tr><td>Requête d'exportation :</td><td>" + req1 + "</td></tr>\n");
                  writer.print("<tr><td>Nom de base de données :</td><td>" + cont.getConnex().getBase() + "</td></tr>\n");
                  writer.print("<tr><td>Date d'exportation :</td><td>" + new java.util.Date().toString() + "</td></tr>\n");
                  writer.print("</table>\n</p>\n");
                  writer.print("<p align='center'>\n<table border=1>\n");
                  // table header
                  writer.print("<tr>");
                  for (int j = 0; j < nbCol; j++)
                     writer.print("<th>" + rsm.getColumnLabel(j + 1) + "</th>\n");
                  writer.println("</tr>");
                  // the data
                  while (rs.next()) {
                     nbl++;
                     writer.print("<tr>");
                     for (int j = 1; j <= nbCol; j++) {
                        String value = rs.getString(j);
                        writer.print("<td>" + (value == null ? (shownulls ? "<NULL>" : "") : value) + "</td>");
                     }
                     writer.println("</tr>");
                  }
                  // conclusion
                  writer.print("</table>\n</p>\n");
                  writer.print("<p>Nombre de lignes : " + nbl + "</p>");
                  writer.print("</body>\n</html>\n");
                  writer.close();
               }// HTML

               else if(ext.equals("XML")) {  // Export format : XML
                  // Création du document
                  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                  DocumentBuilder builder = factory.newDocumentBuilder();
                  Document doc = builder.newDocument();
                  Element results = doc.createElement(table + "_" +
                        new SimpleDateFormat("yyyyMMdd").format(new Date()));
                  doc.appendChild(results);

                  // Lecture des données à exporter
                  while (rs.next()) {
                     Element row = doc.createElement("record");
                     row.setAttribute("rownum", Integer.toString(++nbl));
                     results.appendChild(row);
                     for (int j = 1; j <= nbCol; j++) {
                       String columnName = rsm.getColumnName(j), value = rs.getString(j);
                       Element node = doc.createElement(columnName);
                       node.appendChild(doc.createTextNode(value == null ? (shownulls ? "<NULL>" : "") : value));
                       row.appendChild(node);
                     }
                  }
                  rs.close();
                  DOMSource domSource = new DOMSource(doc);
                  TransformerFactory tf = TransformerFactory.newInstance();
                  Transformer trans = tf.newTransformer();
                  trans.setOutputProperty(OutputKeys.VERSION, "1.0");
                  trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                  trans.setOutputProperty(OutputKeys.METHOD, "xml");
                  trans.setOutputProperty(OutputKeys.ENCODING, cont.getVar(Contexte.ENV_FILE_ENC));
                  trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                  trans.setOutputProperty(OutputKeys.INDENT, "yes");

                  // Génération du fichier xml
                  outp = new FileOutputStream(file);
                  if (cry) {
                     byte[] iv = new byte[16];
                     Cipher c = getCipher(bkey, iv);
                     outp.write(iv);
                     outp = new CipherOutputStream(outp, c);
                  }
                  if (zip || cry) outp = new GZIPOutputStream(outp, 4096);
                  trans.transform(domSource, new StreamResult(outp));
               } // XML

               else {      // Export format autres : CSV, TXT
                  // Options d'export
                  String[] csvopt;     // 0:nol 1:qut 2:esc
                  if (options.has("o")) {
                     csvopt = ((String) options.valueOf("o")).split(",");
                     if (csvopt.length != 2 || csvopt[0].length() != 1 || csvopt[1].length() != 1)
                        return cont.erreur("EXPORT", "format des options d'export incorrect", lng);
                  }
                  else csvopt = new String[]{"\"", "\\"};

                  char sep = (clip || ext.equals("TXT") ? '\t' : ';');
                  outp = clip ? new ByteArrayOutputStream() : new FileOutputStream(file);
                  if (cry) {
                     byte[] iv = new byte[16];
                     Cipher c = getCipher(bkey, iv);
                     outp.write(iv);
                     outp = new CipherOutputStream(outp, c);
                  }
                  if (zip || cry) outp = new GZIPOutputStream(outp, 4096);
                  CSVWriter writer = new CSVWriter(new OutputStreamWriter(outp),
                        sep, csvopt[0].charAt(0), csvopt[1].charAt(0), "\n");
                  String[] row = new String[nbCol];
                  // Ecriture des noms de colonnes
                  for (int j = 0; j < nbCol; j++) row[j] = rsm.getColumnName(j + 1);
                  writer.writeNext(row);
                  // Ecriture des lignes
                  String v;
                  while (rs.next()){
                     for (int j = 0; j < nbCol; j++) {
                        v = rs.getString(j + 1);
                        row[j] = v == null ? (shownulls ? "<NULL>" : "") : v;
                     }
                     writer.writeNext(row);
                     nbl++;
                  }
                  //writer.writeAll(rs, true);
                  writer.close();
                  if (clip){
                     StringSelection ss = new StringSelection
                           (((ByteArrayOutputStream) outp).toString(cont.getVar(Contexte.ENV_FILE_ENC)));
                     Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
                     c.setContents(ss, ss);
                  }
               }
               rs.close();

               // Fermeture et Conclusion
               tm = System.currentTimeMillis() - tm;
               if (clip) ccl.append("Ecriture en presse-papier terminée\n");
               else ccl.append("Fichier '").append(file.getCanonicalPath()).append("' exporté\n");
               ccl.append("-> ").append((cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE) ?
                     Contexte.COLORS[Contexte.BR_CYAN] + nbl + Contexte.COLORS[Contexte.NONE] : nbl))
                     .append(" ligne").append(nbl > 1 ? "s" : "")
                     .append(" écrite").append(nbl > 1 ? "s" : "")
                     .append(zip ? ", format zippé" : (cry ? ", format chiffré" : ""))
                     .append(" (").append(SQLCnx.frmDur(tm)).append(")");
               cont.setVar(Contexte.ENV_RET_NLINES, Integer.toString(nbl));
               nbltot += nbl;
            }
            catch (SQLException|ParserConfigurationException|TransformerException|NoSuchPaddingException|
                   NoSuchAlgorithmException|InvalidKeyException|InvalidAlgorithmParameterException ex) {
               ret = cont.exception("EXPORT", "ERREUR " + ex.getClass().getSimpleName() + " : " +
                     ex.getMessage(), lng, ex);
               err = true;
            }
            finally {
               if (outp != null) outp.close();
            }
            if (err) return ret;
         }// for fnames

         cont.setValeur(new ValeurDef(cont, ccl.toString(), Integer.toString(nbltot), false));
         cont.setVar(Contexte.ENV_CMD_STATE, nbltot > 0 ? Contexte.STATE_TRUE : Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("EXPORT", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("EXPORT", "ERREUR IOException : " + ex.getMessage(), lng, ex);
      }
   }

   /**
    * Préparation du chiffre
    */
   private Cipher getCipher(byte[] bkey, byte[] outiv) throws NoSuchPaddingException, NoSuchAlgorithmException,
         InvalidAlgorithmParameterException, InvalidKeyException {
      Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
      new SecureRandom().nextBytes(outiv);
      c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(bkey, "AES"), new IvParameterSpec(outiv));
      return c;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc() {
      return "  export, ex  Exporte en fichier la table en paramètre selon format\n";
   }
}// class

