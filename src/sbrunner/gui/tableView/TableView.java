package sbrunner.gui.tableView;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The table view main class
 *
 * @author Stphane Brunner, Last modified by: $Author: stbrunner $ 
 * @version $Revision: 1.21 $ $Date: 2007/06/16 15:18:06 $.
 * Revision history:
 * $Log: TableView.java,v $
 * Revision 1.21  2007/06/16 15:18:06  stbrunner
 * fix import
 *
 * Revision 1.20  2007/06/16 09:11:30  stbrunner
 * Add lucterios changes
 *
 * Revision 1.xx  2007/05/31 19:30:00  l.gay@lucterios.org
 * menu disabled option - add row selector callback
 *
 * Revision 1.17  2004/12/28 15:25:54  stbrunner
 * code style
 *
 * Revision 1.15  2004/12/28 15:02:43  stbrunner
 * remove debug messages
 *
 * Revision 1.14  2004/09/05 19:22:16  stbrunner
 * add select by first letter
 *
 * Revision 1.13  2004/05/31 14:39:14  stbrunner
 * *** empty log message ***
 *
 * Revision 1.12  2004/02/15 19:29:43  stbrunner
 * *** empty log message ***
 *
 * Revision 1.11  2003/06/06 06:31:32  stbrunner
 * Change description
 *
 */
public class TableView extends JTable {
    /**
    * 
    */
   private static final long serialVersionUID = 3481167848397907997L;

   public interface IRowSelectCaller
    {
        void action();
    }

    private TableViewColumn mCurrentCollumn;
    private Point mLastMouseClickedPosition;

    private JDialog mDialogSearch;
    //  private JLabel mLabelSearchTest = new JLabel();
    private JTextField mTextFieldSearchTest = new JTextField();
    private JPopupMenu mPopupMenu = new JPopupMenu();
    private JDialog mDialogVisiblity;
    private JMenuItem mMenuItemHide = new JMenuItem();
    private JMenuItem mMenuItemSearch = new JMenuItem();
    private JMenuItem mMenuItemVisiblityProperties = new JMenuItem();
    private JMenuItem mMenuSave = new JMenuItem();
    private JButton mButtonVisiblityOK = new JButton();
    private JRadioButtonMenuItem mRadioButtonMenuItemAutoResizeColumnAll = new JRadioButtonMenuItem();
    private JRadioButtonMenuItem mRadioButtonMenuItemAutoResizeColumnNext = new JRadioButtonMenuItem();
    private JRadioButtonMenuItem mRadioButtonMenuItemAutoResizeColumnNone = new JRadioButtonMenuItem();
    private ButtonGroup mButtonGroupAutoResizeColumn = new ButtonGroup();
    private ResourceBundle mRessource = ResourceBundle.getBundle(getClass().getName());
    private GridLayout mGridLayout = new GridLayout();
    private String mFileName;

    private IRowSelectCaller mRowSelectCaller=null;
    /**
     * Set a new callback of row select.
     * @param pRowSelectCaller the callback
     */
    public void setRowSelectCaller(IRowSelectCaller pRowSelectCaller)
    {
        mRowSelectCaller=pRowSelectCaller;
    }

    /**
     * Construct.
     * @param pOwner the owner frame to set the dialogues modal.
     * @param pModel the model
     * @deprecated pOwner unsued
     */
    public TableView(JFrame pOwner, TableViewModel pModel) {
        this(pModel);
    }

    /**
     * Construct.
     */
    public TableView() {
        this(new DefaultTableViewModel());
    }

    /**
     * Construct.
     * @param pModel the model
     */
    public TableView(TableViewModel pModel) {
        super(getModel(pModel));

        getTableHeader().setDefaultRenderer(new TableHeaderRenderer());
        getTableHeader().addMouseListener(new TableMouseListener());
        addKeyListener(new TableKeyListener());
        initMenu();
    }

    /**
     * Set Visible for menu save.
     * @param pVisible
     */
    public void setVisibleMenuSave(boolean pVisible)
    {
	mMenuSave.setVisible(pVisible);
	mPopupMenu.getComponent(3).setVisible(pVisible);
    }

    /**
     * Set Visible for menu AutoResizeColumn.
     * @param pVisible
     */
    public void setVisibleAutoResizeColumn(boolean pVisible)
    {
    	mRadioButtonMenuItemAutoResizeColumnAll.setVisible(pVisible);
    	mRadioButtonMenuItemAutoResizeColumnNext.setVisible(pVisible);
    	mRadioButtonMenuItemAutoResizeColumnNone.setVisible(pVisible);
	mPopupMenu.getComponent(7).setVisible(pVisible);
    }

    /**
     * Load the setings from the xml file name
     * @param pFileName the xml file name
     */
    public void loadSettings(String pFileName) {
       throw new IllegalArgumentException("fonction loadSettings(String) pas implantée !");
       /*
        mFileName = pFileName;
        TableViewAdapter adapter = (TableViewAdapter) super.getModel();
        TableViewColumn[] columns = adapter.getModel().getColumns();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(mFileName);
            String xpath = "/xml/body/*";
            try {
                NodeList nodelist = XPath.selectNodeList(doc, xpath);
                // Process the elements in the nodelist
                for (int i = 0; i < nodelist.getLength(); i++) {
                    // Get element
                    Element elem = (Element) nodelist.item(i);
                    //enabled/disabled columns
                    if (elem.getTagName().compareTo("column") == 0) {
                        for (int j = 0; j < columns.length; j++) {
                            final TableViewColumn column = columns[j];
                            if (column.getName().compareTo(elem.getAttribute("id")) == 0) {
                                if (elem.getAttribute("visible").compareToIgnoreCase("true") == 0) {
                                    if (!adapter.isVisible(column)) {
                                        adapter.setVisible(column, true);
                                    }
                                }
                                else {
                                    if (adapter.isVisible(column)) {
                                        adapter.setVisible(column, false);
                                    }
                                }
                            }
                        }
                    }
                    //resize
                    if (elem.getTagName().compareTo("auto_resize_column") == 0) {
                        if (elem.getAttribute("selected").compareTo("All") == 0) {
                            mRadioButtonMenuItemAutoResizeColumnAll.setSelected(true);
                            setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
                        }
                        else if (elem.getAttribute("selected").compareTo("Next") == 0) {
                            mRadioButtonMenuItemAutoResizeColumnNext.setSelected(true);
                            setAutoResizeMode(AUTO_RESIZE_NEXT_COLUMN);
                        }
                        else if (elem.getAttribute("selected").compareTo("None") == 0) {
                            mRadioButtonMenuItemAutoResizeColumnNone.setSelected(true);
                            setAutoResizeMode(AUTO_RESIZE_OFF);
                        }
                    }
                }
                // setting of width have to be second part!
                // Process the elements in the nodelist
                for (int i = 0; i < nodelist.getLength(); i++) {
                    // Get element
                    Element elem = (Element) nodelist.item(i);
                    //width
                    if (elem.getTagName().compareTo("column") == 0) {
                        for (int j = 0; j < getColumnModel().getColumnCount(); j++) {
                            TableColumn tableColumn = getColumnModel().getColumn(j);
                            if (tableColumn.getHeaderValue().toString().compareTo(elem.getAttribute("id")) == 0) {
                                if (elem.getAttribute("width").length() > 0) {
                                    tableColumn.setPreferredWidth(new Integer(elem.getAttribute("width")).intValue());
                                }
                            }
                        }
                    }
                }
            }
            catch (TransformerException e) {
                e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
            }
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }
        catch (SAXException e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }
        catch (IOException e) {
            System.out.println("File not found :" + mFileName);
        }
        */
    }

    /**
     * Write to an XML file 
     * @param pFilename the file name
     * @param pDocument the document
     */
    private static void writeXmlToFile(String pFilename, Document pDocument) {
        try {
            // Prepare the DOM document for writing
            Source source = new DOMSource(pDocument);
            // Prepare the output file
            File file = new File(pFilename);
            Result result = new StreamResult(file);
            // Write the DOM document to the file
            // Get Transformer
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            // Write to a file
            xformer.setOutputProperty(OutputKeys.INDENT, "yes"); // add newlines
            xformer.transform(source, result);
        } catch (TransformerException e) {
           System.out.println("VIEW WriteXmlToFile : ERREUR TransformerException : "
                 + e.getMessage());
        } 
    }

    /**
     * Saves the setting to a file
     * @param pFileName the fale nema
     */
    public void saveSettings(String pFileName) {
        TableViewAdapter adapter = (TableViewAdapter) super.getModel();
        TableViewColumn[] columns = adapter.getModel().getColumns();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element xroot = doc.createElement("xml");
            doc.appendChild(xroot);
            Element broot = doc.createElement("body");
            xroot.appendChild(broot);

            //resize
            Element er = doc.createElement("auto_resize_column");
            if (mRadioButtonMenuItemAutoResizeColumnAll.isSelected()) {
                er.setAttribute("selected", "All");
            }
            else if (mRadioButtonMenuItemAutoResizeColumnNext.isSelected()) {
                er.setAttribute("selected", "Next");
            }
            else if (mRadioButtonMenuItemAutoResizeColumnNone.isSelected()) {
                er.setAttribute("selected", "None");
            }
            broot.appendChild(er);

            //enabled/disabled columns
            for (int i = 0; i < columns.length; i++) {
                final TableViewColumn column = columns[i];
                Element e = doc.createElement("column");
                broot.appendChild(e);
                //column name
                e.setAttribute("id", column.getName());

                //visibility
                if (adapter.isVisible(column)) {
                    e.setAttribute("visible", "true");
                }
                else {
                    e.setAttribute("visible", "false");
                }

                //column width
                for (int j = 0; j < getColumnCount(); j++) {
                    TableColumn tableColumn = getColumnModel().getColumn(j);
                    if (tableColumn.getHeaderValue().toString().compareTo(column.getName()) == 0) {
                        e.setAttribute("width", new Integer(tableColumn.getWidth()).toString());
                        break;
                    }
                }
            }

            //save the document
            writeXmlToFile(pFileName, doc);
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Contruct adapter from table viw model.
     * @param pModel the table view model to be adapted
     * @return the adapter
     */
    private static TableViewAdapter getModel(TableViewModel pModel) {
        if (pModel != null) {
            return new TableViewAdapter(pModel);
        }
        return null;
    }

    /**
     * Set a new table view model.
     * @param pModel the new model
     */
    public void setModel(TableViewModel pModel) {
        super.setModel(getModel(pModel));
    }

    /**
     * Select a new row.
     * @param pRow the row Object
     */
    public void addRowSelection(Object pRow) {
        addRowSelection(pRow, false);
    }

    /**
     * Select a new row.
     * @param pRow the row Object
     * @param pScroll scroll to the new row ?
     */
    public void addRowSelection(Object pRow, boolean pScroll) {
        int index = ((TableViewAdapter) super.getModel()).indexOf(pRow);
        addRowSelectionInterval(index, index);
        scrollRectToVisible(this.getCellRect(index, index, true));
    }

    /**
     * Select new rows.
     * @param pRows the rows Objects
     */
    public void addRowSelection(Object[] pRows) {
        for (int i = 0; i < pRows.length; i++) {
            addRowSelection(pRows[i]);
        }
    }

    /**
     * Select new rows.
     * @param pRows the rows Objects
     */
    public void addRowSelection(List pRows) {
        for (Iterator i = pRows.iterator(); i.hasNext();) {
            try {
                addRowSelection(i.next());
            }
            catch (Exception e) {
            }
        }
    }

    /**
     * Auto size the column to fit with his content.
     * @param pColumnIndex visible column index (not table model index)
     */
    public void autoSizeColumn(int pColumnIndex) {
        int width = 0;
        TableColumn column = getColumnModel().getColumn(pColumnIndex);

        for (int i = 0; i < getRowCount(); i++) {
            TableModel model = super.getModel();

            width = Math.max(width,
                    getCellRenderer(i, pColumnIndex).getTableCellRendererComponent(this,
                            model.getValueAt(i, column.getModelIndex()), false, false, i, column.getModelIndex()).getPreferredSize().width + 1);
        }
        sizeColumnsToFit(pColumnIndex, width);
    }

    /**
     * affect ideal column size.
     */
    public void idealColumnSize() 
    {
        for(int colidx=0;colidx<getColumnCount();colidx++)
        {
            TableColumn column = getColumnModel().getColumn(colidx);
            column.setResizable(true);

            int width = column.getMinWidth();
            width = Math.max(width,column.getPreferredWidth());
            for (int rowidx = 0; rowidx < getRowCount(); rowidx++)
            {
                TableModel model = super.getModel();
                width = Math.max(width,getCellRenderer(rowidx, colidx).getTableCellRendererComponent(this, model.getValueAt(rowidx, column.getModelIndex()), false, false, rowidx, column.getModelIndex()).getPreferredSize().width + 1);
            }
            column.setPreferredWidth(width);
        }
    }

    /**
     * Set a column size.
     * @param pColumnIndex visible column index (not table model index)
     * @param pWidth column width
     */
    public void sizeColumnsToFit(int pColumnIndex, int pWidth) {
        TableColumn column = getColumnModel().getColumn(pColumnIndex);

        for (int i = 0; i < getColumnCount(); i++) {
            TableColumn tableColumn = getColumnModel().getColumn(i);
            tableColumn.setPreferredWidth(tableColumn.getWidth());
        }

        pWidth = Math.max(pWidth, column.getMinWidth());
        int diff = column.getWidth() - pWidth;
        column.setPreferredWidth(pWidth);

        // Use the mode to determine how to absorb the changes.
        switch (autoResizeMode) {
            case AUTO_RESIZE_NEXT_COLUMN :
                TableColumn newTableColumn = getColumnModel().getColumn(Math.min(pColumnIndex + 1, getColumnCount() - 1));
                newTableColumn = column == newTableColumn && getColumnCount() > 1 ? getColumnModel().getColumn(
                        getColumnCount() - 2) : newTableColumn;
                // last column ?
                newTableColumn.setPreferredWidth(newTableColumn.getPreferredWidth() + diff);
                break;

            case AUTO_RESIZE_SUBSEQUENT_COLUMNS :
                int cont = getColumnCount() - pColumnIndex;
                if (cont == 1 && getColumnCount() > 1) {
                    // last column
                    column = getColumnModel().getColumn(pColumnIndex - 1);
                    column.setPreferredWidth(column.getPreferredWidth() + diff);
                    break;
                }
                int coldiff = diff / (cont - 1);
                int colrest = diff % (cont - 1);

                for (int i = pColumnIndex + 1; i < getColumnCount(); i++) {
                    column = getColumnModel().getColumn(i);
                    column.setPreferredWidth(column.getPreferredWidth() + coldiff + (colrest > 0 ? 1 : 0));
                    colrest--;
                }
                break;

            case AUTO_RESIZE_LAST_COLUMN :
                column = getColumnModel().getColumn(getColumnCount() - 1);
                column.setPreferredWidth(column.getPreferredWidth() + diff);
                break;

            case AUTO_RESIZE_ALL_COLUMNS :
                coldiff = diff / (getColumnCount() - 1);
                colrest = diff % (getColumnCount() - 1);

                for (int i = 0; i < getColumnCount(); i++) {
                    if (i != pColumnIndex) {
                        column = getColumnModel().getColumn(i);
                        column.setPreferredWidth(column.getPreferredWidth() + coldiff + (colrest > 0 ? 1 : 0));
                        colrest--;
                    }
                }
                break;
        }
    }

    /**
     * Get the selection.
     * @return the row Object to be selected
     */
    public Object getSelectedRowObject() {
        return ((TableViewAdapter) super.getModel()).getNewRow(super.getSelectedRow());
    }

    /**
     * Get the selections.
     * @return list of Objects to be selected
     */
    public List getSelectedRowObjects() {
        int[] visibelSelectedRows = super.getSelectedRows();
        List result = new ArrayList(visibelSelectedRows.length);
        TableViewAdapter model = (TableViewAdapter) super.getModel();
        for (int i = 0; i < visibelSelectedRows.length; i++) {
            result.add(model.getNewRow(visibelSelectedRows[i]));
        }
        return result;
    }

    /**
     * @deprecated use getSelectedRowObject()
     * @return the selected row
     */
    public int getSelectedRow() {
        return super.getSelectedRow();
    }

    /**
     * @deprecated use getSelectedRowObjects()
     * @return the delected rows 
     */
    public int[] getSelectedRows() {
        return super.getSelectedRows();
    }

    /**
     * Init the Menu
     */
    private void initMenu() {
        mRadioButtonMenuItemAutoResizeColumnNext.setSelected(true);
        mButtonGroupAutoResizeColumn.add(mRadioButtonMenuItemAutoResizeColumnAll);
        mButtonGroupAutoResizeColumn.add(mRadioButtonMenuItemAutoResizeColumnNext);
        mButtonGroupAutoResizeColumn.add(mRadioButtonMenuItemAutoResizeColumnNone);
        setAutoResizeMode(AUTO_RESIZE_NEXT_COLUMN);

        mMenuItemHide.setText(mRessource.getString("menu.hide"));
        mMenuItemHide.addActionListener(new ActionListener() {
            /**
             * @param pEvent the event
             */
            public void actionPerformed(ActionEvent pEvent) {
                menuItemHideActionPerformed(pEvent);
            }

        });

        mMenuItemSearch.setText(mRessource.getString("menu.search"));
        mMenuItemSearch.addActionListener(new ActionListener() {
            /**
             * @param pEvent the event
             */
            public void actionPerformed(ActionEvent pEvent) {
                menuItemSearchActionPerformed(pEvent);
            }
        });

        mMenuItemVisiblityProperties.setText(mRessource.getString("menu.visiblepro"));
        mMenuItemVisiblityProperties.addActionListener(new ActionListener() {
            /**
             * @param pEvent the event
             */
            public void actionPerformed(ActionEvent pEvent) {
                menuItemVisiblityPropertiesActionPerformed(pEvent);
            }
        });

        mPopupMenu.setInvoker(this);

        mRadioButtonMenuItemAutoResizeColumnAll.setText(mRessource.getString("menu.autosizeall"));
        mRadioButtonMenuItemAutoResizeColumnAll.addActionListener(new java.awt.event.ActionListener() {
            /**
             * @param pEvent the event
             */
            public void actionPerformed(ActionEvent pEvent) {
                radioButtonMenuItemAutoResizeColumnAllActionPerformed(pEvent);
            }
        });

        mRadioButtonMenuItemAutoResizeColumnNext.setText(mRessource.getString("menu.autosizenext"));
        mRadioButtonMenuItemAutoResizeColumnNext.addActionListener(new java.awt.event.ActionListener() {
            /**
             * @param pEvent the event
             */
            public void actionPerformed(ActionEvent pEvent) {
                radioButtonMenuItemAutoResizeColumnNextActionPerformed(pEvent);
            }
        });

        mRadioButtonMenuItemAutoResizeColumnNone.setText(mRessource.getString("menu.autosizenone"));
        mRadioButtonMenuItemAutoResizeColumnNone.addActionListener(new java.awt.event.ActionListener() {
            /**
             * @param pEvent the event
             */
            public void actionPerformed(ActionEvent pEvent) {
                radioButtonMenuItemAutoResizeColumnNoneActionPerformed(pEvent);
            }

        });

        mMenuSave.setText(mRessource.getString("menu.save"));
        mMenuSave.addActionListener(new ActionListener() {
            /**
             * @param pEvent the event
             */
            public void actionPerformed(ActionEvent pEvent) {
                menuSaveActionPerformed(pEvent);
            }

        });

        mPopupMenu.add(mMenuItemHide);
        mPopupMenu.add(mMenuItemSearch);
        mPopupMenu.add(mMenuItemVisiblityProperties);
        mPopupMenu.addSeparator();
        mPopupMenu.add(mRadioButtonMenuItemAutoResizeColumnAll);
        mPopupMenu.add(mRadioButtonMenuItemAutoResizeColumnNext);
        mPopupMenu.add(mRadioButtonMenuItemAutoResizeColumnNone);
        mPopupMenu.addSeparator();
        mPopupMenu.add(mMenuSave);
    }
    /**
     * Is the index made.
     * @return boolean true if the indes is made
     */
    public boolean isMakeIndex() {
        return ((TableViewAdapter) super.getModel()).isMakeIndex();
    }

    /**
     * Sets the indes is made
     * @param pMakeIndex the value
     */
    public void setMakeIndex(boolean pMakeIndex) {
        ((TableViewAdapter) super.getModel()).setMakeIndex(pMakeIndex);
    }

    /**
     * Gets the model
     * @return the model
     */
    public TableViewModel getDataModel() {
        return ((TableViewAdapter) super.getModel()).getModel();
    }

    /**
     * @deprecated use getDataModel()
     * @return the model
     */
    public TableModel getModel() {
        return super.getModel();
    }

    /**
     * Gets the selected column
     * @return the selected column
     */
    /*  public TableViewColumn getSelectedColumnObject()  {
     return ((TableViewAdapter)super.getModel()).getColumn(getSelectedColumn());
     }*/

    /**
     * Gets the selected column
     * @return the selected column
     */
    public TableViewColumn getSelectedColumnObject() {
        return ((TableViewAdapter) super.getModel()).getColumn(getColumnModel().getColumn(super.getSelectedColumn()).getModelIndex());

    }

    /**
     * Gets the selected columns
     * @return the selected columns
     */
    public List getSelectedColumnObjects() {
        List result = new ArrayList();
        int[] indexies = super.getSelectedColumns();
        for (int i = 0; i < indexies.length; i++) {
            result.add(((TableViewAdapter) super.getModel()).getColumn(indexies[i]));
        }
        return result;
    }

    /**
     * @deprecated use getSelectedColumnObject()
     * @return the selected column
     */
    public int getSelectedColumn() {
        return super.getSelectedColumn();
    }

    /**
     * @deprecated use getSelectedColumnObjects()
     * @return the delected columns
     */
    public int[] getSelectedColumns() {
        return super.getSelectedColumns();
    }

    /**
     * Search
     * @param pEvent the event
     */
    private void buttonSearchActionPerformed(ActionEvent pEvent) {
        TableViewAdapter adapter = (TableViewAdapter) super.getModel();
        List selection = getSelectedRowObjects();
        if ("".equals(mTextFieldSearchTest.getText())) {
            adapter.search(mCurrentCollumn, null);
        }
        else {
            adapter.search(mCurrentCollumn, mTextFieldSearchTest.getText());
        }
        for (Iterator i = selection.iterator(); i.hasNext();) {
            addRowSelection(i.next(), true);
        }
        if (getSelectedRowObjects().size() == 0 && super.getModel().getRowCount() > 0) {
            addRowSelectionInterval(0, 0);
            scrollRectToVisible(this.getCellRect(0, 0, true));
        }

        getTableHeader().repaint();
        mDialogSearch.setVisible(false);
        if (mRowSelectCaller!=null)
        	mRowSelectCaller.action();
    }

    /**
     * Hide column.
     * @param pEvent the event
     */
    private void menuItemHideActionPerformed(ActionEvent pEvent) {
        mMenuItemHide.setEnabled(((TableViewAdapter) super.getModel()).setVisible(mCurrentCollumn, false));
        if (mRowSelectCaller!=null)
        	mRowSelectCaller.action();
    }

    /**
     * Search.
     * @param pEvent the event
     */
    private void menuItemSearchActionPerformed(ActionEvent pEvent) {
        mTextFieldSearchTest.setText(((TableViewAdapter) super.getModel()).getSearchText(mCurrentCollumn));
        mTextFieldSearchTest.selectAll();

        if (mDialogSearch == null) {
            initDialogSearch();
        }

        mDialogSearch.pack();
        mDialogSearch.setLocation(mLastMouseClickedPosition);
        mDialogSearch.setVisible(true);
    }

    /**
     * Show visible properties panel.
     * @param pEvent the event
     */
    private void menuItemVisiblityPropertiesActionPerformed(ActionEvent pEvent) {
        final TableViewAdapter modelAdapter = (TableViewAdapter) super.getModel();
        TableViewColumn[] columns = modelAdapter.getModel().getColumns();

        if (mDialogVisiblity == null) {
            initDialogVisiblity();
        }

        mDialogVisiblity.getContentPane().removeAll();
        mGridLayout.setRows(columns.length + 1);

        for (int i = 0; i < columns.length; i++) {
            final TableViewColumn column = columns[i];
            final JCheckBox checkBoxColumn = new JCheckBox(column.getName());
            checkBoxColumn.setSelected(modelAdapter.isVisible(columns[i]));
            mDialogVisiblity.getContentPane().add(checkBoxColumn, null);

            checkBoxColumn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent pEvent) {
                    mMenuItemHide.setEnabled(modelAdapter.setVisible(column, checkBoxColumn.isSelected()));
                    if (mRowSelectCaller!=null)
                        mRowSelectCaller.action();
                }

            });
        }

        mDialogVisiblity.getContentPane().add(mButtonVisiblityOK, null);
        mDialogVisiblity.pack();
        mDialogVisiblity.setLocation(mLastMouseClickedPosition);
        mDialogVisiblity.setVisible(true);
    }

    /**
     * Hide visible properties panel.
     * @param pEvent the event
     */
    private void buttonVisiblityOKActionPerformed(ActionEvent pEvent) {
        mDialogVisiblity.setVisible(false);
        if (mRowSelectCaller!=null)
            mRowSelectCaller.action();
    }

    /**
     * Change auto-risize mode.
     * @param pEvent the event
     */
    private void radioButtonMenuItemAutoResizeColumnNoneActionPerformed(ActionEvent pEvent) {
        setAutoResizeMode(AUTO_RESIZE_OFF);
    }

    /**
     * Change auto-risize mode.
     * @param pEvent the event
     */
    private void radioButtonMenuItemAutoResizeColumnNextActionPerformed(ActionEvent pEvent) {
        setAutoResizeMode(AUTO_RESIZE_NEXT_COLUMN);
    }

    /**
     * Change auto-risize mode.
     * @param pEvent the event
     */
    private void radioButtonMenuItemAutoResizeColumnAllActionPerformed(ActionEvent pEvent) {
        setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
    }

    /**
     * Save settings.
     * @param pEvent the event
     */
    private void menuSaveActionPerformed(ActionEvent pEvent) {
        if (mFileName == null) {
            mFileName = JOptionPane.showInputDialog(null, mRessource.getString("input.message"), 
                    mRessource.getString("input.header"), JOptionPane.QUESTION_MESSAGE);
        }
        if (mFileName.length() > 0) {
            saveSettings(mFileName);
        }
    }

    /**
     * @return the frame this table is embedded in
     */
    private JFrame getFrame() {
        Container parent = getParent();
        while (!(parent instanceof JFrame)) {
            parent = parent.getParent();
        }
        return (JFrame) parent;
    }

    /**
     * init the search dialog
     */
    private void initDialogSearch() {
        mDialogSearch = new JDialog(getFrame());

        mDialogSearch.setVisible(false);
        mDialogSearch.setTitle(mRessource.getString("search.title"));
        mDialogSearch.setResizable(false);
        mDialogSearch.setModal(true);

        Container contentPane = mDialogSearch.getContentPane();

        contentPane.setLayout(new GridBagLayout());

        // creating label
        JLabel jLabelSearchTest = new JLabel();
        jLabelSearchTest.setText(mRessource.getString("search.text"));
        contentPane.add(jLabelSearchTest, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        // creating JTextField jTextFieldSearchTest
        mTextFieldSearchTest.setMinimumSize(new Dimension(100, 21));
        mTextFieldSearchTest.setPreferredSize(new Dimension(100, 21));
        contentPane.add(mTextFieldSearchTest, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        // creating Search Button
        JButton jButtonSearch = new JButton();
        mDialogSearch.getRootPane().setDefaultButton(jButtonSearch);
        jButtonSearch.setText(mRessource.getString("search.btn"));
        jButtonSearch.addActionListener(new ActionListener() {
            /**
             * @param pEvent the event
             */
            public void actionPerformed(ActionEvent pEvent) {
                buttonSearchActionPerformed(pEvent);
            }

        });
        contentPane.add(jButtonSearch, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    }

    /**
     * Init the dialogue visibility.
     */
    private void initDialogVisiblity() {
        mDialogVisiblity = new javax.swing.JDialog(getFrame());

        mGridLayout.setColumns(1);
        mDialogVisiblity.setModal(true);
        mDialogVisiblity.getContentPane().setLayout(mGridLayout);
        mDialogVisiblity.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mDialogVisiblity.setResizable(false);

        mDialogVisiblity.setTitle(mRessource.getString("visible.title"));

        mButtonVisiblityOK.setText(mRessource.getString("btn.ok"));
        mButtonVisiblityOK.addActionListener(new ActionListener() {
            /**
             * @param pEvent
             */
            public void actionPerformed(ActionEvent pEvent) {
                buttonVisiblityOKActionPerformed(pEvent);
            }
        });
    }

    /*  public void smartScrollToSelectedRow() {
     Component parent = getParent();
     System.out.println("parent = " + parent);
     if (parent instanceof JScrollPane) {
     JViewport viewport = ((JScrollPane)parent).getViewport();
     //viewport.getViewRect();
     System.out.println("ViewRect = " + viewport.getViewRect());
     viewport.setViewPosition(getCellRect(super.getSelectedRow(), 0, true).getLocation());
     
     }
     if (parent instanceof JViewport) {
     JViewport viewport = (JViewport)parent;
     System.out.println("ViewRect = " + viewport.getViewRect());
     viewport.setViewPosition(getCellRect(super.getSelectedRow(), 0, true).getLocation());
     
     }
     }*/

    /**
     * Sets the scroll to the selection.
     */
    protected void scrollToSelection() {
        if (super.getSelectedRow() != -1) {
            Rectangle rect = getVisibleRect();
            Rectangle cRect = getCellRect(super.getSelectedRow(), 0, true);

            rect.y = cRect.y;
            rect.height = cRect.height;
            scrollRectToVisible(rect);
        }
    }

    /**
     * The mouse listener
     * @author sbrunner
     * Create on 28 déc. 2004
     */
    private class TableMouseListener extends MouseAdapter {
        /**
         * Invoked when the mouse button has been clicked (pressed and released) on a component.
         * @param pEvent the event
         */
        public void mouseClicked(MouseEvent pEvent) {
            mLastMouseClickedPosition = pEvent.getPoint();
            Point p = pEvent.getComponent().getLocationOnScreen();
            mLastMouseClickedPosition.x += p.getX();
            mLastMouseClickedPosition.y += p.getY();
            TableViewAdapter adapter = (TableViewAdapter) TableView.super.getModel();
            mCurrentCollumn = adapter.getColumn(getColumnModel().getColumn(columnAtPoint(pEvent.getPoint())).getModelIndex());
            if (pEvent.getComponent().getCursor().getType() == Cursor.E_RESIZE_CURSOR) {
                if (pEvent.getClickCount() == 2) {
                    TableColumnModel model = getColumnModel();
                    JTableHeader header = getTableHeader();
                    int columnIndex = -1;
                    int length = Integer.MAX_VALUE;

                    for (int i = 0; i < model.getColumnCount(); i++) {
                        Rectangle rect = header.getHeaderRect(i);
                        int newLength = Math.abs(rect.x + rect.width - pEvent.getX());
                        if (newLength < length) {
                            columnIndex = i;
                            length = newLength;
                        }
                    }

                    autoSizeColumn(columnIndex);
                }
            }
            else if (pEvent.isMetaDown()) {
                mMenuItemSearch.setEnabled(mCurrentCollumn.isSearchable());
                mPopupMenu.pack();
                mPopupMenu.setLocation(mLastMouseClickedPosition);
                mPopupMenu.setVisible(true);
            }
            else if (pEvent.isControlDown() && mCurrentCollumn.isSearchable()) {
                menuItemSearchActionPerformed(null);
            }
            else if (mCurrentCollumn != null && mCurrentCollumn.isSortable()) {
                List selection = getSelectedRowObjects();
                adapter.sort(mCurrentCollumn);
                getTableHeader().repaint();
                addRowSelection(selection);
                if (mRowSelectCaller!=null)
                    mRowSelectCaller.action();
            }
        }
    }

    /**
     * The key listener
     * @author sbrunner
     * Create on 28 déc. 2004
     */
    private class TableKeyListener extends KeyAdapter {

        private static final int RESET_TIME = 300;
        private long mPreviousTime;
        private String mTextToFind;

        /**
         * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
         */
        public void keyTyped(KeyEvent pEvent) {
            char key = pEvent.getKeyChar();
            String strKey = TableViewAdapter.removeSpecialCharacter(new Character(key).toString());
            if (System.currentTimeMillis() - mPreviousTime < RESET_TIME) {
                mTextToFind = mTextToFind + strKey;
            }
            else {
                mTextToFind = strKey;
            }
            mPreviousTime = System.currentTimeMillis();

            for (int i = 1; i < getRowCount(); i++) {
                int row = (i + TableView.super.getSelectedRow()) % getRowCount();
                if (TableViewAdapter.removeSpecialCharacter(getSelectedColumnObject().getValue(
                        ((TableViewAdapter) TableView.super.getModel()).getNewRow(row)).toString()).startsWith(mTextToFind)) {
                    setRowSelectionInterval(row, row);
                    scrollToSelection();
                    i = getRowCount();
                }
            }

        }
    }
}