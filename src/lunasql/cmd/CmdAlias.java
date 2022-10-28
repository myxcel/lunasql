package lunasql.cmd;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import lunasql.Config;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande ALIAS <br>
 * (Interne) Ne pas utiliser, est appelée par getInstruction
 * @author M.P.
 */
public class CmdAlias extends Instruction {

   private static int deep = 0; // compteur d'appels sur la pile
   private static HashSet<String> CMD_NAME = new HashSet<>(Arrays.asList("LIST", "DICT", "FILE", "TIME"));
   private static HashSet<String> CMD_SWITCH = new HashSet<>(
         Arrays.asList("EACH", "EACHLN", "APPLY", "FILTER", "REDUCE", "ANY?", "ALL?", "AT", "AFTER", "REPEAT"));

   private final ArrayList<String> lcmd0 = new ArrayList<>(); // pile des appels de macros

   public CmdAlias(Contexte cont){
      super(cont, TYPE_INVIS, "ALIAS", null);
   }

   @Override
   public int execute() {
      // Contrôle de la profondeur de la pile pour réduire le risque de StackOverflow
      if (deep > Config.CF_MAX_CALL_DEEP) {
         cont.erreur("ALIAS", "Halte là ! Exécution circulaire irréfléchie dépassant " +
               Config.CF_MAX_CALL_DEEP + " appels\n" +
               "(sachez ce que vous faites quand vous jouez avec la récursivité !)" , lng);
         return RET_EXIT_SCR;
      }

      String cmd = getCommandName();
      boolean issym = false, denyrec = true;
      if (cmd.charAt(0) == ':') {
         issym = true;
         cmd = cmd.substring(1);
      }
      else if (cmd.charAt(0) == '*') {
         denyrec = false;
         cmd = cmd.substring(1);
      }

      // Recherche de référence circulaire d'appels de macros
      if (denyrec && !cont.getVar(Contexte.ENV_ALLOW_REC).equals(Contexte.STATE_TRUE) &&
            !cont.isCtrlCircVar(cmd) && controlCirc(cmd)) {
         StringBuilder sb = new StringBuilder();
         for (String s : lcmd0) sb.append(s).append(" -> ");
         lcmd0.clear();
         return cont.erreur("ALIAS", "Référence circulaire : " + sb.substring(0, sb.length() - 4)
               + "\n(positionnez " + Contexte.ENV_ALLOW_REC + " à " + Contexte.STATE_TRUE
               + " pour jouer avec la récursivité)", lng);
      }

      // Résolution de l'alias
      String code = cont.getVar(cmd); // variable lecteur, puis globale
      if (code == null)
         return cont.erreur("ALIAS", "Pas de correspondance pour '" + cmd + "' en tant qu'alias", lng);
      // Ajout des paramètres
      if (issym || cont.getVar(Contexte.ENV_ALIAS_ARG).equals(Contexte.STATE_TRUE)) code = code + ' ' + getSCommand(1);

      // Exécution
      // TODO : essayer d'intégrer ici la recherche de la macro en tampon de compilation
      // Si existe, l'exécute directement par Lecteur.runCompiled(ArrayList<ArrayList>),
      // sinon, compilation préalable puis exécution du compilé
      //  if (bin = cont.getCompiled(cmd) != null) {
      //     Lecteur.runCompiled(bin);
      //  } else
      if (code.length() > 0) {
         // Préparation des variables de lecteur
         deep++;
         HashMap<String, String> vars = new HashMap<>();
         if (!denyrec) vars.put(Contexte.ENV_ALLOW_REC, Contexte.STATE_TRUE);
         vars.put(Contexte.LEC_VAR_NAME, cmd);
         vars.put(Contexte.LEC_THIS, cmd);
         vars.put(Contexte.LEC_SUPER, "");
         StringBuilder args = new StringBuilder();

         vars.put(Contexte.LEC_VAR_ARG_NB, Integer.toString(getLength() - 1));
         for (int i = 1; i < getLength(); i++) {
            vars.put(Contexte.LEC_VAR_ARG + i, getArg(i));
            args.append(Tools.putBraces(getArg(i))).append(' ');
         }
         if (args.length() > 1) args.deleteCharAt(args.length() - 1);
         vars.put(Contexte.LEC_VAR_ARG_LS, args.toString());

         // Exécution
         Lecteur lec = new Lecteur(cont, code, vars);
         Valeur vr = cont.getValeur();
         cont.setValeur(vr == null ? null : new ValeurDef(cont, null, vr.getSubValue()));
         deep--;
         return lec.getCmdState();
      }
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc() {
      return null;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getHelp() {
      return null;
   }

   /**
    * Recherche de référence circulaire dans la commande
    * @param cmd nom de commande (alias) à tester
    * @return true si une boubcle est trouvée
    */
   private boolean controlCirc(String cmd) {
      String key = cmd;
      if (key == null || key.isEmpty()) return false;

      boolean r = false;
      Lecteur lec = new Lecteur(cont, key, false);
      for (ArrayList<String> cmdi : lec.getCurrentCmd()) {
         int l = cmdi.size();
         key = cmdi.get(0);
         //cont.println("k=" + cmdi);
         if (lcmd0.indexOf(key) >= 0) { // boucle trouvée, on ->[]
            lcmd0.add(key);
            return true;
         }

         // Recherche en commande exécutant du code
         int i;
         String val, keyu = key.toUpperCase();
         if (keyu.equals("IF")) {  // if 1 {} elseif 2 {} ... else {}
            for (i = 2; i < l; i++) {
               if ((i + 1) % 3 == 0 || i == l - 1) r = controlCirc(Tools.removeBracesIfAny(cmdi.get(i)));
            }
         }
         else if (keyu.equals("CASE")) { // case x a {} b {} ... else {}
            for (i = 3; i < l; i += 2) r = controlCirc(Tools.removeBracesIfAny(cmdi.get(i)));
         }
         else if (keyu.equals("FOR")) { // for [-sqf...] i iter {}
            r = controlCirc(Tools.removeBracesIfAny(cmdi.get(l - 1)));
         }
         else if (keyu.equals("WHILE")) { // while else
            for (i = 2; i < l; i += 2) r = controlCirc(Tools.removeBracesIfAny(cmdi.get(i)));
         }
         else if (keyu.equals("EVAL")) { // eval -tv {} [-c {}] [-f {}]
            for (i = 1; i < l; i++) {
               String c = cmdi.get(i);
               if (!c.isEmpty() && c.charAt(0) != '-') r = controlCirc(Tools.removeBracesIfAny(c));
            }
         }
         else if (CMD_NAME.contains(keyu) && l > 3) { // commandes outils à bloc final
            if (CMD_SWITCH.contains(cmdi.get(1).toUpperCase())) r = controlCirc(Tools.removeBraces(cmdi.get(3)));
         }
         // ... autres commandes à exécution de code
         // S'il n'est pas possible de déterminer la position du bloc de code, penser parcourir les arg.
         // et à reconnaître le bloc par ses {}
         // for (i = 1; i < l; i++) { String s = cmdi.get(i); if (Tools.hasBraces(s)) r = controlCirc(Tools.removeBraces(s)); }

         // Recherche en variables
         else if ((val = cont.getVar(key)) == null) { // bout de chaîne, on efface
            if ((i = lcmd0.size() - 1) >= 0 && lcmd0.get(i).equals(key)) lcmd0.remove(i);
         }
         else {
            // Sinon on continue le parcours en l'ajoutant à la liste
            lcmd0.add(key);
            r = controlCirc(val);
            if (!r && lcmd0.get(i = (lcmd0.size() - 1)).equals(key)) lcmd0.remove(i);
         }
      }
      return r;
   }
}// class
