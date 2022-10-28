package lunasql.cmd;

import lunasql.lib.Contexte;

import java.util.ConcurrentModificationException;

/**
 * Commande START <br>
 * (Interne) Exécution d'un fichier de commandes Etend CmdExec pour en hériter
 * les fonctions d'exécution
 * @author M.P.
 */
public class CmdStart extends CmdExec {

   public int retValue;

   public CmdStart(Contexte cont) {
      super(cont, "START", "@@");
   }

   @Override
   public int execute() {
      try {
         //Contexte localctx = new Contexte(cont); // TODO: exécuter START en new Contexte : constructeur copie
         (new Thread(() -> retValue = CmdStart.super.execute())).start();
      }
      catch (ConcurrentModificationException ex) {
         // On vous avait bien prévenu que START était risquée...
         retValue = cont.exception("START", "ERREUR ConcurrentModificationException : " + ex.getMessage()
               + "\n\tSachez ce que vous faites quand vous jouez avec la concurrence !", lng, ex);
      //} catch (IOException ex) {
      //   retValue = cont.exception("START", "ERREUR IOException : " + ex.getMessage(), lng, ex);
      }
      return retValue;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  start, @@   Comme EXEC, mais l'exécution est faite en tâche de fond\n";
   }
}// class

