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
package org.codehaus.groovy.eclipse.editor.highlighting;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.codehaus.groovy.eclipse.GroovyPlugin;
import org.codehaus.groovy.eclipse.core.GroovyCore;
import org.codehaus.groovy.eclipse.core.preferences.PreferenceConstants;
import org.codehaus.groovy.eclipse.editor.GroovyEditor;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingPresenter;
import org.eclipse.jdt.internal.ui.text.JavaPresentationReconciler;
import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * @author Andrew Eisenberg
 * @created Oct 13, 2009
 */
public class GroovySemanticReconciler implements IJavaReconcilingListener {

    private static final String GROOVY_HIGHLIGHT_PREFERENCE             = PreferenceConstants.GROOVY_EDITOR_HIGHLIGHT_GJDK_COLOR.replaceFirst("\\.color$", "");
    private static final String STRING_HIGHLIGHT_PREFERENCE             = PreferenceConstants.GROOVY_EDITOR_HIGHLIGHT_STRINGS_COLOR.replaceFirst("\\.color$", "");
    private static final String NUMBER_HIGHLIGHT_PREFERENCE             = "semanticHighlighting.number";
    private static final String VARIABLE_HIGHLIGHT_PREFERENCE           = "semanticHighlighting.localVariable";
    private static final String PARAMETER_HIGHLIGHT_PREFERENCE          = "semanticHighlighting.parameterVariable";
    private static final String ANNOTATION_HIGHLIGHT_PREFERENCE         = "semanticHighlighting.annotationElementReference";
    private static final String DEPRECATED_HIGHLIGHT_PREFERENCE         = "semanticHighlighting.deprecatedMember";
    private static final String OBJECT_FIELD_HIGHLIGHT_PREFERENCE       = "semanticHighlighting.field";
    private static final String STATIC_FIELD_HIGHLIGHT_PREFERENCE       = "semanticHighlighting.staticField";
    private static final String STATIC_VALUE_HIGHLIGHT_PREFERENCE       = "semanticHighlighting.staticFinalField";
    private static final String OBJECT_METHOD_HIGHLIGHT_PREFERENCE      = "semanticHighlighting.method";
    private static final String STATIC_METHOD_HIGHLIGHT_PREFERENCE      = "semanticHighlighting.staticMethodInvocation";
    private static final String METHOD_DECLARATION_HIGHLIGHT_PREFERENCE = "semanticHighlighting.methodDeclarationName";

    // these types have package-private visibility
    private static Method GET_HIGHLIGHTING = null;
    private static Constructor<?> HIGHLIGHTING_STYLE;
    private static Constructor<?> HIGHLIGHTED_POSITION;
    static {
        try {
            Class<?> style = Class.forName("org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager$Highlighting");
            HIGHLIGHTING_STYLE = ReflectionUtils.getConstructor(style, TextAttribute.class, boolean.class);

            Class<?> position = Class.forName("org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager$HighlightedPosition");
            HIGHLIGHTED_POSITION = ReflectionUtils.getConstructor(position, int.class, int.class, style, Object.class);

            GET_HIGHLIGHTING = position.getDeclaredMethod("getHighlighting");
            GET_HIGHLIGHTING.setAccessible(true);

        } catch (ClassNotFoundException cnfe) {
            HIGHLIGHTING_STYLE = null;
            HIGHLIGHTED_POSITION = null;
            GroovyPlugin.getDefault().logException("Semantic highlighting disabled", cnfe);
        } catch (NoSuchMethodException nsme) {
        }
    }

    private volatile GroovyEditor editor;
    private final Semaphore lock = new Semaphore(1);
    private SemanticHighlightingPresenter presenter;

    // make these configurable
    private Object mapKeyHighlighting;
    private Object tagKeyHighlighting;
    private Object numberRefHighlighting;
    private Object regexpRefHighlighting;
    private Object undefinedRefHighlighting;
    private Object deprecatedRefHighlighting;

    private Object localHighlighting;
    private Object paramHighlighting;

    private Object objectFieldHighlighting;
    private Object staticFieldHighlighting;
    private Object staticValueHighlighting;

    private Object methodDefHighlighting;
    private Object methodUseHighlighting;
    private Object groovyMethodUseHighlighting;
    private Object staticMethodUseHighlighting;


    public GroovySemanticReconciler() {
        // TODO: Reload colors and styles when preferences are changed.
        IPreferenceStore javaPrefs = JavaPlugin.getDefault().getPreferenceStore();
        IPreferenceStore groovyPrefs = GroovyPlugin.getDefault().getPreferenceStore();

        Color groovyColor      = loadColorFrom(groovyPrefs, GROOVY_HIGHLIGHT_PREFERENCE);
        Color numberColor      = loadColorFrom(javaPrefs, NUMBER_HIGHLIGHT_PREFERENCE);
        Color stringColor      = loadColorFrom(groovyPrefs, STRING_HIGHLIGHT_PREFERENCE);
        Color tagKeyColor      = loadColorFrom(javaPrefs, ANNOTATION_HIGHLIGHT_PREFERENCE);
        Color parameterColor   = loadColorFrom(javaPrefs, PARAMETER_HIGHLIGHT_PREFERENCE);
        Color variableColor    = loadColorFrom(javaPrefs, VARIABLE_HIGHLIGHT_PREFERENCE);
        Color objectFieldColor = loadColorFrom(javaPrefs, OBJECT_FIELD_HIGHLIGHT_PREFERENCE);
        Color staticFieldColor = loadColorFrom(javaPrefs, STATIC_FIELD_HIGHLIGHT_PREFERENCE);
        Color staticValueColor = loadColorFrom(javaPrefs, STATIC_VALUE_HIGHLIGHT_PREFERENCE);
        Color staticCallColor  = loadColorFrom(javaPrefs, STATIC_METHOD_HIGHLIGHT_PREFERENCE);
        Color methodCallColor  = loadColorFrom(javaPrefs, OBJECT_METHOD_HIGHLIGHT_PREFERENCE);
        Color methodDeclColor  = loadColorFrom(javaPrefs, METHOD_DECLARATION_HIGHLIGHT_PREFERENCE);

        mapKeyHighlighting = newHighlightingStyle(stringColor);
        tagKeyHighlighting = newHighlightingStyle(tagKeyColor, loadStyleFrom(javaPrefs, ANNOTATION_HIGHLIGHT_PREFERENCE));
        numberRefHighlighting = newHighlightingStyle(numberColor, loadStyleFrom(javaPrefs, NUMBER_HIGHLIGHT_PREFERENCE));
        regexpRefHighlighting = newHighlightingStyle(stringColor, SWT.ITALIC | loadStyleFrom(groovyPrefs, STRING_HIGHLIGHT_PREFERENCE));
        deprecatedRefHighlighting = newHighlightingStyle(null, loadStyleFrom(javaPrefs, DEPRECATED_HIGHLIGHT_PREFERENCE));
        undefinedRefHighlighting = newHighlightingStyle(null, TextAttribute.UNDERLINE);

        localHighlighting = newHighlightingStyle(variableColor, loadStyleFrom(javaPrefs, VARIABLE_HIGHLIGHT_PREFERENCE));
        paramHighlighting = newHighlightingStyle(parameterColor, loadStyleFrom(javaPrefs, PARAMETER_HIGHLIGHT_PREFERENCE));

        objectFieldHighlighting = newHighlightingStyle(objectFieldColor, loadStyleFrom(javaPrefs, OBJECT_FIELD_HIGHLIGHT_PREFERENCE));
        staticFieldHighlighting = newHighlightingStyle(staticFieldColor, loadStyleFrom(javaPrefs, STATIC_FIELD_HIGHLIGHT_PREFERENCE));
        staticValueHighlighting = newHighlightingStyle(staticValueColor, loadStyleFrom(javaPrefs, STATIC_VALUE_HIGHLIGHT_PREFERENCE));

        methodDefHighlighting = newHighlightingStyle(methodDeclColor, loadStyleFrom(javaPrefs, METHOD_DECLARATION_HIGHLIGHT_PREFERENCE));
        methodUseHighlighting = newHighlightingStyle(methodCallColor, loadStyleFrom(javaPrefs, OBJECT_METHOD_HIGHLIGHT_PREFERENCE));
        groovyMethodUseHighlighting = newHighlightingStyle(groovyColor, loadStyleFrom(groovyPrefs, GROOVY_HIGHLIGHT_PREFERENCE));
        staticMethodUseHighlighting = newHighlightingStyle(staticCallColor, loadStyleFrom(javaPrefs, STATIC_METHOD_HIGHLIGHT_PREFERENCE));
    }

    protected static Color loadColorFrom(IPreferenceStore prefs, String which) {
        RGB color;
        if (!prefs.contains(which + ".enabled") || prefs.getBoolean(which + ".enabled")) {
            color = PreferenceConverter.getColor(prefs, which + ".color");
        } else {
            return null; // allow contextual default (i.e. string color)
            //color = PreferenceConverter.getColor(prefs, "java_default");
            //color = PreferenceConverter.getColor(GroovyPlugin.getDefault().getPreferenceStore(), PreferenceConstants.GROOVY_EDITOR_DEFAULT_COLOR);
        }
        return GroovyPlugin.getDefault().getTextTools().getColorManager().getColor(color);
    }

    protected static int loadStyleFrom(IPreferenceStore prefs, String which) {
        int style = SWT.NONE;

        if (!prefs.contains(which + ".enabled") || prefs.getBoolean(which + ".enabled")) {

            if (prefs.getBoolean(which + ".bold") ||
                    prefs.getBoolean(which + ".color_bold"))
                style |= SWT.BOLD;
            if (prefs.getBoolean(which + ".italic"))
                style |= SWT.ITALIC;
            if (prefs.getBoolean(which + ".underline"))
                style |= TextAttribute.UNDERLINE;
            if (prefs.getBoolean(which + ".strikethrough"))
                style |= TextAttribute.STRIKETHROUGH;
        }

        return style;
    }

    protected Object newHighlightingStyle(Color color) {
        //return new HighlightingStyle(new TextAttribute(color), true);
        return ReflectionUtils.invokeConstructor(HIGHLIGHTING_STYLE, new TextAttribute(color), Boolean.TRUE);
    }

    protected Object newHighlightingStyle(Color color, int style) {
        //return new HighlightingStyle(new TextAttribute(color, null, style), true);
        return ReflectionUtils.invokeConstructor(HIGHLIGHTING_STYLE, new TextAttribute(color, null, style), Boolean.TRUE);
    }

    public void install(GroovyEditor editor, JavaSourceViewer viewer) {
        this.editor = editor;
        this.presenter = new SemanticHighlightingPresenter();
        presenter.install(viewer, (JavaPresentationReconciler) editor.getGroovyConfiguration().getPresentationReconciler(viewer));
    }

    public void uninstall() {
        presenter.uninstall();
        presenter = null;
        editor = null;
    }

    public void aboutToBeReconciled() {
        // anything to do here?
    }

    public void reconciled(CompilationUnit ast, boolean forced, IProgressMonitor monitor) {
        // ensure that only one thread performs this task
        if (ast != null && lock.tryAcquire())
        try {
            if (editor == null) return; // uninstalled?
            monitor.beginTask("Groovy semantic highlighting", 10);
            GroovyCompilationUnit unit = editor.getGroovyCompilationUnit();
            if (unit != null) {
                presenter.setCanceled(monitor.isCanceled());
                if (update(monitor, 1)) return;

                GatherSemanticReferences finder = new GatherSemanticReferences(unit);
                Collection<HighlightedTypedPosition> semanticReferences = finder.findSemanticHighlightingReferences();
                if (update(monitor, 5)) return;

                List<Position> newPositions = new ArrayList<Position>(semanticReferences.size());
                List<Position> oldPositions = new LinkedList<Position>(getHighlightedPositions());
                if (update(monitor, 1)) return;

                for (HighlightedTypedPosition ref : semanticReferences) {
                    Position pos = newHighlightedPosition(ref);
                    tryAddPosition(newPositions, oldPositions, pos);
                }
                if (update(monitor, 2)) return;

                TextPresentation textPresentation = null;
                if (!presenter.isCanceled()) {
                    textPresentation = presenter.createPresentation(newPositions, oldPositions);
                }
                if (!presenter.isCanceled()) {
                    updatePresentation(textPresentation, newPositions, oldPositions);
                }
                update(monitor, 1);
            }
        } catch (Exception e) {
            GroovyCore.logException("Semantic highlighting failed", e);
        } finally {
            monitor.done();
            lock.release();
        }
    }

    private boolean update(IProgressMonitor monitor, int units) {
        monitor.worked(units);
        return monitor.isCanceled();
    }

    @SuppressWarnings("unchecked")
    private List<Position> getHighlightedPositions() {
        // NOTE: Be very careful with this; fPositions is often accessed synchronously!
        return (List<Position>) ReflectionUtils.getPrivateField(SemanticHighlightingPresenter.class, "fPositions", presenter);
    }

    private Position newHighlightedPosition(HighlightedTypedPosition pos) {
        Object style = null;
        switch (pos.kind) {
            case DEPRECATED:
                style = deprecatedRefHighlighting;
                break;
            case UNKNOWN:
                style = undefinedRefHighlighting;
                break;
            case NUMBER:
                style = numberRefHighlighting;
                break;
            case REGEXP:
                style = regexpRefHighlighting;
                break;
            case MAP_KEY:
                style = mapKeyHighlighting;
                break;
            case TAG_KEY:
                style = tagKeyHighlighting;
                break;
            case VARIABLE:
                style = localHighlighting;
                break;
            case PARAMETER:
                style = paramHighlighting;
                break;
            case FIELD:
                style = objectFieldHighlighting;
                break;
            case STATIC_FIELD:
                style = staticFieldHighlighting;
                break;
            case STATIC_VALUE:
                style = staticValueHighlighting;
                break;
            case CTOR:
            case METHOD:
            case STATIC_METHOD:
                style = methodDefHighlighting;
                break;
            case CTOR_CALL:
            case METHOD_CALL:
                style = methodUseHighlighting;
                break;
            case GROOVY_CALL:
                style = groovyMethodUseHighlighting;
                break;
            case STATIC_CALL:
                style = staticMethodUseHighlighting;
                break;
        }
        //return new HighlightedPosition(pos.offset, pos.length, style, this);
        return (Position) ReflectionUtils.invokeConstructor(HIGHLIGHTED_POSITION, pos.offset, pos.length, style, this);
    }

    private void tryAddPosition(List<Position> newPositions, List<Position> oldPositions, Position maybePosition) {
        boolean found = false; // TODO: Is there a quicker way to search for matches?  These can be sorted easily.
        for (Iterator<Position> it = oldPositions.iterator(); it.hasNext();) {
            Position oldPosition = it.next();
            if (!oldPosition.isDeleted() && oldPosition.equals(maybePosition) && isSameStyle(oldPosition, maybePosition)) {
                found = true;
                it.remove();
                break;
            }
        }
        if (!found) {
            newPositions.add(maybePosition);
        }
    }

    private boolean isSameStyle(Position a, Position b) {
        if (GET_HIGHLIGHTING != null) {
            try {
                return (GET_HIGHLIGHTING.invoke(a) == GET_HIGHLIGHTING.invoke(b));
            } catch (IllegalAccessException e) {
                // fall through
            } catch (IllegalArgumentException e) {
                // fall through
            } catch (InvocationTargetException e) {
                // fall through
            }
        }
        return true;
    }

    /**
     * Update the presentation.
     *
     * @param textPresentation the text presentation
     * @param addedPositions the added positions
     * @param removedPositions the removed positions
     */
    private void updatePresentation(TextPresentation textPresentation, List<Position> addedPositions, List<Position> removedPositions) {
        Runnable runnable = presenter.createUpdateRunnable(textPresentation, addedPositions, removedPositions);
        if (runnable == null)
            return;

        JavaEditor thisEditor = editor;
        if (thisEditor == null)
            return;

        IWorkbenchPartSite site = thisEditor.getSite();
        if (site == null)
            return;

        Shell shell = site.getShell();
        if (shell == null || shell.isDisposed())
            return;

        Display display = shell.getDisplay();
        if (display == null || display.isDisposed())
            return;

        display.asyncExec(runnable);
    }
}
