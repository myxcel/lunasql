package sbrunner.gui.tableView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This abstract class provides default implementations the <code>TableViewModel</code> interface.
 *
 * @author St�phane Brunner, Last modified by: $Author: stbrunner $ 
 * @version $Revision: 1.11 $ $Date: 2004/12/28 14:56:48 $.
 * Revision history:
 * $Log: DefaultTableViewModel.java,v $
 * Revision 1.11  2004/12/28 14:56:48  stbrunner
 * fix in addRows
 *
 * Revision 1.10  2004/09/05 19:22:16  stbrunner
 * add select by first letter
 *
 * Revision 1.9  2004/05/31 14:39:14  stbrunner
 * *** empty log message ***
 *
 * Revision 1.8  2004/02/15 19:29:43  stbrunner
 * *** empty log message ***
 *
 * Revision 1.7  2003/06/06 06:31:32  stbrunner
 * Change description
 *
 */
public class DefaultTableViewModel extends AbstractTableViewModel {

    private List mContent = new ArrayList();
    private TableViewColumn[] mColumn;

    /**
     * Construct.
     * @see java.lang.Object#Object()
     */
    public DefaultTableViewModel() {
        this((Collection) null);
    }

    /**
     * Construct.
     * @param pColumns all the collumns
     */
    public DefaultTableViewModel(TableViewColumn[] pColumns) {
        this(pColumns, null);
    }

    /**
     * Construct.
     * @param pContent the rows
     */
    public DefaultTableViewModel(Collection pContent) {
        this(null, pContent);
    }

    /**
     * Construct.
     * @param pColumns all the collumns
     * @param pContent the rows
     */
    public DefaultTableViewModel(TableViewColumn[] pColumns, Collection pContent) {
        if (pContent != null) {
            mContent.addAll(pContent);
        }
        mColumn = pColumns == null
                ? new TableViewColumn[]{new DefaultTableViewColumn("Name")}
                : pColumns;
    }

    /**
     * Gets the numur o
     * Returns the number of rows.
     * @return the number of rows
     * @see sbrunner.gui.tableView.TableViewModel#getRowCount()
     */
    public int getRowCount() {
        return mContent.size();
    }

    /**
     * Returns the Object represent the row.
     * @param pRowIndex  the row whose value is to be queried
     * @return the Object represent the row
     * @see sbrunner.gui.tableView.TableViewModel#getRowObject(int)
     */
    public Object getRowObject(int pRowIndex) {
        return mContent.get(pRowIndex);
    }

    /**
     * Gets the columns
     * @return TableViewColumn[] the columns
     * @see sbrunner.gui.tableView.TableViewModel#getColumns()
     */
    public TableViewColumn[] getColumns() {
        return mColumn;
    }

    /**
     * Adds a row
     * @param pRowObject the new row
     */
    public void addRow(Object pRowObject) {
        mContent.add(pRowObject);
        fireTableRowInserted(pRowObject);
    }

    /**
     * Adds some rows
     * @param pRowObjects the rows object
     */
    public void addRows(Collection pRowObjects) {
        mContent.addAll(pRowObjects);
        fireTableRowsInserted(pRowObjects);
    }

    /**
     * Removes a row.
     * @param pRowObject the row to remove
     */
    public void removeRow(Object pRowObject) {
        mContent.remove(pRowObject);
        fireTableRowDeleted(pRowObject);
    }

    /** 
     * Removes some rows.
     * @param pRowObjects The rows to removes
     */
    public void removeRows(Collection pRowObjects) {
        mContent.removeAll(pRowObjects);
        fireTableRowsDeleted(pRowObjects);
    }

    /**
     * Updates a row.
     * @param pRowObject the row to update
     */
    public void updateRow(Object pRowObject) {
        fireTableRowUpdated(pRowObject);
    }

    /**
     * Updates some rows.
     * @param pRowObjects the rows to update
     */
    public void updateRows(Collection pRowObjects) {
        fireTableRowsUpdated(pRowObjects);
    }

    /**
     * Update row by a new object.
     * @param pOldRowObject the old row object
     * @param pNewRowObject the new row object
     */
    public void updateRow(Object pOldRowObject, Object pNewRowObject) {
        removeRow(pOldRowObject);
        addRow(pNewRowObject);
    }
}