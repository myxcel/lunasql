/*
 * UndoableJTextManager.java
 *
 * Copyright (C) 2006  Yann D'ISANTO
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package lunasql.ui.undo;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

/**
 * <code>UndoableJTextManager</code> provides methods to implement undo and 
 * redo on <code>JTextComponent</code>s using <code>UndoMapping</code>.
 * 
 * @author Yann D'ISANTO
 * @version 1.0.1 11/01/2007
 * @see UndoMapping
 */
public class UndoableJTextManager {
    
    /**
     * The Default redo <code>KeyStroke</code> builded with the 
     * <code>KeyEvent.VK_Y</code> key code and the 
     * <code>InputEvent.CTRL_MASK modifier</code>.
     */
    public static final KeyStroke DEFAULT_REDO_KEYSTROKE = 
            KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK);
    
    
    /**
     * The Default undo <code>KeyStroke</code> builded with the 
     * <code>KeyEvent.VK_Z</code> key code and the 
     * <code>InputEvent.CTRL_MASK modifier</code>.
     */
    public static final KeyStroke DEFAULT_UNDO_KEYSTROKE = 
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK);
    
    private static UndoableJTextManager undoableJTextComponentManager;
    static {
        undoableJTextComponentManager = new UndoableJTextManager();
    }
    
    private Map map;
    
    private UndoableJTextManager() {
        map = new HashMap();
    }
    
    /**
     * Returns the redo <code>Action</code> associated with the specified 
     * <code>JTextComponent</code>.
     * @return the redo <code>Action</code> associated with the specified 
     * <code>JTextComponent</code>. 
     * (null if the specified <code>JTextComponent</code> is not undoable).
     */
    public Action getRedoAction(JTextComponent component) {
        return ((UndoMapping) map.get(component)).getRedoAction();
    }
    
    /**
     * Returns the <code>UndoableJTextManager</code> object associated 
     * with the current Java application.
     * 
     * @return the <code>UndoableJTextManager</code> object associated 
     * with the current Java application.
     */
    public static UndoableJTextManager getUndoableJTextManager() {
        return undoableJTextComponentManager;
    }
    
    /**
     * Returns the undo <code>Action</code> associated with the specified 
     * <code>JTextComponent</code>.
     * @return the undo <code>Action</code> associated with the specified 
     * <code>JTextComponent</code>. 
     * (null if the specified <code>JTextComponent</code> is not undoable).
     */
    public Action getUndoAction(JTextComponent component) {
        return ((UndoMapping) map.get(component)).getUndoAction();
    }
    
    /**
     * Returns the <code>UndoManager</code> associated with the specified 
     * <code>JTextComponent</code>.
     * @return the <code>UndoManager</code> associated with the specified 
     * <code>JTextComponent</code>. 
     * (null if the specified <code>JTextComponent</code> is not undoable).
     */
    public UndoManager getUndoManager(JTextComponent component) {
        return ((UndoMapping) map.get(component)).getUndoManager();
    }
    
    /**
     * Returns the <code>UndoMapping</code> associated with the specified 
     * <code>JTextComponent</code>.
     * 
     * @return the <code>UndoMapping</code> associated with the specified 
     * <code>JTextComponent</code>. 
     * (null if the specified <code>JTextComponent</code> is not undoable).
     */
    public UndoMapping getUndoMapping(JTextComponent component) {
        return (UndoMapping) map.get(component);
    }
    /**
     * Returns true if the specified <code>JTextComponent</code> is undoable.
     * @return true if the specified <code>JTextComponent</code> is undoable.
     */
    public boolean isUndoable(JTextComponent component) {
        return map.containsKey(component);
    }
    
    /**
     * Makes the specified <code>JTextComponent</code> undoable. Also maps undo 
     * to CTRL + Z keystroke and redo to CTRL + Y keystroke.
     * 
     * @param component the <code>JTextComponent</code>.
     * @return the <code>UndoMapping</code> associated with the specified 
     * </code>JTextComponent</code>.
     */
    public UndoMapping makesUndoable(JTextComponent component) {
        return makesUndoable(component, DEFAULT_UNDO_KEYSTROKE, 
                DEFAULT_REDO_KEYSTROKE, null);
    }
    
    /**
     * Makes the specified <code>JTextComponent</code> undoable. Also maps undo 
     * to CTRL + Z keystroke and redo to CTRL + Y keystroke.
     * 
     * @param component the <code>JTextComponent</code>.
     * @param listener a <code>UndoListener</code>.
     * @return the <code>UndoMapping</code> associated with the specified 
     * </code>JTextComponent</code>.
     */
    public UndoMapping makesUndoable(JTextComponent component, UndoListener listener) {
        return makesUndoable(component, DEFAULT_UNDO_KEYSTROKE, 
                DEFAULT_REDO_KEYSTROKE, listener);
    }
    
    /**
     * Makes the specified <code>JTextComponent</code> undoable. Also maps undo 
     * and redo to the specified <code>KeyStroke</code>s (null can be specified 
     * for no mapping).
     * 
     * @param component the <code>JTextComponent</code>.
     * @param undoKeyStroke <code>KeyStroke</code> to maps undo on.
     * @param redoKeyStroke <code>KeyStroke</code> to maps redo on.
     * @return the <code>UndoMapping</code> associated with the specified 
     * <code>JTextComponent</code>.
     */
    public UndoMapping makesUndoable(JTextComponent component, 
            KeyStroke undoKeyStroke, KeyStroke redoKeyStroke) {
        return makesUndoable(component, undoKeyStroke, redoKeyStroke, null);
    }
    
    /**
     * Makes the specified <code>JTextComponent</code> undoable. Also maps undo 
     * and redo to the specified <code>KeyStroke</code>s (null can be specified 
     * for no undoMapping).
     * 
     * 
     * @param component the <code>JTextComponent</code>.
     * @param undoKeyStroke <code>KeyStroke</code> to maps undo on.
     * @param redoKeyStroke <code>KeyStroke</code> to maps redo on.
     * @param listener a <code>UndoListener</code>.
     * @return the <code>UndoMapping</code> associated with the specified 
     * <code>JTextComponent</code>.
     */
    public UndoMapping makesUndoable(JTextComponent component, 
            KeyStroke undoKeyStroke, KeyStroke redoKeyStroke, 
            UndoListener listener) {
        UndoMapping undoMapping = new UndoMapping(component);
        if(undoKeyStroke != null) {
            component.getInputMap().put(undoKeyStroke, "Undo");
            component.getActionMap().put("Undo", undoMapping.getUndoAction());
        }
        if(redoKeyStroke != null) {
            component.getInputMap().put(redoKeyStroke, "Redo");
            component.getActionMap().put("Redo", undoMapping.getRedoAction());
        }
        if(listener != null) {
            undoMapping.addUndoListener(listener);
        }
        map.put(component, undoMapping);
        return undoMapping;
    }
    
}
