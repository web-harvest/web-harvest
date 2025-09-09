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
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog which offers user to insert text value
 */
public class InputDialog extends CommonDialog {

    private Component caller;
    private String result = null;
    private WHTextField inputField;

    public InputDialog(Component caller, String title, String message, String value, int textFieldSize, Icon icon) {
        super(title);
        this.caller = caller;

        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel(message);
        label.setIcon(icon);
        inputField = new WHTextField(value, textFieldSize);
        inputField.selectAll();

        panel.add(label, BorderLayout.WEST);
        panel.add(inputField, BorderLayout.CENTER);

        contentPane.add(panel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 4));
        buttonPanel.add(createOkButton());
        buttonPanel.add(createCancelButton());

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        pack();
    }

    protected void onOk() {
        if (!"".equals(inputField.getText().trim())) {
            result = inputField.getText();
            setVisible(false);
        } else {
            inputField.requestFocus();
        }
    }

    public void setVisible(boolean b) {
        if (b) {
            Component relativeTo = caller;
            if (relativeTo == null) {
                relativeTo = GuiUtils.getActiveFrame();
            }
            if (relativeTo != null) {
                GuiUtils.centerRelativeTo(this, relativeTo);
            }
        }
        super.setVisible(b);
    }

    public String getValue() {
        setVisible(true);
        return result;
    }

}