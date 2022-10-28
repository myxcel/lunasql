package lunasql.cmd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.*;

import javax.script.ScriptException;

import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.val.ValeurDef;

/**
 * Commande DICT <br>
 * (Interne) Outils de gestion d'un disctionnaire
 * @author M.P.
 */
public class CmdDict extends Instruction {

   private String dic;

   public CmdDict(Contexte cont){
      super(cont, TYPE_CMDINT, "DICT", null);
   }

   @Override
   public int execute() {
      if (getLength() < 2)
         return cont.erreur("DICT", "une commande de dictionnaire est requise", lng);

      int ret = RET_CONTINUE, r;
      dic = "";
      String cmd = getArg(1).toUpperCase();
      if (cmd.equals("NEW")) {
         int n = getLength();
         if (n % 2 != 0) return cont.erreur("DICT", "NEW : n (clef valeur) attendus", lng);
         Properties prop = new Properties();
         for (int i = 2; i < n; i+=2) {
            String k = getArg(i), v = getArg(i + 1);
            if (k.isEmpty()) continue;
            prop.put(k, v);
         }
         if ((r = dictStore(prop, "NEW")) >= 0) return r;
         //dic = Integer.toString(cont.addVarDict(prop)); // ***NEW***
      }
      else if (cmd.equals("GET")) {
         int l = getLength();
         if (l < 4 || l > 5) return cont.erreur("DICT", "GET : 1 dict., 1 clef, 1 valeur attendus", lng);
         Properties prop = Tools.getProp(getArg(2));
         if (prop == null) return cont.erreur("DICT", "GET : dictionnaire invalide" , lng);
         dic = prop.getProperty(getArg(3));
         if (dic == null) { // attribution de la valeur par défaut
            if (l == 5) {
               Lecteur lec = cont.getCurrentLecteur();
               dic = lec.substituteExt(getArg(4));
               if ((r = lec.getCmdState()) != RET_CONTINUE) return r;
            }
            else dic = "";
         }
      }
      else if (cmd.equals("HAS-KEY?")) {
         if (getLength() != 4) return cont.erreur("DICT", "HAS-KEY? : 1 dict., 1 chaîne attendus", lng);
         Properties prop = Tools.getProp(getArg(2));
         if (prop == null) return cont.erreur("DICT", "HAS-KEY? : dictionnaire invalide" , lng);
         dic = prop.containsKey(getArg(3)) ? "1" : "0";
      }
      else if (cmd.equals("HAS-VAL?")) {
         if (getLength() != 4) return cont.erreur("DICT", "HAS-VAL? : 1 dict., 1 chaîne attendus", lng);
         Properties prop = Tools.getProp(getArg(2));
         if (prop == null) return cont.erreur("DICT", "HAS-VAL? : dictionnaire invalide" , lng);
         dic = prop.containsValue(getArg(3)) ? "1" : "0";
      }
      else if (cmd.equals("SIZE")) {
         if (getLength() != 3) return cont.erreur("DICT", "SIZE : 1 dict. attendu", lng);
         Properties prop = Tools.getProp(getArg(2));
         if (prop == null) return cont.erreur("DICT", "SIZE : dictionnaire invalide" , lng);
         dic = Integer.toString(prop.size());
      }
      else if (cmd.equals("EMPTY?")) {
         if (getLength() != 3) return cont.erreur("DICT", "EMPTY? : 1 dict. attendu", lng);
         Properties prop = Tools.getProp(getArg(2));
         if (prop == null) return cont.erreur("DICT", "EMPTY? : dictionnaire invalide" , lng);
         dic = prop.isEmpty() ? "1" : "0";
      }
      else if (cmd.equals("PUT")) {
         if (getLength() != 5) return cont.erreur("DICT", "PUT : 1 dict., 2 chaînes attendus", lng);
         Properties prop = Tools.getProp(getArg(2));
         if (prop == null) return cont.erreur("DICT", "PUT : dictionnaire invalide" , lng);
         prop.put(getArg(3), getArg(4));
         if ((r = dictStore(prop, "PUT")) >= 0) return r;
      }
      else if (cmd.equals("REMOVE")) {
         if (getLength() != 4) return cont.erreur("DICT", "REMOVE : 1 dict., 1 chaîne attendus", lng);
         Properties prop = Tools.getProp(getArg(2));
         if (prop == null) return cont.erreur("DICT", "REMOVE : dictionnaire invalide" , lng);
         prop.remove(getArg(3));
         if ((r = dictStore(prop, "REMOVE")) >= 0) return r;
      }
      else if (cmd.equals("MERGE")) {
         if (getLength() != 4) return cont.erreur("DICT", "MERGE : 2 dict. attendus", lng);
         Properties prop1 = Tools.getProp(getArg(2)), prop2 = Tools.getProp(getArg(3));
         prop1.putAll(prop2);
         if ((r = dictStore(prop1, "MERGE")) >= 0) return r;
      }
      else if (cmd.equals("KEYS")) {
         if (getLength() != 3) return cont.erreur("DICT", "KEYS : 1 dict. attendu", lng);
         Properties prop = Tools.getProp(getArg(2));
         if (prop == null) return cont.erreur("DICT", "KEYS : dictionnaire invalide" , lng);
         List<String> keys = new ArrayList<>();
         for (Enumeration<Object> e = prop.keys(); e.hasMoreElements();)
            keys.add((String)e.nextElement());
         dic = Tools.arrayToString(keys);
      }
      else if (cmd.equals("VALUES")) {
         if (getLength() != 3) return cont.erreur("DICT", "VALUES : 1 dict. attendu", lng);
         Properties prop = Tools.getProp(getArg(2));
         if (prop == null) return cont.erreur("DICT", "VALUES : dictionnaire invalide" , lng);
         List<String> vals = new ArrayList<>();
         for (Iterator<Object> e = prop.values().iterator(); e.hasNext();)
            vals.add((String)e.next());
         dic = Tools.arrayToString(vals);
      }
      else if (cmd.equals("EACH")) {
         if (getLength() != 4) return cont.erreur("DICT", "EACH : 1 dict., 1 fonction attendus", lng);
         Properties prop = Tools.getProp(getArg(2));
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "dict each");
         Lecteur lec = new Lecteur(cont);
         int nbi = 0;
         for (Map.Entry<Object, Object> me : prop.entrySet()) {
            nbi++;
            vars.put("arg1", (String) me.getKey());
            vars.put("arg2", (String) me.getValue());
            cont.addSubMode();
            lec.add(cmds, vars);
            lec.doCheckWhen();
            cont.remSubMode();
         }
         lec.fin();
         dic = Integer.toString(nbi);
      }
      else if (cmd.equals("APPLY")) {
         if (getLength() != 4) return cont.erreur("DICT", "APPLY : 1 dict., 1 bloc attendus", lng);
         Properties prop = Tools.getProp(getArg(2));
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "dict apply");
         Lecteur lec = new Lecteur(cont);
         for (Map.Entry<Object, Object> me : prop.entrySet()) {
            vars.put("arg1", (String) me.getKey());
            vars.put("arg2", (String) me.getValue());
            cont.addSubMode();
            lec.add(cmds, vars);
            lec.doCheckWhen();
            cont.remSubMode();
            prop.put(cont.getLecVar("arg1"), cont.getLecVar("arg2"));
         }
         lec.fin();
         if ((r = dictStore(prop, "APPLY")) >= 0) return r;
      }
      else if (cmd.equals("FILTER")) {
         if (getLength() != 4) return cont.erreur("DICT", "FILTER : 1 dict., 1 bloc attendus", lng);
         Properties prop = Tools.getProp(getArg(2));
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "dict filter");
         Properties prop2 = new Properties();
         for (Map.Entry<Object, Object> me : prop.entrySet()) {
            if (ret != RET_CONTINUE) break;
            String key = (String) me.getKey(), val = (String) me.getValue();
            vars.put("arg1", key);
            vars.put("arg2", val);
            int[] re = cont.evaluerBoolLec(cmds, vars);
            ret = re[1];
            if (re[0] == 1) prop2.put(key, val);
         }
         if ((r = dictStore(prop2, "FILTER")) >= 0) return r;
      }
      else if (cmd.equals("ANY?")) {
         if (getLength() != 4) return cont.erreur("DICT", "ANY? : 1 dict., 1 bloc attendus", lng);
         dic = Contexte.STATE_FALSE;
         Properties prop = Tools.getProp(getArg(2));
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "dict any?");
         for (Map.Entry<Object, Object> me : prop.entrySet()) {
            if (ret != RET_CONTINUE) break;
            String key = (String) me.getKey(), val = (String) me.getValue();
            vars.put("arg1", key);
            vars.put("arg2", val);
            int[] e = cont.evaluerBoolLec(cmds, vars);
            ret = e[1];
            if (e[0] == 1) {
               dic = Contexte.STATE_TRUE;
               break;
            }
         }
      }
      else if (cmd.equals("ALL?")) {
         if (getLength() != 4) return cont.erreur("DICT", "ALL? : 1 dict., 1 bloc attendus", lng);
         dic = Contexte.STATE_TRUE;
         Properties prop = Tools.getProp(getArg(2));
         String cmds = getArg(3);
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "dict all?");
         for (Map.Entry<Object, Object> me : prop.entrySet()) {
            if (ret != RET_CONTINUE) break;
            String key = (String) me.getKey(), val = (String) me.getValue();
            vars.put("arg1", key);
            vars.put("arg2", val);
            int[] e = cont.evaluerBoolLec(cmds, vars);
            ret = e[1];
            if (e[0] == 0) {
               dic = Contexte.STATE_FALSE;
               break;
            }
         }
      }
      else if (cmd.equals("LOCAL")) {
         int l = getLength();
         if (l < 3 || l > 4) return cont.erreur("DICT", "LOCAL : 1 dict., [1 liste] attendus", lng);
         Properties prop = Tools.getProp(getArg(2));
         List keep = l == 4 ? Arrays.asList(getArg(3).split(",")) : null;
         for (Map.Entry<Object, Object> me : prop.entrySet()) {
            String key = (String) me.getKey();
            if (keep == null || keep.contains(key)) { // si liste de keys fournie : on ne garde qu'elles
               if (cont.valideKey(key)) cont.setLecVar(key, (String) me.getValue());
               else return cont.erreur("DICT", "affectation de variable invalide : " + key, lng);
            }
         }
      }
      else return cont.erreur("DICT", cmd + " : commande inconnue", lng);

      cont.setValeur(new ValeurDef(cont, null, dic));
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
      return ret;
   }

   private int dictStore(Properties prop, String cmd) {
      try {
         ByteArrayOutputStream os = new ByteArrayOutputStream();
         prop.store(os, null);
         dic = "\n" + os.toString(cont.getVar(Contexte.ENV_FILE_ENC));
         // saut de ligne à conserver : pour échapper le # (dégroupeur d'arguments)
         return -1;
      } catch (IOException ex) {
         return cont.erreur("DICT", cmd + " : " + ex.getMessage(), lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc() {
      return "  dict        Outils de traitement de dictionnaires de chaînes\n";
   }
}// class
