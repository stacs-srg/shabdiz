package uk.ac.standrews.cs.shabdiz.impl;

import java.io.File;
import java.io.IOException;

public class Platform {

    public static final Platform LOCAL = new Platform(File.pathSeparatorChar, File.separatorChar);
    public static final Platform UNIX = new UnixPlatform();
    public static final Platform WINDOWS = new WindowsPlatform();
    private static final String UNAME_COMMAND = "uname -a";
    private final char path_separator;
    private final char separator;

    public static Platform getHostPlatform(final Host host) throws IOException, InterruptedException {

        return host.isLocal() ? LOCAL : getHostPlatformByUname(host);
    }

    static Platform getHostPlatformByUname(final Host host) throws IOException, InterruptedException {

        final RemoteCommandBuilder uname = new RemoteCommandBuilder(UNAME_COMMAND);
        final Process uname_process = uname.start(host);
        final String uname_output = ProcessUtil.waitForAndReadOutput(uname_process);
        return createPlatformFromUnameOutput(uname_output);
    }

    private static Platform createPlatformFromUnameOutput(final String uname_output) {

        return uname_output.toLowerCase().contains("windows") ? WINDOWS : UNIX;
    }

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
