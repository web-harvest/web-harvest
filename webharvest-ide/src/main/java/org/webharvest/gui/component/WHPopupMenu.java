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

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common popup menu.
 */
public class WHPopupMenu extends JPopupMenu {

    // single currently opened popup menu
    private static WHPopupMenu openPopup = null;

    public static WHPopupMenu getOpenPopup() {
        return openPopup;
    }

    private Map submenus = new LinkedHashMap();

    public WHPopupMenu() {
        addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                openPopup = WHPopupMenu.this;
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                openPopup = null;
            }
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        MenuElements.setPopupMenuLook(this);
    }

    public void show(Component invoker, int x, int y) {
        int lastCompIndex = getComponentCount() - 1;
        if (lastCompIndex >= 0) {
            Component lastComp = getComponent(lastCompIndex);
            if (lastComp instanceof JSeparator) {
                remove(lastCompIndex);
            }
        }
        super.show(invoker, x, y);
    }

    public void addSeparator() {
        int count = getComponentCount();
        if (count == 0) {
            return;
        }

        int lastCompIndex = count - 1;
        if (lastCompIndex >= 0) {
            Component lastComp = getComponent(lastCompIndex);
            if (lastComp instanceof JSeparator) {
                return;
            }
        }
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        add(separator);
    }
    
}