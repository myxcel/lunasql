package lunasql.cmd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;
//import jline.console.ConsoleReader;

/**
 * Commande SPOOL <br>
 * (Interne) Redirection de la sortie standard vers un fichier
 * @author M.P.
 */
public class CmdSpool extends Instruction {

   private final OptionParser parser;
   private PrintStream streamout, streamerr;
   private String outname, errname;

   public CmdSpool(Contexte cont) {
      super(cont, TYPE_CMDINT, "SPOOL", "!");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("f", "nom de fichier de sortie");
      parser.accepts("u", "nom de fichier d'erreur");
      parser.accepts("c", "ferme les redirections en cours");
      parser.accepts("s", "suspend les redirections en cours");
      parser.accepts("r", "reprend les redirections en cours");
      parser.accepts("a", "retour seul de l'état actuel");
      parser.accepts("w", "écrase le fichier si existe");
      parser.accepts("e", "redirection de la sortie des erreurs").withRequiredArg().ofType(File.class)
         .describedAs("fichier");
      parser.nonOptions("fichier").ofType(String.class);
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
         Valeur vr = new ValeurDef(cont);
         List<?> la = options.nonOptionArguments();
         if (la.size() > 1)
            return cont.erreur("SPOOL", "une seule option ou commande ou un nom de fichier est attendu", lng);

         String cmd = la.isEmpty() ? "" : ((String) la.get(0)).toUpperCase();
         if (options.has("f") || cmd.equals("FILE")) {
            vr.setSubValue(outname == null ? "" : outname);
         }
         else if (options.has("u") || cmd.equals("ERRFILE")) {
            vr.setSubValue(errname == null ? "" : errname);
         }
         else if (options.has("c") || cmd.equals("OFF")) {
            if (streamout == null && streamerr == null)
               return cont.erreur("SPOOL", "aucune sortie vers fichier en cours", lng);
            if (streamout != null) {
               streamout.close();
               streamout = null;
               outname = null;
            }
            if (streamerr != null) {
               streamerr.close();
               streamerr = null;
               errname = null;
            }
            cont.setWriter(null);
            cont.setErrWriter(null);
            vr.setDispValue("");
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         else if (options.has("s") || cmd.equals("SUSPEND")) {
            if (streamout == null && streamerr == null)
               return cont.erreur("SPOOL", "aucune sortie vers fichier en cours", lng);
            cont.setWriter(null);
            cont.setErrWriter(null);
            vr.setDispValue("");
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         else if (options.has("r") || cmd.equals("RESUME")) {
            if (streamout == null && streamerr == null)
               return cont.erreur("SPOOL", "aucune sortie vers fichier en cours", lng);
            if (cont.getWriter() != null && cont.getErrWriter() != null) {
               System.err.println("La sortie vers fichier n'est pas suspendue"); // sur console
               return cont.erreur("SPOOL", "la sortie vers fichier n'est pas suspendue", lng);
            }
            cont.setWriter(streamout);
            cont.setErrWriter(streamerr);
            vr.setDispValue("");
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         else {
            boolean iserr = options.has("e");
            if (la.size() == 1 || iserr) {  // noms de fichier de redirection (out et/ou err)
               // Ouverture
               boolean isout = la.size() == 1, append = !options.has("w");
               if (isout) {
                  File fout = new File((String) la.get(0));
                  if (cont.askWriteFile(fout)) {
                     if (streamout != null) streamout.close();
                     outname = fout.getCanonicalPath();
                     streamout = new PrintStream(new FileOutputStream(fout, append), true,
                           cont.getVar(Contexte.ENV_FILE_ENC));
                     cont.setWriter(streamout, outname);
                     cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
                  }
                  else isout = false;
               }
               if (iserr) {
                  if (streamerr != null) streamerr.close();
                  File ferr = (File) options.valueOf("e");
                  if (cont.askWriteFile(ferr)) {
                     errname = ferr.getCanonicalPath();
                     streamerr = new PrintStream(new FileOutputStream(ferr, append), true,
                           cont.getVar(Contexte.ENV_FILE_ENC));
                     cont.setErrWriter(streamerr, errname);
                     cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
                  }
                  else iserr = false;
               }
               // Rapport
               if ((isout || iserr) && cont.getVerbose() >= Contexte.VERB_BVR) {
                  String comm = (outname == null ? "" : "Redirection de la sortie vers " + outname + "\n") +
                        (errname == null ? "" : "Redirection de l'erreur vers " + errname);
                  System.out.println(comm);
                  vr.setDispValue(comm, Contexte.VERB_BVR);
               }
               else {
                  vr.setDispValue("");
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
               }
               vr.setSubValue(null);
            }
            else { // sans option ni commande
               String sw, ew;
               // out
               if (streamout == null) sw = "OFF";
               else if (cont.getWriter() == null) sw = "SUSPEND:" + outname;
               else sw = cont.getWriterName();
               // err
               if (streamerr == null) ew = "OFF";
               else if (cont.getErrWriter() == null) ew = "SUSPEND:" + errname;
               else ew = cont.getErrWriterName();
               // Rapport
               if (!options.has("a") && cont.getVerbose() >= Contexte.VERB_AFF) {
                  if (sw != null) System.out.println("Sortie normale : " + sw); // sur console uniquement
                  if (ew != null) System.out.println("Sortie erreurs : " + ew); // idem
               }
               vr.setDispValue("");
               vr.setSubValue(sw + (ew == null ? "" : ";" + ew));
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
            }
         }
         vr.setRet();
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("SPOOL", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("SPOOL", "ERREUR IOException : "+ getArg(1), lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  spool, !    Redirige les sorties OUT et ERR vers le fichier indiqué\n";
   }
}// class
