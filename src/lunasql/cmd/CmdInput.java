package lunasql.cmd;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.PatternSyntaxException;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Tools;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;
//import jline.console.ConsoleReader;

/**
 * Commande INPUT <br>
 * (Interne) Demande d'une donnée à l'utilisateur
 * @author M.P.
 */
public class CmdInput extends Instruction {

   private final OptionParser parser;

   public CmdInput(Contexte cont) {
      super(cont, TYPE_CMDINT, "INPUT", ">");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("p", "saisie de mot de passe");
      parser.accepts("i", "invalidation des $");
      parser.accepts("t", "temps maximal d'attente (sec.)").withRequiredArg().ofType(Integer.class)
         .describedAs("time");
      parser.accepts("f", "format de saisie attendu sur patron").withRequiredArg().ofType(String.class)
         .describedAs("pattern");
      parser.accepts("d", "valeur par défaut si saisie vide").withRequiredArg().ofType(String.class)
              .describedAs("str");
      parser.accepts("r", "valeur par défaut si interruption").withRequiredArg().ofType(String.class)
         .describedAs("str");
      parser.nonOptions("nom_var invite").ofType(String.class);
   }

   @Override
   public int execute() {
      if (cont.isHttpMode())
         return cont.erreur("INPUT", "cette commande n'est pas autorisée en mode HTTP", lng);

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
        List<?> lt = options.nonOptionArguments();
        String val, prompt = listToString(lt);
        String defd = options.has("d") ? (String) options.valueOf("d") : "";
        String defr = options.has("r") ? (String) options.valueOf("r") : "";
        Character echo = options.has("p") ? '*' : null;
        // Timeout selon format
        if (options.has("t")) {
           Future<String> res = Executors.newSingleThreadExecutor()
                    .submit(new ConsoleInput(prompt, echo));
           try {
              val = res.get(((Integer) options.valueOf("t")), TimeUnit.SECONDS);
           }
           catch(TimeoutException ex) {
              res.cancel(true);
              cont.errprintln("\nInterrompu");
              val = defr;
           }
        }
        else val = cont.getInput(prompt, echo);
        if (val == null) {
           cont.errprintln("\nInterrompu");
           val = defr;
        }
        else if (val.isEmpty() && defd.length() > 0) val = defd;

        // Test éventuel du format
        if (options.has("f")) {
           try {
              boolean frmok;
              Tools.BQRet rbq = Tools.removeBQuotes((String) options.valueOf("f"));
              String frm = rbq.value;
              if (rbq.hadBQ) { // recherche regexp
                 frmok = val.matches(frm);
              }
              else {
                 if (frm.equals("int"))
                    frmok = val.matches("[+-]?\\d+");
                 else if (frm.equals("num"))
                    frmok = val.matches("[+-]?\\d*(\\.\\d+)?([eE][+-]?\\d+)?");
                 else if (frm.equals("bool"))
                    frmok = val.matches("(?i)0|1|t(rue)?|f(alse)?");
                 else if (frm.equals("yesno"))
                    frmok = val.matches("(?i)(?i)y(es)?|n(o(n)?)?|o(ui)?");
                 else if (frm.equals("id"))
                    frmok = val.matches("[A-Za-z][A-Za-z0-9_.-]*");
                 else if (frm.equals("char"))
                    frmok = val.length() == 1;
                 else if (frm.equals("word"))
                    frmok = val.matches("\\w+");
                 else if (frm.equals("date"))
                    frmok = val.matches("(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/((19|20)\\d\\d)");
                 else if (frm.equals("datetime"))
                    frmok = val.matches("(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/((19|20)\\d\\d) ([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d");
                 else if (frm.equals("dateus"))
                    frmok = val.matches("(0?[1-9]|1[012])/(0?[1-9]|[12][0-9]|3[01])/((19|20)\\d\\d)");
                 else if (frm.equals("alpha"))
                    frmok = val.matches("[A-Za-z]+");
                 else if (frm.equals("alphanum"))
                    frmok = val.matches("[A-Za-z0-9]+");
                 else if (frm.equals("alphauc"))
                    frmok = val.matches("[A-Z]+");
                 else if (frm.equals("alphalc"))
                    frmok = val.matches("[a-z]+");
                 else if (frm.equals("email"))
                    frmok = val.matches("(?i)([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6})");
                 else if (frm.equals("str")) frmok = true;
                 else return cont.erreur("INPUT", "commande de format inconnue : " + frm, lng);
              }
              if (!frmok)
                 return cont.erreur("INPUT", "la saisie ne correspond pas au format : " + frm, lng);
           }
           catch (PatternSyntaxException ex) {
              return cont.erreur("INPUT", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
           }
        }

        // Invalidation éventuelle des substitutions par #
        if (options.has("i")) val = val.replace('$', '#');
        cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);

        // Retour
        vr.setSubValue(val);
        vr.setRet();
        cont.setValeur(vr);
        return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("INPUT", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException|ExecutionException|InterruptedException ex) {
         return cont.exception("INPUT", "ERREUR " + ex.getClass().getSimpleName() + " : " +
               ex.getMessage(), lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  input, >    Affiche une invite pour saisie au clavier\n";
   }

   /**
    * Classe de saisie en entrée standard avec TimeOut
    * Merci à Heinz Kabutz pour l'idée (http://www.javaspecialists.eu/archive/Issue153.html)
    */
   class ConsoleInput implements Callable<String> {
      private final String prompt;
      private final Character echo;

      public ConsoleInput(String prompt, Character echo) {
         this.prompt = prompt;
         this.echo = echo;
      }

      @Override
      public String call() throws IOException {
         return cont.getInput(prompt, echo);
      }
   }
   
}// class
