package uk.ac.standrews.cs.shabdiz.impl;

import java.io.File;

public class Platform {

    public static final Platform LOCAL = new Platform(File.pathSeparatorChar, File.separatorChar);
    public static final Platform UNIX = new UnixPlatform();
    public static final Platform WINDOWS = new WindowsPlatform();
    private final char path_separator;
    private final char separator;

    public char getPathSeparator() {

        return path_separator;
    }

    public char getSeparator() {

        return separator;
    }

    private static class UnixPlatform extends Platform {

        protected UnixPlatform() {

            super(':', '/');
        }
    }

    private static class WindowsPlatform extends Platform {

        protected WindowsPlatform() {

            super(';', '\\');
        }
    }

    protected Platform(final char path_separator, final char separator) {

        this.path_separator = path_separator;
        this.separator = separator;
    }
}
