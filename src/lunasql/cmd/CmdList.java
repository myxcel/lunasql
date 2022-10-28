package lunasql.cmd;

import static lunasql.lib.Tools.blankSplitLec;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;
import opencsv.CSVWriter;

/**
 * Commande LIST <br>
 * (Interne) Commande d'utilitaires de traitement de liste de chaîne
 * @author M.P.
 */
public class CmdList extends Instruction {

   public CmdList(Contexte cont){
      super(cont, TYPE_CMDINT, "LIST", null);
   }

   @Override
   public int execute() {
      if (getLength() < 2)
         return cont.erreur("LIST", "une commande de liste est requise", lng);

      int ret = RET_CONTINUE;
      String cmd = getArg(1).toUpperCase(), lst = "";
      if (cmd.equals("NEW")) {
         int l = getLength();
         if (l < 3) return cont.erreur("LIST", "NEW : listes attendues", lng);
         List<String> lt = new ArrayList<String>();
         for (int i = 2; i < l; i++) lt.add(getArg(i));
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("FLAT")) {
         if (getLength() < 3) return cont.erreur("LIST", "FLAT : listes attendues", lng);
         lst = Tools.flatList(getSCommand(2));
      }
      else if (cmd.equals("CONCAT")) {
         int l = getLength();
         if (l < 3) return cont.erreur("LIST", "CONCAT : listes attendues", lng);
         List<String> lt = new ArrayList<String>();
         for (int i = 2; i <= l; i++) lt.addAll(Arrays.asList(blankSplitLec(cont, getArg(i))));
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("SIZE")) {
         if (getLength() != 3) return cont.erreur("LIST", "SIZE : 1 liste attendue", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         lst = Integer.toString(t.length);
      }
      else if (cmd.equals("GET")) {
         if (getLength() != 4) return cont.erreur("LIST", "GET : 1 liste, 1 index attendus", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         try {
            int i = Integer.parseInt(getArg(3));
            if (i < 0) i = t.length + i;
            if (i < 0 || i >= t.length)
               return cont.erreur("LIST", "GET : indice incorrect : " + getArg(3), lng);
            lst = t[i];
         }
         catch(NumberFormatException ex) {
            return cont.erreur("LIST", "GET : format de sous-liste incorrect : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("PUSH")) {
         if (getLength() != 4) return cont.erreur("LIST", "PUSH : 1 liste, 1 valeur attendues", lng);
         List<String> lt = new ArrayList<String>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         lt.add(getArg(3));
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("INDEX")) {
         if (getLength() != 4) return cont.erreur("LIST", "INDEX : 1 liste, 1 chaîne attendues", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         String c = getArg(3);
         lst = "-1";
         for (int i = 0; i < t.length; i++)
            if (t[i].equals(c)) {
               lst = Integer.toString(i); break;
            }
      }
      else if (cmd.equals("LINDEX")) {
         if (getLength() != 4) return cont.erreur("LIST", "LINDEX : 1 liste, 1 chaîne attendues", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         String c = getArg(3);
         lst = "-1";
         for (int i = t.length - 1; i >= 0; i--)
            if (t[i].equals(c)) {
               lst = Integer.toString(i); break;
            }
      }
      else if (cmd.equals("INDEX-IC")) {
         if (getLength() != 4) return cont.erreur("LIST", "INDEX-IC : 1 liste, 1 chaîne attendues", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         String c = getArg(3);
         lst = "-1";
         for (int i = 0; i < t.length; i++)
            if (t[i].equalsIgnoreCase(c)) {
               lst = Integer.toString(i); break;
            }
      }
      else if (cmd.equals("LINDEX-IC")) {
         if (getLength() != 4) return cont.erreur("LIST", "LINDEX-IC : 1 liste, 1 chaîne attendues", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         String c = getArg(3);
         lst = "-1";
         for (int i = t.length - 1; i >= 0; i--)
            if (t[i].equalsIgnoreCase(c)) {
               lst = Integer.toString(i); break;
            }
      }
      else if (cmd.equals("HAS?")) {
         if (getLength() != 4) return cont.erreur("LIST", "HAS? : 1 liste, 1 chaîne attendues", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         String c = getArg(3);
         lst = "0";
         for (String e : t)
            if (e.equals(c)) {
               lst = "1"; break;
            }
      }
      else if (cmd.equals("HAS-IC?")) {
         if (getLength() != 4) return cont.erreur("LIST", "HAS-IC? : 1 liste, 1 chaîne attendues", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         String c = getArg(3);
         lst = "0";
         for (String e : t)
            if (e.equalsIgnoreCase(c)) {
               lst = "1"; break;
            }
      }
      else if (cmd.equals("SUBLST")) {
         if (getLength() != 4) return cont.erreur("LIST", "SUBLST : 1 liste, 1 index attendus", lng);
         try { lst = Tools.subList(cont, getArg(2), getArg(3)); }
         catch(NumberFormatException ex) {
            return cont.erreur("LIST", "SUBLST : format de sous-liste incorrect : " + getArg(3), lng);
         }
      }
      else if (cmd.equals("JOIN")) {
         int l = getLength();
         if (l < 3 || l > 4)
            return cont.erreur("LIST", "JOIN : 1 liste, 1 chaîne attendues", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         StringBuilder sb = new StringBuilder();
         String c = l == 4 ? getArg(3) : "";
         for (int i = 0; i < t.length - 1; i++) sb.append(t[i]).append(c);
         if (t.length > 0) sb.append(t[t.length - 1]);
         lst = sb.toString();
      }
      else if (cmd.equals("FIND")) {
         if (getLength() != 4) return cont.erreur("LIST", "FIND : 1 liste, 1 regexp attendues", lng);
         try {
            Pattern p = Pattern.compile(Tools.removeBQuotes(getArg(3)).value);
            String[] t = blankSplitLec(cont, getArg(2));
            List<String> lt = new ArrayList<String>();
            for (String s : t) {
               Matcher m = p.matcher(s);
               if (m.find()) lt.add(s);
            }
            lst = Tools.arrayToString(lt);
         }
         catch (PatternSyntaxException ex) {
            return cont.erreur("LIST", "FIND : syntaxe regexp incorrecte : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("FIRST")) {
         int l = getLength();
         if (l < 3 || l > 4)
            return cont.erreur("LIST", "FIRST : 1 liste, [1 nombre] attendus", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         int nb;
         if (l == 4) {
            try {
               nb = Math.min(Integer.parseInt(getArg(3)), t.length);
               if (nb < 0) throw new NumberFormatException("négatif : " + nb);
            }
            catch (NumberFormatException ex) {
               return cont.erreur("LIST", "FIRST : nombre incorrect : " + ex.getMessage(), lng);
            }
         }
         else nb = 1;
         // Découpage
         switch (nb) {
            case 0 : lst = "";   break;
            case 1 : lst = t[0]; break;
            default:
               List<String> lt = new ArrayList<>();
               for (int i = 0; i < nb; i++) lt.add(t[i]);
               lst = Tools.arrayToString(lt);
         }
      }
      else if (cmd.equals("LAST")) {
         int l = getLength();
         if (l < 3 || l > 4)
            return cont.erreur("LIST", "LAST : 1 liste, [1 nombre] attendus", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         int nb;
         if (l == 4) {
            try {
               nb = Math.min(Integer.parseInt(getArg(3)), t.length);
               if (nb < 0) throw new NumberFormatException("négatif : " + nb);
            }
            catch (NumberFormatException ex) {
               return cont.erreur("LIST", "LAST : nombre incorrect : " + ex.getMessage(), lng);
            }
         }
         else nb = 1;
         // Découpage
         switch (nb) {
            case 0 : lst = "";   break;
            case 1 : lst = t[t.length - 1]; break;
            default:
               List<String> lt = new ArrayList<>();
               for (int i = t.length - nb; i < t.length; i++) lt.add(t[i]);
               lst = Tools.arrayToString(lt);
         }
      }
      else if (cmd.equals("SHIFT")) {
         if (getLength() != 3) return cont.erreur("LIST", "SHIFT : 1 liste attendue", lng);
         List<String> lt = new ArrayList<String>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         if (!lt.isEmpty()) lt.remove(0);
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("POP")) {
         if (getLength() != 3) return cont.erreur("LIST", "POP : 1 liste attendue", lng);
         List<String> lt = new ArrayList<String>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         if (!lt.isEmpty()) lt.remove(lt.size() - 1);
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("INSERT")) {
         int l = getLength();
         if (l < 4 || l > 5)
            return cont.erreur("LIST", "INSERT : 1 liste, 1 valeur, [1 index] attendus", lng);
         List<String> lt = new ArrayList<>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         int idx = 0, ls = lt.size();
         if (l == 5) {
            try {
               idx = Integer.parseInt(getArg(4));
               if (idx < 0) idx = ls + idx + 1;
               if (idx < 0 || idx > ls)
                  return cont.erreur("LIST", "INSERT : indice incorrect : " + getArg(4), lng);
            }
            catch(NumberFormatException ex) {
               return cont.erreur("LIST", "INSERT : nombre incorrect : " + getArg(4), lng);
            }
         }
         lt.add(idx, getArg(3));
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("REMOVE")) {
         if (getLength() != 4) return cont.erreur("LIST", "REMOVE : 1 liste, 1 index attendus", lng);
         List<String> lt = new ArrayList<>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         int idx, l = lt.size();
         try {
            idx = Integer.parseInt(getArg(3));
            if (idx < 0) idx = l + idx;
            if (idx < 0 || idx >= l)
               return cont.erreur("LIST", "REMOVE : indice incorrect : " + getArg(3), lng);
         }
         catch(NumberFormatException ex) {
            return cont.erreur("LIST", "REMOVE : nombre incorrect : " + getArg(3), lng);
         }
         lt.remove(idx);
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("REPLACE")) {
         if (getLength() != 5)
            return cont.erreur("LIST", "REPLACE : 1 liste, 1 index, 1 valeur attendus", lng);
         List<String> lt = new ArrayList<>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         int idx, l = lt.size();
         try {
            idx = Integer.parseInt(getArg(3));
            if (idx < 0) idx = l + idx;
            if (idx < 0 || idx >= l)
               return cont.erreur("LIST", "REPLACE : indice incorrect : " + getArg(3), lng);
         }
         catch(NumberFormatException ex) {
            return cont.erreur("LIST", "REPLACE : nombre incorrect : " + getArg(3), lng);
         }
         lt.remove(idx);
         lt.add(idx, getArg(4));
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("REVERSE")) {
         if (getLength() != 3) return cont.erreur("LIST", "MAX : 1 liste attendue", lng);
         List<String> lt = new ArrayList<>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         Collections.reverse(lt);
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("MAX")) {
         if (getLength() != 3) return cont.erreur("LIST", "MAX : 1 liste attendue", lng);
         String[] tval = blankSplitLec(cont, getArg(2));
         double max = Double.MIN_VALUE;
         for(String s : tval){
            try { max = Math.max(Double.parseDouble(s), max); }
            catch (NumberFormatException ex) {
               return cont.erreur("LIST", "MAX : nombre invalide : " + s, lng);
            }
         }
         lst = (max == (int) max ? Integer.toString((int) max) : Double.toString(max));
      }
      else if (cmd.equals("MIN")) {
         if (getLength() != 3) return cont.erreur("LIST", "MIN : 1 liste attendue", lng);
         String[] tval = blankSplitLec(cont, getArg(2));
         double min = Double.MAX_VALUE;
         for(String s : tval){
            try { min = Math.min(Double.parseDouble(s), min); }
            catch (NumberFormatException ex) {
               return cont.erreur("LIST", "MIN : nombre invalide : " + s, lng);
            }
         }
         lst = (min == (int) min ? Integer.toString((int) min) : Double.toString(min));
      }
      else if (cmd.equals("SUM")) {
         if (getLength() != 3) return cont.erreur("LIST", "SUM : 1 liste attendue", lng);
         String[] tval = blankSplitLec(cont, getArg(2));
         double sum = 0.0;
         for(String s : tval){
            try { sum += Double.parseDouble(s); }
            catch (NumberFormatException ex) {
               return cont.erreur("LIST", "SUM : nombre invalide : " + s, lng);
            }
         }
         lst = (sum == (int) sum ? Integer.toString((int) sum) : Double.toString(sum));

      }
      else if (cmd.equals("SORT")) {
         int l = getLength();
         if (l < 3 || l > 4)
            return cont.erreur("LIST", "SORT : 1 liste, [nbr|nbr-rev|str|str-rev] attendus", lng);
         Comparator<String> cp = null;
         List<String> lt = Arrays.asList(blankSplitLec(cont, getArg(2)));
         if (l == 4) {
            final String mode = getArg(3);
            cp = (a, b) -> {
               try {
                  if (mode.equals("nbr"))          return new Double(a).compareTo(new Double(b));
                  else if (mode.equals("nbr-rev")) return new Double(b).compareTo(new Double(a));
                  else if (mode.equals("str"))     return a.compareTo(b);
                  else if (mode.equals("str-rev")) return b.compareTo(a);
               }
               catch (NumberFormatException ex) {}
               return a.compareTo(b);
            };
         }
         Collections.sort(lt, cp);
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("RANGE")) {
         if (getLength() != 3) return cont.erreur("LIST", "RANGE : 1 séquence attendue", lng);
         String[] seq = getArg(2).split(":");
         int deb = 0, fin, pas = 1;
         try {
            switch (seq.length) {
            case 1:
               fin = Integer.parseInt(seq[0]);
               break;
            case 2:
               deb = Integer.parseInt(seq[0]);
               fin = Integer.parseInt(seq[1]);
               break;
            case 3:
               deb = Integer.parseInt(seq[0]);
               fin = Integer.parseInt(seq[1]);
               pas = Integer.parseInt(seq[2]);
               break;
            default:
               return cont.erreur("LIST", "RANGE : séquence attendue : début:fin:pas", lng);
            }
            if (pas == 0) return cont.erreur("LIST", "RANGE : séquence infinie non autorisée", lng);
         }
         catch (NumberFormatException ex) {
            return cont.erreur("LIST", "RANGE : paramètre de séquence invalide : " + ex.getMessage(), lng);
         }
         StringBuilder sb = new StringBuilder();
         for (int i = deb; (pas > 0 ? i < fin : i > fin); i += pas) sb.append(i).append(' ');
         if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
         lst = sb.toString();
      }
      else if (cmd.equals("NVL")) {
         if (getLength() != 3) return cont.erreur("LIST", "NVL : 1 liste attendue", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         for (String s : t)
            if (!s.isEmpty()) {
               lst = s;
               break;
            }
      }
      else if (cmd.equals("PICK")) {
         int l = getLength();
         if (l < 3 || l > 4)
            return cont.erreur("LIST", "PICK : 1 liste, [1 nombre] attendus", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         try {
            int n = l == 4 ? Integer.parseInt(getArg(3)) : 1;
            if (n < 0 || n > t.length)
               return cont.erreur("LIST", "PICK : nombre incorrect : " + getArg(3), lng);
            List<String> lt = new ArrayList<>();
            for (int i = 0; i < n; i++) {
               int r = (int)(Math.random() * t.length);
               lt.add(t[r]);
            }
            lst = Tools.arrayToString(lt);
         }
         catch(NumberFormatException ex) {
            return cont.erreur("LIST", "PICK : nombre incorrect : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("RAND")) {
         if (getLength() != 3) return cont.erreur("LIST", "RAND : 1 liste attendue", lng);
         List<String> lt = Arrays.asList(blankSplitLec(cont, getArg(2)));
         Collections.shuffle(lt);
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("UNIQ")) {
         if (getLength() != 3) return cont.erreur("LIST", "UNIQ : 1 liste attendue", lng);
         Set<String> st = new LinkedHashSet<>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         List<String> lt = new ArrayList<>(st);
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("MINUS")) {
         if (getLength() != 4) return cont.erreur("LIST", "MINUS : 2 listes attendues", lng);
         List<String> lt = new ArrayList<>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         lt.removeAll(Arrays.asList(blankSplitLec(cont, getArg(3))));
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("UNION")) {
         if (getLength() != 4) return cont.erreur("LIST", "UNION : 2 listes attendues", lng);
         Set<String> t = new TreeSet<>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         t.addAll(Arrays.asList(blankSplitLec(cont, getArg(3))));
         List<String> lt = new ArrayList<>(t);
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("INTER")) {
         if (getLength() != 4) return cont.erreur("LIST", "INTER : 2 listes attendues", lng);
         Set<String> st = new TreeSet<>(Arrays.asList(blankSplitLec(cont, getArg(2))));
         st.retainAll(Arrays.asList(blankSplitLec(cont, getArg(3))));
         List<String> lt = new ArrayList<>(st);
         lst = Tools.arrayToString(lt);
      }
      else if (cmd.equals("EACH")) {
         if (getLength() != 4) return cont.erreur("LIST", "EACH : 1 liste, 1 bloc attendus", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "list each");
         Lecteur lec = new Lecteur(cont);
         int nbi = 0;
         for (int i = 0; ret == RET_CONTINUE && i < t.length; i++) {
            nbi++;
            vars.put("arg1", t[i]);
            cont.addSubMode();
            lec.add(cmds, vars);
            lec.doCheckWhen();
            cont.remSubMode();
            ret = lec.getCmdState();
         }
         lec.fin();
         lst = Integer.toString(nbi);
      }
      else if (cmd.equals("APPLY")) {
         if (getLength() != 4) return cont.erreur("LIST", "APPLY : 1 liste, 1 bloc attendus", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "list apply");
         Lecteur lec = new Lecteur(cont);
         StringBuilder sb = new StringBuilder();
         for (int i = 0; ret == RET_CONTINUE && i < t.length; i++) {
            vars.put("arg1", t[i]);
            cont.addSubMode();
            lec.add(cmds, vars);
            lec.doCheckWhen();
            cont.remSubMode();
            ret = lec.getCmdState();
            Valeur vr = cont.getValeur();
            sb.append(Tools.putBraces(vr == null ? "" : vr.getSubValue())).append(' ');
         }
         lec.fin();
         lst = sb.toString();
      }
      else if (cmd.equals("FILTER")) {
         if (getLength() != 4) return cont.erreur("LIST", "FILTER : 1 liste, 1 bloc attendus", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "list filter");
         StringBuilder sb = new StringBuilder();
         for (String s : t) {
            if (ret != RET_CONTINUE) break;
            vars.put("arg1", s);
            int[] e = cont.evaluerBoolLec(cmds, vars);
            ret = e[1];
            if (e[0] == 1) sb.append(Tools.putBraces(s)).append(' ');
         }
         lst = sb.toString();
      }
      else if (cmd.equals("REDUCE")) {
         int l = getLength();
         if (l < 4 || l > 5)
            return cont.erreur("LIST", "REDUCE : 1 liste, 1 bloc, [1 valeur] attendus", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         boolean z = l == 5; // valeur init fournie
         String red = z ? getArg(4) : (t.length > 0 ? t[0] : ""); // réduction
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "list reduce");
         Lecteur lec = new Lecteur(cont);
         for (int i = (z ? 0 : 1); ret == RET_CONTINUE && i < t.length; i++) {
            vars.put("arg1", red);   // réduction
            vars.put("arg2", t[i]);  // élément
            cont.addSubMode();
            lec.add(cmds, vars);
            lec.doCheckWhen();
            cont.remSubMode();
            ret = lec.getCmdState();
            Valeur vr = cont.getValeur();
            red = vr == null ? "" : vr.getSubValue();
         }
         lec.fin();
         lst = red;
      }
      else if (cmd.equals("ANY?")) {
         if (getLength() != 4) return cont.erreur("LIST", "ANY? : 1 liste, 1 bloc attendus", lng);
         lst = Contexte.STATE_FALSE;
         String[] t = blankSplitLec(cont, getArg(2));
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "list any?");
         for (String s : t) {
            vars.put("arg1", s);
            int[] e = cont.evaluerBoolLec(cmds, vars);
            ret = e[1];
            if (e[0] == 1) {
               lst = Contexte.STATE_TRUE;
               break;
            }
         }
      }
      else if (cmd.equals("ALL?")) {
         if (getLength() != 4) return cont.erreur("LIST", "ALL? : 1 liste, 1 bloc attendus", lng);
         lst = Contexte.STATE_TRUE;
         String[] t = blankSplitLec(cont, getArg(2));
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "list all?");
         for (String s : t) {
            vars.put("arg1", s);
            int[] e = cont.evaluerBoolLec(cmds, vars);
            ret = e[1];
            if (e[0] == 0) {
               lst = Contexte.STATE_FALSE;
               break;
            }
         }
      }
      else if (cmd.equals("REPEAT")) {
         if (getLength() != 4) return cont.erreur("LIST", "REPEAT : 1 chaîne, 1 nombre attendus", lng);
         String s = Tools.putBraces(getArg(2));
         int n;
         try {
            n = Integer.parseInt(getArg(3));
            if (n < 0) return cont.erreur("LIST", "REPEAT : nombre incorrect : " + n, lng);
         }
         catch(NumberFormatException ex) {
            return cont.erreur("LIST", "REPEAT : nombre incorrect : " + getArg(3), lng);
         }
         lst = new String(new char[n]).replace("\0", s + " ");
      }
      else if (cmd.equals("ZEROS")) {
         if (getLength() != 3) return cont.erreur("LIST", "ZEROS : 1 nombre attendu", lng);
         int n;
         try {
            n = Integer.parseInt(getArg(2));
            if (n < 0) return cont.erreur("LIST", "ZEROS : nombre incorrect : " + n, lng);
         }
         catch(NumberFormatException ex) {
            return cont.erreur("LIST", "ZEROS : nombre incorrect : " + getArg(2), lng);
         }
         lst = new String(new char[n]).replace("\0", "0 ");
      }
      else if (cmd.equals("ONES")) {
         if (getLength() != 3) return cont.erreur("LIST", "ONES : 1 nombre attendu", lng);
         int n;
         try {
            n = Integer.parseInt(getArg(2));
            if (n < 0) return cont.erreur("LIST", "ONES : nombre incorrect : " + n, lng);
         }
         catch(NumberFormatException ex) {
            return cont.erreur("LIST", "ONES : nombre incorrect : " + getArg(2), lng);
         }
         lst = new String(new char[n]).replace("\0", "1 ");
      }
      else if (cmd.equals("COMMAND?")) {
         if (getLength() != 3) return cont.erreur("LIST", "COMMAND? : 1 liste attendue", lng);
         String[] t = blankSplitLec(cont, getArg(2));
         if (t[0].length() > 1 && t[0].charAt(0) == ':') t[0] = t[0].substring(1);
         lst = (cont.getCommand(t[0].toUpperCase()) != null || cont.isSet(t[0]) || cont.isLec(t[0])) ? "1" : "0";
      }
      else if (cmd.equals("TOCSV")) {
         if (getLength() != 3) return cont.erreur("LIST", "TOCSV : 1 liste attendue", lng);
         try {
            StringWriter sw = new StringWriter();
            CSVWriter writer = new CSVWriter(sw, ';', '"', '\\', "\n");
            String[] lines = getArg(2).split("\n");
            for (String l : lines) writer.writeNext(blankSplitLec(cont, l));
            writer.close();
            lst = sw.toString();
         }
         catch (IOException ex) {
            return cont.exception("LIST", "ERREUR IOException : " + ex.getMessage(), lng, ex);
         }
      }
      else return cont.erreur("LIST", cmd + " : commande inconnue", lng);

      cont.setValeur(new ValeurDef(cont, null, lst));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return ret;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  list        Outils de traitement de listes de chaînes\n";
   }
}// class
