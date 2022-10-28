package lunasql.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.lib.Tools;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Commande XML <br>
 * (Interne) Lecture de chaîne XML par requête XPath
 * @author M.P.
 */
public class CmdXML extends Instruction {

   private final OptionParser parser;
   private final DocumentBuilderFactory dfac = DocumentBuilderFactory.newInstance();
   private final XPathFactory xfac = XPathFactory.newInstance();

   public CmdXML(Contexte cont) {
      super(cont, TYPE_CMDPLG, "XML", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("f", "fichier XML").withRequiredArg().ofType(String.class)
         .describedAs("url");
      parser.accepts("s", "chaîne XML").withRequiredArg().ofType(String.class)
         .describedAs("str");
      parser.accepts("q", "requête SQL").withRequiredArg().ofType(String.class)
            .describedAs("sql");
      parser.accepts("m", "multiples requêtes");
      parser.nonOptions("xpath").ofType(String.class);
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
         return cont.exception("XML", "erreur d'option : " + ex.getMessage(), lng, ex);
      }

      // Exécution avec autres options
      try {
         List<?> lf = options.nonOptionArguments();
         // Lecture de l'XML
         Valeur vr = new ValeurDef(cont);
         DocumentBuilder db = dfac.newDocumentBuilder();
         Document doc;

         if (options.has("q")) {
            String sql = Tools.removeBracketsIfAny((String) options.valueOf("q"));
            if (sql.isEmpty()) return cont.erreur("XML", "requête SQL vide avec option -q", lng);

            doc = db.newDocument();
            Element results = doc.createElement("recordset");
            doc.appendChild(results);

            if (!sql.matches("(?is)SELECT\\s+.*")) sql = "SELECT * FROM " + sql;
            ResultSet rs = cont.getConnex().getResultset(sql);
            ResultSetMetaData rsm = rs.getMetaData();
            int nbCol = rsm.getColumnCount(), nbl = 0, norow = 0, rowmax = cont.getRowMaxNumber(lng);
            // Lecture des données à exporter
            while (rs.next()) {
               Element row = doc.createElement("record");
               row.setAttribute("rownum", Integer.toString(++nbl));
               results.appendChild(row);
               for (int j = 1; j <= nbCol; j++) {
                 String columnName = rsm.getColumnName(j), value = rs.getString(j);
                 Element node = doc.createElement(columnName);
                 node.appendChild(doc.createTextNode(value == null ? "NULL" : value));
                 row.appendChild(node);
               }
               if (++norow > rowmax) break;
            }
            rs.close();
            DOMSource domSource = new DOMSource(doc);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.setOutputProperty(OutputKeys.VERSION, "1.0");
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(OutputKeys.METHOD, "xml");
            trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            // Préparation de la sortie
            OutputStream out = new ByteArrayOutputStream();
            trans.transform(domSource, new StreamResult(out));
            vr.setSubValue(lf.isEmpty() ? out.toString() : queryDoc(doc, lf, options.has("m")));
         }
         else if (options.has("f") || options.has("s")) {
            if (lf.isEmpty())
               return cont.erreur("XML", "un doc. XML et une requête XPath minimum sont attendus", lng);

            doc = options.has("f") ? db.parse((String) options.valueOf("f"))
                  : db.parse(new ByteArrayInputStream(((String) options.valueOf("s")).getBytes()));
            vr.setSubValue(queryDoc(doc, lf, options.has("m")));
         }
         else return cont.erreur("XML", "une option -f, -s ou -q est attendue", lng);

         vr.setRet();
         cont.setValeur(vr);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (SQLException|ParserConfigurationException|DOMException|SAXException|TransformerException|
             IOException|XPathExpressionException ex) {
         return cont.erreur("XML", "ERREUR " + ex.getClass().getSimpleName() + " : " +
               ex.getMessage() , lng);
      }
   }

   /**
    * Requête le document par XPath
    * @param doc le document
    * @param lf liste d'arguments de requête
    * @param m si option "m"
    * @return le résultat
    * @throws XPathExpressionException si c'est la cas
    */
   private String queryDoc(Document doc, List<?> lf, boolean m) throws XPathExpressionException {
      StringBuilder sb = new StringBuilder();
      boolean unq = lf.size() == 1;
      for (Object req : lf) {
         String[] treq;
         if (m) {
            treq = Tools.blankSplitLec(cont, Tools.removeBracketsIfAny((String) req));
            unq = false;
         }
         else treq = new String[]{Tools.removeBracketsIfAny((String) req)};

         // Pour chaque sous-requête
         for (String req2 : treq) {
            XPathExpression expr = xfac.newXPath().compile(req2);
            sb.append(unq ? expr.evaluate(doc) : Tools.putBraces(expr.evaluate(doc)));
            sb.append(' ');
         }
      }
      return sb.toString();
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  xml         Lit le contenu d'un XML depuis requête XPath\n";
   }
}
