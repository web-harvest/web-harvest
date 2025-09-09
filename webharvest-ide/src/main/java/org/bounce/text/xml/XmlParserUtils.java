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

package org.bounce.text.xml;

/**
 * @author Vladimir Nikic
 * Date: May 23, 2007
 */
public class XmlParserUtils {

    /**
     * @param source
     * @return Identifier that stands for tag name or tag attribute at the start
     *         of specified string.
     */
    public static String getIdentifier(String source) {
        if (source.length() == 0) {
            return null;
        }
        
        StringBuilder buffer = new StringBuilder();
        int index = 0;
        int len = source.length();
        boolean charOk;
        do {
            char ch = source.charAt(index);
            charOk = ( index == 0 && Character.isJavaIdentifierStart(ch) ) ||
                     ( Character.isJavaIdentifierPart(ch) || ch == '-' || ch == ':' || ch == '.' );
            if (charOk) {
                buffer.append(ch);
            }
            index++;
        } while (charOk && index < len);

        String identifier = buffer.toString();
        return identifier.length() > 0 ? identifier : null;
    }

    /**
     * Tries to find out if the line finishes with an element start
     * @param line
     */
    public static String getStartElement(String line) {
        int first = line.lastIndexOf( "<");
        int last = line.lastIndexOf( ">");

        if ( last < first) { // In the Tag
            return null;
        } else {
            int firstEnd = line.lastIndexOf( "</");
            int lastEnd = line.lastIndexOf( "/>");

            // Last Tag is not an End Tag
            if ( (firstEnd != first) && ((lastEnd + 1) != last)) {
                return XmlParserUtils.getIdentifier( line.substring(first + 1) );
            }
        }

        return null;
    }
    
}