package sbrunner.gui.tableView;

import java.io.Serializable;

import javax.swing.event.EventListenerList;

/**
 * This abstract class provides default implementations for most of
 * the methods in the <code>TableViewColumn</code> interface.
 *
 * @author Stéphane Brunner, Last modified by: $Author: stbrunner $ 
 * @version $Revision: 1.10 $ $Date: 2004/09/05 19:22:16 $.
 * Revision history:
 * $Log: AbstractTableViewColumn.java,v $
 * Revision 1.10  2004/09/05 19:22:16  stbrunner
 * add select by first letter
 *
 * Revision 1.9  2004/05/31 14:39:13  stbrunner
 * *** empty log message ***
 *
 * Revision 1.8  2004/02/15 19:29:42  stbrunner
 * *** empty log message ***
 *
 * Revision 1.7  2003/06/06 06:31:32  stbrunner
 * Change description
 *
 */
public abstract class AbstractTableViewColumn
        implements
            TableViewColumn,
            Serializable {

    //
    // Instance Variables
    //

    /**
     * List of listeners
     */
    protected EventListenerList mListenerList = new EventListenerList();

    //
    // Default Implementation of the Interface
    //

    /**
     * Returns a default name for the column using spreadsheet conventions:
     * A, B, C, ... Z, AA, AB, etc.  If <code>column</code> cannot be found,
     * returns an empty string.
     *
     * @param pColumn  the column being queried
     * @return a string containing the default name of <code>column</code>
     */
    public String getColumnName(int pColumn) {
        String result = "";
        for (; pColumn >= 0; pColumn = pColumn / 26 - 1) {
            result = (char) ((char) (pColumn % 26) + 'A') + result;
        }
        return result;
    }

    /**
     * Returns <code>Object.class</code> regardless of <code>columnIndex</code>.
     *
     * @return the Object.class
     */
    public Class getColumnClass() {
        return Object.class;
    }

    /**
     * Returns false.  This is the default implementation for all cells.
     *
     * @param  pRowObject  the row being queried
     * @return false
     */
    public boolean isCellEditable(Object pRowObject) {
        return false;
    }

    /**
     * This empty implementation is provided so users don't have to implement
     * this method if their data model is not editable.
     *
     * @param  pValue   value to assign to cell
     * @param  pRowIndex   row of cell
     * @param  pColumnIndex  column of cell
     */
    public void setValueAt(Object pValue, int pRowIndex, int pColumnIndex) {
    }

    /**
     * The sortable status.
     *
     * @return true
     */
    public boolean isSortable() {
        return true;
    }

    /**
     * The searchable status.
     *
     * @return true
     */
    public boolean isSearchable() {
        return true;
    }
}