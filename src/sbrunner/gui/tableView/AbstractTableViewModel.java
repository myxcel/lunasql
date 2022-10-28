package sbrunner.gui.tableView;

import java.io.Serializable;
import java.util.Collection;
import java.util.EventListener;

import javax.swing.event.EventListenerList;

/**
 * This abstract class provides default implementations for most of
 * the methods in the <code>TableViewModel</code> interface.
 *
 * @author Stéphane Brunner, Last modified by: $Author: stbrunner $ 
 * @version $Revision: 1.11 $ $Date: 2006/08/05 13:34:15 $.
 * Revision history:
 * $Log: AbstractTableViewModel.java,v $
 * Revision 1.11  2006/08/05 13:34:15  stbrunner
 * *** empty log message ***
 *
 * Revision 1.10  2004/09/05 19:22:16  stbrunner
 * add select by first letter
 *
 * Revision 1.9  2004/05/31 14:39:14  stbrunner
 * *** empty log message ***
 *
 * Revision 1.8  2004/02/15 19:29:42  stbrunner
 * *** empty log message ***
 *
 * Revision 1.7  2003/06/06 06:31:32  stbrunner
 * Change description
 *
 */
public abstract class AbstractTableViewModel
        implements
            TableViewModel,
            Serializable {

    /**
     * List of listeners
     */
    protected EventListenerList mListenerList = new EventListenerList();

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

    //
    // Managing Listeners
    //

    /**
     * Adds a listener to the list that's notified each time a change
     * to the data model occurs.
     *
     * @param pListner the TableModelListener
     */
    public void addTableViewModelListener(TableViewModelListener pListner) {
        mListenerList.add(TableViewModelListener.class, pListner);
    }

    /**
     * Removes a listener from the list that's notified each time a
     * change to the data model occurs.
     *
     * @param pListner the TableModelListener
     */
    public void removeTableViewModelListener(TableViewModelListener pListner) {
        mListenerList.remove(TableViewModelListener.class, pListner);
    }

    //
    // Fire methods
    //

    /**
     * Notifies all listeners that all cell values in the table's
     * rows may have changed. The number of rows may also have changed
     * and the <code>JTable</code> should redraw the
     * table from scratch. The structure of the table (as in the order of the
     * columns) is assumed to be the same.
     *
     * @see TableModelEvent
     * @see EventListenerList
     */
    protected void fireTableDataChanged() {
        fireTableChanged(new TableViewModelEvent(this));
    }

    /**
     * Notifies all listeners that row have been inserted.
     *
     * @param pRowObjects the rows
     *
     * @see TableModelEvent
     * @see EventListenerList
     */
    protected void fireTableRowsInserted(Collection pRowObjects) {
        fireTableChanged(new TableViewModelEvent(this, pRowObjects,
                TableViewModelEvent.INSERT));
    }

    /**
     * Notifies all listeners that row have been inserted.
     *
     * @param  pRowObject  the row
     *
     * @see TableModelEvent
     * @see EventListenerList
     */
    protected void fireTableRowInserted(Object pRowObject) {
        fireTableChanged(new TableViewModelEvent(this, pRowObject,
                TableViewModelEvent.INSERT));
    }

    /**
     * Notifies all listeners that row have been updated.
     *
     * @param  pRowObjects  the rows
     *
     * @see TableModelEvent
     * @see EventListenerList
     */
    protected void fireTableRowsUpdated(Collection pRowObjects) {
        fireTableChanged(new TableViewModelEvent(this, pRowObjects,
                TableViewModelEvent.UPDATE));
    }

    /**
     * Notifies all listeners that row have been deleted.
     *
     * @param  pRowObjects  the rows
     *
     * @see TableModelEvent
     * @see EventListenerList
     */
    protected void fireTableRowsDeleted(Collection pRowObjects) {
        fireTableChanged(new TableViewModelEvent(this, pRowObjects,
                TableViewModelEvent.DELETE));
    }

    /**
     * Notifies all listeners that row have been deleted.
     *
     * @param  pRowObject  the row
     *
     * @see TableModelEvent
     * @see EventListenerList
     */
    protected void fireTableRowDeleted(Object pRowObject) {
        fireTableChanged(new TableViewModelEvent(this, pRowObject,
                TableViewModelEvent.DELETE));
    }

    /**
     * Notifies all listeners that the value of the row has been updated.
     *
     * @param  pRowObject  the row
     *
     * @see TableModelEvent
     * @see EventListenerList
     */
    protected void fireTableRowUpdated(Object pRowObject) {
        fireTableChanged(new TableViewModelEvent(this, pRowObject,
                TableViewModelEvent.UPDATE));
    }

    /**
     * Forwards the given notification event to all
     * <code>TableModelListeners</code> that registered
     * themselves as listeners for this table model.
     *
     * @param pEvent the event to be forwarded
     *
     * @see #addTableModelListener
     * @see TableModelEvent
     * @see EventListenerList
     */
    protected void fireTableChanged(TableViewModelEvent pEvent) {

        // Guaranteed to return a non-null array
        Object[] listeners = mListenerList.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TableViewModelListener.class) {
                ((TableViewModelListener) listeners[i + 1])
                        .tableViewChanged(pEvent);
            }
        }
    }

    /**
     * Returns an array of all the listeners of the given type that
     * were added to this model.
     *
     * @param pListenerType tistener class type
     * @return all of the objects receiving <code>listenerType</code> notifications from this model
     */
    public EventListener[] getListeners(Class pListenerType) {
        return mListenerList.getListeners(pListenerType);
    }
}