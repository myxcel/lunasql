package sbrunner.gui.tableView;

/**
 * Represent the model interfance of a TableView
 *
 * @author Stéphane Brunner, Last modified by: $Author: stbrunner $ 
 * @version $Revision: 1.11 $ $Date: 2006/08/05 13:37:06 $.
 * Revision history:
 * $Log: TableViewModel.java,v $
 * Revision 1.11  2006/08/05 13:37:06  stbrunner
 * *** empty log message ***
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
public interface TableViewModel {
    /**
     * Gets the columns
     * @return TableViewColumn[] the columns
     */
    public TableViewColumn[] getColumns();

    /**
     * Returns the number of rows.
     * @return the number of rows
     */
    public int getRowCount();

    /**
     * Returns the Object represent the row.
     * @param pRowIndex  the row whose value is to be queried
     * @return the Object represent the row
     */
    public Object getRowObject(int pRowIndex);

    /**
     * Adds a listener to the list that is notified each time a change
     * to the data model occurs.
     * @param pListener the TableModelListener
     */
    public void addTableViewModelListener(TableViewModelListener pListener);

    /**
     * Removes a listener from the list that is notified each time a
     * change to the data model occurs.
     * @param pListener the TableModelListener
     */
    public void removeTableViewModelListener(TableViewModelListener pListener);
}