package lunasql.cmd;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import lunasql.lib.Contexte;
import lunasql.sql.SQLCnx;
import lunasql.val.ValeurDef;
import sbrunner.gui.tableView.DefaultTableViewColumn;
import sbrunner.gui.tableView.DefaultTableViewModel;
import sbrunner.gui.tableView.TableView;
import sbrunner.gui.tableView.TableViewColumn;

/**
 * Commande VIEW <br>
 * (Interne) Affiche un resultset en JTable au lieu de la sortie standard
 * @author M.P.
 */
public class CmdView extends Instruction {

   public CmdView(Contexte cont){
      super(cont, TYPE_CMDINT, "VIEW", null);
   }

   @Override
   public int execute() {
      if (cont.isHttpMode())
         return cont.erreur("VIEW", "cette commande n'est pas autorisée en mode HTTP", lng);
      if (getLength() < 2)
         return cont.erreur("VIEW", "une commande SQL SELECT ou une table est attendue", lng);

      String s = getSCommand(1);
      int id = s.indexOf(' ');
      String c = (id < 0 ? s : s.substring(0, id)).toUpperCase();
      if (getLength() == 2) // nom de table fourni
         s = "SELECT * FROM " + c;
      else if (!c.equals("SELECT")) // commande SQL fournie
         return cont.erreur("VIEW", "seule une commande SELECT (ou une table) est autorisée", lng);

      // Exécution
      try {
         long tm = System.currentTimeMillis();
         ResultSet rs = cont.getConnex().getResultset(s);
         ResultSetMetaData rsSchema = rs.getMetaData();
         tm = System.currentTimeMillis() - tm;
         int nbCol = rsSchema.getColumnCount(), nbLig = 0;
         if (nbCol > 16) // 16 colonnes max car limite de fonctions en VarRow
            return cont.erreur("VIEW", "la requête retourne " + nbCol + " colonnes (max. 16)", lng);

         TableViewColumn[] columns = new TableViewColumn[nbCol];
         List<VarRow> rows = new ArrayList<>();
         for (int i = 0; i < nbCol; i++) 
            columns[i] = new DefaultTableViewColumn(rsSchema.getColumnName(i + 1),
                  VarRow.class.getDeclaredMethod("getVar" + i, (Class<?>[])null),
                  VarRow.class.getDeclaredMethod("setVar" + i, String.class)
                  );
         while (rs.next() && ++nbLig <= 200) {  // limite de taille à 200 lignes
            List<String> l = new ArrayList<>();
            for (int i = 0; i < nbCol; i++) l.add(i, rs.getString(i + 1));
            rows.add(new VarRow(l));
         }
         rs.close();

         // Construction de la fenêtre de vue
         try {
            JFrame frame = new JFrame(s);
            final TableView view = new TableView(new DefaultTableViewModel(columns, rows));
            view.setMakeIndex(true);
            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(new JScrollPane(view), BorderLayout.CENTER);

            frame.setSize(600, 400);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.validate();
            frame.setVisible(true);
         }
         catch (HeadlessException ex) {
            return cont.exception("VIEW", "ERREUR HeadlessException : " + ex.getMessage(), lng, ex);
         }
         catch (MissingResourceException ex) {
            return cont.exception("VIEW", "ERREUR MissingResourceException : " + ex.getMessage(), lng, ex);
         }

         cont.setValeur(new ValeurDef(cont, "-> " + nbLig + " ligne" + (nbLig > 1 ? "s" : "") +
                 " trouvée" + (nbLig > 1 ? "s" : "") + " (" + SQLCnx.frmDur(tm) + ")",
            Contexte.VERB_MSG, Integer.toString(nbLig)));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch(SQLException ex){
         return cont.exception("VIEW", "ERREUR SQLException : " + ex.getMessage(), lng, ex);
      }
      catch (NoSuchMethodException ex) {
         return cont.exception("VIEW", "ERREUR NoSuchMethodException : " + ex.getMessage(), lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  view        Affiche le résultat d'une requête ou d'une table\n";
   }

   /**
    * Classe représentant une ligne variable
    * @author MP
    *
    */
   public static class VarRow {
      private final List<String> data;
      
      public VarRow(List<String> d){
         data = d;
      }
      
      public String getVar0(){
         return data.get(0);
      }
      
      public void setVar0(String element){
         data.set(0, element);
      }
      
      public String getVar1(){
         return data.get(1);
      }
      
      public void setVar1(String element){
         data.set(1, element);
      }
      
      public String getVar2(){
         return data.get(2);
      }
      
      public void setVar2(String element){
         data.set(2, element);
      }
      
      public String getVar3(){
         return data.get(3);
      }
      
      public void setVar3(String element){
         data.set(3, element);
      }
      
      public String getVar4(){
         return data.get(4);
      }
      
      public void setVar4(String element){
         data.set(4, element);
      }
      
      public String getVar5(){
         return data.get(5);
      }
      
      public void setVar5(String element){
         data.set(5, element);
      }
      
      public String getVar6(){
         return data.get(6);
      }
      
      public void setVar6(String element){
         data.set(6, element);
      }
      
      public String getVar7(){
         return data.get(7);
      }
      
      public void setVar7(String element){
         data.set(7, element);
      }
      
      public String getVar8(){
         return data.get(8);
      }
      
      public void setVar8(String element){
         data.set(8, element);
      }
      
      public String getVar9(){
         return data.get(9);
      }
      
      public void setVar9(String element){
         data.set(9, element);
      }
      
      public String getVar10(){
         return data.get(10);
      }
      
      public void setVar10(String element){
         data.set(10, element);
      }
      
      public String getVar11(){
         return data.get(11);
      }
      
      public void setVar11(String element){
         data.set(11, element);
      }
      
      public String getVar12(){
         return data.get(12);
      }
      
      public void setVar12(String element){
         data.set(12, element);
      }
      
      public String getVar13(){
         return data.get(13);
      }
      
      public void setVar13(String element){
         data.set(13, element);
      }
      
      public String getVar14(){
         return data.get(14);
      }
      
      public void setVar14(String element){
         data.set(14, element);
      }
      
      public String getVar15(){
         return data.get(15);
      }
      
      public void setVar15(String element){
         data.set(15, element);
      }
   }
}// class
