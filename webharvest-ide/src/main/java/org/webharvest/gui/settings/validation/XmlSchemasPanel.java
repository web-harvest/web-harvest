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

package org.webharvest.gui.settings.validation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.webharvest.gui.component.FixedSizeButton;
import org.webharvest.gui.component.WHList;
import org.webharvest.gui.component.WHScrollPane;

/**
 * Default implementation of MVP's {@link XmlSchemasView} interface which also
 * extends {@link JPanel} class to display list of XML schemas registered by the
 * user.
 *
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 */
//TODO Code of this class is almost the same as DatabaseDriversPanel.
//TODO Missing unit test.
public final class XmlSchemasPanel extends JPanel implements XmlSchemasView {

    /**
     * Serialized class version identifier.
     */
    private static final long serialVersionUID = 3708025923040993890L;

    private XmlSchemasView.Presenter presenter;

    private final ControlButtonsPanel controlButtonsPanel;
    private final SchemaListPanel schemasList;

    public XmlSchemasPanel() {
        super();

        setUpLayout();

        this.controlButtonsPanel = new ControlButtonsPanel();
        this.schemasList = new SchemaListPanel();

        add(controlButtonsPanel, BorderLayout.EAST);
        add(schemasList, BorderLayout.CENTER);
    }

    private void setUpLayout() {
        setLayout(new BorderLayout(5, 0));
        setBorder(new EmptyBorder(3, 0, 3, 3));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addToList(final XmlSchemaDTO schema) {
        schemasList.addSchema(schema);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromList(final XmlSchemaDTO schema) {
        schemasList.removeSchema(schema);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    /**
     * Panel displaying list of registered XML schemas.
     */
    private class SchemaListPanel extends JPanel {

        /**
         * Serialized class version identifier.
         */
        private static final long serialVersionUID = 5728340099709794539L;

        private final JList schemasList;
        private final DefaultListModel schemasListModel;

        public SchemaListPanel() {
            super(new BorderLayout(5, 5));
            setBorder(new EmptyBorder(3, 3, 3, 3));

            this.schemasListModel = new DefaultListModel();
            this.schemasList = new WHList(schemasListModel);
            add(new WHScrollPane(schemasList), BorderLayout.CENTER);
        }

        public void addSchema(final XmlSchemaDTO schema) {
            schemasListModel.addElement(schema);
            schemasList.setSelectedIndex(schemasListModel.size() - 1);
        }

        public void removeSchema(final XmlSchemaDTO schema) {
            schemasListModel.removeElement(schema);
        }

        public XmlSchemaDTO getSelected() {
            return (XmlSchemaDTO) schemasList.getSelectedValue();
        }
    }

    /**
     * Panel displaying control buttons - that is, buttons allowing to add or
     * remove XML schemas.
     */
    private class ControlButtonsPanel extends JPanel {

        /**
         * Serialized class version identifier.
         */
        private static final long serialVersionUID = 2427784094590470356L;

        public ControlButtonsPanel() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            initControlButtons();
        }

        private void initControlButtons() {
            final JButton addSchemaButton = new FixedSizeButton(
                    "Add XML schema", 170, 22);
            final JButton removeSchemaButton = new FixedSizeButton(
                    "Remove XML schema", 170, 22);

            addSchemaButton.addActionListener(new AddButtonListener());
            removeSchemaButton.addActionListener(new RemoveButtonListner());

            add(addSchemaButton);
            add(Box.createRigidArea(new Dimension(0, 5)));
            add(removeSchemaButton);
        }

        private class AddButtonListener implements ActionListener {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter(
                        "XML schema file", "xsd"));
                int result = chooser.showOpenDialog(XmlSchemasPanel.this);

                if (result == JFileChooser.APPROVE_OPTION) {
                    final File file = chooser.getSelectedFile();
                    presenter.registerSchema(new XmlSchemaDTO(file
                            .getAbsolutePath()));
                }
            }
        }

        private class RemoveButtonListner implements ActionListener {
            @Override
            public void actionPerformed(final ActionEvent e) {
                presenter.unregisterSchema(schemasList.getSelected());
            }
        }
    }

}
