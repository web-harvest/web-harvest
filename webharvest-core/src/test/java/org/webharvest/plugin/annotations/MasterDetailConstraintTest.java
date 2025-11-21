/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.annotations;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.core.IfPlugin;
import org.webharvest.plugin.core.TryPlugin;
import org.webharvest.plugin.core.CasePlugin;

/**
 * Tests for @MasterDetailConstraint annotation.
 */
public class MasterDetailConstraintTest {

    @Test
    void testIfPluginHasConstraint() {
        MasterDetailConstraint constraint = IfPlugin.class.getAnnotation(MasterDetailConstraint.class);
        assertNotNull(constraint, "IfPlugin should have @MasterDetailConstraint annotation");
        assertEquals("else", constraint.detailType());
        assertEquals(1, constraint.maxOccurs());
        assertEquals(0, constraint.minOccurs());
        assertEquals("Else Branch", constraint.sectionTitle());
        assertEquals("Add Else", constraint.addLabel());
    }

    @Test
    void testTryPluginHasConstraint() {
        MasterDetailConstraint constraint = TryPlugin.class.getAnnotation(MasterDetailConstraint.class);
        assertNotNull(constraint, "TryPlugin should have @MasterDetailConstraint annotation");
        assertEquals("catch", constraint.detailType());
        assertEquals(1, constraint.maxOccurs());
        assertEquals(0, constraint.minOccurs());
        assertEquals("Catch Block", constraint.sectionTitle());
        assertEquals("Add Catch", constraint.addLabel());
    }

    @Test
    void testCasePluginHasConstraint() {
        MasterDetailConstraint constraint = CasePlugin.class.getAnnotation(MasterDetailConstraint.class);
        assertNotNull(constraint, "CasePlugin should have @MasterDetailConstraint annotation");
        assertEquals("else", constraint.detailType());
        assertEquals(1, constraint.maxOccurs());
        assertEquals(0, constraint.minOccurs());
        assertEquals("Else Branch", constraint.sectionTitle());
        assertEquals("Add Else", constraint.addLabel());
    }

    @Test
    void testAnnotationRetention() {
        // Verify annotation is retained at runtime
        MasterDetailConstraint constraint = IfPlugin.class.getAnnotation(MasterDetailConstraint.class);
        assertNotNull(constraint, "Annotation should be available at runtime");
    }
}

