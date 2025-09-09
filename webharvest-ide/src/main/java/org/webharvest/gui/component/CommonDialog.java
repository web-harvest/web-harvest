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

import org.webharvest.gui.GuiUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Common dilaog containing OK, Cancel buttons
 */
abstract public class CommonDialog extends JDialog {

    private Map components = new HashMap();
    private DefaultKeyboardFocusManager keyManager;
    private Component lastFocusOwner;

    public CommonDialog() {
        super(GuiUtils.getActiveFrame());
        lastFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        setResizable(false);
        setModal(true);

        keyManager = new DefaultKeyboardFocusManager() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    int keyCode = e.getKeyCode();
                    Object source = e.getSource();
                    if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER) {
                        if (source instanceof JTextArea && keyCode == KeyEvent.VK_ENTER) {
                            return false;
                        }
                        JComboBox comboBox = source instanceof Component ? getParentComboBox((Component)source) : null;
                        if (comboBox != null && comboBox.isPopupVisible()) {
                            return false;
                        } else if (isActive()) {
                            if (keyCode == KeyEvent.VK_ESCAPE) {
                                onCancel();
                                return true;
                            } else {
                                onOk();
                            }
                        }
                    }
                }
                return false;
            }
        };
    }

    private JComboBox getParentComboBox(Component comp) {
        do {
            if (comp instanceof JComboBox) {
                return (JComboBox) comp;
            }
            comp = comp.getParent();
        } while (comp != null);
        return null;
    }

    public CommonDialog(String title) {
        this();
        this.setTitle(title);
    }

    public void setVisible(boolean b) {
        KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (b) {
            keyboardFocusManager.addKeyEventDispatcher(keyManager);
        } else {
            keyboardFocusManager.removeKeyEventDispatcher(keyManager);
        }
        super.setVisible(b);

        Window owner = getOwner();
        if (owner != null) {
            owner.requestFocus();
            if (lastFocusOwner != null) {
                lastFocusOwner.requestFocusInWindow();
            }
        }
    }

    protected JButton createOkButton() {
        FixedSizeButton okButton = new FixedSizeButton("OK", 80, -1);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });
        return okButton;
    }

    protected JButton createCancelButton() {
        FixedSizeButton cancelButton = new FixedSizeButton("Cancel", 80, -1);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        return cancelButton;
    }

    protected void onCancel() {
        setVisible(false);
    }

    abstract protected void onOk();

    public void addComponent(String id, Component comp) {
        if (id != null) {
            components.put(id, comp);
        }
    }

    public Component getComponent(String id) {
        return id != null ? (Component) components.get(id) : null;
    }

    public Object getComponentValue(String componentId) {
        Object comp = components.get(componentId);
        if (comp != null) {
            if (comp instanceof JTextComponent) {
                return ((JTextComponent)comp).getText();
            } else if (comp instanceof JComboBox) {
                return ((JComboBox)comp).getSelectedItem();
            }
        }

        return null;
    }

    public boolean getBooleanValue(String componentId) {
        Object comp = components.get(componentId);
        if (comp != null) {
            if (comp instanceof JRadioButton) {
                return ((JRadioButton)comp).isSelected();
            } else if (comp instanceof JCheckBox) {
                return ((JCheckBox)comp).isSelected();
            }
        }

        return false;
    }

    public JTextField getTextField(String name) {
        Object comp = components.get(name);
        return comp instanceof JTextField ? (JTextField)comp : null;
    }

    public JTextArea getTextArea(String name) {
        Object comp = components.get(name);
        return comp instanceof JTextArea ? (JTextArea)comp : null;
    }

    public JComboBox getComboBox(String name) {
        Object comp = components.get(name);
        return comp instanceof JComboBox ? (JComboBox)comp : null;
    }

    public JList getList(String name) {
        Object comp = components.get(name);
        return comp instanceof JList ? (JList)comp : null;
    }

    public JCheckBox getCheckBox(String name) {
        Object comp = components.get(name);
        return comp instanceof JCheckBox ? (JCheckBox)comp : null;
    }

    public JRadioButton getRadioButton(String name) {
        Object comp = components.get(name);
        return comp instanceof JRadioButton ? (JRadioButton) comp : null;
    }

    public JLabel getLabel(String name) {
        Object comp = components.get(name);
        return comp instanceof JLabel ? (JLabel)comp : null;
    }

    public JButton getButton(String name) {
        Object comp = components.get(name);
        return comp instanceof JButton ? (JButton)comp : null;
    }

    public JSlider getSlider(String name) {
        Object comp = components.get(name);
        return comp instanceof JSlider ? (JSlider)comp : null;
    }
    
}