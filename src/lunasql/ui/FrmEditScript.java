package lunasql.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import lunasql.cmd.CmdTree;

import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Security;
import lunasql.lib.Tools;
import lunasql.cmd.Instruction;
import lunasql.ui.undo.UndoMapping;
import lunasql.ui.undo.UndoableJTextManager;
import lunasql.val.Valeur;

/**
 * FrmEditScript.java
 *
 * @author M.P
 * Created on 18 oct. 2011, 11:04:00
 */
public class FrmEditScript extends javax.swing.JFrame {

   private static final long serialVersionUID = -8193614549582676146L;

   private static final String[][] CONTENT_TYPES = {
      {"bash", "sh"},
      {"c"},
      {"clojure","clj"},
      {"cpp"},
      {"dosbatch", "bat"},
      {"groovy"},
      {"java", "bsh"},
      {"javascript", "js"},
      {"flex"},
      {"lua"},
      {"lunasql", "lun"},
      {"plain", "txt"},
      {"properties"},
      {"python", "py"},
      {"ruby", "rb"},
      {"scala"},
      {"sql", "lsql", "luna"},
      {"tal"},
      {"xhtml", "html", "htm"},
      {"xml"},
      {"xpath"}
   };
   private final Properties complWords = new Properties();

   public static String[] getSyntaxes(){
      String[] tsyn = new String[CONTENT_TYPES.length];
      for (int i=0; i<tsyn.length; i++) tsyn[i] = CONTENT_TYPES[i][0];
      return tsyn;
   }

   /**
    * Constructeur
    *
    * @param cont le contexte
    * @param uniq si la fenêtre n'est pas dépendante de la console
    * @param type le type de coloration
    */
   public FrmEditScript(Contexte cont, boolean uniq, String type) {
      if (cont == null) throw new IllegalArgumentException("Contexte null");
      initComponents();
      this.uniq = uniq;
      this.cont = cont;
      this.path = cont.getVar(Contexte.ENV_SCR_PATH);
      if (this.path.isEmpty()) this.path = cont.getVar(Contexte.ENV_WORK_DIR);
      else this.path = this.path.split(File.pathSeparator)[0];
      setDefaultCloseOperation(uniq ? EXIT_ON_CLOSE : DISPOSE_ON_CLOSE);
      setSyntax();
      setEngine(type == null ? "sql" : type);

      // Tableau de complètement
      try {
         complWords.load(getClass().getResourceAsStream("/lunasql/doc/complwords.txt"));
      } catch (IOException ex) {}

      // Undo Redo
      UndoMapping unre = UndoableJTextManager.getUndoableJTextManager().makesUndoable(txeditor);
      miUndo.addActionListener(unre.getUndoAction());
      miRedo.addActionListener(unre.getRedoAction());

      // Création des Plugins
      Map<String, Object> objs = new HashMap<>();
      String[] funcs = new String[]{
            // "function test(){return 'test';}"
            // ... fonctions diverses
      };
      objs.put("sql_connex", cont.getConnex()); // Palette disponible pour les scripts
      ArrayList<MenuPlugin> plugins = MenuPlugin.createMenuPlugins(this, this.path, objs, funcs, this.cont);
      for (MenuPlugin mp : plugins) mPlugin.add(mp.getMenuItem());

      // Initialisation des listes
      majTables();
      majVars();
   }

   public FrmEditScript(Contexte cont, boolean uniq) {
      this(cont, uniq, "sql");
   }

   public FrmEditScript(Contexte cont) {
      this(cont, false, "sql");
   }

   public void openFile(String f){
      if(f == null || f.length() == 0) throw new IllegalArgumentException("Fichier null");
      file = new File(f);
      openFile();
   }

   public void openFile(File f){
      if(f == null) throw new IllegalArgumentException("Fichier null");
      file = f;
      openFile();
   }

   /**
    * Application de la syntaxe (ContentType) à l'éditeur
    */
   private void setSyntax() {
      if (engine == null) return;
      String e = null;
      OUT: for (String[] ttp : CONTENT_TYPES)
         for (String tp : ttp)
            if (engine.equalsIgnoreCase(tp)) {
               e = ttp[0];
               break OUT;
            }
      txeditor.setContentType("text/" + (e == null ? engine : e));
      txeditor.getDocument().putProperty(PlainDocument.tabSizeAttribute, 2);
   }

   /**
    * Application de l'engine
    */
   private void setEngine(String e) {
      engine = e;
      mCode.setEnabled(engine != null &&
            (engine.equals("sql") || engine.equals("lsql") || engine.equals("luna")));
   }

   /**
    * Mise-à-jour de la liste des tables
    */
   private void majTables() {
      SwingUtilities.invokeLater(() -> {
         lstTables.removeAll();
         List<String> lt = new ArrayList<>();
         try {
            DatabaseMetaData dMeta = cont.getConnex().getMetaData();
            ResultSet result = dMeta.getTables(null, null, null, new String[] {
               "TABLE", "ALIAS", "SYNONYM", "VIEW"});
            while(result.next()) lt.add(result.getString(3));
            result.close();
         }
         catch (SQLException ex) {
            cont.errprintln("Erreur IOException :\n" + ex.getMessage());
            Tools.textMessage(FrmEditScript.this, "SQLException :\n" + ex.getMessage(),
                  "Liste de tables", MSGERROR);
         }
         Collections.sort(lt);
         lstTables.setListData(lt.toArray(new String[]{}));
      });
   }

   /**
    * Mise-à-jour de la liste des variables glogales
    */
   private void majVars() {
      SwingUtilities.invokeLater(() -> {
         lstVars.removeAll();
         boolean sys = cbSysVars.isSelected();
         Map<String, String> vars = cont.getAllVars();
         List<String> lv = new ArrayList<>();
         for (String k : vars.keySet()) {
            if (sys || cont.isNonSys(k)) lv.add(k);
         }
         Collections.sort(lv);
         lstVars.setListData(lv.toArray(new String[]{}));
      });
   }

   /**
    * Sauvegarde du fichier
    */
   private void saveFile() {
      if (saved) return;

      boolean tosave = false;
      if (file == null) {
         JFileChooser fc = new JFileChooser();
         fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
         fc.setCurrentDirectory(new File(this.path));
         if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
         file = fc.getSelectedFile();
         setEngine(getEngine());
         if (!file.exists() || JOptionPane.showConfirmDialog(FrmEditScript.this,
                 "Le fichier '" + file.getName() + "' existe déjà. L'écraser ?", "Enregistrement",
                 JOptionPane.YES_NO_OPTION, MSGQUEST) == REPOUI) tosave = true;
      } else tosave = true;

      // Le fichier doit être sauvé
      if (tosave){
         try {
            FileWriter writer = new FileWriter(file);
            writer.write(txeditor.getText());
            writer.close();
            saved = true;
         }
         catch (IOException e) {
            cont.errprintln("Erreur IOException :\n" + e.getMessage());
            Tools.textMessage(this, "IOException :\n" + e.getMessage(),
                  "Enregistrement de : " + file.getName(), MSGERROR);
         }
      }
   }

   /**
    * Obtient le nom de l'engine
    * @return le nom
    */
   private String getEngine() {
      String sname = (file == null ? "(non enregistré)" : file.getName());
      int iext = sname.lastIndexOf('.');
      return (iext >= 0 && iext < sname.length() - 1) ? sname.substring(iext + 1) : sname;
   }

   /**
    * Sauvegarde du fichier
    */
   private void openFile() {
      if (file == null) return;
      try {
         // Extension
         int i = file.getName().lastIndexOf('.');
         setEngine(i > 0 ? file.getName().substring(i + 1) : "");
         // Ouverture
         BufferedReader reader = new BufferedReader(new FileReader(file));
         StringBuilder texte = new StringBuilder();
         String line;
         while ((line = reader.readLine()) != null) texte.append(line).append('\n');
         reader.close();
         setSyntax();
         txeditor.setText(texte.toString());
         saved = true;
      }
      catch (IOException e) {
         cont.errprintln("Erreur IOException :\n" + e.getMessage());
         JOptionPane.showMessageDialog(this, "IOException :\n" + e.getMessage(),
                 "Enregistrement de : " + file.getName(), MSGERROR);
      }
   }

   /**
    * Numéro de lignes
    */
   private void MAJNoLines() {
      StringBuilder s = new StringBuilder("<html>");
      String txt = txeditor.getText(), seltxt = txeditor.getSelectedText();
      int curpos = txeditor.getCaretPosition(), sellen = seltxt == null ? 0 : seltxt.length();
      if (sellen == 0) s.append("<b>Curseur</b> : ").append(curpos);
      else {
         s.append("<b>Sélection</b> : de ").append(curpos - sellen).append(" à ").append(curpos);
         s.append(" (").append(sellen).append(" car.)");
      }

      int txtl = txt.length(), nbl = txt.split("\\n").length;
      s.append(" - <b>Total</b> : ").append(txtl).append(" caractère").append(txtl > 1 ? "s" : "").append(", ");
      s.append(nbl).append(" ligne").append(nbl > 1 ? "s" : "").append("</html>");
      lbLines.setText(s.toString());
   }

   /**
    * Insertion de texte en zone de code
    * @param txt le texte
    */
   private void insertString(String txt) {
      javax.swing.text.Document doc = txeditor.getDocument();
      try {
         doc.insertString(txeditor.getSelectionEnd(), txt, null);
      }
      catch (BadLocationException e) {
         cont.errprintln("Erreur BadLocationException :\n" + e.getMessage());
      }
   }

   /**
    * Inspection d'une variable
    *
    * @param key son nom
    */
   private void inspectVar(String key) {
      if (!cont.valideSysKey(key)) {
         JOptionPane.showMessageDialog(this, "Sélection de variable invalide", "Inspection", MSGWARN);
         return;
      }
      String val = cont.getGlbVar(key);
      if (val == null) {
         JOptionPane.showMessageDialog(this,
               "Variable globale '" + key + "' non définie", "Contenu de " + key + " : ", MSGINFO);
      } else {
         Tools.codeMessage(FrmEditScript.this, val, "Contenu de " + key + " : ");
      }
   }

   /**
    * Recherche de la documentation pour une commande ou macro
    *
    * @param key le nom de la commande
    */
   private void showVarHelp(String key) {
      if (!cont.valideSysKey(key)) {
         JOptionPane.showMessageDialog(this, "Sélection de variable invalide", "Documentation", MSGWARN);
         return;
      }
      String val = cont.getVarHelp(key);
      if (val == null) {
        Instruction cmd = cont.getCommand(key.toUpperCase());
        if (cmd != null) val = cmd.getHelp();
      }
      if (val == null) {
         JOptionPane.showMessageDialog(FrmEditScript.this,
               "Aucune aide disponible pour '" + key + "'", "Documentation de " + key + " : ", MSGINFO);
      } else {
         Tools.textMessage(FrmEditScript.this, val, "Documentation de " + key + " : ");
      }
   }

   /**
    * Signature d'un script
    */
   private String signScript(String content) throws IOException, NoSuchAlgorithmException, InvalidKeyException,
         SignatureException, InvalidKeySpecException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {
      String[] tkey = cont.getVar(Contexte.ENV_SIGN_KEY).split("\\|");
      if (tkey.length != 2) {
         Tools.warningMessage(FrmEditScript.this,
               "Aucune clef privée n'est définie en  " + Contexte.ENV_SIGN_KEY, "Erreur de signature");
         return null;
      }

      String psw = Tools.passwordInput(FrmEditScript.this,"Mot de passe de la clef : ");
      if (psw == null) return null;

      Security sec = new Security();
      sec.setSecretString(psw);
      byte[] pk = Security.b64decode(tkey[0]), ms = Security.getTimeMs();
      sec.setPublicKey(pk);
      sec.setPrivateKey(sec.decrypt(tkey[1]));

      return content + Security.SIG_EMBED_BEGIN +
            Security.getSignatureStr(pk, ms, sec.sign(content, ms)) + Security.SIG_EMBED_END;
   }

   /**
    * Vérification de la signature du script
    */
   private void verifySign(String content) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
      Security sec = new Security();
      String[] pkarr = new String[2];
      if (!sec.verifyPk(content.getBytes(StandardCharsets.UTF_8), pkarr)) {
         Tools.errorMessage(FrmEditScript.this, "Signature numérique erronée", "Mauvaise signature");
         return;
      }

      String ms, pk;
      pk = pkarr[0];
      try {
         ms = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(Long.parseLong(pkarr[1],16)));
      } catch (NumberFormatException ex) {
         ms = "(date invalide)";
      }

      // Confiance dans la clef
      String[] tkey = cont.getVar(Contexte.ENV_SIGN_KEY).split("\\|");
      String sure = null;

      if (tkey.length == 2 && tkey[0].equals(pk)) sure = "personnelle";
      else if (cont.getVar(Contexte.ENV_SIGN_TRUST).contains(pk)) sure = "de confiance";
      if (sure == null) {
         Tools.warningMessage(FrmEditScript.this,
               "Signature numérique non fiable\nSigné le " + ms + " par clef inconnue " + pk.substring(0, 10),
               "Signature non fiable");
      } else {
         Tools.infoMessage(FrmEditScript.this,
               "Signature numérique valide\nSigné le " + ms + " par clef " + sure + " " + pk.substring(0, 10),
               "Signature valide");
      }
   }

   /**
    * This method is called from within the constructor to initialize the form.
    */
   private void initComponents() {
       try {
          Class.forName("jsyntaxpane.DefaultSyntaxKit");
          jsyntaxpane.DefaultSyntaxKit.initKit();
       } catch (ClassNotFoundException ex) {/* pas d'erreur */}

      JPanel pnGlobal = new JPanel();
      txeditor = new JEditorPane();
      JScrollPane scrPane = new JScrollPane(txeditor);
      JPanel pnEtat = new JPanel();
      lbLines = new JLabel();
      JMenuBar menuBar = new JMenuBar();
      JMenu mFile = new JMenu();
      JMenuItem miOpen = new JMenuItem();
      JMenuItem miSave = new JMenuItem();
      JMenuItem miClose = new JMenuItem();
      JMenu mEdit = new JMenu();
      miUndo = new JMenuItem();
      miRedo = new JMenuItem();
      JMenuItem miCopy = new JMenuItem();
      JMenuItem miCut = new JMenuItem();
      JMenuItem miPaste = new JMenuItem();
      JMenuItem miSelAll = new JMenuItem();
      JMenu mScript = new JMenu();
      JMenuItem miSyntax = new JMenuItem();
      JMenuItem miEngine = new JMenuItem();
      JMenuItem miRun = new JMenuItem();
      JMenuItem miSign = new JMenuItem();
      JMenuItem miVerif = new JMenuItem();
      mCode = new JMenu();
      JMenuItem miCompl = new JMenuItem();
      JMenuItem miEval = new JMenuItem();
      JMenuItem miInsp = new JMenuItem();
      JMenuItem miMDoc = new JMenuItem();
      JMenuItem miSDoc = new JMenuItem();
      mPlugin = new JMenu();
      lstTables = new JList<>();
      lstVars = new JList<>();
      cbSysVars = new JCheckBox();
      JScrollPane scrTables = new JScrollPane(lstTables);
      JScrollPane scrVars = new JScrollPane(lstVars);
      JPanel pnVars = new JPanel();
      JSplitPane splVert = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrTables, pnVars);
      JSplitPane splHori = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splVert, scrPane);
      popTables = new JPopupMenu();
      popVars = new JPopupMenu();
      JMenu mTSQL = new JMenu();
      JMenu mTData = new JMenu();
      JMenuItem miTInsNom = new JMenuItem();
      JMenuItem miVInsNom = new JMenuItem();
      JMenuItem miVInsRef = new JMenuItem();
      JMenuItem miVInsVal = new JMenuItem();
      JMenuItem miVInsp = new JMenuItem();
      JMenuItem miVAjout = new JMenuItem();
      JMenuItem miVModif = new JMenuItem();
      JMenuItem miVDupli = new JMenuItem();
      JMenuItem miVMDoc = new JMenuItem();
      JMenuItem miVSupp = new JMenuItem();
      JMenuItem miTActu = new JMenuItem();
      JMenuItem miVActu = new JMenuItem();
      JMenuItem miTCount = new JMenuItem();
      JMenuItem miTCols = new JMenuItem();
      JMenuItem miTTree = new JMenuItem();
      JMenuItem miTSelect = new JMenuItem();
      JMenuItem miTInsert = new JMenuItem();
      JMenuItem miTUpdate = new JMenuItem();
      JMenuItem miTDelete = new JMenuItem();
      JMenuItem miTCreate = new JMenuItem();
      JMenuItem miTDrop = new JMenuItem();
      JMenuItem miTVider = new JMenuItem();
      JMenuItem miTImport = new JMenuItem();
      JMenuItem miTExport = new JMenuItem();
      JMenuItem miTVoir = new JMenuItem();
      mWait = new JMenu();

        setTitle("Édition d'un script");
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        pnGlobal.setBackground(Color.white);
        pnGlobal.setLayout(new java.awt.BorderLayout());

        scrPane.setBackground(Color.white);
        scrPane.setPreferredSize(new java.awt.Dimension(800, 500));
        txeditor.setContentType("text/lunasql");
        txeditor.setFont(new java.awt.Font("Courier New", Font.PLAIN, 12));
        txeditor.getDocument().putProperty(javax.swing.text.PlainDocument.tabSizeAttribute, 2);
        txeditor.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                txeditorMouseReleased(evt);
            }
        });
        txeditor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txeditorKeyReleased(evt);
            }
        });

        lstTables.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstVars.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstTables.addMouseListener(new java.awt.event.MouseAdapter() {
           @Override
           public void mouseReleased(java.awt.event.MouseEvent evt) {
              if (evt.getClickCount() == 2) miTInsNomActionPerformed(null);
              else lstTablesMouseReleased(evt);
           }
        });
        lstVars.addMouseListener(new java.awt.event.MouseAdapter() {
           @Override
           public void mouseReleased(java.awt.event.MouseEvent evt) {
              if (evt.getClickCount() == 2) miVInsNomActionPerformed(null);
              else lstVarsMouseReleased(evt);
           }
        });
        cbSysVars.setText("Var. système");
        cbSysVars.setBackground(Color.white);
        cbSysVars.addActionListener(this::cbSysVarsActionPerformed);
        pnVars.setLayout(new java.awt.BorderLayout());
        pnVars.add(scrVars, java.awt.BorderLayout.CENTER);
        pnVars.add(cbSysVars, java.awt.BorderLayout.SOUTH);

        java.awt.Dimension lstdim = new java.awt.Dimension(160, 200);
        scrTables.setPreferredSize(lstdim);
        scrVars.setPreferredSize(lstdim);

        pnGlobal.add(splHori, java.awt.BorderLayout.WEST);
        getContentPane().add(pnGlobal, java.awt.BorderLayout.CENTER);

        pnEtat.setBackground(Color.white);
        lbLines.setBackground(Color.white);
        lbLines.setText("0");
        pnEtat.add(lbLines);
        getContentPane().add(pnEtat, java.awt.BorderLayout.SOUTH);

        menuBar.setBackground(Color.white);
        mFile.setBackground(Color.white);
        mFile.setText("Fichier");
        miOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        miOpen.setBackground(Color.white);
        miOpen.setText("Ouvrir");
        miOpen.addActionListener(this::miOpenActionPerformed);
        mFile.add(miOpen);

        miSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        miSave.setBackground(Color.white);
        miSave.setText("Sauver");
        miSave.addActionListener(this::miSaveActionPerformed);
        mFile.add(miSave);

        miClose.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
        miClose.setBackground(Color.white);
        miClose.setText("Quitter");
        miClose.addActionListener(this::miCloseActionPerformed);
        mFile.add(miClose);

        menuBar.add(mFile);

        mEdit.setBackground(Color.white);
        mEdit.setText("Édition");

        miUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
        miUndo.setBackground(Color.white);
        miUndo.setText("Annuler");
        mEdit.add(miUndo);

        miRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
        miRedo.setBackground(Color.white);
        miRedo.setText("Rétablir");
        mEdit.add(miRedo);
        mEdit.add(new JPopupMenu.Separator());

        miCopy.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
        miCopy.setBackground(Color.white);
        miCopy.setText("Copier");
        miCopy.addActionListener(this::miCopyActionPerformed);
        mEdit.add(miCopy);

        miCut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
        miCut.setBackground(Color.white);
        miCut.setText("Couper");
        miCut.addActionListener(this::miCutActionPerformed);
        mEdit.add(miCut);

        miPaste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
        miPaste.setBackground(Color.white);
        miPaste.setText("Coller");
        miPaste.addActionListener(this::miPasteActionPerformed);
        mEdit.add(miPaste);

        miSelAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
        miSelAll.setBackground(Color.white);
        miSelAll.setText("Sélect.");
        miSelAll.addActionListener(this::miSelAllActionPerformed);
        mEdit.add(miSelAll);

        menuBar.add(mEdit);

        mScript.setBackground(Color.white);
        mScript.setText("Script");

        miSyntax.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK));
        miSyntax.setBackground(Color.white);
        miSyntax.setText("Syntaxe");
        miSyntax.addActionListener(this::miSyntaxActionPerformed);
        mScript.add(miSyntax);

        miEngine.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK));
        miEngine.setBackground(Color.white);
        miEngine.setText("Moteur");
        miEngine.addActionListener(this::miEngineActionPerformed);
        mScript.add(miEngine);

        miRun.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK));
        miRun.setBackground(Color.white);
        miRun.setText("Exécuter");
        miRun.addActionListener(this::miRunActionPerformed);
        mScript.add(miRun);

        miSign.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
        miSign.setBackground(Color.white);
        miSign.setText("Signer");
        miSign.addActionListener(this::miSignActionPerformed);
        mScript.add(miSign);

        miVerif.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
        miVerif.setBackground(Color.white);
        miVerif.setText("Vérifier");
        miVerif.addActionListener(this::miVerifActionPerformed);
        mScript.add(miVerif);

        menuBar.add(mScript);

        mCode.setBackground(Color.white);
        mCode.setText("Code");

        miCompl.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK));
        miCompl.setBackground(Color.white);
        miCompl.setText("Compléter");
        miCompl.addActionListener(this::miComplActionPerformed);
        mCode.add(miCompl);

        miEval.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK));
        miEval.setBackground(Color.white);
        miEval.setText("Évaluer");
        miEval.addActionListener(this::miEvalActionPerformed);
        mCode.add(miEval);

        miInsp.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
        miInsp.setBackground(Color.white);
        miInsp.setText("Inspecter");
        miInsp.addActionListener(this::miInspActionPerformed);
        mCode.add(miInsp);

        miMDoc.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
        miMDoc.setBackground(Color.white);
        miMDoc.setText("Voir doc");
        miMDoc.addActionListener(this::miMDocActionPerformed);
        mCode.add(miMDoc);

        miSDoc.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK));
        miSDoc.setBackground(Color.white);
        miSDoc.setText("Doc SQL");
        miSDoc.addActionListener(this::miSDocActionPerformed);
        mCode.add(miSDoc);

        menuBar.add(mCode);

        mPlugin.setBackground(Color.white);
        mPlugin.setText("Greffons");
        menuBar.add(mPlugin);

        setJMenuBar(menuBar);

        miTInsNom.setBackground(Color.white);
        miTInsNom.setText("Insérer nom");
        miTInsNom.addActionListener(this::miTInsNomActionPerformed);
        popTables.add(miTInsNom);

        miTCount.setBackground(Color.white);
        miTCount.setText("Nb lignes");
        miTCount.addActionListener(this::miTCountActionPerformed);
        popTables.add(miTCount);

        miTCols.setBackground(Color.white);
        miTCols.setText("Colonnes");
        miTCols.addActionListener(this::miTColsActionPerformed);
        popTables.add(miTCols);

        miTTree.setBackground(Color.white);
        miTTree.setText("Relations");
        miTTree.addActionListener(this::miTTreeActionPerformed);
        popTables.add(miTTree);

        miTVoir.setBackground(Color.white);
        miTVoir.setText("Contenu");
        miTVoir.addActionListener(this::miTmiTVoirActionPerformed);
        popTables.add(miTVoir);
        popTables.add(new JPopupMenu.Separator());

        miTSelect.setBackground(Color.white);
        miTSelect.setText("SELECT");
        miTSelect.addActionListener(this::miTSelectActionPerformed);

        mTSQL.setBackground(Color.white);
        mTSQL.setText("SQL");
        mTSQL.add(miTSelect);

        miTInsert.setBackground(Color.white);
        miTInsert.setText("INSERT");
        miTInsert.addActionListener(this::miTInsertActionPerformed);
        mTSQL.add(miTInsert);

        miTUpdate.setBackground(Color.white);
        miTUpdate.setText("UPDATE");
        miTUpdate.addActionListener(this::miTUpdateActionPerformed);
        mTSQL.add(miTUpdate);

        miTDelete.setBackground(Color.white);
        miTDelete.setText("DELETE");
        miTDelete.addActionListener(this::miTDeleteActionPerformed);
        mTSQL.add(miTDelete);

        miTCreate.setBackground(Color.white);
        miTCreate.setText("CREATE");
        miTCreate.addActionListener(this::miTCreateActionPerformed);
        mTSQL.add(miTCreate);

        miTDrop.setBackground(Color.white);
        miTDrop.setText("DROP");
        miTDrop.addActionListener(this::miTDropActionPerformed);
        mTSQL.add(miTDrop);

        popTables.add(mTSQL);
        popTables.add(new JPopupMenu.Separator());

        mTData.setBackground(Color.red);
        mTData.setText("Données");

        miTVider.setBackground(Color.white);
        miTVider.setText("Vider");
        miTVider.addActionListener(this::miTViderActionPerformed);
        mTData.add(miTVider);

        miTImport.setBackground(Color.white);
        miTImport.setText("Importer");
        miTImport.addActionListener(this::miTImportActionPerformed);
        mTData.add(miTImport);

        miTExport.setBackground(Color.white);
        miTExport.setText("Exporter");
        miTExport.addActionListener(this::miTExportActionPerformed);
        mTData.add(miTExport);

        popTables.add(mTData);
        popTables.add(new JPopupMenu.Separator());

        miVInsNom.setBackground(Color.white);
        miVInsNom.setText("Insérer nom");
        miVInsNom.addActionListener(this::miVInsNomActionPerformed);
        popVars.add(miVInsNom);

        miVInsRef.setBackground(Color.white);
        miVInsRef.setText("Insérer ref");
        miVInsRef.addActionListener(this::miVInsRefActionPerformed);
        popVars.add(miVInsRef);

        miVInsVal.setBackground(Color.white);
        miVInsVal.setText("Insérer val");
        miVInsVal.addActionListener(this::miVInsValActionPerformed);
        popVars.add(miVInsVal);

        miVInsp.setBackground(Color.white);
        miVInsp.setText("Inspecter");
        miVInsp.addActionListener(this::miVInspActionPerformed);
        popVars.add(miVInsp);

        miVMDoc.setBackground(Color.white);
        miVMDoc.setText("Voir doc");
        miVMDoc.addActionListener(this::miVMDocActionPerformed);
        popVars.add(miVMDoc);
        popVars.add(new JPopupMenu.Separator());

        miVAjout.setBackground(Color.white);
        miVAjout.setText("Ajouter");
        miVAjout.addActionListener(this::miVAjoutActionPerformed);
        popVars.add(miVAjout);

        miVModif.setBackground(Color.white);
        miVModif.setText("Modifier");
        miVModif.addActionListener(this::miVModifActionPerformed);
        popVars.add(miVModif);

        miVDupli.setBackground(Color.white);
        miVDupli.setText("Dupliquer");
        miVDupli.addActionListener(this::miVDupliActionPerformed);
        popVars.add(miVDupli);

        miVSupp.setBackground(Color.white);
        miVSupp.setText("Supprimer");
        miVSupp.addActionListener(this::miVSuppActionPerformed);
        popVars.add(miVSupp);
        popVars.add(new JPopupMenu.Separator());

        miTActu.setBackground(Color.white);
        miTActu.setText("Actualiser");
        miTActu.addActionListener(this::miTActuActionPerformed);
        popTables.add(miTActu);

        miVActu.setBackground(Color.white);
        miVActu.setText("Actualiser");
        miVActu.addActionListener(this::miVActuActionPerformed);
        popVars.add(miVActu);

        mWait.setText("Exécution...");
        mWait.setForeground(Color.red);
        mWait.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lunasql/http/res/load.gif")));
        mWait.setVisible(false);
        menuBar.add(mWait);

        pack();
    }

   /**
    * Fermeture
    *
    * @param evt l'événement
    */
   private void miCloseActionPerformed(ActionEvent evt) {
      formWindowClosing(null);
   }

   /**
    * Compètement
    *
    * @param evt l'événement
    */
   private void miComplActionPerformed(ActionEvent evt) {
      String mot = txeditor.getSelectedText(), compl;
      if (mot == null || mot.length() == 0) return;
      compl = complWords.getProperty(mot);
      if (compl == null) return;
      insertString(' ' + compl);
   }

   /**
    * Évaluation
    *
    * @param evt l'événement
    */
   private void miEvalActionPerformed(ActionEvent evt) {
      final String texte = txeditor.getSelectedText();
      if (texte == null || texte.length() == 0) {
         JOptionPane.showMessageDialog(this, "Aucun code sélectionné", "Évaluation", MSGWARN);
         return;
      }
      mWait.setVisible(true);

      new Thread(() -> {
         new Lecteur(cont, texte, new HashMap<String, String>() {{ put(Contexte.LEC_THIS, "(éditeur)"); }});
         Valeur vr = cont.getValeur();
         String sub = null;
         if (vr != null) sub = vr.getSubValue();
         if (sub != null) {
            sub = sub.replaceAll("[\r\n]", " ");
            int pos = txeditor.getSelectionEnd();
            insertString(" " + sub);
            txeditor.setSelectionStart(pos);
            txeditor.setSelectionEnd(pos + sub.length() + 1);
         }
         mWait.setVisible(false);
      }).start();
   }

   private void miInspActionPerformed(ActionEvent evt) {
      String s = txeditor.getSelectedText();
      if (s == null) JOptionPane.showMessageDialog(this,
              "Aucune variable sélectionnée", "Inspection", MSGWARN);
      else inspectVar(s.trim());
   }

   private void miMDocActionPerformed(ActionEvent evt) {
      String s = txeditor.getSelectedText();
      if (s == null) JOptionPane.showMessageDialog(this,
              "Aucune variable sélectionnée", "Documentation", MSGWARN);
      else showVarHelp(s.trim());

   }

   private void miSDocActionPerformed(ActionEvent evt) {
      String kw = txeditor.getSelectedText();
      if (Desktop.isDesktopSupported()) {
         try {
            Desktop dk = Desktop.getDesktop();
            String url = "http://www.w3schools.com/sql/";
            if (dk.isSupported(Desktop.Action.BROWSE))
               dk.browse(new URI(url + (kw == null ? "" : "sql_" + kw + ".asp")));
         } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Doc SQL", MSGERROR);
         }
      }
   }

   private void miCopyActionPerformed(ActionEvent evt) {
      txeditor.copy();
   }

   private void miCutActionPerformed(ActionEvent evt) {
      txeditor.cut();
   }

   private void miPasteActionPerformed(ActionEvent evt) {
      txeditor.paste();
   }

   private void miSelAllActionPerformed(ActionEvent evt) {
      txeditor.selectAll();
   }

   private void miSaveActionPerformed(ActionEvent evt) {
      saveFile();
   }

   private void miOpenActionPerformed(ActionEvent evt) {
      JFileChooser fc = new JFileChooser();
      fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      fc.setCurrentDirectory(new File(this.path));
      if (fc.showOpenDialog(FrmEditScript.this) == JFileChooser.APPROVE_OPTION) {
         setEngine(getEngine());
         if (saved || JOptionPane.showConfirmDialog(this,
                 "Le fichier '" + (file == null ? "(non enregistré)" : file.getName())
                 + "' a été modifié.\nOuvrir tout de même un autre fichier ?", "Ouverture",
                 JOptionPane.YES_NO_OPTION, MSGQUEST) == REPOUI) {
            file = fc.getSelectedFile();
            path = file.getAbsolutePath();
            openFile();
         }
      }
   }

   private void miRunActionPerformed(ActionEvent evt) {
      String texte = txeditor.getSelectedText();
      if (texte == null) texte = txeditor.getText();
      if (texte == null || texte.length() == 0) return;
      final String texte2 = texte;
      mWait.setVisible(true);

      new Thread(() -> {
         cont.println("\n=== Sortie du script ===");
         if (engine == null || engine.equals("sql") || engine.equals("lsql") || engine.equals("luna")) {
            new Lecteur(cont, texte2, new HashMap<String, String>() {{ put(Contexte.LEC_THIS, "(éditeur)"); }});
         } else {
            Object o = null;
            try {
               ScriptEngine se = new ScriptEngineManager().getEngineByExtension(engine);
               if (se == null) JOptionPane.showMessageDialog(FrmEditScript.this,
                        "Moteur indisponible :\n" + engine, "Exécution", MSGERROR);
               else o = se.eval(texte2);
            } catch (Exception e) {
               Tools.textMessage(FrmEditScript.this, "Erreur d'exécution :\n" + e.getMessage(),
                     "Exécution " + engine, MSGERROR);
            }
            if (o != null) JOptionPane.showMessageDialog(FrmEditScript.this,
                  "Objet retourné :\n" + o.toString(), "Exécution", MSGINFO);
         }
         cont.println("========================");
         mWait.setVisible(false);
      }).start();
   }

   private void miSignActionPerformed(ActionEvent evt) {
      try {
         String newcontent = signScript(txeditor.getText());
         if (newcontent != null) txeditor.setText(newcontent);
      } catch (IllegalArgumentException ex) {
         Tools.errorMessage(FrmEditScript.this,"Mot de passe incorrect", "Signature");
      } catch (NoSuchAlgorithmException|SignatureException|InvalidKeyException|IOException|InvalidKeySpecException |
               NoSuchPaddingException|BadPaddingException|IllegalBlockSizeException ex) {
         Tools.textMessage(FrmEditScript.this, "Erreur de création de la signature :\n" + ex.getMessage(),
               "Signature", MSGERROR);
      }
   }

   private void miVerifActionPerformed(ActionEvent evt) {
      try {
         verifySign(txeditor.getText());
      } catch (IllegalArgumentException ex) {
         Tools.errorMessage(FrmEditScript.this,
               "Signature numérique absente (cartouche non reconnu)", "Mauvaise signature");
      } catch (NoSuchAlgorithmException|SignatureException|InvalidKeyException ex) {
         Tools.textMessage(FrmEditScript.this, "Erreur de vérification de la signature :\n" + ex.getMessage(),
               "Signature", MSGERROR);
      }
   }

   private void miEngineActionPerformed(ActionEvent evt) {
      String e = JOptionPane.showInputDialog(this,
              "Moteur actuel : " + (engine == null ? "(aucun)" : engine)
              + "\nPrécisez un nouveau moteur de script",
              "Script Engine", JOptionPane.QUESTION_MESSAGE);
      if (e != null) {
         setEngine(e);
         setSyntax();
      }
   }

   private void miSyntaxActionPerformed(ActionEvent evt) {
      String[] tsyn = getSyntaxes();
      String s = (String) JOptionPane.showInputDialog(this,
            "Type syntaxique actuel : " + txeditor.getContentType()
            + "\nPrécisez un nouveau type syntaxique\n(cela va vider le tampon)",
            "Type syntaxique", JOptionPane.QUESTION_MESSAGE, null, tsyn, tsyn[0]);
      if (s != null) {
         setEngine(s);
         setSyntax();
      }
   }

   private void formWindowClosing(WindowEvent evt) {
      if (!saved && JOptionPane.showConfirmDialog(this,
              "Le fichier '" + (file == null ? "(non enregistré)" : file.getName())
              + "' a été modifié. L'enregistrer ?", "Enregistrement",
              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == REPOUI) {
         saveFile();
      }
      if (uniq) {
         if (cont.fermerConnex()) System.exit(0);
      } else {
         this.txeditor.setText("");
         this.setVisible(false);
      }
   }

   private void txeditorMouseReleased(MouseEvent evt) {
      MAJNoLines();
   }

   private void txeditorKeyReleased(KeyEvent evt) {
      saved = false;
      MAJNoLines();
   }

   private void lstTablesMouseReleased(MouseEvent evt) {
      if (evt.getButton() == MouseEvent.BUTTON3)
         popTables.show(lstTables, evt.getX(), evt.getY());
   }

   private void lstVarsMouseReleased(MouseEvent evt) {
      if (evt.getButton() == MouseEvent.BUTTON3)
         popVars.show(lstVars, evt.getX(), evt.getY());
   }

   private void miTInsNomActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) insertString(lstTables.getSelectedValue());
   }

   private void miVInsNomActionPerformed(ActionEvent evt) {
      if (lstVars.getSelectedIndex() != -1) insertString(lstVars.getSelectedValue());
   }

   private void miVInsValActionPerformed(ActionEvent evt) {
      if (lstVars.getSelectedIndex() != -1){
         String val = cont.getGlbVar(lstVars.getSelectedValue());
         if (val != null) insertString(val);
      }
   }

   private void miVInsRefActionPerformed(ActionEvent evt) {
      if (lstVars.getSelectedIndex() != -1) insertString("$(" + lstVars.getSelectedValue() + ")");
   }

   private void miVInspActionPerformed(ActionEvent evt) {
      if (lstVars.getSelectedIndex() != -1) inspectVar(lstVars.getSelectedValue());
   }

   private void miVMDocActionPerformed(ActionEvent evt) {
      if (lstVars.getSelectedIndex() != -1) showVarHelp(lstVars.getSelectedValue());
   }

   private void miVAjoutActionPerformed(ActionEvent evt) {
      String key = lstVars.getSelectedIndex() != -1 ? lstVars.getSelectedValue() : "";
      key = JOptionPane.showInputDialog(this, "Entrez le nom de la variable :", key);
      if (key == null) return;
      if (cont.valideKey(key)) {
         if (cont.isSet(key) && JOptionPane.showConfirmDialog(this,
            "La variable " + key + " existe déjà.\nL'écraser ?",
            "Confirmation", JOptionPane.YES_NO_OPTION, MSGQUEST) != REPOUI) return;
         String val = Tools.codeMessage(this, "", "Nouvelle variable " + key + " :", true);
         if (val != null) {
            cont.setVar(key, val);
            majVars();
         }
      } else JOptionPane.showMessageDialog(this, "Affectation de variable invalide",
              "Nouvelle variable", MSGWARN);
   }

   private void miVModifActionPerformed(ActionEvent evt) {
      if (lstVars.getSelectedIndex() != -1) {
         String key = lstVars.getSelectedValue(), val = cont.getGlbVar(key);
         if (cont.isSys(key))
            JOptionPane.showMessageDialog(this, "Modification de variable système interdite",
              "Modification", MSGWARN);
         else {
            val = Tools.codeMessage(this, val, "Modification de " + key + " :", true);
            if (val != null) cont.setVar(key, val);
         }
      }
   }

   private void miVDupliActionPerformed(ActionEvent evt) {
      if (lstVars.getSelectedIndex() != -1) {
         String key = lstVars.getSelectedValue(), val = cont.getGlbVar(key);
         if (cont.isNonSys(key)) {
            int i = 0;
            String newkey;
            do {
               newkey = key + "-" + (++i);
            } while(cont.isSet(newkey));

            String key2 = JOptionPane.showInputDialog(this,"Entrez le nouveau nom de la variable :", newkey);
            if (!key.equals(key2)) {
               cont.setVar(key2, val);
               majVars();
            }
         } else {
            JOptionPane.showMessageDialog(this, "Duplication de variable système interdite",
                  "Duplication", MSGWARN);
         }
      }
   }

   private void miVSuppActionPerformed(ActionEvent evt) {
      if (lstVars.getSelectedIndex() != -1) {
         String key = lstVars.getSelectedValue();
         if (cont.isNonSys(key)) {
            if (JOptionPane.showConfirmDialog(this,
                  "Supprimer la variable " + key + " ?", "Confirmation",
                  JOptionPane.YES_NO_OPTION, MSGQUEST) == REPOUI) {
               cont.unsetVar(key);
               majVars();
            }
         } else
            JOptionPane.showMessageDialog(this, "Suppression de variable système interdite",
              "Suppression", MSGWARN);
      }
   }

   private void miTActuActionPerformed(ActionEvent evt) {
      majTables();
   }

   private void miVActuActionPerformed(ActionEvent evt) {
      majVars();
   }

   private void miTCountActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         try {
            String t = lstTables.getSelectedValue();
            int nbl = cont.getConnex().seekNbl(t);
            JOptionPane.showMessageDialog(this, "Nombre de lignes :  " + nbl, t, MSGINFO);
         } catch (SQLException ex) {
            cont.errprintln("Erreur SQLException :\n" + ex.getMessage());
         }
      }
   }

   private void miTColsActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         try {
            String t = lstTables.getSelectedValue();
            DatabaseMetaData dMeta = cont.getConnex().getMetaData();
            ResultSet col = dMeta.getColumns(null, null, t, null);
            StringBuilder sb = new StringBuilder();
            sb.append("Nom\tType\tTaille\n");
            sb.append("----------------------------------------------------------------\n");
            while (col.next()) {
               sb.append(col.getString(4)).append('\t') // Colonne
               .append(col.getString(6)).append('\t') // Type
               .append(col.getInt(7)).append(' ') // Taille
               .append('\n');
            }
            Tools.textMessage(this, sb.toString(), t);
         } catch (SQLException ex) {
            cont.errprintln("Erreur SQLException :\n" + ex.getMessage());
         }
      }
   }

   private void miTTreeActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         String t = lstTables.getSelectedValue();
         DatabaseMetaData dMeta = cont.getConnex().getMetaData();
         CmdTree ct = (CmdTree)cont.getCommand("TREE");
         try {
            ct.setDeepMax(3);
            String r = ct.drawTree(dMeta, t, 0, "");
            if (r.isEmpty()) JOptionPane.showMessageDialog(this, "Aucune relation", t, MSGINFO);
            else Tools.textMessage(this, r + "\n", t);
         } catch (SQLException ex) {
            cont.errprintln("Erreur SQLException :\n" + ex.getMessage());
         }
      }
   }

   private void miTSelectActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         try {
            String t = lstTables.getSelectedValue();
            DatabaseMetaData dMeta = cont.getConnex().getMetaData();
            ResultSet col = dMeta.getColumns(null, null, t, null);
            StringBuilder sb = new StringBuilder();
            while (col.next()) sb.append(col.getString(4)).append(", ");
            String c = sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "";
            sb.setLength(0);
            sb.append("SELECT ").append(c).append(" FROM ").append(t).append(" WHERE 1=0");
            insertString(sb.toString());
         } catch (SQLException ex) {
            cont.errprintln("Erreur SQLException :\n" + ex.getMessage());
         }
      }
   }

   private void miTInsertActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         try {
            String t = lstTables.getSelectedValue();
            DatabaseMetaData dMeta = cont.getConnex().getMetaData();
            ResultSet col = dMeta.getColumns(null, null, t, null);
            StringBuilder sb = new StringBuilder();
            while (col.next()) sb.append(col.getString(4)).append(", ");
            String c = sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "";
            sb.setLength(0);
            sb.append("INSERT INTO ").append(t).append(" (").append(c).append(") VALUES ()");
            insertString(sb.toString());
         } catch (SQLException ex) {
            cont.errprintln("Erreur SQLException :\n" + ex.getMessage());
         }
      }
   }

   private void miTUpdateActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         try {
            String t = lstTables.getSelectedValue(), c = "";
            DatabaseMetaData dMeta = cont.getConnex().getMetaData();
            ResultSet col = dMeta.getColumns(null, null, t, null);
            if (col.next()) c = col.getString(4);
            insertString("UPDATE " + t + " SET " + c + " = 0 WHERE 1=0");
         } catch (SQLException ex) {
            cont.errprintln("Erreur SQLException :\n" + ex.getMessage());
         }
      }
   }

   private void miTDeleteActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         String t = lstTables.getSelectedValue();
         insertString("DELETE FROM " + t + " WHERE 1=0");
      }
   }

   private void miTCreateActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         try {
            String t = lstTables.getSelectedValue();
            DatabaseMetaData dMeta = cont.getConnex().getMetaData();
            ResultSet col = dMeta.getColumns(null, null, t, null);
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ").append(t).append(" (\n");
            while (col.next()) {
               sb.append("  ")
                  .append(col.getString(4)).append(' ') // Colonne
                  .append(col.getString(6)).append(" (") // Type
                  .append(col.getInt(7)).append(")") // Taille
                  .append(",\n");
            }
            sb.delete(sb.length() - 2, sb.length() - 1).append(")\n");
            insertString(sb.toString());
         } catch (SQLException ex) {
            cont.errprintln("Erreur SQLException :\n" + ex.getMessage());
         }
      }
   }

   private void miTDropActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         String t = lstTables.getSelectedValue();
         insertString("DROP TABLE " + t);
      }
   }

   private void cbSysVarsActionPerformed(ActionEvent evt) {
      majVars();
   }

   private void miTViderActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         if (JOptionPane.showConfirmDialog(this, "La table va être vidée.\nContinuer ?",
            "Confirmation", JOptionPane.YES_NO_OPTION, MSGQUEST) == REPOUI)
            new Lecteur(cont, "DELETE FROM " + lstTables.getSelectedValue() + ";");
      }
   }

   private void miTImportActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         JFileChooser fc = new JFileChooser();
         fc.setCurrentDirectory(new File(this.path));
         if (fc.showOpenDialog(FrmEditScript.this) == JFileChooser.APPROVE_OPTION) {
            String t = lstTables.getSelectedValue(), f = fc.getSelectedFile().getAbsolutePath();
            new Lecteur(cont, "IMPORT  \"" + f + "\" " + t + ";");
         }
      }
   }

   private void miTExportActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         JFileChooser fc = new JFileChooser();
         if (fc.showSaveDialog(FrmEditScript.this) == JFileChooser.APPROVE_OPTION) {
            String t = lstTables.getSelectedValue(), f = fc.getSelectedFile().getAbsolutePath();
            new Lecteur(cont, "EXPORT  \"" + f + "\" " + t + ";");
         }
      }
   }

   private void miTmiTVoirActionPerformed(ActionEvent evt) {
      if (lstTables.getSelectedIndex() != -1) {
         new Lecteur(cont, "VIEW " + lstTables.getSelectedValue() + ";");
      }
   }

   // Constantes
   private final int MSGINFO = JOptionPane.INFORMATION_MESSAGE,
             MSGWARN = JOptionPane.WARNING_MESSAGE,
             MSGERROR = JOptionPane.ERROR_MESSAGE,
             MSGQUEST = JOptionPane.QUESTION_MESSAGE,
             REPOUI = JOptionPane.YES_OPTION;

    // Variables declaration
   private JLabel lbLines;
   private JMenu mPlugin;
   private JMenu mCode;
   private JMenu mWait;
   private JMenuItem miRedo;
   private JMenuItem miUndo;
   private JEditorPane txeditor;
   private JList<String> lstTables;
   private JList<String> lstVars;
   private JCheckBox cbSysVars;
   private JPopupMenu popTables;
   private JPopupMenu popVars;

   // Déclarations utilisateur
   private final Contexte cont;
   private String path;
   private String engine;
   private File file;
   private boolean saved = true;
   private final boolean uniq;
}
