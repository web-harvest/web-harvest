/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.gui.component;

import org.webharvest.utils.CommonUtil;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Vladimir Nikic
 * Date: Apr 23, 2007
 */
abstract public class EditableComboBox extends WHComboBox {

    private boolean addNewItemsOnTop = false;

    private boolean isUppercase = false;

    // tells how menu items are fixed and are not removing from the drop down
    private int fixedItemsSize = -1;

    // number of items limit for the drop down
    private int dropDownLimit = -1;

    private int length;
    private ActionListener[] actionListeners;

    private boolean isSelectedFromPopup = false;

    /**
     * Constructor that allows specifying wether new items are added to the top.
     */
    public EditableComboBox(int length, boolean addNewItemsOnTop) {
        this(length);
        this.addNewItemsOnTop = addNewItemsOnTop;
    }

    /**
     * Constructor.
     */
    public EditableComboBox(int length) {
        super();
        this.length = length;
        this.setEditable(true);
        final JTextField editField = (JTextField) this.getEditor().getEditorComponent();
        editField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if ( e.getKeyChar() == '\n' && !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() ) {
                    Object value = getEditor().getItem();
                    if (isValidValue(value)) {
                        select(value);
                        onValue();
                    }
                } else {
                    if (isUppercase) {
                        e.setKeyChar(Character.toUpperCase(e.getKeyChar()));
                    }
                    super.keyTyped(e);
                }
            }
        });

        addPopupMenuListener(new PopupMenuListener() {
            private int selectedIndex = -1;
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                isSelectedFromPopup = true;
                selectedIndex = getSelectedIndex();
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if (isSelectedFromPopup && selectedIndex != getSelectedIndex()) {
                    onValue();
                }
            }
            public void popupMenuCanceled(PopupMenuEvent e) {
                isSelectedFromPopup = false;
            }
        });

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( "comboBoxChanged".equals(e.getActionCommand()) ) {
                    if (!isPopupVisible()) {
                        Object value = getEditor().getItem();
                        if (isValueInList(value)) {
                            onValue();
                        }
                    } else {
                        isSelectedFromPopup = true;
                    }
                }
            }
        });

        editField.addFocusListener (new FocusListener () {
            public void focusGained (FocusEvent e)
            {
                ((JTextField)getEditor ().getEditorComponent ()).selectAll();
            }

            public void focusLost (FocusEvent e) {}
        });
    }

    private boolean isValueInList(Object value) {
        if (value != null) {
            int itemCount = getItemCount();
            for (int i = 0; i < itemCount; i++) {
                if ( value.equals(getItemAt(i)) ) {
                    return true;
                }
            }
        }
        return false;
    }

    public final void onValue() {
        Object value = getEditor().getItem();
        if (isValidValue(value)) {
            select(value);
            execute(value);
        }
    }

    protected boolean isValidValue(Object value) {
        return value != null;
    }

    abstract protected void execute(Object value);

    public void setDropDownLimit(int dropDownLimit) {
        this.dropDownLimit = dropDownLimit;
    }

    public void setFixedItemsSize(int fixedItemsSize) {
        this.fixedItemsSize = fixedItemsSize;
    }

    public Object getValue() {
        return getEditor().getItem();
    }

    public Dimension getPreferredSize () {
        Dimension size = super.getPreferredSize ();
        int charsWidth = getFontMetrics (getFont ()).charWidth ('H') * length;
        return new Dimension (charsWidth, size.height);
    }

    public void removeFromCombo(Object item) {
        if (item != null) {
            String itemAsString = item.toString();
            int itemCount = getItemCount();
            for ( int i = Math.max(0, fixedItemsSize); i < itemCount; i++ ) {
                Object currItem = getItemAt(i);
                if ( itemAsString.equals(currItem.toString()) ) {
                    disableActionListeners();
                    removeItem(currItem);
                    enableActionListeners();

                    return;
                }
            }
        }
    }

    private boolean itemExists(Object item) {
        if (item != null) {
            String itemAsString = item.toString();
            int itemCount = getItemCount();
            for ( int i = 0; i < itemCount; i++ ) {
                Object currItem = getItemAt(i);
                if ( currItem != null && itemAsString.equals(currItem.toString()) ) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addItem(Object item) {
        if (CommonUtil.isEmptyString(item)) {
            return;
        }

        removeFromCombo(item);

        if ( itemExists(item) ) {
            return;
        }

        disableActionListeners();

        if (this.addNewItemsOnTop) {
            super.insertItemAt(item, 0);
        } else {
            super.addItem(item);
        }

        int count = this.getItemCount();

        // if list is oversized
        if (this.dropDownLimit > 0 && count > this.dropDownLimit && count > this.fixedItemsSize) {
            if (this.addNewItemsOnTop) {
                this.removeItemAt(this.dropDownLimit - this.fixedItemsSize);
            } else {
                this.removeItemAt(this.fixedItemsSize);
            }
        }

        enableActionListeners();
    }

    public void select(Object item) {
        if ( item != null ) {
            addItem(item);
            String itemAsString = item.toString();
            int itemCount = getItemCount();
            for ( int i = 0; i < itemCount; i++ ) {
                Object currItem = getItemAt(i);
                if ( currItem != null && itemAsString.equals(currItem.toString()) ) {
                    disableActionListeners();
                    setSelectedIndex(i);
                    enableActionListeners();
                    break;
                }
            }
        }
    }

    private void disableActionListeners() {
        if (actionListeners == null) {
            actionListeners = getActionListeners();
        }
        for (ActionListener actionListener : actionListeners) {
            removeActionListener(actionListener);
        }
    }

    private void enableActionListeners() {
        if (actionListeners != null) {
            for (ActionListener actionListener : actionListeners) {
                addActionListener(actionListener);
            }
            actionListeners = null;
        }
    }

    public boolean isUppercase() {
        return isUppercase;
    }

    public void setUppercase(boolean uppercase) {
        isUppercase = uppercase;
    }

}