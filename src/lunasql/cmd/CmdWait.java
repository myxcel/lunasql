package lunasql.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jline.ConsoleReader;
import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;
//import jline.console.ConsoleReader;

/**
 * Commande WAIT <br>
 * (Interne) Attente d'un évènement et affichage éventuel d'un message
 * @author M.P.
 */
public class CmdWait extends Instruction {

   public CmdWait(Contexte cont) {
      super(cont, TYPE_CMDINT, "WAIT", "~");
   }

   @Override
   public int execute() {
      if (getLength() == 1)
         return cont.erreur("WAIT", "une durée en ms et un message optionnel sont attendus", lng);

      try {
         String t = getArg(1);
         long delai;
         if (t.length() > 2 && t.charAt(0) == '\'' && t.charAt(t.length() - 1) == '\'') {
            Date d = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS").parse(t.substring(1, t.length() - 1));
            delai = d.getTime() - System.currentTimeMillis();
            if (delai < 0) delai = 1L;
         }
         else delai = Long.parseLong(t);
         if (delai <= 0) {
            if (cont.isHttpMode())
               return cont.erreur("WAIT", "cette commande n'est pas autorisée en mode HTTP", lng);

            // Attente de la frappe sur ENTREE
            if (cont.getVerbose() >= Contexte.VERB_AFF) cont.println(getLength() == 2 ?
                  "Veuillez appuyer sur <ENTREE> pour continuer..." : getSCommand(2));
            ConsoleReader reader = cont.getConsoleReader();
            if (reader == null) new BufferedReader(new InputStreamReader(System.in)).readLine();
            else reader.readLine("");
         }
         else {
            cont.showWheel();
            Thread.sleep(delai); // Attente du délai fourni
            cont.hideWheel();
         }

         cont.setValeur(new ValeurDef(cont, getSCommand(2), Contexte.VERB_AFF, Long.toString(delai)));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (ParseException ex) {// erreur prévisible > cont.erreur
         return cont.erreur("WAIT", "format de date incorrect : " + getArg(1), lng);
      }
      catch (NumberFormatException ex) {// erreur prévisible > cont.erreur
         return cont.erreur("WAIT", "délai incorrect : " + getArg(1), lng);
      }
      catch (IOException ex) {
         return cont.exception("WAIT", "erreur IO sur entrée standard : ", lng, ex);
      }
      catch (InterruptedException ex) {
         cont.hideWheel();
         return cont.exception("WAIT", "ERREUR InterruptedException : ", lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  wait, ~     Attend le temps indiqué ou une frappe clavier\n";
   }
}// class

