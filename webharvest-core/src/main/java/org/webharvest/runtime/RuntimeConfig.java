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

package org.webharvest.runtime;

import net.sf.saxon.Configuration;
import net.sf.saxon.query.StaticQueryContext;
import org.webharvest.runtime.processors.XQueryExpressionPool;

/**
 * Facade for runtime objects needed for specific processors' execution.
 * 
 * @author Vladimir Nikic
 * Date: Jul 4, 2007
 */
public class RuntimeConfig {

    private StaticQueryContext staticQueryContext;

    private XQueryExpressionPool xQueryExpressionPool;

    public synchronized StaticQueryContext getStaticQueryContext() {
        if (staticQueryContext == null) {
            Configuration config = new Configuration();
            staticQueryContext = new StaticQueryContext(config);
        }
        return staticQueryContext;
    }

    public synchronized XQueryExpressionPool getXQueryExpressionPool() {
        if (xQueryExpressionPool == null) {
            xQueryExpressionPool = new XQueryExpressionPool(getStaticQueryContext());
        }

        return xQueryExpressionPool;
    }

}