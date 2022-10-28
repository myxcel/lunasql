package sbrunner.gui.tableView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

/**
 * Adapte <code>TableViewModel</code> to <code>TableModel</code>.
 *
 * @author St�phane Brunner, Last modified by: $Author: stbrunner $ 
 * @version $Revision: 1.17 $ $Date: 2006/08/05 13:19:16 $.
 * Revision history:
 * $Log: TableViewAdapter.java,v $
 * Revision 1.17  2006/08/05 13:19:16  stbrunner
 * *** empty log message ***
 *
 * Revision 1.16  2004/12/28 15:04:37  stbrunner
 * add comment
 *
 * Revision 1.15  2004/09/05 19:22:16  stbrunner
 * add select by first letter
 *
 * Revision 1.14  2004/05/31 17:19:08  stbrunner
 * *** empty log message ***
 *
 * Revision 1.13  2004/05/31 14:39:13  stbrunner
 * *** empty log message ***
 *
 * Revision 1.12  2004/02/15 19:29:42  stbrunner
 * *** empty log message ***
 *
 * Revision 1.11  2003/06/06 06:31:32  stbrunner
 * Change description
 *
 */
public class TableViewAdapter extends AbstractTableModel
        implements
            TableViewModelListener {
    private TableViewModel mModel;

    private Map mSearch = new HashMap();
    private List mViewRow = new ArrayList();

    private TableViewColumn mSortColumn = null;
    private boolean mAsendent = true;

    private List mVisibleColumn = new ArrayList();

    private boolean mMakeIndex = false;
    private Map mColumnToIndexes;

    /**
     * Construct.
     *
     * @param pModel model to adapte
     */
    public TableViewAdapter(TableViewModel pModel) {
        pModel.addTableViewModelListener(this);
        mModel = pModel;

        TableViewColumn[] columns = mModel.getColumns();
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].isDefaultVisible()) {
                mVisibleColumn.add(columns[i]);
            }
        }

        search();
    }

    /**
     * Sets the column visible
     * @param pColumn the column
     * @param pVisible to visible value
     * @return boolean
     */
    public boolean setVisible(TableViewColumn pColumn, boolean pVisible) {
        if (pVisible) {
            mVisibleColumn.add(pColumn);
        }
        else {
            mVisibleColumn.remove(pColumn);
        }

        fireTableStructureChanged();

        return mVisibleColumn.size() > 1;
    }

    /**
     * Gets the display value for the search
     * @param pSearchText the search test
     * @return String the display value
     */
    private final String getDisplaySearch(String pSearchText) {
        boolean first = true;
        StringBuffer result = new StringBuffer();

        for (Iterator i = getStrings(pSearchText).iterator(); i.hasNext();) {
            if (first) {
                first = false;
            }
            else {
                result.append(" ");
            }
            result.append((String) i.next());
        }

        return result.toString();
    }

    /**
     * Search
     * @param pColumn the column to search
     * @param pText the text to search
     */
    public void search(TableViewColumn pColumn, String pText) {
        pText = pText == null ? null : getDisplaySearch(pText);
        mSearch.put(pColumn, pText);
        search();
        sort();
    }

    /**
     * Remove all the maj and accents
     * @param pSourceText the source
     * @return the source without accents
     */
    public static String removeSpecialCharacter(String pSourceText) {

        pSourceText = pSourceText.trim().toLowerCase();
        pSourceText = pSourceText.replace('á', 'a');
        pSourceText = pSourceText.replace('à', 'a');
        pSourceText = pSourceText.replace('â', 'a');
        pSourceText = pSourceText.replace('ä', 'a');
        pSourceText = pSourceText.replace('é', 'e');
        pSourceText = pSourceText.replace('è', 'e');
        pSourceText = pSourceText.replace('ê', 'e');
        pSourceText = pSourceText.replace('ë', 'e');
        pSourceText = pSourceText.replace('í', 'i');
        pSourceText = pSourceText.replace('ì', 'i');
        pSourceText = pSourceText.replace('î', 'i');
        pSourceText = pSourceText.replace('ï', 'i');
        pSourceText = pSourceText.replace('ó', 'o');
        pSourceText = pSourceText.replace('ò', 'o');
        pSourceText = pSourceText.replace('ô', 'o');
        pSourceText = pSourceText.replace('ö', 'o');
        pSourceText = pSourceText.replace('ú', 'u');
        pSourceText = pSourceText.replace('ù', 'u');
        pSourceText = pSourceText.replace('û', 'u');
        pSourceText = pSourceText.replace('ü', 'u');
        pSourceText = pSourceText.replace('ý', 'y');
        pSourceText = pSourceText.replace('ÿ', 'y');
        pSourceText = pSourceText.replace('ç', 'c');
        pSourceText = replace(pSourceText, "œ", "oe");
        pSourceText = replace(pSourceText, "æ", "ae");

        return pSourceText;
    }

    /**
     * Gets the string to search from a user text.
     * @param pSearchText the user text
     * @return Collection the strings to search
     */
    private Collection getStrings(String pSearchText) {
        ArrayList result = new ArrayList();

        pSearchText = removeSpecialCharacter(pSearchText);

        if (mMakeIndex) {
            for (StringTokenizer tokenizer = new StringTokenizer(pSearchText,
                    " ,.;:?!&+-/\\=<>(){}\"'"); tokenizer.hasMoreTokens();) {
                String value = tokenizer.nextToken();
                if (value.endsWith("s")) {
                    value = value.substring(0, value.length() - 1);
                }

                if (value.length() >= 3) {
                    result.add(value);
                }
            }
        }
        else {
            result.add(pSearchText);
        }

        return result;
    }

    /**
     * Search
     */
    private void search() {
        mViewRow.clear();
        if (mMakeIndex) {
            boolean isSearch = false;

            for (Iterator i = mVisibleColumn.iterator(); i.hasNext();) {
                TableViewColumn column = (TableViewColumn) i.next();
                String searchText = getSearchText(column);
                if (searchText != null) {
                    isSearch = true;

                    Map indexes = (Map) mColumnToIndexes.get(column);
                    if (indexes == null) {
                        makeIndex(column);
                        indexes = (Map) mColumnToIndexes.get(column);
                    }
                    for (Iterator j = getStrings(searchText).iterator(); j
                            .hasNext();) {
                        Set toAdd = (Set) indexes.get(j.next());
                        if (toAdd != null) {
                            Collection newToAdd = new ArrayList();
                            newToAdd.addAll(toAdd);
                            newToAdd.removeAll(mViewRow);
                            mViewRow.addAll(newToAdd);
                        }
                    }
                }
            }

            if (!isSearch) {
                for (int i = 0; i < mModel.getRowCount(); i++) {
                    mViewRow.add(mModel.getRowObject(i));
                }
            }

        }
        else {
            String[] searchText = new String[mVisibleColumn.size()];
            for (int i = 0; i < searchText.length; i++) {
                searchText[i] = getSearchText((TableViewColumn) mVisibleColumn
                        .get(i));
            }

            for (int i = 0; i < mModel.getRowCount(); i++) {
                Object rowObject = mModel.getRowObject(i);
                boolean toAdd = true;
                for (int j = 0; j < searchText.length; j++) {
                    if (searchText[j] != null
                            && String.valueOf(
                                    ((TableViewColumn) mVisibleColumn.get(j))
                                            .getValue(rowObject)).toLowerCase()
                                    .indexOf(searchText[j]) < 0) {
                        toAdd = false;
                        break;
                    }
                }
                if (toAdd) {
                    mViewRow.add(rowObject);
                }
            }
        }
        fireTableDataChanged();
    }

    /**
     * Gets the search text
     * @param pColumn the colimn to query
     * @return String the string to search
     */
    public String getSearchText(TableViewColumn pColumn) {
        return (String) mSearch.get(pColumn);
    }

    /**
     * Sort
     * @param pColumn the column to sort
     */
    public void sort(TableViewColumn pColumn) {
        mAsendent = mSortColumn == pColumn ? !mAsendent : true;
        mSortColumn = pColumn;
        sort();
        fireTableDataChanged();
    }

    /**
     * Sort
     */
    private void sort() {
        try {
            if (mSortColumn != null) {
                java.util.Collections.sort(mViewRow, new Comparator() {
                    /**
                     * Compares its two arguments for order.
                     *
                     * @param pO1 the first object to be compared.
                     * @param pO2 the second object to be compared.
                     * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to,
                     *    or greater than the second.
                     */
                    public int compare(Object pO1, Object pO2) {
                        return (mAsendent ? 1 : -1)
                                * mSortColumn.getComparator().compare(pO1, pO2);
                    }
                });
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        fireTableDataChanged();
    }

    /**
     * Convert a model index to row Object.
     *
     * @param pRowIndex the table model index
     * @return the row Object
     */
    public Object getNewRow(int pRowIndex) {
        if (pRowIndex < 0) {
            return null;
        }

        try {
            Object cont = mViewRow.size() <= pRowIndex ? null : mViewRow
                    .get(pRowIndex);

            return cont != null ? cont : (mViewRow.size() <= pRowIndex
                    ? null
                    : mViewRow.get(pRowIndex));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert row Object to index.
     *
     * @param pRow the row Object
     * @return the index of the row
     */
    public int indexOf(Object pRow) {
        return mViewRow.indexOf(pRow);
    }

    /**
     * Get the number of column.
     *
     * @return the column cont
     */
    public int getRowCount() {
        return mViewRow.size();
    }

    /**
     * Get the table view column from table model index.
     *
     * @param pVisibleIndex table model index
     * @return the table view column
     */
    public TableViewColumn getColumn(int pVisibleIndex) {
        return pVisibleIndex < 0 ? null : (TableViewColumn) mVisibleColumn
                .get(pVisibleIndex);
    }

    /**
     * Returns the number of column
     *
     * @return the column cont
     */
    public int getColumnCount() {
        return mVisibleColumn.size();
    }

    /**
     * Returns the name of the column
     *
     * @param pColumnIndex the table model column index
     * @return the name of the column
     */
    public String getColumnName(int pColumnIndex) {
        TableViewColumn column = (TableViewColumn) mVisibleColumn
                .get(pColumnIndex);
        return column.getName();
    }

    /**
     * Returns the column class
     *
     * @param pColumnIndex the table model column index
     * @return the column class
     */
    public Class getColumnClass(int pColumnIndex) {
        return ((TableViewColumn) mVisibleColumn.get(pColumnIndex))
                .getColumnClass();
    }

    /**
     * Return the editable status.
     *
     * @param pRowIndex the table model row index
     * @param pColumnIndex the table model column index
     * @return true if it is editable.
     */
    public boolean isCellEditable(int pRowIndex, int pColumnIndex) {
        return ((TableViewColumn) mVisibleColumn.get(pColumnIndex))
                .isCellEditable(getNewRow(pRowIndex));
    }

    /**
     * Returns the cell value.
     *
     * @param pRowIndex the table model row index
     * @param pColumnIndex the table model column index
     * @return the visible value
     */
    public Object getValueAt(int pRowIndex, int pColumnIndex) {
        try {
            Object cont = mViewRow.size() <= pRowIndex ? null : mViewRow
                    .get(pRowIndex);
            return ((TableViewColumn) mVisibleColumn.get(pColumnIndex))
                    .getValue(cont != null ? cont : getNewRow(pRowIndex));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Set a sell value.
     *
     * @param pValue the new value
     * @param pRowIndex the table model row index
     * @param pColumnIndex the table model column index
     */
    public void setValueAt(Object pValue, int pRowIndex, int pColumnIndex) {
        ((TableViewColumn) mVisibleColumn.get(pColumnIndex)).setValue(
                getNewRow(pRowIndex), pValue);
    }

    /**
     * Redirect tTable view model changes to table model changes.
     *
     * @param pEvent the event
     */
    public void tableViewChanged(TableViewModelEvent pEvent) {
        if (pEvent.getType() == TableModelEvent.INSERT) {
            if (mMakeIndex) {
                for (Iterator icol = mColumnToIndexes.keySet().iterator(); icol
                        .hasNext();) {
                    TableViewColumn column = (TableViewColumn) icol.next();

                    Map indexes = (Map) mColumnToIndexes.get(column);

                    for (Iterator i = pEvent.getRowObjects().iterator(); i
                            .hasNext();) {
                        Object row = i.next();

                        for (Iterator j = getStrings(
                                String.valueOf(column.getValue(row)))
                                .iterator(); i.hasNext();) {
                            String next = (String) j.next();
                            for (int k = 3; k < next.length(); k++) {
                                String key = next.substring(k);
                                Set rows = (Set) indexes.get(key);
                                if (rows == null) {
                                    rows = new HashSet();
                                    indexes.put(key, rows);
                                }
                                rows.add(row);
                            }
                        }
                    }
                }
            }
            mViewRow.addAll(pEvent.getRowObjects());
        }
        else if (pEvent.getType() == TableModelEvent.DELETE) {
            if (mMakeIndex) {
                for (Iterator icol = mColumnToIndexes.keySet().iterator(); icol
                        .hasNext();) {
                    TableViewColumn column = (TableViewColumn) icol.next();

                    Map indexes = (Map) mColumnToIndexes.get(column);

                    for (Iterator i = indexes.values().iterator(); i.hasNext();) {
                        Set rows = (Set) i.next();
                        rows.removeAll(pEvent.getRowObjects());
                    }
                }
            }
            mViewRow.removeAll(pEvent.getRowObjects());
        }
        if (pEvent.getType() == TableModelEvent.UPDATE && pEvent.getRowObjects() != null) {
            fireTableRowsUpdated(0, getRowCount());
        }
        else {
            search();
//            fireTableDataChanged();
        }
    }

    /** 
     * Gets all the string to search fom a word
     * @param pText the worf
     * @return Collection the string to search
     */
    private static Collection getAllSubstring(String pText) {
        ArrayList result = new ArrayList();

        for (int i = 0; i < pText.length() - 2; i++) {
            for (int j = i + 3; j <= pText.length(); j++) {
                result.add(pText.substring(i, j));
            }
        }

        return result;
    }

    /**
     * Make the index of a column
     * @param pColumn the column
     */
    private void makeIndex(TableViewColumn pColumn) {
        Map indexes = (Map) mColumnToIndexes.get(pColumn);
        if (indexes == null) {
            indexes = new HashMap();
            mColumnToIndexes.put(pColumn, indexes);
        }

        int rowCount = mModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            Object row = mModel.getRowObject(i);
            for (Iterator j = getStrings(String.valueOf(pColumn.getValue(row)))
                    .iterator(); j.hasNext();) {
                String next = (String) j.next();
                for (Iterator k = getAllSubstring(next).iterator(); k.hasNext();) {
                    String key = (String) k.next();
                    Set rows = (Set) indexes.get(key);
                    if (rows == null) {
                        rows = new HashSet();
                        indexes.put(key, rows);
                    }
                    rows.add(row);
                }
            }
        }
    }

    /**
     * Get the model to be adapted
     *
     * @return the table vie model
     */
    public TableViewModel getModel() {
        return mModel;
    }

    /**
     * Returns the visiblity of a column
     *
     * @param pColumn the table view column
     * @return true it it is visible
     */
    public boolean isVisible(TableViewColumn pColumn) {
        return mVisibleColumn.contains(pColumn);
    }

    /**
     * Returns the index if the sorted column.
     *
     * @return the index if the sorted column
     */
    public int getSortColumnIndex() {
        return mVisibleColumn.indexOf(mSortColumn);
    }

    /**
     * Returns the sorted direction
     *
     * @return true if it is asending
     */
    public boolean isAsendent() {
        return mAsendent;
    }

    /**
     * Returns the text to search.
     *
     * @param pColumn table model column index
     * @return the text to search
     */
    public String getSearchText(int pColumn) {
        return getSearchText((TableViewColumn) mVisibleColumn.get(pColumn));
    }

    /**
     * Is the index made
     * @return boolean index is made
     */
    public boolean isMakeIndex() {
        return mMakeIndex;
    }

    /**
     * Sets the index is made
     * @param pMakeIndex the index is made
     */
    public void setMakeIndex(boolean pMakeIndex) {
        this.mMakeIndex = pMakeIndex;
        if (pMakeIndex) {
            mColumnToIndexes = new HashMap();
        }
    }

    /**
     * Replave in a string
     * @param pSource the source
     * @param pReplace the searche sub string
     * @param pWith the replace with sub string
     * @return the result
     */
    public static String replace(String pSource, String pReplace, String pWith) {
        if (pSource == null) {
            return "";
        }

        String strCompare = pSource;

        if (pSource == null || pReplace == null || pReplace.equals("")
                || pWith == null) {
            return pSource;
        }

        StringBuffer target = new StringBuffer();
        int indexSource = 0;

        while (true) {
            int index = -1;
            int length = 0;
            int tmpIndex = strCompare.indexOf(pReplace, indexSource);
            if (tmpIndex >= 0 && (index < 0 || index > tmpIndex)) {
                length = pReplace.length();
                index = tmpIndex;
            }

            if (index < 0) {
                target.append(pSource.substring(indexSource));
                return target.toString();
            }

            target.append(pSource.substring(indexSource, index));
            target.append(pWith);
            indexSource = index + length;
        }
    }
}