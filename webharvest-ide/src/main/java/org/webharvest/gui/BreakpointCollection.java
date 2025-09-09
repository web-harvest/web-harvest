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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Collection of breakpoints sorted by line number.
 */
public class BreakpointCollection extends ArrayList {

    public BreakpointCollection() {
    }

    /**
     * Adds new breakpoint (if none exists on the same line), keeping list
     * sorted by line numbers of the breakpoints.
     * @param breakpointInfo Breakpoint to be inserted
     * @return Position where breakpoint is inserted, or -1 if there was no insert
     */
    public int addBreakpoint(BreakpointInfo breakpointInfo) {
        if (breakpointInfo != null) {
            int lineNumber = breakpointInfo.getLineNumber();
            final int size = this.size();
            for (int i = 0; i < size; i++) {
                BreakpointInfo breakpoint = (BreakpointInfo) this.get(i);
                int currLineNumber = breakpoint.getLineNumber();
                if (currLineNumber == lineNumber) {
                    return -1;
                } else if (currLineNumber > lineNumber) {
                    add(i, breakpointInfo);
                    return i;
                }
            }
            add(breakpointInfo);
            return size;
        }

        return -1;
    }

    /**
     * Removes breakpoint on specified line number, if such exists.
     * @param lineNumber Line number of the breakpoint to be removed.
     */
    public void removeBreakpoint(int lineNumber) {
        final int size = this.size();
        for (int i = 0; i < size; i++) {
            BreakpointInfo breakpoint = (BreakpointInfo) this.get(i);
            int currLineNumber = breakpoint.getLineNumber();
            if (currLineNumber == lineNumber) {
                remove(i);
                return;
            } else if (currLineNumber > lineNumber) {
                return;
            }
        }
    }

    /**
     * Removes breakpoints in specified range of lines.
     * @param from Range start
     * @param to Range end
     */
    public void removeBreakpointsInRange(int from, int to) {
        Iterator iterator = iterator();
        while (iterator.hasNext()) {
            BreakpointInfo breakpoint = (BreakpointInfo) iterator.next();
            int currLineNumber = breakpoint.getLineNumber();
            if (currLineNumber >= from && currLineNumber <= to) {
                iterator.remove();
            } else if (currLineNumber > to) {
                return;
            }
        }
    }

    /**
     * Updates breakpoints from specified line for specified amount.
     * @param from Range start
     */
    public void updateBreakpoints(int from, int amount) {
        final int size = this.size();
        for (int i = 0; i < size; i++) {
            BreakpointInfo breakpoint = (BreakpointInfo) this.get(i);
            int currLineNumber = breakpoint.getLineNumber();
            if (currLineNumber >= from) {
                breakpoint.updateForAmount(amount);
            }
        }
    }

    /**
     * Checks if breakpoint at specified line number already exists.
     * @param lineNumber Line number for which breakpoint is looked for
     * @return True if breakpoint exists, false otherwise
     */
    public boolean isThereBreakpoint(int lineNumber) {
        final int size = this.size();
        for (int i = 0; i < size; i++) {
            BreakpointInfo breakpoint = (BreakpointInfo) this.get(i);
            int currLineNumber = breakpoint.getLineNumber();
            if (currLineNumber == lineNumber) {
                return true;
            } else if (currLineNumber > lineNumber) {
                return false;
            }
        }

        return false;
    }

}