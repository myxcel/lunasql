package lunasql.cmd;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Tools;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande SHELL <br>
 * (Interne) Exécution d'une commande du shell externe
 * @author M.P.
 */
public class CmdShell extends Instruction {

   private final OptionParser parser;

   public CmdShell(Contexte cont){
      super(cont, TYPE_CMDINT, "SHELL", "$");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("d", "ouverture en mode Desktop");
      parser.nonOptions("commande").ofType(String.class);
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
         List<?> lf = options.nonOptionArguments();
         if (lf.isEmpty())
            return cont.erreur("SHELL", "une commande Shell ou une ressource est attendue", lng);

         Valeur vr = new ValeurDef(cont);
         if (options.has("d")) {
            if (Desktop.isDesktopSupported()) {
               Desktop dk = Desktop.getDesktop();
               String adr = (String) lf.get(0);
               if (adr.matches("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")){
                  if (dk.isSupported(Desktop.Action.BROWSE)) dk.browse(new URI(adr));
                  else return cont.erreur("SHELL", "Desktop mode BROWSE non supporté", lng);
               }
               else if(adr.matches("mailto:[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}\\?.*")) {
                  if (dk.isSupported(Desktop.Action.MAIL)) dk.mail(new URI(adr));
                  else return cont.erreur("SHELL", "Desktop mode MAIL non supporté", lng);
               }
               else {
                  File fadr = new File(adr);
                  if (!fadr.isFile() || !fadr.canRead()){
                     return cont.erreur("SHELL", "le fichier '" + fadr.getCanonicalPath()
                           + "' est inaccessible ou n'est pas une ressource valide", lng);
                  }
                  if (dk.isSupported(Desktop.Action.OPEN)) dk.open(fadr);
                  else return cont.erreur("SHELL", "Desktop mode OPEN non supporté", lng);
               }

               vr.setDispValue("-> ressource ouverte : " + adr, Contexte.VERB_BVR);
               vr.setSubValue(adr);
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
            }
            else return cont.erreur("SHELL", "Desktop non supporté sur cette plateforme", lng);
         }
         else {
            if (((String) lf.get(0)).isEmpty()) return cont.erreur("SHELL", "Commande vide", lng);
            else {
               Process p = Runtime.getRuntime().exec(lf.toArray(new String[0]));
               InputStream out = p.getInputStream(), err = p.getErrorStream();
               String s1 = Tools.streamToString(out), s2 = Tools.streamToString(err);
               out.close();
               err.close();
               if (!s2.isEmpty()) cont.errprintln(s2); // contenu en STDERR pas forcément erreur
               int status = p.waitFor();
               if (status != 0) return cont.erreur("SHELL", "Processus terminé avec code " + status, lng);

               vr.setDispValue("-> envoyé au shell : " + listToString(lf), Contexte.VERB_BVR);
               vr.setSubValue(s1);
            }
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }

         vr.setRet();
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("SHELL", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException|URISyntaxException|IllegalArgumentException|InterruptedException ex) {
         return cont.exception("SHELL", "ERREUR " + ex.getClass().getSimpleName() + " : " +
               ex.getMessage(), lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  shell, $    Exécute la commande shell externe en paramètre\n";
   }
}// class
