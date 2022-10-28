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
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande DEF <br> (Interne) Affectation d'une donnée ou d'un code à une variable
 * de l'environnement d'exécution
 * @author M.P.
 */
public class CmdDef extends Instruction {

   private final OptionParser parser;

   public CmdDef(Contexte cont) {
      super(cont, TYPE_CMDINT, "DEF", ":");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("d", "test d'existence de la variable").withRequiredArg().ofType(String.class)
         .describedAs("var").withValuesSeparatedBy(',');
      parser.accepts("y", "test d'existence et si non null").withRequiredArg().ofType(String.class)
            .describedAs("var").withValuesSeparatedBy(',');
      parser.accepts("e", "évaluation du contenu").withRequiredArg().ofType(String.class)
         .describedAs("vars");
      parser.accepts("l", "référence locale au lecteur en cours");
      parser.accepts("u", "référence en lecteur père").withRequiredArg().ofType(Integer.class)
         .describedAs("niveau");
      parser.accepts("h", "ajout d'une aide de variable").withRequiredArg().ofType(String.class)
         .describedAs("help");
      parser.accepts("a", "ajout à la variable existante");
      parser.accepts("c", "mise à jour du complètement");
      parser.accepts("r", "pas de contrôle de ref. circulaire");
      parser.nonOptions("nom_var définition_var").ofType(String.class);
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#execute(lunasql.lib.Contexte)
    */
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
         boolean toret = true; // si false on ne retourne rien (option -e)
         Valeur vr = new ValeurDef(cont);
         boolean local = options.has("l"), link = options.has("u");

         // Listage de toutes les variables
         if (lc.isEmpty()) {
            if (options.has("d")) { // test d'existence
               boolean isset = true;
               for (Object o : options.valuesOf("d")) {
                  String key = (String) o;
                  boolean isloc = false;
                  if (cont.isNonSys(key) && (cont.isSet(key) || (isloc = cont.isLec(key)))) {
                     vr.appendDispValue(SQLCnx.frm(key) + (isloc ? cont.getLecVar(key) : cont.getGlbVar(key)),
                           Contexte.VERB_BVR, true);
                  } else {
                     vr.appendDispValue(key + " non définie", Contexte.VERB_BVR, true);
                     isset = false;
                  }
               }
               vr.setSubValue(isset ? "1" : "0");
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
            } else if (options.has("y")) { // test d'existence et non vide
               boolean isset = true;
               for (Object o : options.valuesOf("y")) {
                  String key = (String) o;
                  if (cont.isNonSys(key)) {
                     String val = cont.getLecVar(key);
                     if (val == null) val = cont.getGlbVar(key);
                     isset = val != null && !val.isEmpty();
                     vr.appendDispValue(SQLCnx.frm(key) + val, Contexte.VERB_BVR, true);
                  } else {
                     vr.appendDispValue(key + " non définie ou vide", Contexte.VERB_BVR, true);
                     isset = false;
                  }
               }
               vr.setSubValue(isset ? "1" : "0");
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
            } else if (options.has("e")) {
               String key = (String) options.valueOf("e");
               toret = false;
               if (cont.isNonSys(key) && (cont.isSet(key) || cont.isLec(key))) {
                  HashMap<String, String> vars = new HashMap<>();
                  vars.put(Contexte.LEC_THIS, key);
                  vars.put(Contexte.LEC_SUPER, "");
                  cont.addSubMode();
                  new Lecteur(cont, cont.getVar(key), vars);
                  cont.remSubMode();
               } else return cont.erreur("DEF", "variable '" + key + "' non définie", lng);
            } else if (options.has("c")) {
               // Mise à jour du complètement
               cont.setCompletors();
            } else {
               int nb = 0;
               StringBuilder sb = new StringBuilder();
               SortedSet<String> sort;
               Iterator<String> iter;
               // Variables lecteur
               HashMap<String, String> vlec = cont.getAllLecVars();
               if (vlec != null) {
                  sort = new TreeSet<>(vlec.keySet());
                  iter = sort.iterator();
                  while (iter.hasNext()) {
                     String key = iter.next();
                     if (cont.isNonSys(key)) {
                        nb++;
                        sb.append(SQLCnx.frm(key + " *")).append(' ')
                              .append(link ? cont.getLinkVar(key, (Integer) options.valueOf("u")) :
                                    cont.getLecVar(key)).append('\n');
                     }
                  }
               }
               if (!local && !link) { // Variables globales
                  sort = new TreeSet<>(cont.getAllVars().keySet());
                  iter = sort.iterator();
                  while (iter.hasNext()) {
                     String key = iter.next();
                     if (cont.isNonSys(key)) {
                        nb++;
                        sb.append(SQLCnx.frm(key)).append(' ').append(cont.getGlbVar(key)).append('\n');
                     }
                  }
               }
               if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
               vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
               vr.setSubValue(Integer.toString(nb));
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
            }
         } else if (lc.size() == 1) {    // Affichage, exécution ou test d'une variable
            // Simple affichage du contenu
            try {
               String key = Tools.removeBQuotes((String) lc.get(0)).value, val = null;
               int nbv = 0;
               StringBuilder sb = new StringBuilder();
               SortedSet<String> sort;
               Iterator<String> iter;
               Pattern ptnb = Pattern.compile(key);

               // Variables lecteur
               HashMap<String, String> vlec = cont.getAllLecVars();
               if (vlec != null) {
                  sort = new TreeSet<>(vlec.keySet());
                  iter = sort.iterator();
                  while (iter.hasNext()) {
                     String k = iter.next();
                     if (cont.isNonSys(k) && ptnb.matcher(k).matches()) {
                        val = link ? cont.getLinkVar(k, (Integer) options.valueOf("u"))
                              : cont.getLecVar(k);
                        sb.append(SQLCnx.frm(k + " *")).append(' ').append(val).append('\n');
                        nbv++;
                        if (!key.equals(k)) nbv++;
                        cont.setVar(Contexte.ENV_RET_VALUE, val);
                     }
                  }
               }
               if (!local && !link) { // Variables globales
                  sort = new TreeSet<>(cont.getAllVars().keySet());
                  iter = sort.iterator();
                  while (iter.hasNext()) {
                     String key2 = iter.next();
                     if (cont.isNonSys(key2) && ptnb.matcher(key2).matches()) {
                        val = cont.getGlbVar(key2);
                        sb.append(SQLCnx.frm(key2)).append(' ').append(val).append('\n');
                        nbv++;
                        if (!key.equals(key2)) nbv++;
                        cont.setVar(Contexte.ENV_RET_VALUE, val);
                     }
                  }
               }
               // Erreur si non définie
               if (val == null)
                  return cont.erreur("DEF", "variable '" + key + "' non définie", lng);
               if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
               if (nbv > 1) vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
               vr.setSubValue(val);
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
            } catch (PatternSyntaxException ex) {
               return cont.erreur("DEF", "syntaxe regexp incorrecte : " + ex.getMessage(), lng);
            }
         } else {
            // Affectation en variable
            StringBuilder sb = new StringBuilder();
            String[] tkeys = ((String) lc.get(0)).split(",");
            String val = "", help = null;
            boolean append = options.has("a"), docirc = options.has("r");
            if (options.has("h")) help = (String) options.valueOf("h");
            for (int i = 0; i < tkeys.length; i++) { // pour chaque variable de la liste
               val = (1 + i >= lc.size() ? "" :
                     (i < tkeys.length - 1 ? (String) lc.get(1 + i) : listToString(lc, 1 + i)));
               if (cont.valideKey(tkeys[i]) || cont.isLec(tkeys[i])) {
                  if (append) {
                     String val0 = cont.getVar(tkeys[i]);
                     if (val0 != null) val = val0 + val;
                  }
                  if (link) cont.setLinkVar(tkeys[i], (Integer) options.valueOf("u"));
                  if (local || link) cont.setLecVar(tkeys[i], val);
                  else {
                     cont.setVar2(tkeys[i], val, docirc);
                     if (help != null) cont.addVarHelp(tkeys[i], help);
                  }
                  sb.append("-> variable fixée : ").append(tkeys[i])
                        .append(help != null ? " (avec aide)" : "").append('\n');
               } else return cont.erreur("DEF", "affectation de variable invalide : " +
                     (tkeys[i].isEmpty() ? "(chaîne vide)" : "'" + tkeys[i] + "'"), lng);
            }
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);

            vr.setDispValue(sb.toString(), Contexte.VERB_BVR);
            vr.setSubValue(val);
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
            // Mise à jour du complètement
            if (options.has("c")) cont.setCompletors();
         }

         if (toret) { // si retour autorisé
            vr.setRet();
            cont.setValeur(vr);
         }
         return RET_CONTINUE;
      }
      catch (OptionException ex) {
         return cont.exception("PRINT", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex){
         return cont.exception("PRINT", "ERREUR IOException : " + ex.getMessage() , lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  def, :      Affecte une valeur/fonction à une variable de console\n";
   }
}// class

