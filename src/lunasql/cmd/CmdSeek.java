package lunasql.cmd;

import java.sql.SQLException;

import lunasql.lib.Contexte;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande SEEK <br>
 * (Interne) Evaluation d'une expression SQL
 * @author M.P.
 */
public class CmdSeek extends Instruction {

   public CmdSeek(Contexte cont) {
      super(cont, TYPE_CMDINT, "SEEK", "/");
   }

   @Override
   public int execute() {
      Valeur vr = new ValeurDef(cont);
      String s = getSCommand(1), val, nbl;
      int id = s.indexOf(' ');
      String c = (id < 0 ? s : s.substring(0, id)).toUpperCase();
      try {
         long tm = System.currentTimeMillis();
         if (c.equals("SELECT") || c.equals("CALL") || c.equals("EXPLAIN")) {
            val = cont.getConnex().seek(s, cont.getVar(Contexte.ENV_FILE_ENC), cont.getRowMaxNumber(lng));
            tm = System.currentTimeMillis() - tm;
            if (val == null) val = "";
            nbl = "1";
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         else {
            int n = cont.getConnex().execute(s);
            tm = System.currentTimeMillis() - tm;
            val = "0";
            nbl = Integer.toString(n);
            cont.setVar(Contexte.ENV_CMD_STATE, n > 0 ? Contexte.STATE_TRUE : Contexte.STATE_FALSE);
         }
         cont.setVar(Contexte.ENV_EXEC_TIME, Long.toString(tm));
      }
      catch (SQLException ex) {
         return cont.exception("SEEK", "ERREUR SQLException : " + ex.getMessage(), lng, ex);
      }

      // Retour
      vr.setSubValue(val);
      vr.setRet(nbl);
      cont.setValeur(vr);
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  seek, /     Évalue une commande SQL et retourne le résultat\n";
   }
}// class

