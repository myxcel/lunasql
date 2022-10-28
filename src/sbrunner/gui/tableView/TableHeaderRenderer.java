package sbrunner.gui.tableView;

import java.awt.*;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * This class is the header renderer for the TableView.
 * This renderer allows the display of the sort order and
 * the good alignment of the column title and the sort icon.
 * It renderer as the rearch status.
 *
 * @author St�phane Brunner, Last modified by: $Author: stbrunner $ 
 * @version $Revision: 1.10 $ $Date: 2004/09/05 19:22:16 $.
 * Revision history:
 * $Log: TableHeaderRenderer.java,v $
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
public class TableHeaderRenderer extends DefaultTableCellRenderer {
    private Icon mAscending = new AscendingIcon();
    private Icon mDescending = new DescendingIcon();
    private Icon mNull = new NullIcon();

    /**
     * Constructs the header with the good up and down icon
     */
    public TableHeaderRenderer() {

        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        setHorizontalTextPosition(JLabel.LEFT);
        setHorizontalAlignment(JLabel.CENTER);
        setBorder(new javax.swing.border.CompoundBorder(getBorder(),
                new javax.swing.border.EmptyBorder(0, mNull.getIconWidth(), 0,
                        0)));
    }

    /**
     * Returns the graphical component of the renderer.
     * @param pTable the JTable whose cells are being rendered.
     * @param pValue the object occupying the cell
     * @param pSelected true if the cell is selected
     * @param pHasFocus true if the cell has focus
     * @param pRow the row number of the cell
     * @param pColumn the column number of the cell
     * @return Component
     */
    public Component getTableCellRendererComponent(JTable pTable,
            Object pValue, boolean pSelected, boolean pHasFocus, int pRow,
            int pColumn) {
        pColumn = pTable.getColumnModel().getColumn(pColumn).getModelIndex();

        TableViewAdapter model = (TableViewAdapter) pTable.getModel();

        String search = model.getSearchText(pColumn);

        if (pTable != null) {
            JTableHeader header = pTable.getTableHeader();
            if (header != null) {
                setForeground(header.getForeground());
                setBackground(header.getBackground());
                //            setFont(header.getFont());
                Font font = new Font(header.getFont().getFontName(), Font.BOLD,
                        header.getFont().getSize());
                setFont(font);
            }
        }

        setText(pValue + (search == null ? "" : " (" + search + ")"));

        if (pColumn != model.getSortColumnIndex()) {
            setIcon(mNull);
        }
        else if (model.isAsendent()) {
            setIcon(mAscending);
        }
        else {
            setIcon(mDescending);
        }

        return this;
    }

    /**
     * En empty icon with the same size.
     */
    public class NullIcon implements Icon {
        /**
         * Returns the icon's height.
         * @return the icon height
         */
        public int getIconHeight() {
            return 6;
        }

        /**
         * Returns the icon's width.
         * @return the icon width
         */
        public int getIconWidth() {
            return 6;
        }

        /**
         * Draw the icon at the specified location. Icon implementations may use the Component argument to get properties
         * useful for painting, e.g. the foreground or background color.
         * @see javax.swing.Icon#paintIcon(Component, Graphics, int, int)
         * @param pComponent the component
         * @param pGraphics the graphics
         * @param pX x position
         * @param pY y position
         */
        public void paintIcon(Component pComponent, Graphics pGraphics, int pX,
                int pY) {
        }
    }

    /**
     * The dessanding icon
     */
    public class DescendingIcon extends NullIcon {
        /**
         * Draw the icon at the specified location. Icon implementations may use the Component argument to get properties
         * useful for painting, e.g. the foreground or background color.
         * @param pComponent the component
         * @param pGraphics the graphics
         * @param pX x position
         * @param pY y position
         */
        public void paintIcon(Component pComponent, Graphics pGraphics, int pX,
                int pY) {
            int[] xx = {pX, pX + 3, pX + 6};
            int[] yy = {pY, pY + 6, pY + 0};

            Color initialColor = pGraphics.getColor();
            float[] initial = Color.RGBtoHSB(initialColor.getRed(),
                    initialColor.getGreen(), initialColor.getBlue(), null);

            boolean need = initial[2] < .5f;
            initial[2] = invertAsNeed(initial[2], need);

            Color line = Color.getHSBColor(initial[0], initial[1],
                    invertAsNeed(initial[2] * .7f, need));
            Color fill = Color.getHSBColor(initial[0], initial[1],
                    invertAsNeed(initial[2] * .35f, need));
            pGraphics.setColor(fill);
            pGraphics.fillPolygon(xx, yy, 3);
            pGraphics.setColor(line);
            pGraphics.drawPolygon(xx, yy, 3);
        }

    }

    /**
     * The assending icon.
     */
    public class AscendingIcon extends NullIcon {
        /**
         * Draw the icon at the specified location. Icon implementations may use the Component argument to get properties
         * useful for painting, e.g. the foreground or background color.
         * @param pComponent the component
         * @param pGraphics the graphics
         * @param pX x position
         * @param pY y position
         */
        public void paintIcon(Component pComponent, Graphics pGraphics, int pX,
                int pY) {
            int[] xx = {pX, pX + 3, pX + 6};
            int[] yy = {pY + 6, pY, pY + 6};

            Color initialColor = pGraphics.getColor();
            float[] initial = Color.RGBtoHSB(initialColor.getRed(),
                    initialColor.getGreen(), initialColor.getBlue(), null);

            boolean need = initial[2] < .5f;
            initial[2] = invertAsNeed(initial[2], need);

            Color line = Color.getHSBColor(initial[0], initial[1],
                    invertAsNeed(initial[2] * .7f, need));
            Color fill = Color.getHSBColor(initial[0], initial[1],
                    invertAsNeed(initial[2] * .35f, need));
            pGraphics.setColor(fill);
            pGraphics.fillPolygon(xx, yy, 3);
            pGraphics.setColor(line);
            pGraphics.drawPolygon(xx, yy, 3);
        }
    }

    /**
     * Invert the bright value as nead
     * @param pBright the bright value
     * @param pNeed is nead
     * @return float the new brighr value
     */
    private float invertAsNeed(float pBright, boolean pNeed) {
        return pNeed ? 1 - pBright : pBright;
    }
}