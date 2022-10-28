package lunasql.cmd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.script.ScriptException;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande USE <br>
 * (Interne) Chargement d'une bibliothèque en environnement
 * @author M.P.
 */
public class CmdUse extends CmdExec {

   private final OptionParser parser;

   public CmdUse(Contexte cont){
      super(cont, "USE", "|");
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("j", "invoque une classe d'un fichier jar").withRequiredArg().ofType(File.class)
         .describedAs("jar");
      parser.accepts("m", "invoque une méthode").withRequiredArg().ofType(String.class)
         .describedAs("method");
      parser.accepts("i", "fixe le mode d'appel").withRequiredArg().ofType(String.class)
         .describedAs("new|static");
      parser.accepts("c", "types requis pour le constructeur si new").withRequiredArg().ofType(String.class)
         .describedAs("type1,type2").withValuesSeparatedBy(',');
      parser.accepts("r", "arguments pour le constructeur si new").withRequiredArg().ofType(String.class)
         .describedAs("arg1,arg2").withValuesSeparatedBy(',');
      parser.accepts("a", "arguments pour l'option m").withRequiredArg().ofType(String.class)
         .describedAs("arg1,arg2").withValuesSeparatedBy(',');
      parser.accepts("t", "types requis pour l'option m").withRequiredArg().ofType(String.class)
         .describedAs("type1,type2").withValuesSeparatedBy(',');
      parser.nonOptions("bibliothèques").ofType(String.class);
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
         int ret = RET_CONTINUE;
         Valeur vr = new ValeurDef(cont);
         List<?> lb = options.nonOptionArguments();
         if (options.has("i")) { // chargement d'une classe Java
            if (lb.size() != 1)
               return cont.erreur("USE", "un (et un seul) nom de classe Java est requis", lng);

            File jar = null;
            if (options.has("j")) {
               jar = (File)options.valueOf("j");
               if(!jar.canRead())
                  return cont.erreur("USE", "le fichier '" + jar.getName() + "' n'est pas accessible", lng);
            }
            String c = (String) lb.get(0);
            try {
               // Chargement de la classe
               Class<?> cl;
               if (jar == null) cl = Class.forName(c);
               else {
                  ClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()});
                  cl = loader.loadClass(c);
               }

               String mode = (String) options.valueOf("i");
               Object obj = null;
               if (mode.equals("new")) {
                  List<?> args, types;
                  if (options.has("r") && options.has("c")) { // constructeur avec signature non vide
                     args = options.valuesOf("r");
                     types = options.valuesOf("c");
                     if (types.size() != args.size())
                        return cont.erreur("USE", "arguments et types incohérents pour new", lng);
                  }
                  else if (options.has("r") || options.has("c")) {
                     return cont.erreur("USE", "les options -c et -r sont interdépendantes", lng);
                  }
                  else { // constructeur sans arguments
                     args = new ArrayList<String>();
                     types = new ArrayList<String>();
                  }
                  Constructor<?> ctr = cl.getConstructor(getTypes(types));
                  obj = ctr.newInstance(args.isEmpty() ? null : args.toArray());
               }

               if (options.has("m")) {// méthode demandée
                  List<?> args, types;
                  if (options.has("a") && options.has("t")) { // méthodes avec argument
                     args = options.valuesOf("a");
                     types = options.valuesOf("t");
                     if (types.size() != args.size())
                        return cont.erreur("USE", "arguments et types incohérents pour l'option m", lng);
                  }
                  else if (options.has("t") || options.has("a")) {
                     return cont.erreur("USE", "les options -t et -a sont interdépendantes", lng);
                  }
                  else { // méthode sans argument
                     args = new ArrayList<String>();
                     types = new ArrayList<String>();
                  }
                  // Chargement de la méthode
                  Method met = cl.getDeclaredMethod((String) options.valueOf("m"), getTypes(types));
                  obj = met.invoke(obj, args.isEmpty() ? null : args.toArray());
                  vr.setSubValue(String.valueOf(obj));
               }
               else { // pas de méthode : retourne la classe
                  vr.setDispValue("classe chargée : " + c, Contexte.VERB_BVR);
                  vr.setSubValue(String.valueOf(cl));
               }
               cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
            }
            catch (MalformedURLException ex){
               return cont.exception("USE", "ERREUR MalformedURLException : " + ex.getMessage(), lng, ex);
            }
            catch (ClassNotFoundException ex) {// erreur prévisible > cont.erreur
               return cont.erreur("USE", "classe '" + ex.getMessage() + "' inaccessible dans le " +
                     (jar == null ? "CLASSPATH" : "fichier JAR " + jar.getName()), lng);
            }
            catch (NullPointerException ex){
               return cont.erreur("USE", "méthode appelée sur un objet null ou déclaré static", lng);
            }
            catch (Exception ex) {// toute autre chose
               return cont.exception("USE", "ERREUR Reflexion " + ex.getClass().getSimpleName() +
                     " : " + ex.getMessage(), lng, ex);
            }
         }
         else { // chargement d'une bibliothèque LunaSQL normale
            if (lb.isEmpty())
               return cont.erreur("USE", "un nom de bibliothèque au moins est requis", lng);

            Lecteur lec = null;
            for (Object o : lb) { // pour chaque lib
               String l = (String) o;
               if (l.indexOf('.') < 0) l += ".sql";

               // La lib est-elle déjà chargée ?
               String llib = cont.getVar(Contexte.ENV_LOADED_LIBS);
               String[] tllib = llib.split(File.pathSeparator);
               if (Arrays.asList(tllib).contains(l)) {
                  if (cont.getVerbose() >= Contexte.VERB_BVR)
                     cont.println("Bibliothèque '" + l + "' déjà chargée");
                  cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
                  vr.setSubValue("0");
                  continue;    // sortie
               }

               // Mise-à-jour de la variable de liste des bibliothèques chargées
               cont.setVar(Contexte.ENV_LOADED_LIBS,
                     (llib.length() == 0 ? "" : llib + File.pathSeparator) + l);
                              // Chargement en ressource stream : 1) /lunasql/misc/ 2) /lunasql/jsextras/
               if (cont.getVerbose() >= Contexte.VERB_BVR)
                  cont.println("Bibliothèque '" + l + "' en chargement");

               InputStream is = getClass().getResourceAsStream("/lunasql/misc/" + l);
               if (is == null) is = getClass().getResourceAsStream("/lunasql/jsextras/" + l);
               // Si absente du CLASSPATH > passage à la commande EXEC
               if (is == null) {
                  super.execute();
                  Valeur vr2 = cont.getValeur();
                  vr.setSubValue(vr2 == null ? null : vr2.getSubValue());
               }
               else {  // présente > lecteur en archive
                  HashMap<String, String> vars = new HashMap<>();
                  vars.put(Contexte.LEC_THIS, l);
                  vars.put(Contexte.LEC_SUPER, "");
                  vars.put(Contexte.LEC_SCR_NAME, l);
                  long tm = System.currentTimeMillis();
                  try (Reader read = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                     if (l.endsWith(".sql")) lec = new Lecteur(cont, read, vars);
                     else cont.getEvalEngine().eval(read);
                     tm = System.currentTimeMillis() - tm;
                     vr.setDispValue("Exécuté en : " + SQLCnx.frmDur(tm), Contexte.VERB_BVR);
                     Valeur vr2 = cont.getValeur();
                     vr.setSubValue(vr2 == null ? null : vr2.getSubValue());
                  }
                  catch (IOException ex) {
                     ret = cont.erreur("USE", "IOException : " + ex.getMessage(), lng);
                  }
               }
            }// for
            if (lec != null) ret = lec.getCmdState();
         }

         vr.setRet();
         cont.setValeur(vr);
         return ret;
      }
      catch (ScriptException ex){
         return cont.erreur("USE", "erreur lecture script SE : " + ex.getMessage() , lng);
      }
      catch (OptionException ex) {
         return cont.exception("USE", "erreur d'option : " + ex.getMessage(), lng, ex);
      }
      catch (IOException ex) {
         return cont.exception("USE", "ERREUR IOException : " + ex.getMessage(), lng, ex);
      }
   }

   private Class<?>[] getTypes(List<?> types) throws ClassNotFoundException {
      Class<?>[] prmcl = new Class[types.size()];
      for (int i=0; i<prmcl.length; i++) prmcl[i] = Class.forName((String)types.get(i));
      return prmcl;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  use, |      Charge en environnement une bibliothèque\n";
   }
}// class
