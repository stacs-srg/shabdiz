package uk.ac.standrews.cs.shabdiz.platform;

public class UnixPlatform extends SimplePlatform {

    static final String TEMP_DIR = "/tmp/";

    public UnixPlatform(final String os_name) {

        super(os_name, UNIX_PATH_SEPARATOR, UNIX_SEPARATOR, TEMP_DIR);
    }
}
