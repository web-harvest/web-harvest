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
import java.util.Collection;
import java.util.Iterator;

/**
 * Combo boxes used in application
 */
public class WHList extends JList {

    {
        setBorder(new WHControlsBorder());
    }

    public WHList() {
        defineList();
    }

    public WHList(Object items[]) {
        defineList();
        addValues(items);
    }

    public WHList(Collection values) {
        defineList();
        addValues(values);
    }

    public WHList(ListModel dataModel) {
        super(dataModel);
    }

    private void defineList() {
        setModel(new DefaultListModel());
    }

    public void addValue(Object value) {
        if (value != null) {
            DefaultListModel model = (DefaultListModel) getModel();
            model.addElement(value);
        }
    }

    public void removeValue(Object value) {
        if (value != null) {
            DefaultListModel model = (DefaultListModel) getModel();
            model.removeElement(value);
        }
    }

    public void clearList() {
        DefaultListModel model = (DefaultListModel) getModel();
        model.removeAllElements();
    }

    public void addValues(Collection values) {
        DefaultListModel model = (DefaultListModel) getModel();
        if (values != null) {
            final Iterator iterator = values.iterator();
            while (iterator.hasNext()) {
                model.addElement(iterator.next() );
            }
        }
    }

    public void addValues(Object values[]) {
        DefaultListModel model = (DefaultListModel) getModel();
        for (int i = 0; i < values.length; i++) {
            model.addElement(values[i]);
        }
    }

    public Object[] getValues() {
        DefaultListModel model = (DefaultListModel) getModel();
        int size = model.getSize();
        Object result[] = new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = model.getElementAt(i);
        }

        return result;
    }

    public void insertValue(Object value, int index) {
        DefaultListModel model = (DefaultListModel) getModel();
        model.insertElementAt(value, index);
    }

    public int getModelSize() {
        DefaultListModel model = (DefaultListModel) getModel();
        return model.getSize();
    }

}