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
package org.codehaus.groovy.alltests;

import junit.framework.Test;
import junit.framework.TestSuite;

// From org.eclipse.jdt.groovy.core.tests.builder plug-in:
import org.eclipse.jdt.core.groovy.tests.builder.*;
import org.eclipse.jdt.core.groovy.tests.compiler.*;
import org.eclipse.jdt.core.groovy.tests.locations.*;
import org.eclipse.jdt.core.groovy.tests.model.*;
import org.eclipse.jdt.core.groovy.tests.search.*;
// From org.eclipse.jdt.groovy.core.tests.compiler plug-in:
import org.eclipse.jdt.groovy.core.tests.basic.*;

/**
 * All Groovy-JDT integration tests.
 *
 * @author Andrew Eisenberg
 * @created Jul 8, 2009
 */
public class GroovyJDTTests {

    public static Test suite() throws Exception {
        // ensure that the compiler chooser starts up
        GroovyTestSuiteSupport.initializeCompilerChooser();

        TestSuite suite = new TestSuite(GroovyJDTTests.class.getName());

        suite.addTestSuite(SanityTest.class);

        // Builder tests
        suite.addTest(BasicGroovyBuildTests.suite());
        suite.addTest(FullProjectTests.suite());

        // Compiler tests
        suite.addTest(AnnotationsTests.suite());
        suite.addTest(ErrorRecoveryTests.suite());
        suite.addTest(GenericsTests.suite());
        suite.addTest(GroovySimpleTest.suite());
        suite.addTest(GroovySimpleTests_Compliance_1_8.suite());
        suite.addTest(ScriptFolderTests.suite());
        suite.addTest(STCScriptsTests.suite());
        if (org.eclipse.jdt.core.tests.util.GroovyUtils.isAtLeastGroovy(23))
            suite.addTest(TraitsTests.suite());
        suite.addTest(TransformationsTests.suite());

        // Location tests
        suite.addTest(ASTConverterTests.suite());
        suite.addTest(ASTNodeSourceLocationsTests.suite());
        suite.addTestSuite(LocationSupportTests.class);
        suite.addTest(SourceLocationsTests.suite());

        // Model tests
        suite.addTest(ASTTransformsTests.suite());
        suite.addTest(GroovyClassFileTests.suite());
        suite.addTest(GroovyCompilationUnitTests.suite());
        suite.addTest(GroovyContentTypeTests.suite());
        suite.addTest(GroovyPartialModelTests.suite());
        suite.addTest(MoveRenameCopyTests.suite());

        // Search tests
        suite.addTest(AllSearchTests.suite());

        return suite;
    }
}
