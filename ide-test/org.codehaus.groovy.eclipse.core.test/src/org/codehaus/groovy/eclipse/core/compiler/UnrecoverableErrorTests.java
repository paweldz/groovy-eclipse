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
package org.codehaus.groovy.eclipse.core.compiler;

import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.eclipse.test.EclipseTestCase;

/**
 * All of these tests should produce {@link ModuleNode}s with
 * {@code encounteredUnrecoverableError} set to {@code true}.
 */
public class UnrecoverableErrorTests extends EclipseTestCase {
    private GroovySnippetCompiler compiler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        compiler = new GroovySnippetCompiler(testProject.getGroovyProjectFacade());
    }

    @Override
    protected void tearDown() throws Exception {
        compiler.cleanup();
        super.tearDown();
    }

    protected ModuleNode compileScript(String script) {
        return compiler.compile(script, "Test");
    }

    public void testSomething() throws Exception {
        ModuleNode result = compileScript("package a\n" +
                "void method() {\n" +
                "if (###) {\n" +
                "    \n" +
                " }\n" +
                "} ");
        assertTrue(result.encounteredUnrecoverableError());
    }
}
