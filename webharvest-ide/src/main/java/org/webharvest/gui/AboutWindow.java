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

package org.webharvest.gui;

import org.webharvest.ApplicationInfo;
import org.webharvest.utils.CommonUtil;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

public class AboutWindow extends JWindow implements HyperlinkListener {

    private static final Dimension WINDOW_DIMENSION = new Dimension(350, 320);

    // Ide instance where this dialog belongs.
    private Ide ide;

    public AboutWindow(Ide ide) throws HeadlessException {
        super(ide);
        this.ide = ide;

        createGUI();
    }

    private void createGUI() {
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setEditable(false);
        htmlPane.setContentType("text/html");
        htmlPane.setEditorKit(new HTMLEditorKit());
        htmlPane.setBorder(new LineBorder(Color.black));
        htmlPane.addHyperlinkListener(this);
        htmlPane.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                AboutWindow.this.setVisible(false);
            }
        });
        htmlPane.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                setVisible(false);
            }
        });

        try {
            URL aboutUrl = ResourceManager.getAboutUrl();
            String content = CommonUtil.readStringFromUrl(aboutUrl);
            content = content.replace("#program.version#", ApplicationInfo.WEB_HARVEST_VERSION);
            content = content.replace("#program.date#", ApplicationInfo.WEB_HARVEST_DATE);
            content = content.replace("#java.version#", System.getProperty("java.version"));
            content = content.replace("#java.vendor#", System.getProperty("java.vendor"));
            content = content.replace("#year#", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
            ((HTMLDocument) htmlPane.getDocument()).setBase(ResourceManager.getAboutUrl());
            htmlPane.setText(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        contentPane.add(htmlPane, BorderLayout.CENTER);

        this.pack();
    }

    public Dimension getPreferredSize() {
        return WINDOW_DIMENSION;
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String url = e.getDescription();
            ide.openURLInBrowser(url);
        }
    }

    public void open() {
        setLocationRelativeTo(this.ide);
        setVisible(true);
    }

}
