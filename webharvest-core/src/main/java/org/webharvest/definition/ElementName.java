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

import java.io.Serializable;

import org.webharvest.utils.Assert;
import org.webharvest.utils.CommonUtil;

/**
 * Name of the single element (tag in configuraton xml). It consists of the pair -
 * pure name and xml namespace URI. Both values are required and names are equal
 * if and only if both names and URIs match.
 */
public class ElementName implements Comparable, Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String uri;

    public ElementName(String name, String uri) {
        Assert.isFalse(CommonUtil.isEmptyString(name), "Element name cannot be empty!");
        this.name = name;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return name + ", " + uri;
    }

    @Override
    public int hashCode() {
        return (name + "," + uri).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ElementName) {
            ElementName elementName = (ElementName) obj;
            return name.equals(elementName.name) && uri.equals(elementName.uri);
        }
        return false;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof ElementName) {
            ElementName elementName = (ElementName) o;
            int cmp = name.compareTo(elementName.name);
            if (cmp != 0) {
                return cmp;
            } else {
                return uri.compareTo(elementName.uri);
            }
        }

        return -1;
    }

}