package lunasql.cmd;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.PatternSyntaxException;

import lunasql.lib.Contexte;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande SIZE <br>
 * (Interne) Affichage du nombre de lignes d'une table de la base
 * @author M.P.
 */
public class CmdSize extends Instruction {

   private int nbl;
   private Valeur vr;

   public CmdSize(Contexte cont) {
      super(cont, TYPE_CMDINT, "SIZE", "*");
   }

   @Override
   public int execute() {
      if(getLength() == 1)
         return cont.erreur("SIZE", "un nom d'objet (table ou vue) au moins est attendu", lng);

      String[] usertables = {"TABLE", "GLOBAL TEMPORARY", "VIEW"};
      DatabaseMetaData dMeta = cont.getConnex().getMetaData();
      int ret = RET_CONTINUE;
      nbl = 0;
      vr = null;
      for (int i = 1; i < getLength(); i++) { // pour chaque objet
         try {
            ResultSet result = dMeta.getTables(null, null, null, usertables);
            String tb, nom = Tools.removeBQuotes(getArg(i)).value;
            boolean found = false;
            while(result.next()){
               tb = result.getString(3);
               if (tb.matches("(?i)" + nom)) {
                  found = true;
                  seekSize(tb, true);
               }
            }
            if (!found) seekSize(nom, false);
            cont.setValeur(vr);
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

         }
         catch (SQLException ex) {
            ret = cont.exception("SIZE", "ERREUR SQLException : "+ ex.getMessage(), lng, ex);
         }
         catch (PatternSyntaxException ex) {
            ret = cont.erreur("SIZE", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
         }
      }
      return ret;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  size, *     Affiche la taille de la (ou les) table(s) en paramètre\n";
   }

   /*
    * Construit la chaîne à afficher en fonction de la table donnée
    * @param tb le nom de la table
    * @return le nombre de lignes de la table tb
    */
   private void seekSize(String tb, boolean f) throws SQLException {
      long tm = System.currentTimeMillis();
      int v = cont.getConnex().seekNbl(tb);
      tm = System.currentTimeMillis() - tm;
         if (vr == null) vr = new ValeurDef(cont);
         else vr.appendDispValue("\n", Contexte.VERB_AFF);
         nbl += v;
         vr.appendDispValue("- " + SQLCnx.frm(tb.toUpperCase() + ' ' + (f ? "(user)" : "(non user)"), 33, ' ') +
                 ' ' + v + " ligne" + (v > 1 ? "s" : "") + " (" + SQLCnx.frmDur(tm) + ")", Contexte.VERB_AFF);
         vr.setSubValue(Integer.toString(nbl));
         vr.setRet(Integer.toString(v));
   }
}// class

