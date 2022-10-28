package lunasql.cmd;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurExe;
import lunasql.val.ValeurReq;

/**
 * Commande SPEC <br>
 * (SQL) Exécution d'une commande spécifique d'une base de données
 * @author M.P.
 */
public class CmdSpec extends Instruction {

   private final OptionParser parser;

   public CmdSpec(Contexte cont) {
      super(cont, TYPE_CMDSQL, "SPEC", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("s", "commande de sélection");
      parser.nonOptions("commande_sql").ofType(String.class);
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
         List<?> lc = options.nonOptionArguments();
         if (lc.isEmpty())
            return cont.erreur("SPEC", "une commande SQL est attendue", lng);

         long tm;
         if (options.has("s")) {
            tm = System.currentTimeMillis();
            cont.showWheel();
            ValeurReq vr = new ValeurReq(cont, cont.getConnex().getResultString(listToString(lc),
                  cont.getVar(Contexte.ENV_SELECT_ARR).equals(Contexte.STATE_TRUE),
                  cont.getColMaxWidth(lng), cont.getRowMaxNumber(lng),
                  cont.getVar(Contexte.ENV_ADD_ROW_NB).equals(Contexte.STATE_TRUE),
                  cont.getVar(Contexte.ENV_COLORS_ON).equals(Contexte.STATE_TRUE)));
            tm = System.currentTimeMillis() - tm;
            cont.hideWheel();

            int n = vr.getNbLines();
            vr.setDispValue("-> " + n + " ligne" + (n > 1 ? "s" : "") + " trouvée" + (n > 1 ? "s" : "") + " (" +
                    SQLCnx.frmDur(tm) + ")");
            cont.setValeur(vr);
            cont.setVar(Contexte.ENV_EXEC_TIME, Long.toString(tm));
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         else {
            cont.showWheel();
            tm = System.currentTimeMillis();
            Valeur vr = new ValeurExe(cont, cont.getConnex().execute(listToString(lc)));
            tm = System.currentTimeMillis() - tm;
            cont.hideWheel();
            vr.setDispValue("-> commande exécutée (" + SQLCnx.frmDur(tm) + ")");
            cont.setValeur(vr);
            cont.setVar(Contexte.ENV_EXEC_TIME, Long.toString(tm));
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         return RET_CONTINUE;
      }
      catch (IOException|SQLException ex) {
         cont.hideWheel();
         return cont.exception("SPEC", "ERREUR " + ex.getClass().getSimpleName() + " : " +
               ex.getMessage(), lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  spec    command    Lance une commande spécifique à la base\n";
   }
}// class

