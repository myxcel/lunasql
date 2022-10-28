package sbrunner.gui.tableView;

import java.util.Comparator;

/**
 * Interface Of a colimn in a TableViewModel
 *
 * @author Stéphane Brunner, Last modified by: $Author: stbrunner $ 
 * @version $Revision: 1.12 $ $Date: 2006/08/05 13:35:15 $.
 * Revision history:
 * $Log: TableViewColumn.java,v $
 * Revision 1.12  2006/08/05 13:35:15  stbrunner
 * *** empty log message ***
 *
 * Revision 1.11  2004/09/05 19:22:16  stbrunner
 * add select by first letter
 *
 * Revision 1.10  2004/05/31 14:39:13  stbrunner
 * *** empty log message ***
 *
 * Revision 1.9  2004/02/15 19:29:42  stbrunner
 * *** empty log message ***
 *
 * Revision 1.8  2003/06/06 06:31:32  stbrunner
 * Change description
 *
 */
public interface TableViewColumn {
    /**
     * Returns the number of columns in the model. A
     * <code>JTable</code> uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     */
    public Comparator getComparator();

    /**
     * Returns the name of the column. This is used
     * to initialize the table's column header name.  Note: this name does
     * not need to be unique; two columns in a table can have the same name.
     *
     * @return  the name of the column
     */
    public String getName();

    /**
     * Returns the most specific superclass for all the cell values
     * in the column.  This is used by the <code>JTable</code> to set up a
     * default renderer and editor for the column.
     *
     * @return the common ancestor class of the object values in the model.
     */
    public Class getColumnClass();

    /**
     * Returns true if the cell is editable.
     * Otherwise, <code>setValueAt</code> on the cell will not
     * change the value of that cell.
     *
     * @param pRowObject the row whose value to be queried
     * @return true if the cell is editable
     * @see #setValueAt
     */
    public boolean isCellEditable(Object pRowObject);

    /**
     * Returns the value for the cell.
     *
     * @param pRowObject the row whose value is to be queried
     * @return the value Object at the specified cell
     */
    public Object getValue(Object pRowObject);

    /**
     * Sets the value in the cell to <code>aValue</code>.
     *
     * @param pValue the new value
     * @param pRowObject the row whose value is to be changed
     * @see #getValueAt
     * @see #isCellEditable
     */
    public void setValue(Object pValue, Object pRowObject);

    /**
     * Return the sortable status.
     *
     * @return true if it is sortable
     */
    public boolean isSortable();

    /**
     * Return the searchable status.
     *
     * @return true if it is searchable
     */
    public boolean isSearchable();

    /**
     * Return the default visibility status
     *
     * @return truen if it is visible as default
     */
    public boolean isDefaultVisible();
}