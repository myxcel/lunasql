package lunasql.cmd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import lunasql.lib.Contexte;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande OPT <br>
 * (Interne) Affectation d'une donnée à une variable SYSTEME de l'environnement d'exécution
 * @author M.P.
 */
public class CmdOpt extends Instruction {

   private final OptionParser parser;

   public CmdOpt(Contexte cont){
      super(cont, TYPE_CMDINT, "OPT", "_");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("l", "référence locale au lecteur en cours");
      parser.accepts("u", "référence en lecteur père").withRequiredArg().ofType(Integer.class)
            .describedAs("niveau");
      parser.accepts("d", "test d'existence de l'option").withRequiredArg().ofType(String.class)
            .describedAs("opt");
      parser.nonOptions("nom_opt définition_opt").ofType(String.class);
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
         Valeur vr = new ValeurDef(cont);
         List<?> lc = options.nonOptionArguments();
         boolean local = options.has("l"), link = options.has("u");

         // Listage de toutes les variables système
         if (lc.isEmpty()) {
            if (options.has("d")) { // test d'existence
               boolean isset = true, isloc = false;
               String key = (String) options.valueOf("d");
               if (!cont.isNonSys(key) && (cont.isSet(key) || (isloc = cont.isLec(key)))) {
                  vr.appendDispValue(SQLCnx.frm(key) + (isloc ? cont.getLecVar(key) : cont.getGlbVar(key)),
                        Contexte.VERB_BVR, true);
               } else {
                  vr.appendDispValue(key + " non définie", Contexte.VERB_BVR, true);
                  isset = false;
               }
               vr.setSubValue(isset ? "1" : "0");
            }
            else {
               int nb = 0;
               StringBuilder sb = new StringBuilder();
               SortedSet<String> sort;
               Iterator<String> iter;
               // Options lecteur
               HashMap<String, String> vlec = cont.getAllLecVars();
               if (vlec != null) {
                  sort = new TreeSet<>(vlec.keySet());
                  iter = sort.iterator();
                  while (iter.hasNext()) {
                     nb++;
                     String key = iter.next();
                     if (cont.isSys(key) || cont.isSysUser(key))
                        sb.append(SQLCnx.frm(key + " *")).append(' ')
                              .append(link ? cont.getLinkVar(key, (Integer) options.valueOf("u")) :
                                    cont.getLecVar(key)).append('\n');
                  }
               }

               if (!local && !link) { // Options globales
                  sort = new TreeSet<>(cont.getAllVars().keySet());
                  iter = sort.iterator();
                  while (iter.hasNext()) {
                     nb++;
                     String key = iter.next();
                     if (cont.isSys(key) || cont.isSysUser(key))
                        sb.append(SQLCnx.frm(key)).append(' ').append(cont.getGlbVar(key)).append('\n');
                  }
               }
               if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
               vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
               vr.setSubValue(Integer.toString(nb));
            }
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         }

         // Listage d'une (ou plusieurs) variable(s) système
         else if (lc.size() == 1) {
            try {
               String key = Tools.removeBQuotes((String) lc.get(0)).value, val = null;
               int nbv = 0;
               StringBuilder sb = new StringBuilder();
               SortedSet<String> sort;
               Iterator<String> iter;
               Pattern ptnb = Pattern.compile(key);

               // Options lecteur
               HashMap<String, String> vlec = cont.getAllLecVars();
               if (vlec != null) {
                  sort = new TreeSet<>(vlec.keySet());
                  iter = sort.iterator();
                  while (iter.hasNext()) {
                     String k = iter.next();
                     if ((cont.isSys(k) || cont.isSysUser(k)) && ptnb.matcher(k).matches()) {
                        val = link ? cont.getLinkVar(k, (Integer) options.valueOf("u"))
                                   : cont.getLecVar(k);
                        sb.append(SQLCnx.frm(k + " *")).append(' ').append(val).append('\n');
                        nbv++;
                        if (!key.equals(k)) nbv++;
                        cont.setVar(Contexte.ENV_RET_VALUE, val);
                     }
                  }
               }
               if (!local && !link) { // Options globales
                  sort = new TreeSet<>(cont.getAllVars().keySet());
                  iter = sort.iterator();
                  while (iter.hasNext()) {
                     String k = iter.next();
                     if ((cont.isSys(k) || cont.isSysUser(k)) && ptnb.matcher(k).matches()) {
                        val = cont.getGlbVar(k);
                        nbv++;
                        if (!key.equals(k)) nbv++;
                        sb.append(SQLCnx.frm(k)).append(' ').append(val).append('\n');
                     }
                  }
               }
               if (val == null)
                  return cont.erreur("OPT", "option système '" + key + "' non définie", lng);
               if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
               if (nbv > 1) vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
               vr.setSubValue(val);
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
            }
            catch (PatternSyntaxException ex) {
               return cont.erreur("OPT", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
            }
         }
         else { // Modification d'une option
            String allow = cont.getVar(Contexte.ENV_CONST_EDIT);
            if ("0".equals(allow) || (!local && "1".equals(allow)))
               return cont.erreur("OPT", "modification d'une option non autorisée\n" +
                     "(console lancée avec l'option --deny-opt-cmd. Cf documentation de OPT)", lng);

            String key = (String) lc.get(0);
            if (cont.isSysUser(key)) { // var système uniquement
               String val = listToString(lc, 1);
               if (link) cont.setLinkVar(key, (Integer) options.valueOf("u"));
               if (local || link) cont.setLecVar(key, val);
               else cont.setVar(key, val); // pas setVar2 car système
               vr.setDispValue("-> constante système fixée : " + key, Contexte.VERB_BVR);
               vr.setSubValue(val);
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
            }
            else
               return cont.erreur("OPT", "modification d'option système non autorisée", lng);
         }

         vr.setRet();
         cont.setValeur(vr);
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("OPT", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("OPT", "ERREUR IOException : " + ex.getMessage() , lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  opt, _      Modifie la valeur d'une option système de config.\n";
   }
}// class

