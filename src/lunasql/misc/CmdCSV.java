package lunasql.misc;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.ValeurDef;
import lunasql.val.ValeurReq;

/**
 * Commande CSV <br>
 * (Interne) Lecture de fichier CSV depuis une requête SQL
 * @author M.P.
 * TODO: améliorer tout ça en rendant la connexion disponible en Contexte
 */
public class CmdCSV extends Instruction {

   private final OptionParser parser;

   public CmdCSV(Contexte cont) throws ClassNotFoundException {
      super(cont, TYPE_CMDPLG, "CSV", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("p", "propriétés du driver").withRequiredArg().ofType(String.class)
         .describedAs("prop");
      parser.accepts("d", "sortie en dictionnaire");
      parser.accepts("c", "code à itérer par ligne").withRequiredArg().ofType(String.class)
         .describedAs("code");
      // Test de présence du driver
      Class.forName("org.relique.jdbc.csv.CsvDriver");
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
         return cont.exception("CSV", "erreur d'option : " + ex.getMessage(), lng, ex);
      }

      // Exécution avec autres options
      try {
         Class.forName("org.relique.jdbc.csv.CsvDriver");
         List<?> lc = options.nonOptionArguments();
         if (lc.size() < 2)
            return cont.erreur("CSV", "un répertoire et une requête sont attendus", lng);

         String path = (String) lc.get(0), sql = listToString(lc, 1),
               zip = path.endsWith(".zip") ? "zip:" : "";
         Properties prop;
         if (options.has("p")) prop = Tools.getProp((String) options.valueOf("p"));
         else prop = new Properties();

         Connection conn = DriverManager.getConnection("jdbc:relique:csv:" + zip + path, prop);
         if (cont.getVerbose() >= Contexte.VERB_BVR) cont.println("Connecté au répertoire CSV : " + path);

         SQLCnx scnx = new SQLCnx(conn);
         long tm = System.currentTimeMillis();

         boolean hasd = options.has("d"), hasc = options.has("c");
         if (hasd || hasc) {
            ResultSet rs = scnx.getResultset(sql);
            ResultSetMetaData rsm = rs.getMetaData();
            int nbCol = rsm.getColumnCount();

            if (hasd) { // dict
               String enc = cont.getVar(Contexte.ENV_FILE_ENC);
               StringBuilder sb = new StringBuilder();
               while (rs.next()) {
                  Properties p = new Properties();
                  sb.append("{\n");
                  for (int i = 1; i <= nbCol; i++) p.put(rsm.getColumnName(i), rs.getString(i));
                  sb.append(Tools.propStore(p, enc)).append("}\n");
               }
               cont.setValeur(new ValeurDef(cont, null, sb.toString()));
            }
            else { // code
               String cmds = (String) options.valueOf("c");
               HashMap<String, String> vars = new HashMap<>();
               vars.put(Contexte.LEC_SUPER, "CSV code");
               Lecteur lec = new Lecteur(cont);
               int nbl = 0;
               while (rs.next()) {
                  int nbExpr = 0;
                  vars.put("row_id", Integer.toString(++nbl));
                  for (int i = 1; i <= nbCol; i++) {
                     String k = rsm.getColumnName(i);
                     if (k == null || k.isEmpty())
                        vars.put("Expr" + String.format("%02d", ++nbExpr), rs.getString(i));
                     else if (cont.valideKey(k)) vars.put(k, rs.getString(i));
                  }
                  cont.addSubMode();
                  lec.add(cmds, vars);
                  lec.doCheckWhen();
                  cont.remSubMode();
               }
               lec.fin();
            }
            rs.close();
            tm = System.currentTimeMillis() - tm;
         }
         else {
            ValeurReq vr = new ValeurReq(cont, scnx.getResultString(sql,
                  cont.getVar(Contexte.ENV_SELECT_ARR).equals(Contexte.STATE_TRUE),
                  cont.getColMaxWidth(lng), cont.getRowMaxNumber(lng),
                  cont.getVar(Contexte.ENV_ADD_ROW_NB).equals(Contexte.STATE_TRUE),
                  cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE)));
            int n = vr.getNbLines();
            tm = System.currentTimeMillis() - tm;
            vr.setDispValue("-> " + n + " ligne" + (n > 1 ? "s" : "") + " trouvée" +
                  (n > 1 ? "s" : "") + " (" + SQLCnx.frmDur(tm) + ")");
            cont.setValeur(vr);
         }
         scnx.fermer();
         cont.setVar(Contexte.ENV_EXEC_TIME, Long.toString(tm));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (SQLException ex) {
         return cont.erreur("CSV", "ERREUR SQLException : " + ex.getMessage() , lng);
      }
      catch (ClassNotFoundException ex) {
         return cont.erreur("CSV", "impossible de charger le driver : " + ex.getMessage(), lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  csv         Lit le contenu d'un CSV depuis requête SQL\n";
   }
}// class
