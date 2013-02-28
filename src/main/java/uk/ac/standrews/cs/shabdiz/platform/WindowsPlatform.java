package uk.ac.standrews.cs.shabdiz.platform;

public class WindowsPlatform extends SimplePlatform {

    /** Default Windows operating system name. */
    public static final String WINDOWS_OS_NAME = "windows";

    public WindowsPlatform(final String os_name) {

        super(os_name, WINDOWS_PATH_SEPARATOR, WINDOWS_SEPARATOR, "");
    }
}
