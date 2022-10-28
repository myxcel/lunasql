package lunasql.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jline.ConsoleReader;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande BUFFER <br>
 * (SQL) Modification de la structure d'un élément de base de données
 * @author M.P.
 */
public class CmdBuffer extends Instruction {

   private final OptionParser parser;

   public CmdBuffer(Contexte cont) {
      super(cont, TYPE_CMDINT, "BUFFER", "+");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("c", "supprime le contenu du tampon");
      parser.accepts("l", "liste le contenu du tampon");
      parser.accepts("e", "exécute le contenu du tampon");
      parser.accepts("p", "supprime et retourne la dernière entrée");
      parser.accepts("r", "exécute la dernière entrée");
      parser.accepts("i", "exécute la i-ème entrée").withRequiredArg().ofType(Integer.class)
         .describedAs("i");
      parser.accepts("n", "retourne le nombre d'entrées");
      parser.accepts("u", "exécute et supprime la dernière entrée");
      parser.accepts("a", "ajoute une commande au tampon");
      parser.accepts("h", "ajoute la dernière commande au tampon");
      parser.accepts("g", "entre en mode ligne");
      parser.accepts("d", "retourne le contenu");
      parser.accepts("o", "charge le contenu");
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
         List<?> lc = options.nonOptionArguments();
         String cmd = lc.isEmpty() ? "" : ((String) lc.get(0)).toUpperCase();
         Valeur vr = null;
         if (options.has("c") || cmd.equals("CLEAR")) {
            cont.setVar(Contexte.ENV_CMD_BUFFER, "");
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         else if ((options.has("l") || cmd.equals("LIST")) && cont.getVerbose() >= Contexte.VERB_AFF) {
            String[] cmds = getBufferList();
            StringBuilder sb = new StringBuilder();
            int n = cmds.length;
            if (n == 0) sb.append("Tampon de commandes vide\n");
            else {
               for (int i = 0; i < cmds.length; i++) 
                  sb.append(String.format("%03d", i + 1)).append(" | ").append(cmds[i]).append('\n');
            }
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
            vr = new ValeurDef(cont, sb.toString(), Contexte.VERB_AFF, Integer.toString(n));
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         else if (options.has("d") || cmd.equals("DUMP")) {
            vr = new ValeurDef(cont, null, cont.getVar(Contexte.ENV_CMD_BUFFER));
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         else if (options.has("o") || cmd.equals("LOAD")) {
            String oldcmd = cont.getVar(Contexte.ENV_CMD_BUFFER), newcmd = listToString(lc, 1);
            cont.setVar(Contexte.ENV_CMD_BUFFER, oldcmd.length() == 0 ? newcmd : oldcmd + ";;" + newcmd);
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         else if (options.has("e") || cmd.equals("RUNALL")) {
            Lecteur lec = new Lecteur(cont, cont.getVar(Contexte.ENV_CMD_BUFFER));
            vr = cont.getValeur();
            if (vr != null) vr.setDispValue(null, Contexte.VERB_SIL);
            return lec.getCmdState();
         }
         else if (options.has("p") || cmd.equals("POP")) {
            String[] cmds = getBufferList();
            String r = "";
            if (cmds.length > 0) {
               r = cmds[cmds.length - 1];
               cmds[cmds.length - 1] = null;
               setBufferList(cmds);
            }
            vr = new ValeurDef(cont, null, r);
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE); 
         }
         else if (options.has("r") || cmd.equals("RUNLAST")) {
            String[] cmds = getBufferList();
            if (cmds.length > 0) {
               Lecteur lec = new Lecteur(cont, cmds[cmds.length - 1]);
               vr = cont.getValeur();
               if (vr != null) vr.setDispValue(null, Contexte.VERB_SIL);
               return lec.getCmdState();
            }
         }
         else if (options.has("i") || cmd.equals("RUNITH")) {
            int i;
            if (options.has("i")) i = (Integer) options.valueOf("i") - 1;
            else if (lc.size() == 2) {
               try { i = Integer.parseInt((String) lc.get(1)) - 1; }
               catch (NumberFormatException ex) {
                  return cont.erreur("BUFFER", "nombre incorrect : " + ex.getMessage(), lng);
               }
            }
            else return cont.erreur("BUFFER", "avec option -i : 1 nombre attendu", lng);

            String[] cmds = getBufferList();
            for (int j = 0; j < cmds.length; j++) {
               if (i == j) {
                  Lecteur lec = new Lecteur(cont, cmds[i]);
                  vr = cont.getValeur();
                  if (vr != null) vr.setDispValue(null, Contexte.VERB_SIL);
                  return lec.getCmdState();
               }
            }
         }
         else if (options.has("n") || cmd.equals("SIZE")) {
            String l = Integer.toString(getBufferList().length);
            vr = new ValeurDef(cont, l, l);
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }
         else if (options.has("u") || cmd.equals("RUNPOP")) {
            String[] cmds = getBufferList();
            String r = "";
            if (cmds.length > 0) {
               r = cmds[cmds.length - 1];
               cmds[cmds.length - 1] = null;
               setBufferList(cmds);
            }
            if (r.length() > 0) {
               Lecteur lec = new Lecteur(cont, r);
               vr = cont.getValeur();
               if (vr != null) vr.setDispValue(null, Contexte.VERB_SIL);
               return lec.getCmdState();
            }
         }
         else if (options.has("a") || cmd.equals("ADD")) {
            String cc = cmd.equals("ADD") ? listToString(lc, 1) : listToString(lc);
            if (cc.isEmpty()) return cont.erreur("BUFFER", "une commande au moins est attendue", lng);
            appendBuffer(cc);
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         else if (options.has("h") || cmd.equals("ADDLAST")) {
            @SuppressWarnings("unchecked")
            List<String> lhisto = new ArrayList<String>(cont.getHistory().getHistoryList());
            if (lhisto.size() >= 2) {
               Collections.reverse(lhisto);
               appendBuffer(lhisto.get(1));
            }
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         }
         else if (options.has("g") || cmd.equals("LMODE")) {
            ConsoleReader reader = cont.getConsoleReader();
            cont.println("Ajout en tampon en mode ligne. Pour sortir, <ctrl>+D ou ':q'"); // pas en vr
            StringBuilder val = new StringBuilder();
            String s;
            if(reader == null) {
               BufferedReader reader2 = new BufferedReader(new InputStreamReader(System.in));
               while ((s = reader2.readLine()) != null && !s.equals(":q")) val.append(s).append(";;");
               reader2.close();
            }
            else {
               while ((s = reader.readLine("")) != null && !s.equals(":q")) val.append(s).append(";;");
            }
            appendBuffer(val.toString());
         }
         else return cont.erreur("BUFFER", "un nom de commande valide de buffer est attendu", lng);

         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("BUFFER", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("BUFFER", "ERREUR IOException : " + ex.getMessage(), lng, ex);
      }
   }

   /*
    * Ajoute une commande au tampon
    */
   private void appendBuffer(String txt) {
      String cmds = cont.getVar(Contexte.ENV_CMD_BUFFER);
      cont.setVar(Contexte.ENV_CMD_BUFFER, (cmds.isEmpty() ? "" : cmds + ";;") + txt);
   }

   /*
    * Retourne la liste des commandes du tampon sous forme de tableau String[]
    */
   private String[] getBufferList() {
      String[] cmds = cont.getVar(Contexte.ENV_CMD_BUFFER).split(";;");
      if(cmds.length == 1 && cmds[0].isEmpty()) return new String[0];
      return cmds;
   }

   /*
    * Fixe la liste des commandes du tampon
    */
   private void setBufferList(String[] cmds) {
      StringBuilder sb = new StringBuilder();
      for (String s : cmds) {
         if (s != null && s.length() > 0) sb.append(s).append(";;");
      }
      if (sb.length() > 2) sb.delete(sb.length() - 2, sb.length());
      cont.setVar(Contexte.ENV_CMD_BUFFER, sb.toString());
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  buffer, +   Liste et modifie le tampon de commandes\n";
   }
}// class

