package lunasql.misc;

import java.io.StringReader;

import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;

/**
 * Commande FRM <br>
 * (Interne) Formatte une commande SQL par JSQLParseur
 * @author M.P.
 */
public class CmdFrm extends Instruction {

   public CmdFrm(Contexte cont){
      super(cont, TYPE_CMDPLG, "FRM", "F");
   }

   @Override
   public int execute() {
      if (getLength() < 2)
         return cont.erreur("FRM", "une commande SQL est attendue", lng);

      try {
         net.sf.jsqlparser.parser.CCJSqlParserManager pm = new net.sf.jsqlparser.parser.CCJSqlParserManager();
         net.sf.jsqlparser.statement.Statement stat = pm.parse(new StringReader(getSCommand(1)));
         String sql = stat.toString();
         cont.setValeur(new ValeurDef(cont, "SQL formaté :\n" + sql, Contexte.VERB_BVR, sql));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (net.sf.jsqlparser.JSQLParserException ex) {
         Throwable t = ex.getCause();
         if (t == null)
            return cont.exception("FRM", "ERREUR JSQLParserException : " + ex.getMessage(), lng, ex);
         return cont.erreur("FRM", "Erreur de syntaxe dans la commande à formater : " + t.getMessage(), lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  frm, f      Formate une commande SQL\n";
   }
}// class
