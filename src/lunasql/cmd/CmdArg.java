package lunasql.cmd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.script.ScriptException;

import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;

/**
 * Commande ARG <br>
 * (Interne) Affectation de variables locales au sein d'une fonction
 * @author M.P.
 */
public class CmdArg extends Instruction {

   Pattern opt_pattern;

   public CmdArg(Contexte cont){
      super(cont, TYPE_CMDINT, "ARG", null);
   }

   @Override
   public int execute() {
      if (opt_pattern == null) opt_pattern = Pattern.compile(
         "^(?s)(\\*|\\[)?(\\.?" + Contexte.KEY_PATTERN + ")(:((\\w+)|(`(.+)`)))?(\\s+(.*?))?\\]?$");
      //m      1         2                                 4 56      7 8        9    10
      //ex. [var:`pattern` val] [var:type val] *var

      String fname = cont.getLecVar(Contexte.LEC_SCR_NAME), // peut être null
             vname = cont.getLecVar(Contexte.LEC_THIS),     // ne doit pas être null
             sname = cont.getLecVar(Contexte.LEC_SUPER);    // peut être null
      if (vname == null) vname = "script";
      boolean isrootscr = vname.equals(fname) && (sname == null || sname.isEmpty() || vname.equals(sname)),
              hasgrp = false;
      int nbprm = 0;

      List<String> l = getCommand(1), noms = new ArrayList<String>();
      for (int i = 0; i < l.size(); i++) {
         String arg, key = l.get(i);
         if (key.length() == 0) return cont.erreur("ARG", "nom de paramètre vide", lng);

         // Validation des arguments
         if (key.equals("/") && i < l.size() - 1) {
            String expr = getSCommand(i + 2);
            try {
               if (cont.evaluerBool(expr)) break;
               else return cont.erreur("ARG", "précondition non remplie pour " + vname + " : " + expr, lng);
            }
            catch (ScriptException e) {// erreur prévisible > cont.erreur
               return cont.erreur("ARG", "impossible d'évaluer l'expression :\n" + e.getMessage(), lng);
            }
         }

         // Paramètre optionel
         Matcher m = opt_pattern.matcher(key);
         if (m.matches()) {
            //for (int j=1; j<=m.groupCount(); j++) cont.println("group(" + j + ") : " + m.group(j));
            nbprm++;
            key = m.group(2);
            boolean grp = "*".equals(m.group(1)) , opt = "[".equals(m.group(1));
            if (grp) hasgrp = true;
            String typ = m.group(6), typre = m.group(8), val = null;

            if (grp) { // Paramètres groupés des valeurs finales
               StringBuilder sb = new StringBuilder();
               while (cont.isLec(arg = (isrootscr ? Contexte.LEC_SCR_ARG : Contexte.LEC_VAR_ARG) + (++i))) {
                  val = cont.getLecVar(arg);
                  int r = verifType(typ, typre, key, val);
                  if (r > -1) return r;
                  sb.append(Tools.putBraces(val)).append(' ');
               }
               if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
               cont.setLecVar(key, sb.toString());
               break;
            }
            else { // Paramètres simples ou optionnel
               if (cont.isLec(arg = (isrootscr ? Contexte.LEC_SCR_ARG : Contexte.LEC_VAR_ARG) + (i+1))) {
                  val = cont.getLecVar(arg);
               }
               else if (opt) {
                  int r;
                  Lecteur lec = cont.getCurrentLecteur();
                  val = m.group(10) == null ? "" : lec.substituteExt(m.group(10));
                  if ((r = lec.getCmdState()) != RET_CONTINUE) return r;
               }
               if (val == null) {
                  return cont.erreur("ARG", "paramètre '" + key + "' manquant "
                          + " (usage : " + vname + ' ' + getSCommand(1) + ')', lng);
               }
               // Vérification de type
               int r = verifType(typ, typre, key, val);
               if (r > -1) return r;

               // Types ok
               if (noms.contains(key)) cont.errprintln("Attention : argument '" + key + "' dupliqué");
               else noms.add(key);
               cont.setLecVar(key, val);
            }
         } else return cont.erreur("ARG", "affectation de variable invalide : " + key, lng);
      } // for

      // Test des arguments en trop et message d'avertissement
      if (!hasgrp && cont.isLec(isrootscr ? Contexte.LEC_SCR_ARG_NB : Contexte.LEC_VAR_ARG_NB)) {
         int nbarg = Integer.parseInt(isrootscr ? cont.getLecVar(Contexte.LEC_SCR_ARG_NB)
                                                : cont.getLecVar(Contexte.LEC_VAR_ARG_NB));
         if (nbprm < nbarg && cont.getVerbose() >= Contexte.VERB_AFF) {
            String vn = cont.getLecVar(Contexte.LEC_THIS);
            if (vn == null) vn = "script";
            cont.errprintln("Attention : trop d'arguments pour " + vn +
                  " (attendus : " + nbprm + ", reçus : " + nbarg + ')');
         }
      }

      cont.setValeur(null);
      cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
      return RET_CONTINUE;
   }

   /**
    * Vérification de type
    * @param typ le type
    * @param typre sinon le patron de valeur
    * @param key la clef
    * @param val la valeur
    * @return numéro d'erreur ou -1 si ok
    */
   private int verifType(String typ, String typre, String key, String val) {
      try {
         boolean typok = true;
         if (typ != null) {
            if (typ.equals("int")) typok = val.matches("[+-]?\\d+");
            else if (typ.equals("nat")) typok = val.matches("\\d+");
            else if (typ.equals("num")) typok = val.matches("[+-]?\\d*(\\.\\d+)?([eE][+-]?\\d+)?");
            else if (typ.equals("bool")) typok = val.matches("(?i)0|1|t(rue)?|f(alse)?");
            else if (typ.equals("yesno")) typok = val.matches("(?i)y(es)?|n(o(n)?)?|o(ui)?");
            else if (typ.equals("id")) typok = val.matches(Contexte.KEY_PATTERN);
            else if (typ.equals("opt")) typok = val.matches("[_:][_A-Z]+");
            else if (typ.equals("idopt")) typok = val.matches("[_:]?" + Contexte.KEY_PATTERN);
            else if (typ.equals("char")) typok = val.length() == 1;
            else if (typ.equals("word")) typok = val.matches("\\w+");
            else if (typ.equals("date"))
               typok = val.matches("(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/((19|20)\\d\\d)");
            else if (typ.equals("datetime"))
               typok = val.matches("(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/((19|20)\\d\\d) ([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d");
            else if (typ.equals("dateus"))
               typok = val.matches("(0?[1-9]|1[012])/(0?[1-9]|[12][0-9]|3[01])/((19|20)\\d\\d)");
            else if (typ.equals("datets"))
               typok = val.matches("((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])");
            else if (typ.equals("alpha")) typok = val.matches("[A-Za-z]+");
            else if (typ.equals("alphanum")) typok = val.matches("[A-Za-z0-9]+");
            else if (typ.equals("alphauc")) typok = val.matches("[A-Z]+");
            else if (typ.equals("alphalc")) typok = val.matches("[a-z]+");
            else if (typ.equals("email")) typok = val.matches("(?i)([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6})");
            else if (!typ.equals("str")) 
               return cont.erreur("ARG", "type de données inconnu : " + typ, lng);
         }
         else if (typre != null) typok = val.matches(typre);
         if (!typok) return cont.erreur("ARG", "type invalide pour '" + key
                 + "' (usage : " + cont.getLecVar(Contexte.LEC_VAR_NAME) + ' ' + getSCommand(1) + ')', lng);
         return -1;
      }
      catch (PatternSyntaxException ex) {
         return cont.erreur("ARG", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  arg         Affecte les arguments en variables locales\n";
   }
}// class
