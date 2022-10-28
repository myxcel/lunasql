package lunasql.val;

import java.util.HashMap;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

/**
 * Description d'un moteur d'évaluation
 *
 * @author micael p.
 */
public final class EvalEngine {
   private static ScriptEngineManager manager;
   private List<String> names;
   private ScriptEngine engine;

   /**
    * Crée un nouveau moteur sur son nom (ou alias), depuis ScriptEngineFactory
    *
    * @param name le nom à rechercher
    * @return nouvel engine
    */
   public static EvalEngine createEngine(String name) {
      if (name == null) throw new IllegalArgumentException("Nom de moteur null");
      initManager();
      for (ScriptEngineFactory fac : manager.getEngineFactories()) {
         List<String> facnames = fac.getNames();
         if (facnames.contains(name)) return new EvalEngine(facnames, fac.getScriptEngine());
      }
      return null;
   }

   /**
    * Crée tous les moteurs disponibles, depuis ScriptEngineFactory
    *
    * @return liste des engines
    */
   public static HashMap<Integer, EvalEngine> createAllEngines() {
      HashMap<Integer, EvalEngine> map = new HashMap<>();
      initManager();
      for (ScriptEngineFactory fac : manager.getEngineFactories()) {
         map.put(map.size() + 1, new EvalEngine(fac.getNames(), fac.getScriptEngine()));
      }
      return map;
   }

   public EvalEngine(List<String> names, ScriptEngine engine) {
      initManager();
      setNames(names);
      setEngine(engine);
   }

   public final List<String> getNames() {
      return this.names;
   }

   public final ScriptEngine getEngine() {
      return this.engine;
   }

   public final void setNames(List<String> names) {
      this.names = names;
   }

   public final void setEngine(ScriptEngine engine) {
      this.engine = engine;
   }

   private static void initManager() {
      if (manager == null) manager = new ScriptEngineManager();
   }
}
