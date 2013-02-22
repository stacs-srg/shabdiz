package uk.ac.standrews.cs.shabdiz.impl;

import java.io.File;

import org.json.JSONException;

import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONObject;

public class Platform {

    public static final char WINDOWS_PATH_SEPARATOR = ';';
    public static final char WINDOWS_SEPARATOR = '\\';
    public static final char UNIX_PATH_SEPARATOR = ':';
    public static final char UNIX_SEPARATOR = '/';
    public static final String UNIX_TEMP_DIR = "/tmp/";

    /** The local platform. */
    public static final Platform LOCAL = new LocalPlatform();

    /** Default Windows operating system name. */
    public static final String WINDOWS_OS_NAME = "windows";

    /** Default Cygwin operating system name. */
    public static final String CYGWIN_OS_NAME = "cygwin";

    /**
     * Constructs a {@link Platform} from the output that is produced by the execution of {@code uname} command.
     * Sets the platform operating system name to the given output.
     * If the given output contains {@link #WINDOWS_OS_NAME} it is assumed the platform is Windows.
     * If the given output contains {@link #CYGWIN_OS_NAME} it is assumed the platform is Cygwin.
     * Otherwise the platform is assumed to be Unix-based.
     * 
     * @param uname_output the output produced by the execution of {@code uname} commad
     * @return an isntance of {@link Platform} that represents Windowns, Cygwin or Unix platform
     */
    public static Platform fromUnameOutput(String uname_output) {

        uname_output = uname_output.toLowerCase();
        if (uname_output.contains(CYGWIN_OS_NAME)) { return new CygwinPlatform(uname_output); }
        if (uname_output.contains(WINDOWS_OS_NAME)) { return new WindowsPlatform(uname_output); }
        return new UnixPlatform(uname_output);
    }

    /**
     * Checks if a given platform presents a UNIX based platform.
     * 
     * @param target the target platform
     * @return true, if the path separator and separator of the target platform are equal to UNIX platform
     */
    public static boolean isUnixBased(final Platform target) {

        return target.getPathSeparator() == UNIX_PATH_SEPARATOR && target.getSeparator() == UNIX_SEPARATOR;
    }

    private final char path_separator;
    private final char separator;
    private final String temp_dir;
    private final String os_name;

    public char getPathSeparator() {

        return path_separator;
    }

    public char getSeparator() {

        return separator;
    }

    public String getTempDirectory() {

        return temp_dir;
    }

    public String getOperatingSystemName() {

        return os_name;
    }

    public JSONObject toJSON() {

        final JSONObject json = new JSONObject();
        json.put("path_separator", path_separator);
        json.put("separator", separator);
        json.put("temp_dir", temp_dir);
        json.put("os_name", os_name);
        return json;
    }

    public static Platform fromJSON(final JSONObject json) throws JSONException {

        final char path_separator = (char) json.getInt("path_separator");
        final char separator = (char) json.getInt("separator");
        final String temp_dir = json.getString("temp_dir");
        final String os_name = json.getString("os_name");
        json.put("os_name", os_name);
        return new Platform(os_name, path_separator, separator, temp_dir);
    }

    private static class LocalPlatform extends Platform {

        protected LocalPlatform() {

            super(System.getProperty("os.name"), File.pathSeparatorChar, File.separatorChar, System.getProperty("java.io.tmpdir"));
        }
    }

    private static class UnixPlatform extends Platform {

        protected UnixPlatform(final String os_name) {

            super(os_name, UNIX_PATH_SEPARATOR, UNIX_SEPARATOR, UNIX_TEMP_DIR);
        }
    }

    private static class WindowsPlatform extends Platform {

        protected WindowsPlatform(final String os_name) {

            super(os_name, WINDOWS_PATH_SEPARATOR, WINDOWS_SEPARATOR, "");
        }
    }

    private static class CygwinPlatform extends Platform {

        protected CygwinPlatform(final String os_name) {

            super(os_name, WINDOWS_PATH_SEPARATOR, UNIX_SEPARATOR, UNIX_TEMP_DIR);
        }
    }

    protected Platform(final String os_name, final char path_separator, final char separator, final String temp_dir) {

        this.os_name = os_name;
        this.path_separator = path_separator;
        this.separator = separator;
        this.temp_dir = temp_dir;
    }
}
