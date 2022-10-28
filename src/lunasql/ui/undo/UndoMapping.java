/*
 * UndoMapping.java
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

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.EventListenerList;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 * @author Yann D'ISANTO
 * @version 1.0.2 11/01/2007
 * @see UndoableJTextManager
 */
public final class UndoMapping implements UndoableEditListener {
    
    private JTextComponent component;
    private RedoAction redoAction;
    private UndoAction undoAction;
    private UndoManager undoManager;
    private EventListenerList listeners;
    
    /**
     * Constructs a new <code>UndoMapping</code> with the specified <code>JTextComponent</code>.
     * @param component a <code>JTextComponent</code>.
     */
    public UndoMapping(final JTextComponent component) {
        this.component = component;
        undoManager = new UndoManager();
        undoAction = new UndoAction();
        redoAction = new RedoAction();
        listeners = new EventListenerList();
        component.getDocument().addUndoableEditListener(this);
        component.addPropertyChangeListener("document", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                component.getDocument().addUndoableEditListener(UndoMapping.this);
            }
        });
    }
    
    /**
     * Registers <code>UndoListener</code> so that it will receive 
     * <code>ActionEvent</code>s when "undo" or "redo" Action are performed.
     * @param listener a <code>UndoListener</code>.
     */
    public void addUndoListener(UndoListener listener) {
        listeners.add(UndoListener.class, listener);
    }
    
    /**
     * Returns the <code>JTextComponent</code>.
     * @return the <code>JTextComponent</code>.
     */
    public JTextComponent getComponent() {
        return component;
    }

    /**
     * Returns the redo <code>Action</code> af the <code>UndoMapping</code>.
     * @return the redo <code>Action</code> af the <code>UndoMapping</code>.
     */
    public Action getRedoAction() {
        return redoAction;
    }

    /**
     * Returns the undo <code>Action</code> af the <code>UndoMapping</code>.
     * @return the undo <code>Action</code> af the <code>UndoMapping</code>.
     */
    public Action getUndoAction() {
        return undoAction;
    }

    /**
     * Returns the <code>UndoManager</code> af the <code>UndoMapping</code>.
     * @return the <code>UndoManager</code> af the <code>UndoMapping</code>.
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }
    
    /**
     * Unregisters <code>UndoListener</code> so that it will no longer receive 
     * <code>ActionEvent</code>s.
     * @param listener a <code>UndoListener</code>.
     */
    public void removeUndoListener(UndoListener listener) {
        listeners.remove(UndoListener.class, listener);
    }
    
    /**
     * An undoable edit happened
     */
    public void undoableEditHappened(UndoableEditEvent e) {
        undoManager.addEdit(e.getEdit());
        undoAction.updateUndoState();
        redoAction.updateRedoState();
    }
    
    
    
    
    private class UndoAction extends AbstractAction {
        
        public UndoAction() {
            super("Undo");
            setEnabled(false);
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                undoManager.undo();
                EventListener[] undoListeners = listeners.getListeners(UndoListener.class);
                for(int i = 0; i < undoListeners.length; i ++) {
                    UndoListener listener = (UndoListener) undoListeners[i];
                    listener.undoPerformed(new UndoEvent(e, UndoEvent.UNDO));
                }
            } catch (CannotUndoException ex) {
                System.out.println("Unable to undo: " + ex);
            }
            updateUndoState();
            redoAction.updateRedoState();
        }
        
        protected void updateUndoState() {
            if (undoManager.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undoManager.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }
    
    private class RedoAction extends AbstractAction {
        
        public RedoAction() {
            super("Redo");
            setEnabled(false);
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                undoManager.redo();
                EventListener[] undoListeners = listeners.getListeners(UndoListener.class);
                for(int i = 0; i < undoListeners.length; i ++) {
                    UndoListener listener = (UndoListener) undoListeners[i];
                    listener.undoPerformed(new UndoEvent(e, UndoEvent.REDO));
                }
            } catch (CannotRedoException ex) {
                System.out.println("Unable to redo: " + ex);
            }
            updateRedoState();
            undoAction.updateUndoState();
        }
        
        protected void updateRedoState() {
            if (undoManager.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undoManager.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }
}
