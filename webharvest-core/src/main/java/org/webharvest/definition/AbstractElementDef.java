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

package org.webharvest.definition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractElementDef implements IElementDef {

    // TODO Make it private
    protected final XmlNode xmlNode;
    // sequence of operation definitions
    private final List<IElementDef> operationDefs = new ArrayList<IElementDef>();

    // TODO Do we really need createBodyDefs parameter? If not remove this constructor
    protected AbstractElementDef(XmlNode node) {
        if (node == null) {
            throw new IllegalArgumentException("XmlNode must not be null.");
        }
        this.xmlNode = node;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOperations() {
        return operationDefs != null && operationDefs.size() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public IElementDef[] getOperationDefs() {
        IElementDef[] defs = new IElementDef[operationDefs.size()];
        Iterator<IElementDef> it = operationDefs.iterator();
        int index = 0;
        while (it.hasNext()) {
            defs[index++] = it.next();
        }

        return defs;
    }

    @Override
    public String getId() {
        return xmlNode.getAttribute("id");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getShortElementName() {
        return xmlNode.getQName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLineNumber() {
        return xmlNode.getLineNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnNumber() {
        return xmlNode.getColumnNumber();
    }

    public void add(IElementDef element) {
        operationDefs.add(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IElementDef> getElementDefs() {
        return Arrays.asList(getOperationDefs());
    }
    

}
