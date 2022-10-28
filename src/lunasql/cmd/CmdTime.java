package lunasql.cmd;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.ValeurDef;

/**
 * Commande TIME <br>
 * (Interne) Commande d'utilitaires de date et heure
 * @author M.P.
 */
public class CmdTime extends Instruction {

   private static final SimpleDateFormat
         frmD = new SimpleDateFormat("dd/MM/yyyy"),
         frmDT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

   public CmdTime(Contexte cont) {
      super(cont, TYPE_CMDINT, "TIME", null);
   }

   @Override
   public int execute() {
      if (getLength() < 2)
         return cont.erreur("TIME", "une commande de date est requise", lng);

      String cmd = getArg(1).toUpperCase(), tim;
      if (cmd.equals("NEW")) {
         int l = getLength();
         if (l < 3 || l > 5)
            return cont.erreur("TIME", "NEW : 1 date,  [1 format attendus, 1 locale]", lng);
         try {
            long n;
            if (l == 3) n = parseDate(getArg(2), null, null);
            else if (l == 4) n = parseDate(getArg(2), getArg(3), null);
            else n = parseDate(getArg(2), getArg(3), getArg(4));
            tim = Long.toString(n);
         }
         catch (ParseException ex) {
            return cont.erreur("TIME", "NEW : date incorrecte : " + ex.getMessage(), lng);
         }
         catch (IllegalArgumentException ex) {
            return cont.erreur("TIME", "NEW : format incorrect : " + getArg(3), lng);
         }
      }
      else if (cmd.equals("NOW")) {
         int l = getLength();
         if (l > 3) return cont.erreur("TIME", "NOW : 1 format attendu", lng);
         try {
            tim = l == 2 ? Long.toString(System.currentTimeMillis()) :
               new SimpleDateFormat(Tools.removeQuotes(getArg(2))).format(new Date());
         } catch (IllegalArgumentException ex) {
            return cont.erreur("TIME", "NOW : format incorrect : " + getArg(2), lng);
         }
      }
      else if (cmd.equals("FORMAT")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("TIME", "FORMAT : 1 nombre ms, [1 format] attendus", lng);
         try {
            long n = Long.parseLong(getArg(2));
            tim = (l == 4 ? new SimpleDateFormat(Tools.removeQuotes(getArg(3))) : frmDT).format(new Date(n));
         } catch (NumberFormatException ex) {
            return cont.erreur("TIME", "FORMAT : nombre incorrect : " + getArg(2), lng);
         } catch (IllegalArgumentException ex) {
            return cont.erreur("TIME", "FORMAT : format incorrect : " + getArg(3), lng);
         }
      }
      else if (cmd.equals("INIT")) {
         if (getLength() != 2) return cont.erreur("TIME", "INIT : aucun arg. attendu", lng);
         cont.setChrono();
         cont.setVar(Contexte.ENV_EXEC_TIME, "0");
         tim = "0";
      }
      else if (cmd.equals("CHRON")) { // Mesure du temps qui passe...
         if (getLength() != 2) return cont.erreur("TIME", "CHRON : aucun arg. attendu", lng);
         long tm = cont.setChrono();
         tim = SQLCnx.frmDur(tm);
         cont.setVar(Contexte.ENV_EXEC_TIME, Long.toString(tm));
      }
      else if (cmd.equals("DATE")) {
         try {
            if (getLength() != 2) return cont.erreur("TIME", "DATE : aucun arg. attendu", lng);
            tim = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
         }
         catch (IllegalArgumentException ex) {
            return cont.erreur("TIME", "DATE : format incorrect : " + getArg(2), lng);
         }
      }
      else if (cmd.equals("TIME")) {
         if (getLength() != 2) return cont.erreur("TIME", "TIME : aucun arg. attendu", lng);
         tim = new SimpleDateFormat("HH:mm:ss").format(new Date());
      }
      else if (cmd.equals("DATETIME")) {
         if (getLength() != 2) return cont.erreur("TIME", "DATETIME : aucun arg. attendu", lng);
         tim = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
      }
      else if (cmd.equals("DATETIMEMS")) {
         if (getLength() != 2) return cont.erreur("TIME", "DATETIMEMS : aucun arg. attendu", lng);
         tim = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS").format(new Date());
      }
      else if (cmd.equals("COMPACT")) {
         if (getLength() != 2) return cont.erreur("TIME", "COMPACT : aucun arg. attendu", lng);
         tim = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
      }
      else if (cmd.equals("ADD")) {
         int l = getLength();
         if (l < 4 || l > 6)
            return cont.erreur("TIME", "ADD : 1 date, 1 nombre ms, [1 format, 1 locale] attendus", lng);
         try {
            long n1;
            if (l == 4) n1 = parseDate(getArg(2), null, null);
            else if (l == 5) n1 = parseDate(getArg(2), getArg(4), null);
            else n1 = parseDate(getArg(2), getArg(4), getArg(5));
            tim = Long.toString(n1 + Long.parseLong(getArg(3)));
         }
         catch (NumberFormatException ex) {
            return cont.erreur("TIME", "ADD : nombre incorrect : " + ex.getMessage(), lng);
         }
         catch (ParseException ex) {
            return cont.erreur("TIME", "ADD : date incorrecte : " + ex.getMessage(), lng);
         }
         catch (IllegalArgumentException ex) {
            return cont.erreur("TIME", "ADD : format incorrect : " + getArg(4), lng);
         }
      }
      else if (cmd.equals("DIFF")) {
         int l = getLength();
         if (l < 4 || l > 6) return cont.erreur("TIME", "DIFF : 2 dates, [1 format, 1 locale] attendus", lng);
         try {
            long[] n = readTwoDates(l);
            tim = Long.toString(n[1] - n[0]);
         }
         catch (ParseException ex) {
            return cont.erreur("TIME", "DIFF : date incorrecte : " + ex.getMessage(), lng);
         }
         catch (IllegalArgumentException ex) {
            return cont.erreur("TIME", "DIFF : format incorrect : " + getArg(4), lng);
         }
      }
      else if (cmd.equals("AFTER?")) {
         int l = getLength();
         if (l < 4 || l > 6)
            return cont.erreur("TIME", "AFTER? : 2 dates, [1 format, 1 locale] attendus", lng);
         try {
            long[] n = readTwoDates(l);
            tim = n[0] > n[1] ? "1" : "0";
         }
         catch (ParseException ex) {
            return cont.erreur("TIME", "AFTER? : date incorrecte : " + ex.getMessage(), lng);
         }
         catch (IllegalArgumentException ex) {
            return cont.erreur("TIME", "AFTER? : format incorrect : " + getArg(4), lng);
         }
      }
      else if (cmd.equals("BEFORE?")) {
         int l = getLength();
         if (l < 4 || l > 6)
            return cont.erreur("TIME", "BEFORE? : 2 dates, [1 format, 1 locale] attendus", lng);
         try {
            long[] n = readTwoDates(l);
            tim = n[0] < n[1] ? "1" : "0";
         }
         catch (ParseException ex) {
            return cont.erreur("TIME", "BEFORE? : date incorrecte : " + ex.getMessage(), lng);
         }
         catch (IllegalArgumentException ex) {
            return cont.erreur("TIME", "BEFORE? : format incorrect : " + getArg(4), lng);
         }
      }
      else if (cmd.equals("AT")) {
         int l = getLength();
         if (l < 4 || l > 6)
            return cont.erreur("TIME", "AT : 1 date, [1 format, 1 locale], 1 bloc attendus", lng);
         try {
            long delai, n1;
            if (l == 4) n1 = parseDate(getArg(2), null, null);
            else if (l == 5) n1 = parseDate(getArg(2), getArg(3), null);
            else n1 = parseDate(getArg(2), getArg(3), getArg(4));
            delai = n1 - System.currentTimeMillis();
            if (delai >= 0) runThread(delai, 1, getArg(l - 1), "time at");
            else cont.errprintln("Attention : la date indiquée est dépassée");
            tim = "";
         }
         catch (NumberFormatException ex) {
            return cont.erreur("TIME", "AT : date ms incorrecte : " + ex.getMessage(), lng);
         }
         catch (ParseException ex) {
            return cont.erreur("TIME", "AT : date incorrecte : " + ex.getMessage(), lng);
         }
         catch (IllegalArgumentException ex) {
            return cont.erreur("TIME", "AT : format incorrect : " + getArg(4), lng);
         }
      }
      else if (cmd.equals("AFTER")) {
         if (getLength() != 4) return cont.erreur("TIME", "AFTER : 1 délai, 1 bloc attendus", lng);
         try {
            long t = parsePeriod(getArg(2));
            if (t == 0) return cont.erreur("TIME", "AFTER : délai incorrect : " + getArg(2), lng);
            runThread(t, 1, getArg(3), "time after");
            tim = "";
         }
         catch (NumberFormatException ex) {
            return cont.erreur("TIME", "AFTER : délai ms incorrect : " + getArg(2), lng);
         }
      }
      else if (cmd.equals("REPEAT")) {
         if (getLength() != 5)
            return cont.erreur("TIME", "REPEAT : 1 période, 1 nombre, 1 bloc attendus", lng);
         try {
            long t = parsePeriod(getArg(2));
            if (t == 0) return cont.erreur("TIME", "REPEAT : délai incorrect : " + getArg(2), lng);
            int nb = Integer.parseInt(getArg(3));
            if (nb <= 0) nb = Integer.MAX_VALUE; // "presque" infini !
            runThread(t, nb, getArg(4), "time repeat");
            tim = "";
         }
         catch (NumberFormatException ex) {
            return cont.erreur("TIME", "REPEAT : nombre répétitions incorrect : " + getArg(3), lng);
         }
      }
      else if (cmd.equals("WAIT")) {
         if (getLength() != 3) return cont.erreur("TIME", "WAIT : 1 période attendue", lng);
         long t = parsePeriod(getArg(2));
         if (t == 0) return cont.erreur("TIME", "WAIT : délai incorrect : " + getArg(2), lng);
         try {
            cont.showWheel();
            Thread.sleep(t); // Attente du délai fourni
            cont.hideWheel();
            tim = Long.toString(t);
         }
         catch (InterruptedException ex) {
            cont.hideWheel();
            return cont.exception("WAIT", "erreur InterruptedException : ", lng, ex);
         }
      }
      else if (cmd.equals("DELAY")) {
         if (getLength() != 3) return cont.erreur("TIME", "DELAY : 1 période attendue", lng);
         long t = parsePeriod(getArg(2));
         if (t == 0) return cont.erreur("TIME", "DELAY : délai incorrect : " + getArg(2), lng);
         tim = Long.toString(t);
      }
      else return cont.erreur("TIME", cmd + " : commande inconnue", lng);

      cont.setValeur(new ValeurDef(cont, null, tim));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return RET_CONTINUE;
   }

   /**
    * Lit deux dates
    * @param l nombre d'arguments
    * @return les deux dates en long
    * @throws ParseException
    * @throws IllegalArgumentException
    */
   private long[] readTwoDates(int l) throws ParseException, IllegalArgumentException {
      long n1, n2;
      if (l == 4) {
         n1 = parseDate(getArg(2), null, null);
         n2 = parseDate(getArg(3), null, null);
      }
      else if (l == 5) {
         n1 = parseDate(getArg(2), getArg(4), null);
         n2 = parseDate(getArg(3), getArg(4), null);
      }
      else {
         n1 = parseDate(getArg(2), getArg(4), getArg(5));
         n2 = parseDate(getArg(3), getArg(4), getArg(5));
      }
      return new long[] {n1, n2};
   }

   /**
    * Analyse la date donnée en chaîne de caractères selon le format et la locale donnés
    *
    * @param sdate la date
    * @param frm le format
    * @param sloc la locale (FR, DE, IT, ZH, TW, CA, CAF (français), US, UK, JP, KO)
    * @return la date en ms
    * @throws ParseException
    * @throws IllegalArgumentException
    */
   protected static long parseDate(String sdate, String frm, String sloc)
         throws ParseException, IllegalArgumentException {
      Date dt;
      String d = Tools.removeQuotes(sdate), f;
      if (frm == null) dt = (d.length() == 10 ? frmD : frmDT).parse(d);
      else {
         f = Tools.removeQuotes(frm);
         if (sloc == null) dt = new SimpleDateFormat(f).parse(d);
         else {  // Locale fournie
            Locale loc;
            switch (sloc) {
               case "FR": loc = Locale.FRANCE; break;
               case "DE": loc = Locale.GERMANY; break;
               case "IT": loc = Locale.ITALY; break;
               case "UK": loc = Locale.UK; break;
               case "ZH": loc = Locale.CHINA; break;
               case "TW": loc = Locale.TAIWAN; break;
               case "CA": loc = Locale.CANADA; break;
               case "CAF": loc = Locale.CANADA_FRENCH; break;
               case "JP": loc = Locale.JAPAN; break;
               case "KO": loc = Locale.KOREA; break;
               case "US":
               default: loc = Locale.US;
            }
            dt = new SimpleDateFormat(f, loc).parse(d);
         }
      }
      return dt.getTime();
   }

   /**
    * Exécute le bloc de code en nouveau Thread
    *
    * @param delai le delai entre deux exécutions
    * @param nb le nombre d'exécutions
    * @param code le bloc de code
    * @param cmd la commande à executer
    */
   private void runThread(final long delai, final int nb, final String code, final String cmd) {
      new Thread(() -> {
         try {
            for (int i = 0; i < nb; i++) {
               Thread.sleep(delai);
               cont.addSubMode();
               new Lecteur(cont, code, new HashMap<String, String>() {{ put(Contexte.LEC_SUPER, cmd); }});
               cont.remSubMode();
            }
         }
         catch (InterruptedException ex) {
           cont.erreur("TIME", "Erreur InterruptedException : " + ex.getMessage(), lng);
         }
      }).start();
   }

   /**
    * Analyse le délai pour each
    *
    * @param p la chaîne de délai ex: 1h35m20s
    * @return le résultat en ms
    */
   private long parsePeriod(String p) {
      //                          012             3
      Matcher m = Pattern.compile("(([1-9][0-9]*?)([hmsl]))+?").matcher(p);
      long l = 0L;
      //while (m.find()) for (int i=0; i<m.groupCount(); i++) cont.println(i + ": " +m.group(i));
      while (m.find()) {
         String tps = m.group(3);
         l += ("h".equals(tps) ? 3600000 :
                 ("m".equals(tps) ? 60000 :
                    ("s".equals(tps) ? 1000 : 1))) * Integer.parseInt(m.group(2));
      }
      return l; // ms
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  time        Outils de gestion de date et heure\n";
   }
}// class
