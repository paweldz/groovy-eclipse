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
package org.codehaus.groovy.eclipse.test;

import static org.codehaus.groovy.eclipse.core.model.GroovyRuntime.ensureGroovyClasspathContainer;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.eclipse.core.builder.GroovyClasspathContainer;
import org.codehaus.groovy.eclipse.core.model.GroovyProjectFacade;
import org.codehaus.jdt.groovy.model.GroovyNature;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.groovy.tests.builder.SimpleProgressMonitor;
import org.eclipse.jdt.core.tests.util.Util;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.launching.JavaRuntime;

public class TestProject {

    public static final String TEST_PROJECT_NAME = "TestProject";

    private final IProject project;

    private final IJavaProject javaProject;

    private IPackageFragmentRoot sourceFolder;

    public TestProject() throws CoreException {
        this(TEST_PROJECT_NAME);
    }

    public TestProject(String name) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        project = root.getProject(name);
        if (!project.exists()) {
            project.create(null);
        }
        project.open(null);

        javaProject = JavaCore.create(project);
        prepareForJava();
        prepareForGroovy();

        createOutputFolder(createBinFolder());
        sourceFolder = createSourceFolder();

        javaProject.setOption(CompilerOptions.OPTION_Source, "1.5");
        javaProject.setOption(CompilerOptions.OPTION_Compliance, "1.5");
        javaProject.setOption(CompilerOptions.OPTION_TargetPlatform, "1.5");
    }

    public IProject getProject() {
        return project;
    }

    public IJavaProject getJavaProject() {
        return javaProject;
    }

    public IPackageFragmentRoot getSourceFolder() {
        return sourceFolder;
    }

    public GroovyProjectFacade getGroovyProjectFacade() {
        return new GroovyProjectFacade(javaProject);
    }

    public boolean hasGroovyContainer() throws JavaModelException {
        IClasspathEntry[] entries = javaProject.getRawClasspath();
        for (int i = 0, n = entries.length; i < n; i += 1) {
            if (entries[i].getEntryKind() == IClasspathEntry.CPE_CONTAINER &&
                    entries[i].getPath().equals(GroovyClasspathContainer.CONTAINER_ID)) {
                return true;
            }
        }
        return false;
    }

    /** Adds base Java nature and classpath entries to project. */
    private void prepareForJava() throws CoreException {
        addNature(JavaCore.NATURE_ID);

        javaProject.setRawClasspath(new IClasspathEntry[] {JavaRuntime.getDefaultJREContainerEntry()}, null);
    }

    /** Adds base Groovy nature and classpath entries to project. */
    private void prepareForGroovy() throws CoreException {
        addNature(GroovyNature.GROOVY_NATURE);

        if (!hasGroovyContainer()) {
            ensureGroovyClasspathContainer(getJavaProject(), true);
        }
    }

    public IPackageFragment createPackage(String name) throws CoreException {
        return sourceFolder.createPackageFragment(name, true, null);
    }

    public void deletePackage(String name) throws CoreException {
        sourceFolder.getPackageFragment(name).delete(true, null);
    }

    public ICompilationUnit createJavaType(IPackageFragment packageFrag, String fileName, CharSequence source) throws JavaModelException {
        StringBuilder buf = new StringBuilder();
        if (!packageFrag.isDefaultPackage()) {
            buf.append("package ");
            buf.append(packageFrag.getElementName());
            buf.append(";");
            buf.append(System.getProperty("line.separator"));
            buf.append(System.getProperty("line.separator"));
        }
        buf.append(source);

        ICompilationUnit unit = packageFrag.createCompilationUnit(fileName, buf.toString(), false, null);
        unit.becomeWorkingCopy(null);
        return unit;
    }

    public ICompilationUnit createJavaTypeAndPackage(String packageName, String fileName, CharSequence source) throws CoreException {
        return createJavaType(createPackage(packageName), fileName, source);
    }

    public ICompilationUnit createGroovyTypeAndPackage(String packageName, String fileName, InputStream source) throws CoreException, IOException {
        return createGroovyType(createPackage(packageName), fileName, IOUtils.toString(source));
    }

    public ICompilationUnit createGroovyTypeAndPackage(String packageName, String fileName, CharSequence source) throws CoreException {
        return createGroovyType(createPackage(packageName), fileName, source);
    }

    public ICompilationUnit createGroovyType(IPackageFragment packageFrag, String fileName, CharSequence source) throws CoreException {
        StringBuilder buf = new StringBuilder();
        if (!packageFrag.isDefaultPackage()) {
            buf.append("package ");
            buf.append(packageFrag.getElementName());
            buf.append(";");
            buf.append(System.getProperty("line.separator"));
            buf.append(System.getProperty("line.separator"));
        }
        buf.append(source);

        ICompilationUnit unit = packageFrag.createCompilationUnit(fileName, buf.toString(), false, null);
        unit.becomeWorkingCopy(null);
        return unit;
    }

    public void addBuilder(String newBuilder) throws CoreException {
        final IProjectDescription description = project.getDescription();
        ICommand[] commands = description.getBuildSpec();
        ICommand newCommand = new BuildCommand();
        newCommand.setBuilderName(newBuilder);
        ICommand[] newCommands = new ICommand[commands.length+1];
        newCommands[0] = newCommand;
        System.arraycopy(commands, 0, newCommands, 1, commands.length);
        description.setBuildSpec(newCommands);
        project.setDescription(description, null);
    }

    public void addNature(String natureId) throws CoreException {
        final IProjectDescription description = project.getDescription();
        final String[] ids = description.getNatureIds();
        final String[] newIds = new String[ids.length+1];
        newIds[0] = natureId;
        System.arraycopy(ids, 0, newIds, 1, ids.length);
        description.setNatureIds(newIds);
        project.setDescription(description, null);
    }

    public void removeNature(String natureId) throws CoreException {
        final IProjectDescription description = project.getDescription();
        final String[] ids = description.getNatureIds();
        for (int i = 0; i < ids.length; ++i) {
            if (ids[i].equals(natureId)) {
                final String[] newIds = remove(ids, i);
                description.setNatureIds(newIds);
                project.setDescription(description, null);
                return;
            }
        }
    }

    private String[] remove(String[] ids, int index) {
        String[] newIds = new String[ids.length-1];
        for (int i = 0, j = 0; i < ids.length; i++) {
            if (i != index) {
                newIds[j] = ids[i];
                j++;
            }
        }
        return newIds;
    }

    public void dispose() throws CoreException {
        deleteWorkingCopies();
        Util.delete(project);
    }

    public void deleteContents() throws CoreException {
        deleteWorkingCopies();
        IPackageFragment[] frags = javaProject.getPackageFragments();
        for (IPackageFragment frag : frags) {
            if (!frag.isReadOnly()) {
                frag.delete(true, null);
            }
        }
    }

    private void deleteWorkingCopies() throws JavaModelException {
        SynchronizationUtils.joinBackgroudActivities();

        ICompilationUnit[] workingCopies = JavaModelManager.getJavaModelManager().getWorkingCopies(DefaultWorkingCopyOwner.PRIMARY, true);
        if (workingCopies != null && workingCopies.length > 0) {
            for (ICompilationUnit workingCopy : workingCopies) {
                if (workingCopy.isWorkingCopy()) {
                    workingCopy.discardWorkingCopy();
                }
            }
        }

        System.gc();
    }

    private IFolder createBinFolder() throws CoreException {
        final IFolder binFolder = project.getFolder("bin");
        if (!binFolder.exists())
            ensureExists(binFolder);
        return binFolder;
    }

    private void createOutputFolder(IFolder binFolder) throws JavaModelException {
        IPath outputLocation = binFolder.getFullPath();
        javaProject.setOutputLocation(outputLocation, null);
    }

    private IPackageFragmentRoot createSourceFolder() throws CoreException {
        IFolder folder = project.getFolder("src");
        if (!folder.exists()) ensureExists(folder);
        final IClasspathEntry[] entries = javaProject.getResolvedClasspath(false);
        final IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(folder);
        for (int i = 0, n = entries.length; i < n; i += 1) {
            if (entries[i].getPath().equals(folder.getFullPath())) {
                return root;
            }
        }
        IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
        newEntries[0] = JavaCore.newSourceEntry(root.getPath());
        System.arraycopy(oldEntries, 0, newEntries, 1, oldEntries.length);
        javaProject.setRawClasspath(newEntries, null);
        return root;
    }

    public IPackageFragmentRoot createOtherSourceFolder() throws CoreException {
        return createOtherSourceFolder(null);
    }

    public IPackageFragmentRoot createOtherSourceFolder(String outPath) throws CoreException {
        return createSourceFolder("other", outPath);
    }

    public IPackageFragmentRoot createSourceFolder(String path, String outPath) throws CoreException {
        return createSourceFolder(path, outPath, null);
    }

    public IPackageFragmentRoot createSourceFolder(String path, String outPath, IPath[] exclusionPattern) throws CoreException {
        IFolder folder = project.getFolder(path);
        if (!folder.exists()) {
            ensureExists(folder);
        }

        final IClasspathEntry[] entries = javaProject.getResolvedClasspath(false);
        final IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(folder);
        for (int i = 0; i < entries.length; i++) {
            final IClasspathEntry entry = entries[i];
            if (entry.getPath().equals(folder.getFullPath())) {
                return root;
            }
        }
        IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
        System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
        IPath outPathPath = outPath == null ? null : getProject().getFullPath().append(outPath).makeAbsolute();
        newEntries[oldEntries.length] = JavaCore.newSourceEntry(root.getPath(), exclusionPattern, outPathPath);
        javaProject.setRawClasspath(newEntries, null);
        return root;

    }

    private void ensureExists(IFolder folder) throws CoreException {
        if (folder.getParent().getType() == IResource.FOLDER && !folder.getParent().exists()) {
            ensureExists((IFolder) folder.getParent());
        }
        folder.create(false, true, null);
    }

    public void addProjectReference(IJavaProject referent) throws JavaModelException {
        IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
        System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
        newEntries[oldEntries.length] = JavaCore.newProjectEntry(referent.getPath());
        javaProject.setRawClasspath(newEntries, null);
    }

    public void addJarFileToClasspath(IPath path) throws JavaModelException {
        IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
        System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
        newEntries[oldEntries.length] = JavaCore.newLibraryEntry(path, null, null);
        javaProject.setRawClasspath(newEntries, null);
    }

    public void waitForIndexer() {
        SynchronizationUtils.waitForIndexingToComplete(getJavaProject());
    }

    public void fullBuild() throws CoreException {
        SimpleProgressMonitor spm = new SimpleProgressMonitor("full build of "+this.getProject().getName());
        this.getProject().build(org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD, spm);
        spm.waitForCompletion();
    }

    public String getProblems() throws CoreException {
        IMarker[] markers = getProject().findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        if (markers == null || markers.length == 0) {
            return null;
        }
        boolean errorFound = false;
        StringBuilder sb = new StringBuilder("Problems:\n");
        for (IMarker marker : markers) {
            if (((Number) marker.getAttribute(IMarker.SEVERITY)).intValue() == IMarker.SEVERITY_ERROR) {
                errorFound = true;

                sb.append("  ");
                sb.append(marker.getResource().getName());
                sb.append(" : ");
                sb.append(marker.getAttribute(IMarker.LOCATION));
                sb.append(" : ");
                sb.append(marker.getAttribute(IMarker.MESSAGE));
                sb.append("\n");
            }
        }
        return errorFound ? sb.toString() : null;
    }

    public IFile createFile(String name, String contents) throws Exception {
        String encoding = null;
        try {
            encoding = project.getDefaultCharset(); // get project encoding as file is not accessible
        } catch (CoreException ce) {
            // use no encoding
        }
        InputStream stream = new ByteArrayInputStream(encoding == null ? contents.getBytes() : contents.getBytes(encoding));
        IFile file = project.getFolder("src").getFile(new Path(name));
        if (!file.getParent().exists()) {
            createFolder(file.getParent());
        }
        file.create(stream, true, null);
        return file;
    }

    private void createFolder(IContainer parent) throws CoreException {
        if (!parent.getParent().exists()) {
            assertEquals("Project doesn't exist " + parent.getParent(), parent.getParent().getType(), IResource.FOLDER);
            createFolder(parent.getParent());
        }
        ((IFolder) parent).create(true, true, null);
    }

    public ICompilationUnit[] createUnits(String[] packages, String[] cuNames, String[] cuContents) throws CoreException {
        ICompilationUnit[] units = new ICompilationUnit[packages.length];
        for (int i = 0, n = cuContents.length; i < n; i += 1) {
            units[i] = createPackage(packages[i]).createCompilationUnit(cuNames[i], cuContents[i], false, null);
        }
        return units;
    }

    public static void addEntry(IProject project, IClasspathEntry entryPath) throws JavaModelException {
        IClasspathEntry[] classpath = getClasspath(project);
        IClasspathEntry[] newClaspath = new IClasspathEntry[classpath.length + 1];
        System.arraycopy(classpath, 0, newClaspath, 0, classpath.length);
        newClaspath[classpath.length] = entryPath;
        setClasspath(project, newClaspath);
    }

    public static IClasspathEntry[] getClasspath(IProject project) {
        try {
            JavaProject javaProject = (JavaProject) JavaCore.create(project);
            return javaProject.getExpandedClasspath();
        } catch (JavaModelException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void addExternalLibrary(IProject project, String jar) throws JavaModelException {
        addExternalLibrary(project, jar, false);
    }

    public static void addExternalLibrary(IProject project, String jar, boolean isExported) throws JavaModelException {
        addEntry(project, JavaCore.newLibraryEntry(new Path(jar), null, null, isExported));
    }

    public static void setClasspath(IProject project, IClasspathEntry[] entries) throws JavaModelException {
        IJavaProject javaProject = JavaCore.create(project);
        javaProject.setRawClasspath(entries, null);
    }

    public static void setAutoBuilding(boolean value) {
        try {
            IWorkspace w = ResourcesPlugin.getWorkspace();
            IWorkspaceDescription d = w.getDescription();
            d.setAutoBuilding(value);
            w.setDescription(d);
        } catch (CoreException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static boolean isAutoBuilding() {
        IWorkspace w = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription d = w.getDescription();
        return d.isAutoBuilding();
    }
}
