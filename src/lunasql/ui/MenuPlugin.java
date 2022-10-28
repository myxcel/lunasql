package lunasql.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import lunasql.Config;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;

/**
 * Classe matérialisant un plugin appelable par un MenuItem
 * @author M.P.
 * @version 1.2.2
 * 
 * Utilisation :<br />
 * <code>
 * String path = "/home/mic/dev/scripts"<br />
 * Map objs = new HashMap<String, Object>(); objs.put("var1", obj1); objs.put("var2", obj2);
 * // objs ne sera disponible que pour les ScriptEngines
 * List plugins = MenuPlugin.createMenuPlugins(Fenetre.this, contexte, path, objs);<br />
 *  for (int i = 0; i &lt; plugins.size(); i++) {<br />
 *     MenuPlugin mp = (MenuPlugin)plugins.get(i);<br />
 *     mPlugin.add(mp.getMenuItem());<br />
 *  }
 * </code>
 */
public class MenuPlugin {

   private final JFrame parent;
   private final JMenuItem menu;
   private Thread thread;
   private final ScriptEngine engine;
   private String engname;
   private final String filename;
   private final Contexte cont;

   /**
    * Constructeur MenuPlugin
    * @param frame la fenêtre mère
    * @param file le nom du fichier à exécuter
    * @throws IllegalArgumentException si parent ou nom de fichier vide
    */
   public MenuPlugin(JFrame frame, String file, Map<String, Object> map, String[] func, Contexte ct)
         throws IllegalArgumentException {
      if (frame == null) throw new IllegalArgumentException("conteneur parent nul");
      if (file == null || file.length() == 0) 
         throw new IllegalArgumentException("Nom du fichier script vide, ou fichier inaccessible");

      parent = frame;
      cont = ct;

      int ipth = file.lastIndexOf(File.separator), iext = file.lastIndexOf('.');
      filename = (ipth >= 0 && iext < file.length() - 1) ? file.substring(ipth + 1) : file;
      String extens = (iext >= 0 && iext < file.length()) ? file.substring(iext + 1) : "";
      boolean issql = extens.matches(Config.CT_SQL_EXT);
      engine = issql ? null : new ScriptEngineManager().getEngineByExtension(extens);
      menu = new JMenuItem(filename);
      menu.setBackground(Color.WHITE);
      if (issql) {
         menu.setToolTipText("<html><u>" + filename + "</u> (fichier LunaSQL)</html>");
         menu.addActionListener(new ActionScriptEngine(true, file));
      }
      else if (engine == null) {
         menu.setEnabled(false);
         menu.setToolTipText("<html><u>" + filename + "</u> (Moteur de script indisponible : "
                 + "<font color='red'>" + extens + "</font>)</html>");
      }
      else {
         if (map != null)
            for (String k : map.keySet()) engine.put(k, map.get(k));
         if (func != null)
            for (String f : func) try { engine.eval(f); } catch(ScriptException ex) {}
         engname = engine.getFactory().getEngineName();
         menu.setToolTipText("<html><u>" + filename + "</u> (fichier " + engname + ")</html>");
         menu.addActionListener(new ActionScriptEngine(false, file));
      }
   }

   /**
    * Accesseur
    * @return Thread thread d'exécution
    */
   public JMenuItem getMenuItem() {
      return menu;
   }

   /**
    * Création des plugins<br />
    * @param frame la fenêtre mère
    * @param path le chemin du dossier des plugins
    * @param map les objets à rendre disponible depuis le script
    * @return le tableau de MenuPlugins
    */
   public static ArrayList<MenuPlugin> createMenuPlugins(JFrame frame, String path,
           Map<String, Object> map, String[] func, Contexte ct) {
      if (path == null) throw new IllegalArgumentException("Liste des chemins nulle");
      String[] files;
      ArrayList<MenuPlugin> plugins = new ArrayList<>();
      String[] sdirs = path.split(File.pathSeparator);

      for (String sdir : sdirs) {
         File dir = new File(sdir);
         if (dir.exists() && dir.isDirectory()) {
            files = dir.list();
            if (files == null)
               throw new IllegalArgumentException("Pas un répertoire !");
            for (String filepath : files) {
               filepath = sdir + File.separator + filepath;
               if (new File(filepath).isFile());
                  plugins.add(new MenuPlugin(frame, filepath, map, func, ct));
            }
         }
      }
      return plugins;
   }

   /**
    * ActionListener ActionScriptEngine
    */
   class ActionScriptEngine implements ActionListener {

      String sname;
      boolean issql;

      private ActionScriptEngine(boolean sql, String sname) {
         this.issql = sql;
         this.sname = sname;
      }

      public void actionPerformed(ActionEvent evt) {
         final Thread task = new Thread(() -> {
            // Arguments et retour de valeur
            Object oret = null;
            String arg = JOptionPane.showInputDialog(parent,
                  "Saisissez les arguments d'exécution éventuels\npuis cliquez sur OK pour confirmer",
                  "Exécution de : " + sname, JOptionPane.QUESTION_MESSAGE);
            if (arg == null) return;

            // Modification affichage
            System.out.println("Exécution du script '" + sname + "'");
            menu.setForeground(Color.red);
            menu.setToolTipText(sname + " (en cours d'exécution, cliquer pour interrompre)");
            menu.removeActionListener(menu.getActionListeners()[0]);
            menu.addActionListener(new ActionStop(sname));

            long tm = System.currentTimeMillis();
            try {
               if (issql) new Lecteur(cont, "EXEC \"" + sname + "\" "+ arg +";");
               else {
                  engine.put("script_name", sname);
                  engine.put("script_args", Arrays.asList(arg.split(" ")));
                  FileReader fr = new FileReader(sname);
                  oret = engine.eval(fr);
                  fr.close();
               }
            }
            catch (ScriptException e) {
               System.err.println(sname + " : Erreur d'exécution :\n" + e.getMessage());
               Tools.textMessage(parent,
                     "Erreur d'exécution :\n" + e.getMessage(),
                     "Exécution de : " + sname, JOptionPane.ERROR_MESSAGE);
            }
            catch (IOException e) {
               System.err.println(sname + " : IOException :\n" + e.getMessage());
               Tools.textMessage(parent,
                     "IOException :\n" + e.getMessage(),
                     "Exécution de : " + sname, JOptionPane.ERROR_MESSAGE);
            }
            tm = System.currentTimeMillis() - tm;

            // Réinitialisation de l'affichage
            menu.setForeground(Color.black);
            menu.setToolTipText("<html><u>" + filename + "</u> (fichier " + engname + ")</html>");
            menu.removeActionListener(menu.getActionListeners()[0]);
            menu.addActionListener(new ActionScriptEngine(issql, sname));
            thread = null;

            // Retour d'objet
            String ftm = SQLCnx.frmDur(tm);
            System.out.println("-> Exécuté en " + ftm);
            if (oret != null) JOptionPane.showMessageDialog(parent, "Durée d'éxécution : " + ftm
                  + (oret instanceof Integer ? "\nScript code returné : " + oret
                       : "\nScript objet returné :\n" + oret.toString()),
                  "Exécution de : " + sname, JOptionPane.INFORMATION_MESSAGE);
         });
         thread = task;
         task.start();
         final Thread check = new Thread(() -> {
            while (true) {
              try { Thread.sleep(60000L); }
              catch (InterruptedException ex) {}
              if (! task.isAlive()) break;
              if (JOptionPane.showConfirmDialog(parent,
                 "L'exécution est bien longue... Voulez-vous l'interrompre ?", "Exécution script",
                 JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) task.interrupt();
            }
         });
         check.start();
      }
   }// class ActionScriptEngine

   /**
    * ActionListener ActionStop
    */
   class ActionStop implements ActionListener {

      String sname;

      private ActionStop(String sname) {
         this.sname = sname;
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         if (JOptionPane.showConfirmDialog(parent,
                 "Confirmez-vous l'interruption de l'exécution du script\n'" + sname + "' ?",
                 "Interruption de script", JOptionPane.YES_NO_OPTION,
                 JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {

            if (thread != null && thread.isAlive()) {
               thread.interrupt();
               thread = null;
               System.err.println(sname + " : interrompu");
               JOptionPane.showMessageDialog(parent,
                       "L'exécution du script '" + sname + "' a été interrompue par l'utilisateur",
                       "Exécution de : " + sname, JOptionPane.WARNING_MESSAGE);
            }
            else {
               System.err.println(sname + " : interrompu");
               JOptionPane.showMessageDialog(parent,
                       "L'exécution du script '" + sname + "' est déjà terminée",
                       "Exécution de : " + sname, JOptionPane.WARNING_MESSAGE);
            }
         }
      }
   }// class ActionStop
}
