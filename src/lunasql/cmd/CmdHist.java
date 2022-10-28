package lunasql.cmd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jline.History;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;
//import jline.console.history.FileHistory;
//import jline.console.history.History.Entry;

/**
 * Commande HIST <br>
 * (Interne) Retourne l'historique local de la console
 * @author M.P.
 */
public class CmdHist extends Instruction {

   private final OptionParser parser;

   public CmdHist(Contexte cont) {
      super(cont, TYPE_CMDINT, "HIST", "H");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("c", "supprime tout l'historique");
      parser.accepts("e", "exécute les entrées trouvées");
      parser.accepts("n", "affiche l'entrée n").withRequiredArg().ofType(Integer.class)
         .describedAs("nb");
      parser.accepts("l", "liste les n dernières entrées n").withRequiredArg().ofType(Integer.class)
         .describedAs("nb");
      parser.nonOptions("chaîne").ofType(String.class);
   }

   @SuppressWarnings("unchecked")
   @Override
   public int execute() {
      if (cont.isHttpMode())
         return cont.erreur("HIST", "cette commande n'est pas autorisée en mode HTTP", lng);

      try {
         OptionSet options = parser.parse(getCommandA1());
         // Aide sur les options
         if (options.has("?")) {
            parser.printHelpOn(cont.getWriterOrOut());
            cont.setValeur(null);
            return RET_CONTINUE;
         }

         // Exécution avec autres options
         History histo = cont.getHistory();
         Valeur vr = new ValeurDef(cont);
         List<?> lc = options.nonOptionArguments();

         if (lc.isEmpty()) {
            List<String> lhisto = new ArrayList<String>(histo.getHistoryList());
            Collections.reverse(lhisto);
            int i;
            StringBuilder sb = new StringBuilder();
            for (i = 0; i < lhisto.size(); i++)
               sb.append(String.format("%03d : ", i)).append(lhisto.get(i)).append('\n');
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
            vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
            vr.setSubValue(Integer.toString(i));
            //int i = 0;
            //for (Iterator<Entry> lhisto = histo.iterator(); lhisto.hasNext(); i++) {
            //   cont.println(String.format("%03d : ", i) + lhisto.next().toString());
            //}
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         else {
            boolean exec = options.has("e"); // avec option n ou recherche
            String cmd = lc.isEmpty() ? "" : ((String) lc.get(0)).toUpperCase();
            if (options.has("c") || cmd.equals("CLEAR")) { // suppression
               if (exec) return cont.erreur("HIST", "exécution impossible avec option -c", lng);
               histo.clear();
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);

            }
            else if (options.has("n") || cmd.equals("NTH")) {
               int i;
               if (options.has("n")) i = (Integer)options.valueOf("n") + 1;
               else if (lc.size() == 2) {
                  try { i = Integer.parseInt((String) lc.get(1)) + 1; }
                  catch (NumberFormatException ex) {
                     return cont.erreur("HIST", "nombre incorrect : " + ex.getMessage(), lng);
                  }
               }
               else return cont.erreur("HIST", "avec option -n : 1 nombre attendu", lng);

               if (i < 0 || i > histo.size())
                  return cont.erreur("HIST", "index d'historique hors bornes : " + i, lng);

               String h = histo.getHistory(histo.size() - i - 1);
               if (exec) new Lecteur(cont, h);
               else {
                  vr.setDispValue(String.format("%03d : ", i) + h, Contexte.VERB_AFF);
                  vr.setSubValue(h);
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
               }

            } else if (options.has("l") || cmd.equals("LIST")) {
               int i;
               if (options.has("l")) i = (Integer) options.valueOf("l");
               else if (lc.size() == 2) {
                  try { i = Integer.parseInt((String) lc.get(1)); }
                  catch (NumberFormatException ex) {
                     return cont.erreur("HIST", "nombre incorrect : " + ex.getMessage(), lng);
                  }
               }
               else return cont.erreur("HIST", "avec option -l : 1 nombre attendu", lng);
               if (i < 0) return cont.erreur("HIST", "Index d'historique hors bornes : " + i, lng);
               List<String> lhisto = new ArrayList<String>(histo.getHistoryList());
               Collections.reverse(lhisto);
               StringBuilder sb = new StringBuilder();
               if (exec) {
                  for (int j = 0; j < Math.min(lhisto.size(), i); j++)
                     sb.append(Tools.cleanSQLCodeDelHist(cont, lhisto.get(j)));
                  new Lecteur(cont, sb.toString());
               }
               else {
                  int j;
                  for (j = 0; j < Math.min(lhisto.size(), i); j++)
                     sb.append(String.format("%03d : ", j)).append(lhisto.get(j)).append('\n');
                  if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
                  vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
                  vr.setSubValue(Integer.toString(j));
                  //int j = 0;
                  //for (Iterator<Entry> lhisto = histo.iterator(); lhisto.hasNext() && j < Math.min(histo.size(), i); j++) {
                  //   cont.println(String.format("%03d : ", j) + lhisto.next().toString());
                  //}
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
               }
            }
            else {
               try {
                  List<?> l = options.nonOptionArguments();
                  if (l.size() != 1)
                     return cont.erreur("HIST", "au maximum une chaîne de recherche est attendue", lng);
                  List<String> lhisto = new ArrayList<String>(histo.getHistoryList());
                  Collections.reverse(lhisto);
                  StringBuilder sb = new StringBuilder();
                  Pattern ptnb = Pattern.compile(Tools.removeBQuotes((String) l.get(0)).value);
                  String h;
                  int i, n = 0;
                  for (i = 0; i < lhisto.size(); i++) {
                     h = lhisto.get(i);
                     if (ptnb.matcher(h).matches()) {
                        if (exec) {
                           // exclusion des commandes HIST pour éviter la StackOverflowError
                           sb.append(Tools.cleanSQLCodeDelHist(cont, h));
                        } else {
                           n++;
                           sb.append(String.format("%03d : ", i)).append(h).append('\n');
                        }
                     }
                  }
                  if (exec) new Lecteur(cont, sb.toString());
                  else {
                     if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
                     vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
                     vr.setSubValue(Integer.toString(n));
                     cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
                  }
               }
               catch (PatternSyntaxException ex) {
                  return cont.erreur("HIST", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
               }
            }
         }
         vr.setRet();
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("HIST", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("HIST", "ERREUR IOException : " + ex.getMessage() , lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  hist, h     Affiche l'historique des commandes en console\n";
   }
}// class
