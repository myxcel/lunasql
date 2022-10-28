package sbrunner.gui.tableView;

/**
 * TableViewModelListener defines the interface for an object that listens
 * to changes in a TableViewModel.
 *
 * @author St�phane Brunner, Last modified by: $Author: stbrunner $ 
 * @version $Revision: 1.10 $ $Date: 2004/09/05 19:22:16 $.
 * Revision history:
 * $Log: TableViewModelListener.java,v $
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
public interface TableViewModelListener extends java.util.EventListener {
    /**
     * This fine grain notification tells listeners the exact range
     * of cells, rows, or columns that changed.
     *
     * @param pEvent the event
     */
    public void tableViewChanged(TableViewModelEvent pEvent);
}