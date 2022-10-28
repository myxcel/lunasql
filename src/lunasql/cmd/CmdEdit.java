package lunasql.cmd;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.ui.FrmEditScript;

/**
 * Commande EDITOR <br> (Interne) Affichage d'un éditeur swing
 * @author M.P.
 */
public class CmdEdit extends Instruction {

   private final OptionParser parser;
   private FrmEditScript fedit;
   private int ret;

   public CmdEdit(Contexte cont) {
      super(cont, TYPE_CMDINT, "EDIT", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("d", "ouvre l'éditeur système");
      parser.accepts("p", "ouvre l'éditeur en EDITOR_PATH");
      parser.accepts("t", "type de fichier script").withRequiredArg().ofType(String.class)
         .describedAs("type");
      parser.nonOptions("fichier").ofType(String.class);
   }

   @Override
   public int execute() {
      if (cont.isHttpMode())
         return cont.erreur("EDIT", "cette commande n'est pas autorisée en mode HTTP", lng);

      try {
         OptionSet options = parser.parse(getCommandA1());
         // Aide sur les options
         if (options.has("?")) {
            parser.printHelpOn(cont.getWriterOrOut());
            cont.setValeur(null);
            return RET_CONTINUE;
         }

         // Exécution avec autres options
         final Contexte c = cont;
         List<?> lf = options.nonOptionArguments();
         if (lf.size() > 1)
            return cont.erreur("EDIT", "au maximum un nom de fichier est attendu", lng);

         // Lancement de l'editeur demandé
         ret = RET_CONTINUE;
         final String fname = (lf.size() == 1 ? (String) lf.get(0) : null);
         java.awt.EventQueue.invokeLater(() -> {
            File f = null;
            if (fname != null) {
               f = new File(fname);
               if (!f.exists() || !f.canRead()) {
                  ret = cont.erreur("EDIT", "le fichier '" + f.getAbsolutePath() + "' est inaccessible", lng);
                  return;
               }
            }
            // Ouverture
            if (options.has("d")) {
               if (Desktop.isDesktopSupported()) {
                  try {
                     Desktop dk = Desktop.getDesktop();
                     if (dk.isSupported(Desktop.Action.EDIT)) dk.edit(f);
                     else ret = cont.erreur("EDIT", "Desktop EDIT non supporté", lng);
                  } catch (IOException ex) {
                     ret = cont.exception("EDIT", "ERREUR IOException : " + ex.getMessage(), lng, ex);
                  }
               } else ret = cont.erreur("EDIT", "Desktop non supporté sur cette plateforme", lng);
            } else if (options.has("p")) {
               try {
                  String edt = cont.getVar(Contexte.ENV_EDIT_PATH);
                  if (edt.isEmpty()) {
                     ret = cont.erreur("EDIT", "Option " + Contexte.ENV_EDIT_PATH + " non positionnée", lng);
                     return;
                  }
                  if (f == null) Runtime.getRuntime().exec(edt);
                  else Runtime.getRuntime().exec(new String[]{edt, f.getAbsolutePath()});
               } catch (IOException ex) {
                  ret = cont.exception("EDIT", "ERREUR IOException : " + ex.getMessage(), lng, ex);
               }
            } else {
               String type = null;
               if (options.has("t")) {
                  type = (String) options.valueOf("t");
                  String[] tsyn = FrmEditScript.getSyntaxes();
                  if (!Arrays.asList(tsyn).contains(type)) {
                     ret = cont.erreur("EDIT", "Type syntaxique non supporté : " + type, lng);
                     return;
                  }
               }
               try {
                  if (fedit == null) fedit = new FrmEditScript(c, false, type);
                  fedit.setVisible(true);
                  if (f != null) fedit.openFile(f);
               } catch (HeadlessException ex) {
                  ret = cont.exception("EDIT", "ERREUR HeadlessException : " + ex.getMessage(), lng, ex);
               }
            }
         });
         cont.setValeur(null);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return ret;
      }
      catch (OptionException ex) {
         return cont.exception("EDIT", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex){
         return cont.exception("EDIT", "ERREUR IOException : " + ex.getMessage() , lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  edit        Ouvre un éditeur de script Java Swing\n";
   }
}// class
