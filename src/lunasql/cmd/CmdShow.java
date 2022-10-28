package lunasql.cmd;

import static lunasql.sql.SQLCnx.frm;
import static lunasql.sql.SQLCnx.frmI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import jline.ConsoleReader;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.sql.TypesSGBD;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;
//import jline.console.ConsoleReader;

/**
 * Commande SHOW <br>
 * (Interne) Affichage des caractéristiques des tables/objets de la base
 * @author M.P.
 */
public class CmdShow extends Instruction {

   private final OptionParser parser;

   public CmdShow(Contexte cont) {
      super(cont, TYPE_CMDINT, "SHOW", "#");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("t", "tables utilisateur");
      parser.accepts("v", "vues");
      parser.accepts("s", "tables système");
      parser.accepts("l", "comptage du nb de lignes");
      parser.accepts("c", "liste des catalogs");
      parser.accepts("m", "liste des schémas");
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
         Valeur vr = new ValeurDef(cont);
         List<?> lc = options.nonOptionArguments();
         StringBuilder sb = new StringBuilder();

         int[] rowCounts;
         String schema = null;
         ArrayList<String> tables = new ArrayList<>(),
                 schemas = new ArrayList<>(),
                 remarks = new ArrayList<>();
         long tm = System.currentTimeMillis();
         DatabaseMetaData dMeta = cont.getConnex().getMetaData();

         // Tables et vues
         String[] types = new String[] {};
         if (options.has("v"))
            types = Tools.arrayAppend(types, "VIEW");
         if (options.has("t"))
            types = Tools.arrayAppend(types, "TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM");
         if (options.has("s"))
            types = Tools.arrayAppend(types, "SYSTEM TABLE");
         if (types.length == 0) types = new String[] {
               "TABLE", "VIEW", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"};

         ResultSet result = dMeta.getTables(null, null, null, types);
         while(result.next()) {
            schema = result.getString(2); // on sauvegarde le dernier
            schemas.add(schema);
            tables.add(result.getString(3));
            remarks.add(result.getString(5));
         }
         result.close();
         rowCounts = new int[tables.size()];

         // Catalogs
         if (options.has("c")) {
            ResultSet rscat = dMeta.getCatalogs();
            int n = 0;
            sb.append(frm("Catalogs", 30, ' ')).append('\n').append(frm("", 30, '-')).append('\n');
            while (rscat.next()) {
               n++;
               sb.append(rscat.getString("TABLE_CAT")).append('\n');
            }
            rscat.close();
            vr.setSubValue(Integer.toString(n));
         }

         // Schémas
         else if (options.has("m")) {
            ResultSet rscat = dMeta.getSchemas();
            int n = 0;
            sb.append(frm("Schémas", 30, ' ')).append('\n').append(frm("", 30, '-')).append('\n');
            while (rscat.next()) {
               n++;
               String cat = rscat.getString("TABLE_CATALOG");
               if (cat != null) sb.append(cat).append('.');
               sb.append(rscat.getString("TABLE_SCHEM")).append('\n');
            }
            rscat.close();
            vr.setSubValue(Integer.toString(n));
         }

         // Affichage de la structure globale des tables non sys
         else if (lc.isEmpty() || options.has("t")) {
            int t = cont.getConnex().getType();
            // Test du SGBD : si Oracle ou SQLServer (les usines à gaz), on demande confirmation
            if (t == TypesSGBD.TYPE_ORACLE || t == TypesSGBD.TYPE_SQLSERV) {
               try {
                  String p = "Nous sommes sur un SGBD sensible ! Continuer (O/N) ? ", val;
                  ConsoleReader reader = cont.getConsoleReader();
                  if(reader == null){
                     if (cont.getVerbose() >= Contexte.VERB_AFF) cont.print(p);
                     val = new BufferedReader(new InputStreamReader(System.in)).readLine();
                  } else {
                     val = reader.readLine(p);
                     if (val == null) val = "";
                  }
                  if (!val.equalsIgnoreCase("O")) return RET_CONTINUE;
               } catch (IOException ex) {
                  return cont.exception("SHOW", "erreur IOException : " + ex.getMessage(), lng, ex);
               }
            }
            sb.append(frm("Nom des tables", 20, ' ')).append(frm("Nb lignes", 11, ' '))
                 .append(frm("Schéma", 20, ' ')).append(frm("Description", 25, ' '))
                 .append('\n').append(frm("", 80, '-')).append('\n');
            for (int i = 0; i < tables.size(); i++) {
               try {
                  String schp = schemas.get(i);
                  if (!options.has("l")) rowCounts[i] = 0;
                  else {
                     int n = cont.getConnex().seekNbl((schp == null ? "" : schp + ".") + tables.get(i));
                     rowCounts[i] = n;
                  }
                  sb.append(frm(tables.get(i), 19, ' ')).append(' ')
                       .append(frmI("" + rowCounts[i], 10, ' ')).append("  ")
                       .append(frm(schemas.get(i), 24, ' ')).append(' ')
                       .append(remarks.get(i)).append('\n'); // desciption non formatée
               }
               catch (Exception ex) {
                  cont.exception("SHOW", "impossible d'obtenir la taille de " + tables.get(i), lng, ex);
               }
            }
            tm = System.currentTimeMillis() - tm;
            vr.setDispValue("(" + SQLCnx.frmDur(tm) + ")", Contexte.VERB_MSG);
            vr.setSubValue(Integer.toString(tables.size()));
         }

         // Affichage de la structure d'une table
         else if (lc.size() >= 1) for (int i = 0; i < lc.size(); i++ ) {
            String tblist = (String)lc.get(i), tbname = null;
            boolean prem = i == 0;
            // Recherche de la table (en ignorant la casse) ou de x tables nommées par regexp
            for (String table : tables) {
               if (table.matches("(?i)" + Tools.removeBQuotes(tblist).value)) {
                  tbname = table;
                  tm = System.currentTimeMillis();

                  // Affichage des informations
                  HashMap<String, String> fk = new HashMap<>();
                  ResultSet rsfk = dMeta.getExportedKeys(null, schema, tbname);
                  while (rsfk.next()) {
                     String k = rsfk.getString("PKCOLUMN_NAME"),
                           v = fk.get(rsfk.getString("PKCOLUMN_NAME"));
                     fk.put(k, (v == null ? "" : v + ", ") +
                           rsfk.getString("FKTABLE_NAME") + "(" + rsfk.getString("FKCOLUMN_NAME") + ")");
                  }
                  rsfk.close();
                  ResultSet rscol = dMeta.getColumns(null, schema, tbname, null);
                  sb.append(prem ? "" : "\n")
                        .append("Table trouvée : ").append(tbname).append('\n')
                        .append(frm("Colonne", 20))
                        .append(frm("Type", 10))
                        .append(frm("Taille", 10))
                        .append(frm("Null?", 6))
                        .append(frm("Référencé par", 30))
                        .append(frm("Commentaire", 25)).append('\n')
                        .append(frm("", 99, '-')).append('\n');
                  int nbcol = 0;
                  while (rscol.next()) {
                     nbcol++;
                     String col = rscol.getString(4);
                     sb.append(frm(col, 19, ' ')).append(' ')         // Colonne
                           .append(frm(rscol.getString(6), 9, ' ')).append(' ')      // Type
                           .append(frm(Integer.toString(rscol.getInt(7)), 9, ' ')).append(' ') // Taille
                           .append(rscol.getInt(11) != 0 ? "oui" : "non").append("   "); // Null
                     if (fk.containsKey(col)) sb.append(frm(fk.get(col), 30));
                     sb.append(frm(rscol.getString(12), 24, ' ')).append(' ') // Commentaire
                           .append('\n');
                  }
                  rscol.close();

                  // Count lignes si option -l
                  if (options.has("l")) {
                     tm = System.currentTimeMillis() - tm;
                     vr.setDispValue("Nombre de lignes : " + cont.getConnex().seekNbl(tbname)
                           + " (" + SQLCnx.frmDur(tm) + ")", Contexte.VERB_MSG);
                  }
                  vr.setSubValue(Integer.toString(nbcol));
                  prem = false;
               }
            }
            if (tbname == null)
               return cont.erreur("SHOW", "Impossible de trouver la table '" + tblist + "'", lng);
         }

         if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
         vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
         vr.setRet();
         cont.setValeur(vr);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("EXPORT", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (PatternSyntaxException ex) {
         return cont.erreur("SHOW", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
      }
      catch (IOException|SQLException ex) {
         return cont.exception("SHOW", "ERREUR " + ex.getClass().getSimpleName() + " : " +
               ex.getMessage(), lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  show, #     Affiche le catalogue complet des objets de la base\n";
   }
}// class

