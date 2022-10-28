import lunasql.lib.Contexte;
import lunasql.cmd.Instruction;

/**
 * Commande ECHO <br />
 * Copie de la commande PRINT pour test des greffons
 *
 * Utilisation : 
 * SQL> plugin echo CmdEchoPlugin
 * SQL> echo ?a fonctionne
 *
 * @author M.P.
 */
public class CmdEchoPlugin extends Instruction {

   public CmdEchoPlugin(Contexte cont) {
      super(cont, TYPE_CMDPLG, "ECHO", null);
   }

   public int execute() {
      if (cont.getVerbose() >= Contexte.VERB_AFF) cont.println(getSCommand(1));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      cont.setValeur(null); // ne retourne rien
      return RET_CONTINUE;
   }
  public String getDesc(){
    return "  echo  Affiche un message\n";
  } 
  public String getHelp(){
    return "ECHO msg : affiche le message msg";
  }    
}// class

