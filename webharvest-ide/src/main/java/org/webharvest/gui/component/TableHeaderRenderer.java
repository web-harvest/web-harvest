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
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Renderer for table headers
 */
public class TableHeaderRenderer extends JLabel implements TableCellRenderer {

    protected int aligns[] = null;
    private boolean drawTopLine = true;
    private boolean drawBottomLine = true;

    public TableHeaderRenderer() {
        setOpaque(true);
        Border headerCellBorder = new Border() {
            public Insets getBorderInsets(Component c) {
                return new Insets(1, 2, 1, 2);
            }
            public boolean isBorderOpaque() {
                return false;
            }
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(Color.gray);
                if (drawTopLine) {
                    g.drawLine(0, 0, width - 1, 0);
                }
                g.drawLine(width - 1, 0, width - 1, height - 1);
                if (drawBottomLine) {
                    g.drawLine(0, height - 1, width - 1, height - 1);
                }
            }
        };
        setBorder(headerCellBorder);
    }

    public void setAligns(int[] aligns) {
        this.aligns = aligns;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String text = CommonUtil.nvl(value, "");
        String[] tokens = CommonUtil.tokenize(text, "\n");
        String colName = tokens.length > 0 ? tokens[0] : " ";
        setText(colName);
        setToolTipText(tokens.length > 1 ? tokens[1] : colName);
        setBackground( new Color(200, 198, 188) );
        setForeground(  Color.black );

        if (aligns != null && column < aligns.length) {
            setHorizontalAlignment(aligns[column]);
        }
        return this;
    }

    public boolean isDrawTopLine() {
        return drawTopLine;
    }

    public void setDrawTopLine(boolean drawTopLine) {
        this.drawTopLine = drawTopLine;
    }

    public boolean isDrawBottomLine() {
        return drawBottomLine;
    }

    public void setDrawBottomLine(boolean drawBottomLine) {
        this.drawBottomLine = drawBottomLine;
    }

}