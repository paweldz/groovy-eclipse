/*
 * Copyright 2009-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.eclipse.test.debug;

import java.io.InputStream;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.eclipse.debug.ui.ValidBreakpointLocationFinder;
import org.codehaus.groovy.eclipse.test.EclipseTestSetup;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

/**
 * Tests that breakpoint locations are as expected.
 *
 * @author Andrew Eisenberg
 * @created Jul 24, 2009
 */
public class BreakpointLocationTests extends TestCase {

    public static Test suite() {
        return new EclipseTestSetup(new TestSuite(BreakpointLocationTests.class));
    }

    @Override
    protected void tearDown() throws Exception {
        EclipseTestSetup.removeSources();
    }

    @Override
    protected void setUp() throws Exception {
        String text;
        InputStream input = null;
        URL url = Platform.getBundle("org.codehaus.groovy.eclipse.tests").getEntry("/testData/groovyfiles/BreakpointTesting.groovy");
        try {
            input = url.openStream();
            unit = EclipseTestSetup.addGroovySource((text = IOUtils.toString(input)), "BreakpointTesting");
        } finally {
            IOUtils.closeQuietly(input);
        }
        unit.makeConsistent(null);
        document = new Document(text);
    }

    private GroovyCompilationUnit unit;
    private IDocument document;

    public void testBreakpointInScript1() throws Exception {
        doBreakpointTest(1);
    }

    public void testBreakpointInScript2() throws Exception {
        doBreakpointTest(2);
    }

    public void testBreakpointInScript3() throws Exception {
        doBreakpointTest(3);
    }

    public void testBreakpointInScript4() throws Exception {
        doBreakpointTest(4);
    }

    public void testBreakpointInScript5() throws Exception {
        doBreakpointTest(5);
    }

    public void testBreakpointInScript6() throws Exception {
        doBreakpointTest(6);
    }

    public void testBreakpointInScript7() throws Exception {
        doBreakpointTest(7);
    }

    public void testBreakpointInScript8() throws Exception {
        doBreakpointTest(8);
    }

    public void testBreakpointInScript9() throws Exception {
        doBreakpointTest(9);
    }

    public void testBreakpointInScript10() throws Exception {
        doBreakpointTest(10);
    }

    public void testBreakpointInScript11() throws Exception {
        doBreakpointTest(11);
    }

    public void testBreakpointInScript12() throws Exception {
        doBreakpointTest(12);
    }

    public void testBreakpointInScript13() throws Exception {
        doBreakpointTest(13);
    }

    public void testBreakpointInScript14() throws Exception {
        doBreakpointTest(14);
    }

    public void testBreakpointInScript15() throws Exception {
        doBreakpointTest(15);
    }

    public void testBreakpointInScript16() throws Exception {
        doBreakpointTest(16);
    }

    public void testBreakpointInScript17() throws Exception {
        doBreakpointTest(17);
    }

    public void testBreakpointInScript18() throws Exception {
        doBreakpointTest(18);
    }

    public void testBreakpointInScript19() throws Exception {
        doBreakpointTest(19);
    }

    public void testBreakpointInScript20() throws Exception {
        doBreakpointTest(20);
    }

    public void testBreakpointInScript21() throws Exception {
        doBreakpointTest(21);
    }

    public void testBreakpointInScript22() throws Exception {
        doBreakpointTest(22);
    }

    public void testBreakpointInScript23() throws Exception {
        doBreakpointTest(23);
    }

    private void doBreakpointTest(int i) throws Exception {
        int location = document.get().indexOf("// " + i) - 3;
        int line = document.getLineOfOffset(location) + 1;
        ValidBreakpointLocationFinder finder = new ValidBreakpointLocationFinder(line);
        ASTNode node = finder.findValidBreakpointLocation(unit.getModuleNode());
        assertNotNull("Could not find a breakpoint for line " + line, node);
        assertEquals("Wrong expected line number", line, node.getLineNumber());
    }
}
