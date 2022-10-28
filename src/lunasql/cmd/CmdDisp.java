package lunasql.cmd;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.PatternSyntaxException;

import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurReq;

/**
 * Commande DISP <br>
 * (Interne) Listage du contenu d'une table
 * @author M.P.
 */
public class CmdDisp extends Instruction {

   public CmdDisp(Contexte cont) {
      super(cont, TYPE_CMDINT, "DISP", "\\");
   }

   @Override
   public int execute() {
      if (getLength() >= 2) {
         Lecteur lec = new Lecteur(cont);
         String[] usertables = {"TABLE", "GLOBAL TEMPORARY", "VIEW"};
         DatabaseMetaData dMeta = cont.getConnex().getMetaData();
         boolean err = false;
         int ret = RET_CONTINUE, nbl = 0;
         String prem = null;
         StringBuilder sb = new StringBuilder();

         // Environnement
         boolean addrownb = cont.getVar(Contexte.ENV_ADD_ROW_NB).equals(Contexte.STATE_TRUE),
                 colorson = cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE);

         // Boucle sur arguments
         for (int i = 1; i < getLength(); i++) {
            try {
               ResultSet result = dMeta.getTables(null, null, null, usertables);
               String tb;
               boolean found = false;
               while(result.next()) {
                  tb = result.getString(3);
                  if (tb.matches("(?i)" + Tools.removeBQuotes(getArg(i)).value)) {
                     found = true;
                     sb.append("Table user trouvée : ").append(tb).append('\n');
                     SQLCnx.QueryResult v = cont.getConnex().getResultString("SELECT * FROM " + tb,
                           true, cont.getColMaxWidth(lng), cont.getRowMaxNumber(lng), addrownb, colorson);
                     sb.append(v.result).append('\n');
                     if (v.istrunc) {
                        if (cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE))
                           sb.append("Note : ").append(Contexte.COLORS[Contexte.BR_YELLOW])
                              .append("d'autres lignes existent").append(Contexte.COLORS[Contexte.NONE]).append('\n');
                        else
                           sb.append("Note : d'autres lignes existent\n");
                     }
                     if (prem == null) prem = v.premVal;
                     nbl += v.nbLng;
                  }
               }
               if (!found) lec.add("SELECT * FROM " + getArg(i) + ';');

            }
            catch (SQLException ex) {
               err = true;
               ret = cont.exception("DISP", "ERREUR SQLException : " + ex.getMessage(), lng, ex);
            }
            catch (PatternSyntaxException ex) {
               err = true;
               ret = cont.erreur("DISP", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
            }
         }// for
         lec.fin();
         if (err) return ret;
         if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);

         cont.setValeur(new ValeurReq(cont, sb.toString(), prem, nbl, false));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return lec.getCmdState();
      }
      else return cont.erreur("DISP", "un nom de table/vue au moins est requis", lng);
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  disp, \\     Affiche le contenu de la (ou les) table(s) en paramètre\n";
   }
}// class

