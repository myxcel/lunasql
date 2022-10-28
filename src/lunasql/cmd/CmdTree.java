package lunasql.cmd;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande TREE <br>
 * (Interne) Affiche une représentation en arbre des clefs étangères d'une table
 * TEST(ID)
 * |
 * +-- TEST2(IDTST)
 *     TEST2(ID)
 *     |
 *     +-- TEST3(IDTST)
 *
 * @author M.P.
 */
public class CmdTree extends Instruction {

   private final OptionParser parser;
   private int deepMax;
   private boolean modeDot; // mode arbre normal (false) ou DOT (true)

   public CmdTree(Contexte cont){
      super(cont, TYPE_CMDINT, "TREE", null);// Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("d", "génération au format GraphViz DOT");
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

         // Exécution avec autres options
         modeDot = options.has("d"); // digraph DOT
         Valeur vr = new ValeurDef(cont);
         List<?> lc = options.nonOptionArguments();
         if (lc.isEmpty() || lc.size() > 2)
            return cont.erreur("TREE", "une table, [une profondeur] attendues", lng);

         if (lc.size() == 2) {
            try {
               deepMax = Integer.parseInt((String)lc.get(1)) - 1;
               if (deepMax < 0) throw new NumberFormatException("Profondeur négative invalide");
            }
            catch (NumberFormatException ex) {
               return cont.erreur("TREE", "profondeur incorrecte : " + ex.getMessage(), lng);
            }
         }
         else deepMax = 2;

         String table = (String)lc.get(0);
         DatabaseMetaData dMeta = cont.getConnex().getMetaData();
         
         // Recherche de la table
         boolean hastb = false;
         ResultSet rs = dMeta.getTables(null, null, null, new String[] {
               "TABLE", "VIEW", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"});
         while(rs.next()) {
           if (table.equalsIgnoreCase(rs.getString(3))) {
              hastb = true;
              table = rs.getString(3);
           }
         }
         rs.close();
         if (!hastb) return cont.erreur("TREE", "table '" + table + "' non trouvée", lng);

         StringBuilder ret = new StringBuilder();
         if (modeDot) ret.append("digraph {\n");
         ret.append(drawTree(dMeta, table, 0, ""));
         if (modeDot) {
            ret.append('}');
            vr.setSubValue(ret.toString());
         }
         else {
            vr.setDispValue(ret.length() == 0 ? "Aucune référence vers cette table" : ret.toString(),
                 Contexte.VERB_AFF);
         }
         cont.setValeur(vr);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("TREE", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException|SQLException ex) {
         return cont.exception("TREE", "ERREUR " + ex.getClass().getSimpleName() + " : " +
               ex.getMessage(), lng, ex);
      }
   }

   /**
    * Descente récursive dans les tables par les clefs étrangères
    *
    * @param dMeta le DatabaseMetaData
    * @param tbprnt le nom de la table parent
    * @param deep la profondeur en cours
    * @param tblist la list des tables rencontrées
    * @return la construction
    * @throws SQLException si erreur SQL
    */
   public String drawTree(DatabaseMetaData dMeta, String tbprnt, int deep, String tblist)
           throws SQLException {
      StringBuilder sb = new StringBuilder();
      ResultSet rs = dMeta.getExportedKeys(null, null, tbprnt);
      while (rs.next()) {
         String flprnt = rs.getString("PKCOLUMN_NAME"),
                tbenft = rs.getString("FKTABLE_NAME"), flenft = rs.getString("FKCOLUMN_NAME");
         if (modeDot) {
            sb.append("  ").append(tbprnt).append(" -> ").append(tbenft); // tables
            sb.append(" [label=\"").append(flprnt).append('=').append(flenft); // relations
            sb.append("\", fontsize=12.0];\n");
         } else {
            String rp = repeat(deep);
            sb.append("\n");
            sb.append(rp).append(tbprnt).append('(').append(flprnt).append(")\n"); // parent
            sb.append(rp).append("|\n").append(rp).append("+-- ");
            sb.append(tbenft).append("(").append(flenft).append(")"); // enfant
         }
         if (tblist.contains(tbprnt + "." + flprnt)) sb.append("  (...)");
         else if (deep < deepMax)
            sb.append(drawTree(dMeta, tbenft, deep + 1, tblist + "|" + tbprnt + "." + flprnt));
      }
      rs.close();
      return sb.toString();
   }

   /**
    * Fixe la variable d'instance deepMax (proondeur max)
    * 
    * @param d la nouvelle valeur
    */
   public void setDeepMax(int d) {
      deepMax = d;
   }

   /**
    * Répétition de chaîne
    *
    * @param n le nb de fois
    * @return str
    */
   private static String repeat(int n) {
      return new String(new char[n]).replace("\0", "    ");
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  tree        Affiche un schéma de structure de la base\n";
   }
}// class
