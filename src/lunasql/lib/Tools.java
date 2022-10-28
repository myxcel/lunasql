package lunasql.lib;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.text.PlainDocument;

public class Tools {

   /**
    * Ajout d'une valeur à un tableau
    * 
    * @param a le tableau
    * @param b les valeurs
    * @return concat
    */
   public static String[] arrayAppend(String[] a, String... b) {
      return arrayConcat(a, b);
  }

   /**
    * Concaténation de deux tableaux de chaînes
    * Inspiré du code jeannicolas
    * http://stackoverflow.com/questions/80476/how-to-concatenate-two-arrays-in-java
    * 
    * @param a le tableau 1
    * @param b le tableau 2
    * @return concat
    */
   public static String[] arrayConcat(String[] a, String[] b) {
      int la = a.length, lb = b.length;
      String[] r = new String[la + lb];
      System.arraycopy(a, 0, r, 0, la);
      System.arraycopy(b, 0, r, la, lb);
      return r;
   }

   /**
    * Transforme un tableau List en chaîne, en protégeant les valeurs contenant des espaces par {}
    * 
    * @param lt la liste
    * @param braces si on met des braces  {} 
    * @return la chaîne
    */
   public static String arrayToString(List<String> lt, boolean braces) {
      StringBuilder sb = new StringBuilder();
      for (String s : lt) sb.append(braces ? putBraces(s) : s).append(' ');
      if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
   }
   public static String arrayToString(List<String> lt) {
      return arrayToString(lt, true);
   }

   /**
    * Retourne la chaîne éventuellement protégée par des {}. Si elle est déjà encadrée, on retourne
    * la chaîne sans ajout.
    * 
    * @param s la chaîne
    * @return la châine avec ou non des {}
    */
   public static String putBraces(String s) {
      if (s.isEmpty()) return "{}";
      if ("{[`'\"".indexOf(s.charAt(0)) >= 0) return s;
      if (s.startsWith("$$") && s.endsWith("$$")) return s;
      s = s.replace("{", "^{").replace("}", "^}");

      // Recherche des espaces
      boolean hasws = false;
      for (char c : s.toCharArray())
         if (Character.isWhitespace(c) || "\"'()[]{}".indexOf(c) >= 0) {
            hasws = true;
            break;
         }
      return hasws ? '{' + s + '}' : s;
   }

   /**
    * Teste si la châine a des {} en bout de chaîne
    * 
    * @param s la chaîne
    * @return true si {}
    */
   public static boolean hasBraces(String s) {
      return hasBeginEnd(s, '{', '}');
   }

   /**
    * Teste si la châine a des [] en bout de chaîne
    * 
    * @param s la chaîne
    * @return true si {}
    */
   public static boolean hasBrackets(String s) {
      return hasBeginEnd(s, '[', ']');
   }

   /**
    * Teste si la châine a des caractères de début et de fin en bout de chaîne
    *
    * @param s la chaîne
    * @return true si oui
    */
   public static boolean hasBeginEnd(String s, char begin, char end) {
      return s.length() >= 2 && (s.charAt(0) == begin && s.charAt(s.length() - 1) == end);
   }

   /**
    * Suppression intelligente des [] en bouts de chaîne seulement s'il y en a
    *
    * @param str la chaîne
    * @return la même sans les []
    */
   public static String removeBracketsIfAny(String str) {
      if (hasBrackets(str)) return removeBrackets(str);
      return str;
   }

   /**
    * Suppression intelligente des [] en bouts de chaîne
    *
    * @param str la chaîne
    * @return la même sans les []
    */
   public static String removeBrackets(String str) {
      return removeBeginEnd(str, '[', ']');
   }

   /**
    * Suppression intelligente des {} en bouts de chaîne s'il y en a
    *
    * @param str la chaîne
    * @return la même sans les {}
    */
   public static String removeBracesIfAny(String str) {
      if (hasBraces(str)) return removeBraces(str);
      return str;
   }

   /**
    * Suppression intelligente des {} en bouts de chaîne
    *
    * @param str la chaîne
    * @return la même sans les {}
    */
   public static String removeBraces(String str) {
      return removeBeginEnd(str, '{', '}');
   }

   /**
    * Suppression de caractère ouvrant et fermant en bouts de chaîne
    * La suppession est intelligente : si [a b [c d] e], mais pas si [a b] c [d e]
    *
    * @param str la chaîne
    * @return la même sans les []/{}/<>...
    */
   public static String removeBeginEnd(String str, char begin, char end) {
      int nbc = 0;
      for (int i = 1; i < str.length() - 1; i++) {
         if (str.charAt(i) == begin) nbc ++;
         else if (str.charAt(i) == end) nbc --;
         if (nbc < 0) break;
      }
      if (nbc == 0 && str.length() > 2) str = str.substring(1, str.length() - 1);
      return str;
   }

   /**
    * Suppression des `` en bouts de chaîne pour les patrons regexp
    * Exception : commande ARG qui parse elle-même les chaînes avec ``
    * 
    * @param str la chaîne
    * @return BQRet (la même sans les ``, s'il y a eu retrait des ``)
    */
   public static BQRet removeBQuotes(String str) {
      int l;
      if (str.length() < 2 || str.charAt(0) != '`' || str.charAt(l = str.length() - 1) != '`')
         return new BQRet(str, false);
      return new BQRet(str.substring(1, l), true);
   }

   /**
    * Suppression des '' en bouts de chaîne pour dates
    * Exception : commande WAIT qui parse elle-même son délai avec ''
    *
    * @param str la chaîne
    * @return la même sans les ''
    */
   public static String removeQuotes(String str) {
      int l;
      if (str.length() < 2 || str.charAt(0) != '\'' || str.charAt(l = str.length() - 1) != '\'')
         return str;
      return str.substring(1, l);
   }

   /**
    * Classe qui sert de retour de la fonction removeBQuotes
    */
   public static final class BQRet {
      public boolean hadBQ;
      public String  value;

      public BQRet (String s, boolean b) {
         this.value = s;
         this.hadBQ = b;
      }

      @Override
      public String toString() {
         return this.value;
      }
   }

   /**
    * Supprime les {} et "" d'une liste pour l'applatir
    *
    * @param l la liste
    * @return la list falt
    */
   public static String flatList(String l) {
      return l
         // ""
         .replace("^\"", "%<g>").replaceAll("[\"]", "").replace("%<g>", "\"")
         // {}
         .replace("^{", "%<oc>").replace("^}", "%<cc>")
         .replaceAll("\\{|\\}", "")
         .replace("%<oc>", "{").replace("%<cc>", "}");
   }

   /**
    * Coupure d'une chaîne avec espaces ou non en tenant compte d'eventuels {}, ""...
    * 
    * @param cont le contexte
    * @param str la chaîne à couper
    * @return un tableau
    */
   public static String[] blankSplitLec(Contexte cont, String str) {
      if (str == null || str.length() == 0) return new String[]{};

      // Analyz par Lecteur pour les regroupements internes par "", {}
      Lecteur lec = new Lecteur(cont, removeBracketsIfAny(str), false);
      ArrayList<String> lret = new ArrayList<>();
      ArrayList<ArrayList<String>> lcmd = lec.getCurrentCmd();
      for (ArrayList<String> ltmp : lcmd) {
         for (String s : ltmp) {
            if (hasBraces(s)) s = s.substring(1, s.length() - 1);
            lret.add(s);
         }
      }
      return lret.toArray(new String[]{});
   }

   /**
    * Obtient une sous-liste de liste
    *
    * @param cont le contexte
    * @param list une chaîne représentant la liste
    * @param idx l'index complexe à rechercher
    * @return une chaîne représentant la sous-liste
    */
   public static String subList(Contexte cont, String list, String idx) throws NumberFormatException {
      StringBuilder ret = new StringBuilder();
      String[] tval = blankSplitLec(cont, list);
      for (String kv : idx.split(",")) {
         int i = kv.indexOf(":"), d = 0, l = tval.length, f = l;
         if (i == -1) {
            if (kv.isEmpty()) for (int j = d; j < f; j++) ret.append(putBraces(tval[j])).append(' ');
            else {
               d = Math.min(Integer.parseInt(kv), l - 1);
               if (d < 0) d = l + d;
               if (d < l) ret.append(tval[d]).append(' ');
            }
         }
         else {
            if (i == 0) {
               String sf = kv.substring(1);
               if (!sf.isEmpty()) f = Math.min(Integer.parseInt(sf), l);
               if (f < 0) f = l + f;
            }
            else if (i == kv.length() - 1) {
               d = Integer.parseInt(kv.substring(0, i));
               if (d < 0) d = l + d;
            }
            else {
               d = Integer.parseInt(kv.substring(0, i));
               if (d < 0) d = l + d;
               f = Math.min(Integer.parseInt(kv.substring(i + 1)), l);
               if (f < 0) f = l + f;
            }
            if (f < d) for (int j = Math.min(d, l - 1); j >= Math.max(f + 1, 0); j--) ret.append(putBraces(tval[j])).append(' ');
            else for (int j = d; j < f; j++) ret.append(putBraces(tval[j])).append(' ');
         }
      }
      if (ret.length() > 0) ret.deleteCharAt(ret.length() - 1);
      return ret.toString();
   }

   /**
    * Obtient une sous-chaîne de chaîne
    * 
    * @param str la chaîne
    * @param idx l'index complexe à rechercher
    * @return une sous-chaîne
    */
   public static String subString(String str, String idx) throws NumberFormatException {
      StringBuilder ret = new StringBuilder();
      for (String kv : idx.split(",")) {
         int i = kv.indexOf(':'), d = 0, l = str.length(), f = l;
         if (i == -1) {
            d = Math.min(Integer.parseInt(kv), l - 1);
            if (d < 0) d = l + d;
            if (d < l) ret.append(str.charAt(d));
         }
         else {
            if (i == 0) {
               String sf = kv.substring(1);
               if (!sf.isEmpty()) f = Math.min(Integer.parseInt(sf), l);
               if (f < 0) f = l + f;
            }
            else if (i == kv.length() - 1) {
               d = Integer.parseInt(kv.substring(0, i));
               if (d < 0) d = l + d;
            }
            else {
               d = Integer.parseInt(kv.substring(0, i));
               if (d < 0) d = l + d;
               f = Math.min(Integer.parseInt(kv.substring(i + 1)), l);
               if (f < 0) f = l + f;
            }
            if (f < d) ret.append(new StringBuilder(str.substring(Math.max(f + 1, 0), Math.min(d + 1, l))).reverse());
            else if (d < f && d >= 0 && f <= l) ret.append(str, d, f);
         }
      }
      return ret.toString();
   }

   /**
    * Convertir un InputStream en String
    * Merci à Wayan Saryada (http://kodejava.org/how-do-i-convert-inputstream-to-string/)
    * 
    * @param is le stream
    * @return la châine
    * @throws java.io.IOException si erreur en fichier
    */
   public static String streamToString(InputStream is) throws IOException {
      if (is == null) return "";
      Writer writer = new StringWriter();
      char[] buf = new char[1024];
      try {
         Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
         int n;
         while ((n = reader.read(buf)) != -1) writer.write(buf, 0, n);
      } finally { is.close(); }
      return writer.toString().trim();
   }

   /**
    * Conversion d'une chaîne en une liste de propriétés.
    * La châine peut être encadrée par [] ou {}, et les paires peuvent être séparées par //
    * 
    * @param s la chaîne
    * @return propriétés
    */
   public static Properties getProp(String s) {
      try {
         Properties prop = new Properties();
         s = s.replace("//", "\n");
         prop.load(new StringReader(removeBracesIfAny(removeBracketsIfAny(s))));
         return prop;
      }
      catch (IOException ex) { return null; }
   }

   /**
    * Eregistre le Property dans une chaine
    * @param p l'objet Property
    * @return la chaine
    */
   public static String propStore(Properties p, String enc) {
      try {
         ByteArrayOutputStream os = new ByteArrayOutputStream();
         p.store(os, null);
         return os.toString(enc);
      } catch (IOException ex) {
         return null;
      }
   }

   /**
    * Calcul de la distance de Levenstein
    * Origine : https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
    * Licence : https://creativecommons.org/licenses/by-sa/3.0/ (le code n'a pas été modifié)
    * 
    * @param lhs la chaîne 1
    * @param rhs la chaîne 2
    * @return la distance
    */
   public static int LevenshteinDistance (CharSequence lhs, CharSequence rhs) {
      int len0 = lhs.length() + 1;
      int len1 = rhs.length() + 1;
      // the array of distances
      int[] cost = new int[len0];
      int[] newcost = new int[len0];
      // initial cost of skipping prefix in String s0
      for (int i = 0; i < len0; i++) cost[i] = i;
      // dynamically computing the array of distances
      // transformation cost for each letter in s1
      for (int j = 1; j < len1; j++) {
          // initial cost of skipping prefix in String s1
          newcost[0] = j;
          // transformation cost for each letter in s0
          for(int i = 1; i < len0; i++) {
              // matching current letters in both strings
              int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;
              // computing cost for each transformation
              int cost_replace = cost[i - 1] + match;
              int cost_insert  = cost[i] + 1;
              int cost_delete  = newcost[i - 1] + 1;
              // keep minimum cost
              newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
          }
          // swap cost/newcost arrays
          int[] swap = cost; cost = newcost; newcost = swap;
      }
      // the distance is the cost for transforming all letters in both strings
      return cost[len0 - 1];
  }

   /**
    * Nettoyage d'un code LunaSQL par suppression des commentaires et des espaces en trop
    * Formate aussi le code en y insérant l'indentantion ad hoc
    * 
    * @param cont le contexte
    * @param code le code
    * @return le code nettoyé des commentaires / espaces en trop, et indenté
    */
   public static String cleanSQLCode(Contexte cont, String code) {
      return cleanSQLCode(cont, code, 0);
   }

   /**
    * Nettoyage d'un code LunaSQL par suppression des commentaires et des espaces en trop
    * Si deep >= 0, formate aussi le code en y insérant l'indentantion ad hoc
    *
    * @param cont le contexte
    * @param code le code
    * @param deep la profondeur (>=0 : indentation + multiligne,
    *             -1 : pas d'indentation + multiligne, -2 : pas d'indentation + monoligne)
    * @return le code nettoyé des commentaires / espaces en trop, et indenté
    */
   public static String cleanSQLCode(Contexte cont, String code, int deep) {
      if (code == null) return null;

      char sep = deep == -2 ? ' ' : '\n'; // pas d'indentation + monoligne
      ArrayList<ArrayList<String>> cmds = new Lecteur(cont, code, false).getCurrentCmd();
      StringBuilder sb1 = new StringBuilder();
      for (ArrayList<String> cmdi : cmds) { // pour chaque commande du bloc
         StringBuilder sb2 = new StringBuilder();
         for (String arg : cmdi) { // pour chaque argument de la commande
            if (hasBraces(arg)) {
               arg = arg.substring(1, arg.length() - 1);
               sb2.append('{').append(sep)
                     .append(cleanSQLCode(cont, arg, deep < 0 ? deep : deep + 1));
               for (int i = 0; i < deep * 2; i++) sb2.append(' ');
               sb2.append('}');
            }
            else sb2.append(arg);
            sb2.append(' ');
         }
         if (sb2.length() > 0) sb2.deleteCharAt(sb2.length() - 1);
         for (int i = 0; i < deep * 2; i++) sb1.append(' ');
         sb1.append(sb2).append(';').append(sep);
      }
      return sb1.toString();
   }

   /**
    * Nettoyage d'un code comme cleanSQLCode, mais supprime toute commande HIST
    * N'insère pas d'indentation. Utile pour la commande HIST.
    *
    * @param cont le contexte
    * @param code le code
    * @return le code nettoyé des commentaires / espaces en trop
    */
   public static String cleanSQLCodeDelHist(Contexte cont, String code) {
      if (code == null) return null;

      ArrayList<ArrayList<String>> cmds = new Lecteur(cont, code, false).getCurrentCmd();
      StringBuilder sb1 = new StringBuilder();
      for (ArrayList<String> cmdi : cmds) {
         if (!cmdi.isEmpty() && cmdi.get(0).matches("(?i)HI(ST)?")) continue;
         StringBuilder sb2 = new StringBuilder();
         for (String arg : cmdi) {
            if (hasBraces(arg)) {
               arg = arg.substring(1, arg.length() - 1);
               sb2.append("{").append(cleanSQLCodeDelHist(cont, arg)).append('}');
            }
            else sb2.append(arg);
            sb2.append(' ');
         }
         if (sb2.length() > 0) sb2.deleteCharAt(sb2.length() - 1);
         sb1.append(sb2).append(";\n");
      }
      return sb1.toString();
   }

   /**
    * Coupe un texte en différentes lignes de taille donnée en respectant les mots.
    * Si la dernière ligne ne contient que des blancs, ces espaces sont supprimés des débuts de
    * ligne du texte. Des caractères blanc sont ajoutés en début de ligne selon indent.
    *
    * @param txt la lign de texte
    * @param lg la longueur des lignes
    * @param mrg les caractères de marge
    * @param idt le mode de retrait : 0: uniforme, -1: 1ère ligne en avant, 1: 1ère ligne en arrière
    * @return la chaîne coupée
    */
   public static String textToLines(String txt, int lg, String mrg, int idt) {
      if (txt == null) return null;
      if (lg < 10) lg = 10;

      String sp = null;
      Matcher m = Pattern.compile("\\A(\r?\n)?(.+)\n([\t ]*)\\Z", Pattern.DOTALL).matcher(txt);
      if (m.find()) {
         txt = m.group(2);
         sp = m.group(3);
      }
      String[] lines = txt.split("\r?\n");
      StringBuilder sb = new StringBuilder();
      for (String ln : lines) {// pour chaque ligne de texte
         String s = sp == null ? mrg + ln : ln.replaceAll("^" + sp, mrg);
         String si;
         if (idt == 1) sb.append(mrg);
         int corr = mrg.length() * idt;
         while (s.length() > lg) {
            int i; // Recherche où couper avant lg
            for (i = lg - corr; i > 0; i--) if (i < s.length() && s.charAt(i) == ' ') break;
            if (i == 0) { // Pas d'espace avant, recherche après...
               for (i = lg - corr; i < s.length(); i++) if (s.charAt(i) == ' ') break;
            }
            si = s.substring(0, i);
            sb.append(si).append('\n').append(mrg);
            if (idt == -1) sb.append(mrg);
            if (i == s.length()) s = "";
            else s = s.substring(i + 1);
            corr = 0;
         }
         sb.append(s).append('\n');
      }
      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
   }

   /**
    * Coupe un texte en différentes lignes de 70 car. Cf textToLines(String, int, String)
    *
    * @param txt la lign de texte
    * @param indent les caractères d'indentation
    * @return la chaîne coupée
    */
   public static String textToLines(String txt, String indent) {
      return textToLines(txt, 70, indent, 0);
   }

   /**
    * Transforme un intervalle de caractères de type "a-e" en ensemble "abcde".
    * 
    * @param txt le texte à transformer
    * @return le transformé
    */
   public static String charRangeToSet(String txt) {
      if (txt == null) return null;
      txt = Tools.removeBQuotes(txt).value;
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < txt.length(); i++) {
         char c = txt.charAt(i);
         if (c != '-' || i == 0 || i == txt.length() - 1) sb.append(c);
         else {
            int c0 = sb.charAt(sb.length() - 1), c1 = txt.charAt(++i);
            if (c0 <= c1) for (int j = c0 + 1; j <= c1; j++) sb.append((char) j);
            else for (int j = c0 - 1; j >= c1; j--) sb.append((char) j);
         }
      }
      return sb.toString();
   }

   /**
    * Affichage d'un message en JOptionPane.showMessageDialog et zone de texte
    * @param parent la fenêtre mère
    * @param msg le message à afficher
    * @param title le titre
    */
   public static void textMessage(JFrame parent, String msg, String title) {
      textMessage(parent, msg, title, javax.swing.JOptionPane.INFORMATION_MESSAGE);
   }
   public static void textMessage(JFrame parent, String msg, String title, int type) {
      javax.swing.JTextArea area = new javax.swing.JTextArea(15, 40);
      javax.swing.JScrollPane pane = new javax.swing.JScrollPane(area);
      area.setEditable(false);
      //area.setFont(new java.awt.Font("Monospace", java.awt.Font.PLAIN, 12));
      area.setText(msg);
      JOptionPane.showMessageDialog(parent, pane, title, type);
   }

   public static void infoMessage(JFrame parent, String msg, String title) {
      JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.INFORMATION_MESSAGE);
   }
   public static void errorMessage(JFrame parent, String msg, String title) {
      JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
   }
   public static void warningMessage(JFrame parent, String msg, String title) {
      JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.WARNING_MESSAGE);
   }
   public static String passwordInput(JFrame parent, String title) {
      JPasswordField pf = new JPasswordField();
      int okCxl = JOptionPane.showConfirmDialog(parent, pf, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      return okCxl == JOptionPane.OK_OPTION ? new String(pf.getPassword()) : null;
   }

   /**
    * Affichage d'un code source en JOptionPane.showMessageDialog et zone d'édition
    *
    * @param parent la fenêtre mère
    * @param code le code à afficher
    * @param title le titre
    * @param modif si le code doit être éditable pour retour
    * @return le nouveau code si modif=true, null sinon
    */
   public static String codeMessage(JFrame parent, String code, String title, boolean modif) {
      try {
         Class.forName("jsyntaxpane.DefaultSyntaxKit");
         jsyntaxpane.DefaultSyntaxKit.initKit();
      }
      catch (ClassNotFoundException ex) {}

      JEditorPane edit = new JEditorPane();
      JScrollPane pane = new JScrollPane(edit);
      pane.setPreferredSize(new Dimension(600, 300));
      edit.setFont(new java.awt.Font("Courier New", Font.PLAIN, 12));
      edit.setContentType("text/sql");
      edit.getDocument().putProperty(PlainDocument.tabSizeAttribute, 2);
      edit.setEditable(modif);
      edit.setText(code);

      // Affichage
      String s = null;
      if (modif) {
        int r = JOptionPane.showConfirmDialog(parent, pane, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
         if (r == JOptionPane.OK_OPTION) s = edit.getText();
      } else JOptionPane.showMessageDialog(parent, pane, title, JOptionPane.INFORMATION_MESSAGE);
      return s;
   }
   
   /**
    * Affichage d'un code source en JOptionPane.showMessageDialog et zone d'édition (non éditable)
    * 
    * @param parent la fenêtre mère
    * @param code le code à afficher
    * @param title le titre
    * @return null
    */
   public static String codeMessage(JFrame parent, String code, String title) {
      return codeMessage(parent, code, title, false);
   }
}// et voilou
