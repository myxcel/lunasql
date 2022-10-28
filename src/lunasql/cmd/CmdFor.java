package lunasql.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.val.Valeur;

/**
 * Commande FOR <br>
 * (Interne) Parcours d'un resultset d'une requête SELECT/TABLE ou d'une liste
 * @author M.P.
 */
public class CmdFor extends Instruction {

   private final OptionParser parser;

   public CmdFor(Contexte cont){
      super(cont, TYPE_CMDINT, "FOR", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("n", "avec -q, variables : noms des champs");
      parser.accepts("q", "boucle sur une table/requête").requiredIf("n");
      parser.accepts("r", "boucle sur une séquence");
      parser.accepts("f", "boucle sur les lignes d'un fichier");
      parser.accepts("s", "séparateur de valeurs en mode liste").withRequiredArg()
         .ofType(String.class).describedAs("sep");
      parser.nonOptions("nom_var liste_valeurs liste_commandes").ofType(String.class);
   }

   @Override
   public int execute() {
      try {
         OptionSet options = parser.parse(getCommandA1());
         // Aide sur les options
         if (options.has("?")) {
            parser.printHelpOn(cont.getWriterOrOut());
            cont.setValeur(null);
            return RET_CONTINUE;
         }

         // Exécution avec autres options
         List<?> lc = options.nonOptionArguments();
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "for");
         vars.put(Contexte.LEC_LOOP_DEEP, cont.incrLoopDeep()); // profondeur de boucle
         int ret = RET_CONTINUE;
         boolean exec = false;
         String iter, cmds;

         if (options.has("q")) { // boucle sur requête/table
            if (lc.size() < 2)
               return cont.erreur("FOR", "avec -q, une requête/table et un bloc sont attendus", lng);

            iter = Tools.removeBracketsIfAny((String)lc.get(0)).trim();
            if (iter.isEmpty()) return cont.erreur("FOR", "requête SQL vide avec option -q", lng);

            cmds = (String) lc.get(1);
            if (cmds.isEmpty()) {
               cont.setValeur(null);
               return RET_CONTINUE;
            }

            if (!iter.matches("(?is)SELECT\\s+.*")) iter = "SELECT * FROM " + iter;
            if (cont.getVerbose() >= Contexte.VERB_DBG) cont.println("Requête : " + iter);
            // Ici on ne prend pas la méthode SQLCnx.getResultset() car exécution concurencielle dans evaluerBlockFor()
            Statement statmt = cont.getConnex().getConnex().createStatement(
                  ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = statmt.executeQuery(iter);

            ResultSetMetaData rsm = rs.getMetaData();
            int nbCol = rsm.getColumnCount(), nbl = 0;
            vars.put("col_nb", Integer.toString(nbCol));
            for (int i = 1; i <= nbCol; i++) vars.put("col_n" + i, rsm.getColumnName(i));

            // Parcours de la liste et exécution en Lecteur
            Lecteur lec = new Lecteur(cont);
            while (ret == RET_CONTINUE && rs.next()) {
               // Variables de lecteur
               vars.put("row_id", Integer.toString(++nbl));
               for (int i = 1; i <= nbCol; i++) vars.put("col" + i, rs.getString(i));
               if (options.has("n")) {
                  int nbExpr = 0;
                  for (int i = 1; i <= nbCol; i++) {
                     String k = rsm.getColumnName(i);
                     if (k == null || k.isEmpty()) vars.put("Expr" + String.format("%02d", ++nbExpr), rs.getString(i));
                     else if (cont.valideKey(k)) vars.put(k, rs.getString(i));
                  }
               }
               ret = cont.evaluerBlockFor(lec, cmds, vars);
               exec = true;
            }
            lec.fin();
            rs.close();
            statmt.close();
         }
         else if (options.has("r")) { // boucle sur séquence (range)
            if (lc.size() != 3)
               return cont.erreur("FOR", "avec -r, une variable, une séquence et un bloc sont attendus", lng);

            String key = (String) lc.get(0);
            iter = Tools.removeBracketsIfAny((String) lc.get(1));
            cmds = (String) lc.get(2);
            if (cmds.isEmpty()) {
               cont.setValeur(null);
               return RET_CONTINUE;
            }
            if (!cont.valideKey(key))
               return cont.erreur("FOR", "affectation de variable invalide", lng);
            if (cont.getVerbose() >= Contexte.VERB_DBG) cont.println("Séquence : " + iter);

            String[] seq = iter.split(":");
            int deb = 0, fin, pas = 1;
            try {
               switch (seq.length) {
               case 1:
                  fin = Integer.parseInt(seq[0]);
                  break;
               case 2:
                  deb = Integer.parseInt(seq[0]);
                  fin = Integer.parseInt(seq[1]);
                  break;
               case 3:
                  deb = Integer.parseInt(seq[0]);
                  fin = Integer.parseInt(seq[1]);
                  pas = Integer.parseInt(seq[2]);
                  break;
               default:
                  return cont.erreur("FOR", "séquence attendue : début:fin:pas", lng);
               }
               if (pas == 0)
                  return cont.erreur("FOR", "séquence infinie non autorisée", lng);
            }
            catch (NumberFormatException ex) {
               return cont.erreur("FOR", "paramètre de séquence invalide : " + ex.getMessage(), lng);
            }

            // Boucle for sur séquence
            Lecteur lec = new Lecteur(cont);
            for (int i = deb; ret == RET_CONTINUE && (pas > 0 ? i < fin : i > fin); i += pas) {
               // Variables de lecteur
               vars.put(key, Integer.toString(i));
               ret = cont.evaluerBlockFor(lec, cmds, vars);
               exec = true;
            }
            lec.fin();
         }
         else if (options.has("f")) { // boucle sur lignes de fichier
            if (lc.size() != 3)
               return cont.erreur("FOR",
                     "avec -f, une variable, un fichier et un bloc sont attendus", lng);

            String key = (String) lc.get(0);
            iter = (String) lc.get(1);
            cmds = (String) lc.get(2);
            if (cmds.isEmpty()) {
               cont.setValeur(null);
               return RET_CONTINUE;
            }
            if (!cont.valideKey(key) || !cont.isNonSys(key))
               return cont.erreur("FOR", "affectation de variable invalide", lng);

            // Fichier à itérer
            File f = new File(iter);
            try {
               BufferedReader br = new BufferedReader(new FileReader(f));
               String line;
               Lecteur lec = new Lecteur(cont);
               while ((line = br.readLine()) != null) {
                  // Variables de lecteur
                  vars.put(key, line);
                  ret = cont.evaluerBlockFor(lec, cmds, vars);
                  exec = true;
               }
               lec.fin();
               br.close();
            }
            catch (FileNotFoundException ex) { // IOException prévisible
               return cont.erreur("FOR", "le fichier " + f.getAbsolutePath() + " n'existe pas", lng);
            }
         }
         else { // boucle sur simple liste
            if (lc.size() != 3)
               return cont.erreur("FOR",
                     "une (ou des) var, une liste et un bloc sont attendus", lng);

            String[] keys = ((String)lc.get(0)).split(",");
            if (keys.length == 0) return cont.erreur("FOR", "aucune variable de boucle fournie", lng);

            iter = (String) lc.get(1);
            cmds = (String) lc.get(2);
            if (cmds.isEmpty()) {
               cont.setValeur(null);
               return RET_CONTINUE;
            }

            // Variables et itération
            for (String key : keys) {
               if (!cont.valideKey(key) || !cont.isNonSys(key))
                  return cont.erreur("FOR", "affectation de variable invalide : " + key, lng);
               vars.put(key, "");
            }
            String[] tb;
            if (options.has("s"))  // support des "", [] et {}
               tb = Tools.removeBracketsIfAny(iter).split((String) options.valueOf("s"));
            else tb = Tools.blankSplitLec(cont, iter);  // support des "" et {}
            if (cont.getVerbose() >= Contexte.VERB_DBG) cont.println("Tableau : " + iter);
            if (tb.length == 0) {
               cont.setValeur(null);
               return RET_CONTINUE;
            }

            // Parcours de la liste et exécution en Lecteur
            Lecteur lec = new Lecteur(cont);
            for (int i = 0; ret == RET_CONTINUE && i < tb.length; i += keys.length) {
               for (int j = 0; j < keys.length; j++) {
                  vars.put(keys[j], i + j < tb.length ? tb[i + j] : "");
               }
               ret = cont.evaluerBlockFor(lec, cmds, vars);
               exec = true;
            }
            lec.fin();
         }

         if (exec) {
            Valeur vr = cont.getValeur();
            if (vr != null) vr.setDispValue(null, Contexte.VERB_SIL);
         } else cont.setValeur(null);  // retour sans exécution

         if (ret == RET_BREAK_LP) {
            String lb = cont.getLecVar(Contexte.LEC_LOOP_BREAK); // si _LOOP_BREAK positionnée
            return lb == null ? RET_CONTINUE : RET_BREAK_LP;
         }
         else return ret;
      }
      catch (SQLException e) {// erreur prévisible > cont.erreur
         return cont.erreur("FOR", "Impossible d'exécuter la requête : " + e.getMessage(), lng);
      }
      catch (OptionException ex) {
         return cont.exception("FOR", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("FOR", "ERREUR IOException : " + ex.getMessage() , lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  for         Parcourt une liste ou le résultat d'une requête / table\n";
   }
}// class
