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
import java.io.FileNotFoundException;
import java.util.List;

import uk.ac.standrews.cs.shabdiz.active.config.Config;
import uk.ac.standrews.cs.shabdiz.active.exceptions.UnknownPlatformException;

/**
 * Description of a recognised OS platform.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class PlatformDescriptor {

    /** Linux. */
    public static final String NAME_LINUX = "Linux";

    /** OS X. */
    public static final String NAME_MAC = "Mac OS X";

    /** Windows. */
    public static final String NAME_WINDOWS = "Windows";

    /** Unknown. */
    public static final String NAME_UNKNOWN = "Unknown";

    // -------------------------------------------------------------------------------------------------------

    static final String JAVA_EXECUTABLE_NAME = "java";
    private static final String JAVAC_EXECUTABLE_NAME_WINDOWS = "javac.exe";
    private static final String JAVAC_EXECUTABLE_NAME = "javac";

    // -------------------------------------------------------------------------------------------------------

    // Attributes that can be read directly on the local platform.
    private final String name;
    private final String file_separator;
    private final String class_path_separator;
    private final String temp_path;

    // Attributes that have to be inferred from the platform type.
    private final String wget_path;
    private final String path_quote;

    // -------------------------------------------------------------------------------------------------------

    PlatformDescriptor(final String name, final String file_separator, final String class_path_separator, final String temp_path, final String wget_path, final String path_quote) {

        this.name = name;
        this.file_separator = file_separator;
        this.class_path_separator = class_path_separator;
        this.temp_path = temp_path;

        this.wget_path = wget_path;
        this.path_quote = path_quote;
    }

    PlatformDescriptor(final String name, final String file_separator, final String class_path_separator, final String temp_path) throws UnknownPlatformException {

        this(name, file_separator, class_path_separator, temp_path, getWgetPath(name), getPathQuote(name));
    }

    PlatformDescriptor(final String name) throws UnknownPlatformException {

        this(name, getFileSeparator(name), getClassPathSeparator(name), getTempPath(name));
    }

    PlatformDescriptor() {

        this(NAME_UNKNOWN, "", "", "", "", "");
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {

        return getName();
    }

    /**
     * Returns the platform's name.
     * 
     * @return the platform's name
     */
    public String getName() {

        return name;
    }

    /**
     * Returns the file separator character for this platform.
     * 
     * @return the file separator character for this platform
     * @throws UnknownPlatformException
     */
    public String getFileSeparator() throws UnknownPlatformException {

        if ("".equals(file_separator)) { throw new UnknownPlatformException(); }
        return file_separator;
    }

    /**
     * Returns the class path separator character for this platform.
     * 
     * @return the class path separator character for this platform
     * @throws UnknownPlatformException
     */
    public String getClassPathSeparator() throws UnknownPlatformException {

        if ("".equals(class_path_separator)) { throw new UnknownPlatformException(); }
        return class_path_separator;
    }

    /**
     * Returns the 'temp' directory path for this platform.
     * 
     * @return the 'temp' directory path for this platform
     * @throws UnknownPlatformException
     */
    public String getTempPath() throws UnknownPlatformException {

        if ("".equals(temp_path)) { throw new UnknownPlatformException(); }
        return temp_path;
    }

    /**
     * Returns the 'wget' path for this platform.
     * 
     * @return the 'wget' path for this platform
     * @throws UnknownPlatformException
     */
    public String getWgetPath() throws UnknownPlatformException {

        if ("".equals(wget_path)) { throw new UnknownPlatformException(); }
        return wget_path;
    }

    /**
     * Returns the character used to quote a class path for this platform. This may be the empty string.
     * 
     * @return the character used to quote a class path for this platform
     */
    public String getPathQuote() {

        return path_quote;
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a shell command to invoke the Java compiler with the specified parameters.
     * 
     * @param compilation_class_path the class path to be compiled against
     * @param output_dir_path the path of the output directory
     * @param compiler_flags the compiler flags
     * @param file_paths the paths of the files to be compiled
     * @return a shell command to invoke the compiler
     * @throws FileNotFoundException if the compiler executable cannot be found locally
     * @throws UnknownPlatformException
     */
    public String getJavaCompilerCommand(final ClassPath compilation_class_path, final File output_dir_path, final List<String> compiler_flags, final List<File> file_paths) throws FileNotFoundException, UnknownPlatformException {

        final ClassPath current_class_path = ClassPath.getCurrentClassPath();
        current_class_path.append(compilation_class_path);

        final StringBuilder buffer = new StringBuilder();

        final String path_quote = getPathQuote(name);

        buffer.append(path_quote);
        buffer.append(getJavaCompilerName(name));
        buffer.append(path_quote);

        buffer.append(" -cp ");
        buffer.append(current_class_path.toString());

        buffer.append(" -d ");
        buffer.append(path_quote);
        buffer.append(output_dir_path.getAbsolutePath());
        buffer.append(path_quote);
        buffer.append(" ");

        for (final String element : compiler_flags) {
            buffer.append(element);
            buffer.append(" ");
        }

        for (final File element : file_paths) {
            buffer.append(path_quote);
            buffer.append(element.getAbsolutePath());
            buffer.append(path_quote);
            buffer.append(" ");
        }

        return buffer.toString();
    }

    /**
     * Returns the name of the main Java compiler class.
     * 
     * @return the name of the main Java compiler class
     */
    public static String getJavaCompilerMainClassName() {

        return Config.JAVAC_MAIN_CLASS_NAME;
    }

    /**
     * Returns the name of the Java compiler output class.
     * 
     * @return the name of the Java compiler output class
     */
    public static String getJavaCompilerOutputClassName() {

        return Config.OUTPUT_CLASS_NAME;
    }

    // -------------------------------------------------------------------------------------------------------

    private static String getJavaCompilerName(final String platform_name) throws UnknownPlatformException {

        if (platform_name.equals(NAME_MAC) || platform_name.equals(NAME_LINUX)) { return JAVAC_EXECUTABLE_NAME; }
        if (platform_name.equals(NAME_WINDOWS)) { return JAVAC_EXECUTABLE_NAME_WINDOWS; }

        throw new UnknownPlatformException();
    }

    private static String getFileSeparator(final String platform_name) throws UnknownPlatformException {

        if (platform_name.equals(NAME_MAC)) { return Config.FILE_SEPARATOR_MAC; }
        if (platform_name.equals(NAME_LINUX)) { return Config.FILE_SEPARATOR_LINUX; }
        if (platform_name.equals(NAME_WINDOWS)) { return Config.FILE_SEPARATOR_WINDOWS; }

        throw new UnknownPlatformException();
    }

    private static String getClassPathSeparator(final String platform_name) throws UnknownPlatformException {

        if (platform_name.equals(NAME_MAC)) { return Config.CLASS_PATH_SEPARATOR_MAC; }
        if (platform_name.equals(NAME_LINUX)) { return Config.CLASS_PATH_SEPARATOR_LINUX; }
        if (platform_name.equals(NAME_WINDOWS)) { return Config.CLASS_PATH_SEPARATOR_WINDOWS; }

        throw new UnknownPlatformException();
    }

    private static String getPathQuote(final String platform_name) throws UnknownPlatformException {

        if (platform_name.equals(NAME_MAC) || platform_name.equals(NAME_LINUX)) { return ""; }
        if (platform_name.equals(NAME_WINDOWS)) { return "\""; }

        throw new UnknownPlatformException();
    }

    private static String getTempPath(final String platform_name) {

        if (platform_name.equals(NAME_MAC)) { return Config.DEFAULT_TEMP_PATH_MAC; }
        if (platform_name.equals(NAME_LINUX)) { return Config.DEFAULT_TEMP_PATH_LINUX; }

        return null; // this can't throw an UnknownPlatformException without breaking windows code which starts new processes.
    }

    private static String getWgetPath(final String platform_name) {

        if (platform_name.equals(NAME_MAC)) { return Config.DEFAULT_WGET_PATH_MAC; }
        if (platform_name.equals(NAME_LINUX)) { return Config.DEFAULT_WGET_PATH_LINUX; }

        return null;// this can't throw an UnknownPlatformException without breaking windows code which starts new processes.
    }
}
