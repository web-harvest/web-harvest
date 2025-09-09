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

package org.webharvest.gui.ui;

import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public class WHButtonUI extends BasicButtonUI {

    protected static final Color DISABLED_COLOR = new Color(125, 121, 111);

    private Component comp = null;

    private boolean showDownArrow = false;

    public void installUI(JComponent c) {
        this.comp = c;
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        FontMetrics fm = b.getFontMetrics(b.getFont());
        int textShiftOffset = getTextShiftOffset();

        if (showDownArrow) {
            boolean textAdapted = false;
            do {
                int textWidth = SwingUtilities.computeStringWidth(fm, text);
                if (textWidth >= comp.getWidth() - 7 - textRect.x - textShiftOffset) {
                    if (text != null && text.endsWith("...")) {
                        text = text.substring(0, text.length() - 3);
                    }
                    if (StringUtils.isNotEmpty(text)) {
                        text = text.substring(0, text.length() - 1);
                    } else {
                        text = "...";
                        textAdapted = true;
                    }
                    text += "...";
                } else {
                    textAdapted = true;
                }
            } while (!textAdapted);
        }

        // Draw the Text
        g.setColor(model.isEnabled() ? b.getForeground() : DISABLED_COLOR);
        g.drawString(text, textRect.x + textShiftOffset, textRect.y + fm.getAscent() + textShiftOffset);

        int w = c.getWidth();
        int h = c.getHeight();
        if (showDownArrow) {
            g.setColor(comp.isEnabled() ? Color.black : DISABLED_COLOR);
            int ha = h / 2;
            g.drawLine(w - 8, ha - 1, w - 4, ha - 1);
            g.drawLine(w - 7, ha, w - 5, ha);
            g.drawLine(w - 6, ha + 1, w - 6, ha + 1);
        }

    }

    public boolean isShowDownArrow() {
        return showDownArrow;
    }

}