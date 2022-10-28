package lunasql.cmd;

import java.sql.SQLException;

import lunasql.lib.Contexte;
import lunasql.sql.SQLCnx;
import lunasql.val.ValeurReq;

/**
 * Commande SELECT <br>
 * (SQL) Selection et affichage de données depuis les tables de la base
 * @author M.P.
 */
public class CmdSelect extends Instruction {

   public CmdSelect(Contexte cont){
      super(cont, TYPE_CMDSQL, "SELECT", null);
   }

   @Override
   public int execute() {
      try {
         cont.showWheel();
         long tm = System.currentTimeMillis();
         ValeurReq vr = new ValeurReq(cont, cont.getConnex().getResultString(getSCommand(),
               cont.getVar(Contexte.ENV_SELECT_ARR).equals(Contexte.STATE_TRUE),
               cont.getColMaxWidth(lng), cont.getRowMaxNumber(lng),
               cont.getVar(Contexte.ENV_ADD_ROW_NB).equals(Contexte.STATE_TRUE),
               cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE)));
         tm = System.currentTimeMillis() - tm;
         cont.hideWheel();

         int n = vr.getNbLines();
         if (cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE))
            vr.setDispValue("-> " + Contexte.COLORS[Contexte.BR_CYAN] + n + Contexte.COLORS[Contexte.NONE] +
                  " ligne" + (n > 1 ? "s" : "") + " récupérée" + (n > 1 ? "s" : "") +
                  (vr.isNblTrunc() ? Contexte.COLORS[Contexte.BR_YELLOW] + " mais d'autres lignes existent" +
                        Contexte.COLORS[Contexte.NONE] : "") + " (" + SQLCnx.frmDur(tm) + ")");
         else
            vr.setDispValue("-> " + n + " ligne" + (n > 1 ? "s" : "") + " récupérée" + (n > 1 ? "s" : "") +
                  (vr.isNblTrunc() ? " mais d'autres lignes existent" : "") + " (" + SQLCnx.frmDur(tm) + ")");

         cont.setValeur(vr);
         cont.setVar(Contexte.ENV_EXEC_TIME, Long.toString(tm));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (SQLException e) {
         cont.hideWheel();
         return cont.exception("SELECT", "ERREUR SQLException : " + e.getMessage(), lng, e);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  select  (+instr)   Consulte des données de la base\n";
   }
}// class

