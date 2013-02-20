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
package uk.ac.standrews.cs.shabdiz.active;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import uk.ac.standrews.cs.shabdiz.active.exceptions.UnknownPlatformException;

/**
 * Representation of a Java class path.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class ClassPath implements Iterable<File>, Cloneable {

    private volatile List<String> classpath_entries;
    public static final int NUMBER_OF_BLUB_NODES = 60;
    public static final Map<String, String> JAVA_BIN_PATHS = new HashMap<String, String>();
    static {
        initJavaBinPaths();
    }

    /**
     * Returns the class path of the currently executing program.
     * 
     * @return the class path of the currently executing program
     */
    public static ClassPath getCurrentClassPath() {

        // Work around bug on Windows/Eclipse where part of the classpath contains forward slashes.
        final String environment_class_path = System.getProperty("java.class.path").replace("/", File.separator);
        String proper = environment_class_path.replace(";\\", ";").replace("\\;", ";");
        if (proper.charAt(proper.length() - 1) == '\\') {
            proper = proper.substring(0, proper.length() - 2);
        }

        return new ClassPath(proper);
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

        this(classpath_string.split(File.pathSeparator));
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
            classpath_entries.add(f.getAbsolutePath());
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

    /**
     * Gets the number of classpath entries.
     * 
     * @return the number of classpath entries
     */
    public int size() {

        return classpath_entries.size();
    }

    /**
     * Returns a representation of the class path formatted for the given platform.
     * 
     * @param platform a platform
     * @param use_path_quote true if the resulting string should be surrounded with appropriate quote characters
     * @return a representation of the class path
     * @throws UnknownPlatformException
     */
    public synchronized String toString(final boolean use_path_quote) {

        final String path_quote = ""; //use_path_quote ? platform.getPathQuote() : "";
        final StringBuilder classpath = new StringBuilder(path_quote);

        boolean first = true;
        for (final String path_element : classpath_entries) {

            if (first) {
                first = false;
            }
            else {
                classpath.append(File.pathSeparator);
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

    /**
     * Returns an iterator over the entries in the class path, each an instance of File representing either a directory or a jar file.
     * 
     * @return an iterator over the entries in the class path
     */
    @Override
    public Iterator<File> iterator() {

        return new CopyOnWriteArrayList(classpath_entries).iterator();
    }

    private static void initJavaBinPaths() {

        JAVA_BIN_PATHS.put("teaching-1.cs.st-andrews.ac.uk", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-2.cs.st-andrews.ac.uk", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-3.cs.st-andrews.ac.uk", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-4.cs.st-andrews.ac.uk", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-1", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-2", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-3", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-4", "/usr/local/jdk/bin");

        for (int i = 0; i < NUMBER_OF_BLUB_NODES; i++) {
            JAVA_BIN_PATHS.put("compute-0-" + i, "/usr/java/latest/bin");
        }
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
}
