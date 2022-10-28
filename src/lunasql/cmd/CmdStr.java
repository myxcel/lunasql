package lunasql.cmd;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Security;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.ValeurDef;
import opencsv.CSVReader;

/**
 * Commande STR <br>
 * (Interne) Commande d'utilitaires de traitement d'une chaîne de caractères
 * @author M.P.
 */
public class CmdStr extends Instruction {

   public CmdStr(Contexte cont){
      super(cont, TYPE_CMDINT, "STR", null);
   }

   @Override
   public int execute() {
      if (getLength() < 2)
         return cont.erreur("STR", "une commande de chaîne est requise", lng);

      String cmd = getArg(1).toUpperCase(), str;
      if (cmd.equals("NEW")) {
         str = getSCommand(2);
      }
      else if (cmd.equals("LEN")) {
         if (getLength() != 3) return cont.erreur("STR", "LEN : 1 chaîne attendue", lng);
         str = Integer.toString(getArg(2).length());
      }
      else if (cmd.equals("EQ?")) {
         if (getLength() != 4) return cont.erreur("STR", "EQ? : 2 chaînes attendue", lng);
         str = getArg(2).equals(getArg(3)) ? "1" : "0";
      }
      else if (cmd.equals("EQ-IC?")) {
         if (getLength() != 4) return cont.erreur("STR", "EQ-IC? : 2 chaînes attendue", lng);
         str = getArg(2).equalsIgnoreCase(getArg(3)) ? "1" : "0";
      }
      else if (cmd.equals("NEQ?")) {
         if (getLength() != 4) return cont.erreur("STR", "NEQ? : 2 chaînes attendue", lng);
         str = getArg(2).equals(getArg(3)) ? "0" : "1";
      }
      else if (cmd.equals("NEQ-IC?")) {
         if (getLength() != 4) return cont.erreur("STR", "NEQ-IC? : 2 chaînes attendue", lng);
         str = getArg(2).equalsIgnoreCase(getArg(3)) ? "0" : "1";
      }
      else if (cmd.equals("EMPTY?")) {
         if (getLength() != 3) return cont.erreur("STR", "EMPTY? : 1 chaîne attendue", lng);
         str = getArg(2).isEmpty() ? "1" : "0";
      }
      else if (cmd.equals("NEMPTY?")) {
         if (getLength() != 3) return cont.erreur("STR", "NEMPTY? : 1 chaîne attendue", lng);
         str = getArg(2).isEmpty() ? "0" : "1";
      }
      else if (cmd.equals("INDEX")) {
         if (getLength() != 4) return cont.erreur("STR", "INDEX : 2 chaînes attendues", lng);
         str = Integer.toString(getArg(2).indexOf(getArg(3)));
      }
      else if (cmd.equals("LINDEX")) {
         if (getLength() != 4) return cont.erreur("STR", "LINDEX : 2 chaînes attendues", lng);
         str = Integer.toString(getArg(2).lastIndexOf(getArg(3)));
      }
      else if (cmd.equals("SUBSTR")) {
         if (getLength() != 4) return cont.erreur("STR", "SUBSTR : 1 chaîne, 1 index attendus", lng);
         try { str = Tools.subString(getArg(2), getArg(3)); }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "SUBSTR : format de sous-chaîne incorrect : " + getArg(3), lng);
         }
      }
      else if (cmd.equals("REPLACE")) {
         if (getLength() != 5) return cont.erreur("STR", "REPLACE : 1 chaîne, 1 regexp, 1 chaîne attendues", lng);
         try { str = getArg(2).replaceAll(Tools.removeBQuotes(getArg(3)).value, getArg(4)); }
         catch (PatternSyntaxException ex) {
            return cont.erreur("STR", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("CUT")) {
         int l = getLength();
         if (l < 4 || l > 5) return cont.erreur("STR", "CUT : 1 chaîne, 1 nb, [1 chaîne] attendus", lng);
         int nb;
         try {
            nb = Integer.parseInt(getArg(3));
            if (nb < 0) throw new NumberFormatException(nb + " (nombre positif attendu)");
         }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "CUT : nombre incorrect : " + ex.getMessage(), lng);
         }
         String s = getArg(2), c = l == 5 ? getArg(4) : "";
         if (nb >= s.length()) str = s;
         else if (nb < c.length()) str = c.substring(0, nb);
         else str = s.substring(0, nb - c.length()) + c;
      }
      else if (cmd.equals("HAS?")) {
         if (getLength() != 4) return cont.erreur("STR", "HAS? : 2 chaînes attendues", lng);
         str = getArg(2).contains(getArg(3)) ? "1" : "0";
      }
      else if (cmd.equals("DIGIT?")) {
         if (getLength() != 3) return cont.erreur("STR", "DIGIT? : 1 chaîne attendue", lng);
         str = getArg(2).matches("\\d+") ? "1" : "0";
      }
      else if (cmd.equals("NUM?")) {
         if (getLength() != 3) return cont.erreur("STR", "NUMBER? : 1 chaîne attendue", lng);
         str = getArg(2).matches("[+-]?\\d*(\\.\\d+)?([eE][+-]?\\d+)?") ? "1" : "0";
      }
      else if (cmd.equals("ALPHA?")) {
         if (getLength() != 3) return cont.erreur("STR", "ALPHA? : 1 chaîne attendue", lng);
         str = getArg(2).matches("(?i)[A-Z]+") ? "1" : "0";
      }
      else if (cmd.equals("ALNUM?")) {
         if (getLength() != 3) return cont.erreur("STR", "ALNUM? : 1 chaîne attendue", lng);
         str = getArg(2).matches("(?i)[A-Z0-9]+") ? "1" : "0";
      }
      else if (cmd.equals("LIST?")) {
         if (getLength() != 3) return cont.erreur("STR", "LIST? : 1 chaîne attendue", lng);
         str = getArg(2).indexOf(' ') > 0 ? "1" : "0";
      }
      else if (cmd.equals("DICT?")) {
         if (getLength() != 3) return cont.erreur("STR", "DICT? : 1 chaîne attendue", lng);
         Properties prop =  Tools.getProp(getArg(2));
         if (prop == null) return cont.erreur("STR", "DICT? : dictionnaire incorrect", lng);
         boolean isdict = !prop.isEmpty();
         for (Map.Entry<Object, Object> kv : prop.entrySet()) {
            String k = (String)kv.getKey(), v = (String)kv.getValue();
            isdict = !k.isEmpty() && !v.isEmpty();
            if (!isdict) break;
         }
         str = isdict ? "1" : "0";
      }
      else if (cmd.equals("DATE?")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("STR", "DATE? : 1 chaîne, [1 format] attendu", lng);
         try {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat(l == 3 ? "dd/MM/yyyy" : getArg(3));
            df.setLenient(false);
            df.parse(getArg(2));
            str = "1"; 
         } catch (java.text.ParseException e) {
            str = "0";
         } catch (IllegalArgumentException ex) {
            return cont.erreur("STR", "DATE? : format de date incorrect : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("UPPER")) {
         if (getLength() != 3) return cont.erreur("STR", "UPPER : 1 chaîne attendue", lng);
         str = getArg(2).toUpperCase();
      }
      else if (cmd.equals("LOWER")) {
         if (getLength() != 3) return cont.erreur("STR", "LOWER : 1 chaîne attendue", lng);
         str = getArg(2).toLowerCase();
      }
      else if (cmd.equals("CAPIT")) {
         if (getLength() != 3) return cont.erreur("STR", "CAPIT : 1 chaîne attendue", lng);
         String s = getArg(2);
         str = s.length() > 1 ? s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase()
               : s.toUpperCase();
      }
      else if (cmd.equals("TRIM")) {
         if (getLength() != 3) return cont.erreur("STR", "TRIM : 1 chaîne attendue", lng);
         str = getArg(2).trim();
      }
      else if (cmd.equals("RTRIM")) {
         if (getLength() != 3) return cont.erreur("STR", "RTRIM : 1 chaîne attendue", lng);
         str = getArg(2).replaceAll("\\s+$","");
      }
      else if (cmd.equals("LTRIM")) {
         if (getLength() != 3) return cont.erreur("STR", "LTRIM : 1 chaîne attendue", lng);
         str = getArg(2).replaceAll("^\\s+","");
      }
      else if (cmd.equals("TRIMALL")) {
         if (getLength() != 3) return cont.erreur("STR", "TRIMALL : 1 chaîne attendue", lng);
         str = getArg(2).trim().replaceAll("\\s+"," ");
      }
      else if (cmd.equals("RPAD")) {
         int l = getLength();
         if (l < 4 || l > 5) return cont.erreur("STR", "RPAD : 1 chaîne, 1 nb, [1 car.] attendus", lng);
         int nb;
         try { nb = Integer.parseInt(getArg(3)); }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "RPAD : nombre incorrect : " + ex.getMessage(), lng);
         }
         String s;
         char c = l == 5 && !(s = getArg(4)).isEmpty() ? s.charAt(0) : ' ';
         str = SQLCnx.frm(getArg(2), nb, c);
      }
      else if (cmd.equals("LPAD")) {
         int l = getLength();
         if (l < 4 || l > 5) return cont.erreur("STR", "LPAD : 1 chaîne, 1 nb, [1 car.] attendus", lng);
         int nb;
         try { nb = Integer.parseInt(getArg(3)); }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "LPAD : nombre incorrect : " + ex.getMessage(), lng);
         }
         String s;
         char c = l == 5 && !(s = getArg(4)).isEmpty() ? s.charAt(0) : ' ';
         str = SQLCnx.frmI(getArg(2), nb, c);
      }
      else if (cmd.equals("STARTS?")) {
         if (getLength() != 4) return cont.erreur("STR", "STARTS? : 2 chaînes attendues", lng);
         str = getArg(2).startsWith(getArg(3)) ? "1" : "0";
      }
      else if (cmd.equals("ENDS?")) {
         if (getLength() != 4) return cont.erreur("STR", "ENDS? : 2 chaînes attendues", lng);
         str = getArg(2).endsWith(getArg(3)) ? "1" : "0";
      }
      else if (cmd.equals("COMP")) {
         if (getLength() != 4) return cont.erreur("STR", "COMP : 2 chaînes attendues", lng);
         str = Integer.toString(getArg(3).compareToIgnoreCase(getArg(2)));
      }
      else if (cmd.equals("COMP-IC")) {
         if (getLength() != 4) return cont.erreur("STR", "COMP-IC : 2 chaînes attendues", lng);
         str = Integer.toString(getArg(3).compareToIgnoreCase(getArg(2)));
      }
      else if (cmd.equals("MATCHES?")) {
         if (getLength() != 4) return cont.erreur("STR", "MATCHES? : 1 chaîne, 1 regexp attendues", lng);
         try { str = getArg(2).matches(Tools.removeBQuotes(getArg(3)).value) ? "1" : "0"; }
         catch (PatternSyntaxException ex) {
            return cont.erreur("STR", "MATCHES? : syntaxe regexp incorrecte : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("SPLIT")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("STR", "SPLIT : 1 chaîne, [1 regexp] attendues", lng);
         try {
            List<String> lt = Arrays.asList
                  (getArg(2).split(l == 3 ? " " : Tools.removeBQuotes(getArg(3)).value));
            str = Tools.arrayToString(lt);
         }
         catch (PatternSyntaxException ex) {
            return cont.erreur("STR", "SPLIT : syntaxe regexp incorrecte : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("CONCAT")) {
         int l = getLength();
         if (l < 3) return cont.erreur("STR", "CONCAT : chaînes attendues", lng);
         StringBuilder sb = new StringBuilder();
         for (int i = 2; i <= l; i++) sb.append(getArg(i));
         str = sb.toString();
      }
      else if (cmd.equals("REPEAT")) {
         if (getLength() != 4) return cont.erreur("STR", "REPEAT : 1 chaîne, 1 nb attendus", lng);
         int nb;
         try { nb = Integer.parseInt(getArg(3)); }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "REPEAT : nombre incorrect : " + ex.getMessage(), lng);
         }
         StringBuilder sb = new StringBuilder();
         for (int i = 1; i <= nb; i++) sb.append(getArg(2));
         str = sb.toString();
      }
      else if (cmd.equals("FIRST")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("STR", "FIRST : 1 chaîne, [1 nombre] attendus", lng);
         String s = getArg(2);
         int nb;
         if (l == 4) {
            try {
               nb = Math.min(Integer.parseInt(getArg(3)), s.length());
               if (nb < 0) throw new NumberFormatException("négatif : " + nb);
            }
            catch (NumberFormatException ex) {
               return cont.erreur("STR", "FIRST : nombre incorrect : " + ex.getMessage(), lng);
            }
         }
         else nb = 1;
         str = s.isEmpty() || nb == 0 ? "" : s.substring(0, nb);
      }
      else if (cmd.equals("LAST")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("STR", "LAST : 1 chaîne, [1 nombre] attendus", lng);
         String s = getArg(2);
         int nb;
         if (l == 4) {
            try {
               nb = Math.min(Integer.parseInt(getArg(3)), s.length());
               if (nb < 0) throw new NumberFormatException("négatif : " + nb);
            }
            catch (NumberFormatException ex) {
               return cont.erreur("STR", "LAST : nombre incorrect : " + ex.getMessage(), lng);
            }
         }
         else nb = 1;
         str = s.isEmpty() || nb == 0 ? "" : s.substring(s.length() - nb);
      }
      else if (cmd.equals("CHOP")) {
         if (getLength() != 3) return cont.erreur("STR", "CHOP : 1 chaîne attendue", lng);
         String s = getArg(2);
         str = s.length() > 0 ? s.substring(0, s.length() - 1) : s;
      }
      else if (cmd.equals("REVERSE")) {
         if (getLength() != 3) return cont.erreur("STR", "REVERSE : 1 chaîne attendue", lng);
         StringBuilder sb = new StringBuilder(getArg(2));
         str = sb.reverse().toString();
      }
      else if (cmd.equals("FIND")) {
         if (getLength() != 4) return cont.erreur("STR", "FIND : 1 chaîne, 1 regexp attendues", lng);
         try {
            Matcher m = Pattern.compile(Tools.removeBQuotes(getArg(3)).value).matcher(getArg(2));
            StringBuilder sb = new StringBuilder();
            while (m.find()) sb.append(m.start()).append(' ');
            str = sb.toString();
         }
         catch (PatternSyntaxException ex) {
            return cont.erreur("STR", "FIND : syntaxe regexp incorrecte : " + ex.getMessage(), lng);
         }
      }
      else  if (cmd.equals("GROUPS")) {
         if (getLength() != 4) return cont.erreur("STR", "GROUPS : 1 chaîne, 1 regexp attendues", lng);
         try {
            Matcher m = Pattern.compile(Tools.removeBQuotes(getArg(3)).value).matcher(getArg(2));
            Properties pr = new Properties();
            if (m.matches()) {
               for (int i = 0; i <= m.groupCount(); i++) pr.put(Integer.toString(i), m.group(i));
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            pr.store(os, null);
            str = os.toString(cont.getVar(Contexte.ENV_FILE_ENC));
         } catch (PatternSyntaxException ex) {
            return cont.erreur("STR", "GROUPS : syntaxe regexp incorrecte : " + ex.getMessage(), lng);
         } catch (IOException ex) {
            return cont.erreur("STR", "GROUPS : " + ex.getMessage(), lng);
         }
      }
      else  if (cmd.equals("FORMAT")) {
         int l = getLength();
         if (l < 4) return cont.erreur("STR", "FORMAT : 1 chaîne, x val attendues", lng);
         l = l - 3;
         Object[] t = new Object[l];
         try {
         for (int i = 0; i < l; i++) {
            String s = getArg(i + 3);
            if (s.matches("[0-9]+")) t[i] = Integer.parseInt(s);
            else if (s.matches("[+-]?\\d*(\\.\\d+)?([eE][+-]?\\d+)?")) t[i] = Float.parseFloat(s);
            else t[i] = s;
         }
         str = String.format(getArg(2), t);
         }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "FORMAT : nombre incorrect : " + ex.getMessage(), lng);
         }
         catch (IllegalFormatException ex) {
            return cont.erreur("STR", "FORMAT : format incorrect : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("CONVERT")) {
         if (getLength() != 5) return cont.erreur("STR", "CONVERT : 1 chaîne, 2 ensembles attendues", lng);
         char[] s = getArg(2).toCharArray(), r = Arrays.copyOfRange(s, 0, s.length),
                seti = Tools.charRangeToSet(getArg(3)).toCharArray(),
                seto = Tools.charRangeToSet(getArg(4)).toCharArray();
         if (seti.length != seto.length)
            return cont.erreur("STR", "CONVERT : ensembles de tailles différentes (" +
                                       seti.length + " contre " + seto.length + ")", lng);
         // Parcours de la chaîne pour remplacement
         for (int i = 0; i < s.length; i++) {
            for (int j = 0; j < seti.length; j++) {
               if (s[i] == seti[j]) { r[i] = seto[j]; break; }
            }
         }
         str = new String(r);
      }
      else if (cmd.equals("CLEANSQL")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("STR", "CLEANSQL : 1 chaîne, [indent 0/1/2] attendus", lng);
         int mode; // mode formaté et indenté par défaut
         if (l > 3) {
            if (getArg(3).equals("0")) mode = -2;        // ni formaté, ni indenté
            else if (getArg(3).equals("1")) mode = -1;   // formaté, non indenté
            else if (getArg(3).equals("2")) mode = 0;    // formaté et indenté
            else return cont.erreur("STR", "CLEANSQL : mode 0/1/2 attendu", lng);
         }
         else mode = 0;
         str = Tools.cleanSQLCode(cont, getArg(2), mode);
      }
      else if (cmd.equals("WRAP")) {
         int l = getLength();
         if (l < 3 || l > 6)
            return cont.erreur("STR", "WRAP : 1 chaîne, [1 longueur, 1 chaîne, 1 mode] attendus", lng);
         int lg = 70;
         String mrg = "";
         int idt = 0;
         try {
            if (l > 3) {
               lg = Integer.parseInt(getArg(3));
               if (l > 4) {
                  mrg = getArg(4);
                  if (l > 5) {
                     idt = Integer.parseInt(getArg(5));
                     if (idt < -1 || idt > 1)
                        return cont.erreur("STR", "WRAP : mode de retrait incorrect : " + idt, lng);
                  }
               }
            }
         }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "WRAP : nombre incorrect : " + ex.getMessage(), lng);
         }
         str = Tools.textToLines(getArg(2), lg, mrg, idt);
      }
      else if (cmd.equals("ASC")) {
         String s;
         if (getLength() != 3 || (s = getArg(2)).isEmpty())
            return cont.erreur("STR", "ASC : 1 chaîne non vide attendue", lng);
         str = Integer.toString(s.charAt(0));
      }
      else if (cmd.equals("CHR")) {
         if (getLength() != 3) return cont.erreur("STR", "CHR : 1 code car. attendu", lng);
         try {
            str = "" + (char)Integer.parseInt(getArg(2));
         }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "ORD : nombre incorrect : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("NEXT")) {
         if (getLength() != 3) return cont.erreur("STR", "NEXT : 1 chaîne attendue", lng);
         try { str = Lecteur.stringInc(getArg(2)); }
         catch (IllegalArgumentException ex) {
            return cont.erreur("STR", "NEXT : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("B64ENC")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("STR", "B64ENC : 1 chaîne, [1 nombre] attendus", lng);
         try {
            str = l == 3 ? Security.b64encode(getArg(2).getBytes()) :
               Security.b64encode(getArg(2).getBytes(), Integer.parseInt(getArg(3)));
         }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "B64ENC : nombre incorrect : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("B64DEC")) {
         if (getLength() != 3) return cont.erreur("STR", "B64DEC : 1 chaîne attendue", lng);
         try {
            str = new String(Security.b64decode(getArg(2)));
         } catch (IllegalArgumentException ex) {
            return cont.erreur("STR", "B64DEC : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("HEXENC")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("STR", "HEXENC : 1 chaîne, [1 nombre] attendus", lng);
         try {
            str = l == 3 ? Security.hexencode(getArg(2).getBytes()) :
               Security.hexencode(getArg(2).getBytes(), Integer.parseInt(getArg(3)));
         }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "HEXENC : nombre incorrect : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("HEXDEC")) {
         if (getLength() != 3) return cont.erreur("STR", "HEXDEC : 1 chaîne attendue", lng);
         try {
            str = new String(Security.hexdecode(getArg(2)));
         } catch (IllegalArgumentException ex) {
            return cont.erreur("STR", "HEXDEC : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("ZIPENC")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("STR", "ZIPENC : 1 chaîne, [1 nombre] attendue", lng);
         try {
            int nbc = l == 3 ? 0 : Integer.parseInt(getArg(3));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(bos, 4096);
            gos.write(getArg(2).getBytes(cont.getVar(Contexte.ENV_FILE_ENC)));
            gos.finish();
            str = Security.b64encode(bos.toByteArray(), nbc);
            gos.close();
         }
         catch (IOException ex) {
            return cont.erreur("STR", "ZIPENC : " + ex.getMessage(), lng);
         }
         catch (NumberFormatException ex) {
            return cont.erreur("STR", "ZIPENC : nombre incorrect : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("ZIPDEC")) {
         if (getLength() != 3) return cont.erreur("STR", "ZIPDEC : 1 chaîne attendue", lng);
         try {
            ByteArrayInputStream bis = new ByteArrayInputStream(Security.b64decode(getArg(2)));
            GZIPInputStream gis = new GZIPInputStream(bis, 4096);
            // merci à http://hmkcode.com/java-convert-inputstream-to-string/
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(gis));
            int c;
            while((c = br.read()) != -1) sb.append((char)c);
            br.close();
            str = sb.toString();
         }
         catch (IOException|IllegalArgumentException ex) {
            return cont.erreur("STR", "ZIPDEC : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("VIGENC")) {
         if (getLength() != 3) return cont.erreur("STR", "VIGENC : 1 chaîne attendue", lng);
         str = Security.brouille(getArg(2));
      }
      else if (cmd.equals("VIGDEC")) {
         if (getLength() != 3) return cont.erreur("STR", "VIGDEC : 1 chaîne attendue", lng);
         str = Security.debrouille(getArg(2));
      }
      else if (cmd.equals("CODE")) {
         if (getLength() != 3) return cont.erreur("STR", "CODE : 1 chaîne attendue", lng);
         str = Integer.toHexString(getArg(2).hashCode());
      }
      else if (cmd.equals("HASH")) {
         if (getLength() != 3) return cont.erreur("STR", "HASH : 1 chaîne attendue", lng);
         byte[] tb = getArg(2).getBytes();
         Checksum ck = new CRC32();
         ck.update(tb, 0, tb.length);
         str = Long.toHexString(ck.getValue());
      }
      else if (cmd.equals("DIGEST")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("STR", "DIGEST : 1 chaîne, [hex|b64] attendu", lng);
         str = new Security().getHashStr(getArg(2), l == 3 ? "hex" : getArg(3));
      }
      else if (cmd.equals("LEVEN")) {
         if (getLength() != 4) return cont.erreur("STR", "LEVEN : 2 chaînes attendue", lng);
         str = Integer.toString(Tools.LevenshteinDistance(getArg(2), getArg(3)));
      }
      else if (cmd.equals("ESCAPE")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("STR", "ESCAPE : 1 chaîne, [1 car.] attendue", lng);
         try {
            str = getArg(2).replaceAll("'", l == 3 ? "\\\\'" : getArg(3) + "'");
         }
         catch (IllegalArgumentException ex) {
            return cont.erreur("STR", "ESCAPE : caractère invalide : " + getArg(3) +
                    " (essayer de l'échapper par '\\')", lng);
         }
      }
      else if (cmd.equals("NORM")) {
         if (getLength() != 3) return cont.erreur("STR", "NORM : 1 chaîne attendue", lng);
         str = Security.sansAccent(getArg(2));
      }
      else if (cmd.equals("FROMCSV")) {
         if (getLength() != 3) return cont.erreur("STR", "FROMCSV : 1 chaîne attendue", lng);
         CSVReader reader = new CSVReader(new StringReader(getArg(2)), ';', '"', '\\', 0, false, true);
         List<String> lt = new ArrayList<>();
         String[] line;
         try {
            while ((line = reader.readNext()) != null) 
               lt.add(Tools.arrayToString(Arrays.asList(line)));
            reader.close();
            switch (lt.size()) {
               case 0: str = ""; break;
               case 1: str = lt.get(0); break;
               default:
                  StringBuilder sb = new StringBuilder();
                  for (String s : lt) sb.append('{').append(s).append("}\n");
                  str = sb.toString();
            }
            
         }
         catch (IOException ex) {
            return cont.erreur("STR", "FROMCSV : " + ex.getMessage(), lng);
         }
      }
      else if (cmd.equals("TEST")) { // sous commande cachée, pour tests uniquement
         try {
            // tests
            System.out.println("nothing left to test");
         } catch (Exception ex) { ex.printStackTrace();}
         str = "";
      }
      else return cont.erreur("STR", cmd + " : commande inconnue", lng);

      cont.setValeur(new ValeurDef(cont, null, str));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  str         Outils de traitement de chaînes\n";
   }
}// class
