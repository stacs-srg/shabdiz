/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.nds.madface;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import uk.ac.standrews.cs.nds.madface.exceptions.UnknownPlatformException;
import uk.ac.standrews.cs.nds.util.ErrorHandling;

/**
 * Representation of a Java class path.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class ClassPath implements Iterable<File>, Cloneable {

    private volatile List<String> classpath_entries;
    private static volatile PlatformDescriptor local_platform = null;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Returns the class path of the currently executing program.
     * @return the class path of the currently executing program
     */
    public static ClassPath getCurrentClassPath() {

        // Work around bug on Windows/Eclipse where part of the classpath contains forward slashes.
        final String environment_class_path = System.getProperty("java.class.path").replace("/", getLocalFileSeparator());
        String proper = environment_class_path.replace(";\\", ";").replace("\\;", ";");
        if (proper.charAt(proper.length() - 1) == '\\') {
            proper = proper.substring(0, proper.length() - 2);
        }

        return new ClassPath(proper);
    }

    // -------------------------------------------------------------------------------------------------------

    private static PlatformDescriptor getLocalPlatform() {

        // Race condition here but doesn't matter since it would just get initialized twice.
        // TODO load lazily with separate class.
        if (local_platform == null) {
            local_platform = new HostDescriptor().getPlatform();
        }
        return local_platform;
    }

    private static String getLocalPathSeparator() {

        try {
            return getLocalPlatform().getClassPathSeparator();
        }
        catch (final UnknownPlatformException e) {
            ErrorHandling.hardExceptionError(e, "couldn't get classpath separator for local platform");
            return null;
        }
    }

    private static String getLocalFileSeparator() {

        try {
            return getLocalPlatform().getFileSeparator();
        }
        catch (final UnknownPlatformException e) {
            ErrorHandling.hardExceptionError(e, "couldn't get file path separator for local platform");
            return null;
        }
    }

    /**
     * Creates a new empty class path.
     */
    public ClassPath() {

        classpath_entries = new ArrayList<String>();
    }

    /**
     * Creates a new class path with entries specified in standard string notation for this platform.
     *
     * @param classpath_string the class path entries
     */
    public ClassPath(final String classpath_string) {

        this(classpath_string.split(getLocalPathSeparator()));
    }

    /**
     * Creates a new class path with specified entries.
     *
     * @param classpath_element_paths the class path entries
     */
    public ClassPath(final String[] classpath_element_paths) {

        this();

        for (final String classpath_element_path : classpath_element_paths) {
            classpath_entries.add(classpath_element_path);
        }
    }

    /**
     * Creates a new class path with specified entries.
     *
     * @param classpath_elements the class path entries
     */
    public ClassPath(final File[] classpath_elements) {

        this();
        for (final File f : classpath_elements) {
            classpath_entries.add(f.toString());
        }
    }

    /**
     * Creates a new class path with specified entries.
     *
     * @param classpath_elements the class path entries
     */
    public ClassPath(final Iterable<File> classpath_elements) {

        this();
        for (final File entry : classpath_elements) {
            classpath_entries.add(entry.toString());
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Returns a representation of the class path formatted for the current local platform.
     *
     * @param use_path_quote true if the resulting string should be surrounded with appropriate quote characters
     * @return a representation of the class path
     */
    public synchronized String toString(final boolean use_path_quote) {

        try {
            return toString(getLocalPlatform(), use_path_quote);
        }
        catch (final UnknownPlatformException e) {
            ErrorHandling.hardExceptionError(e, "couldn't get local platform");
            return null;
        }
    }

    /**
     * Returns a representation of the class path formatted for the given platform.
     *
     * @param platform a platform
     * @return a representation of the class path
     */
    public synchronized String toString(final PlatformDescriptor platform) {

        try {
            return toString(platform, true);
        }
        catch (final UnknownPlatformException e) {
            return "classpath for unknown platform";
        }
    }

    /**
     * Returns a representation of the class path formatted for the given platform.
     *
     * @param platform a platform
     * @param use_path_quote true if the resulting string should be surrounded with appropriate quote characters
     * @return a representation of the class path
     * @throws UnknownPlatformException 
     */
    public synchronized String toString(final PlatformDescriptor platform, final boolean use_path_quote) throws UnknownPlatformException {

        final String path_separator = platform.getClassPathSeparator();
        final String path_quote = use_path_quote ? platform.getPathQuote() : "";

        final StringBuilder classpath = new StringBuilder(path_quote);

        boolean first = true;
        for (final String path_element : classpath_entries) {

            if (first) {
                first = false;
            }
            else {
                classpath.append(path_separator);
            }

            classpath.append(path_element);
        }
        classpath.append(path_quote);

        return classpath.toString();
    }

    /**
     * Adds an entry to the class path at the given position. Position 0 corresponds to the start of the class path.
     *
     * @param index the index at which the entry should be added
     * @param entry the entry to be added
     */
    public void add(final int index, final File entry) {

        classpath_entries.add(index, entry.toString());
    }

    /**
     * Adds an entry to the end of the class path.
     *
     * @param entry the entry to be added
     */
    public void append(final File entry) {

        classpath_entries.add(entry.toString());
    }

    /**
     * Adds all entries in the given class path to this class path.
     *
     * @param other_path the other class path
     */
    public void append(final ClassPath other_path) {

        classpath_entries.addAll(other_path.classpath_entries);
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Returns an iterator over the entries in the class path, each an instance of File representing either a directory or a jar file.
     * 
     * @return an iterator over the entries in the class path
     */
    @Override
    public Iterator<File> iterator() {

        final List<File> filelist = new ArrayList<File>();

        for (final String s : classpath_entries) {
            filelist.add(new File(s));
        }

        return filelist.iterator();
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Modifies all relative paths to resolve them relative to the given root.
     *
     * @param root a root directory
     */
    public void resolveRelativePaths(final File root) {

        final List<String> new_classpath_entries = new ArrayList<String>();

        for (final String entry : classpath_entries) {
            if (new File(entry).isAbsolute()) {
                new_classpath_entries.add(entry);
            }
            else {
                new_classpath_entries.add(root.getAbsolutePath() + File.separator + new File(entry).getPath());
            }
        }

        classpath_entries = new_classpath_entries;
    }

    @Override
    public ClassPath clone() {

        final int size = size();
        final String[] strings = new String[size];

        for (int i = 0; i < size; i++) {
            strings[i] = classpath_entries.get(i);
        }

        return new ClassPath(strings);
    }

    @Override
    public String toString() {

        return toString(true);
    }

    public int size() {

        return classpath_entries.size();
    }
}
