package uk.ac.standrews.cs.shabdiz.impl;

import com.ariabod.barreleye.SSHSession;
import com.ariabod.barreleye.UIKeyboardInteractive;
import com.ariabod.barreleye.UserInfo;
import com.ariabod.barreleye.exception.SSHException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PasswordCredentials extends Credentials {

    private static final Logger LOGGER = Logger.getLogger(PasswordCredentials.class.getName());
    private transient final char[] password;

    public PasswordCredentials(final char[] password) {

        super();
        this.password = password;
    }

    public PasswordCredentials(final String username, final char[] password) {

        super(username);
        this.password = password;
    }

    protected char[] getPassword() {

        return password;
    }

    @Override
    void authenticate(final SSHSession session) throws IOException {
        session.setUserInfo(createUserInfo());
    }

    protected UserInfo createUserInfo() throws SSHException {
        return new PasswordUserInfo(getPasswordAsBytes());
    }

    protected byte[] getPasswordAsBytes() {

        return toBytes(password);
    }

    private static class PasswordUserInfo implements UserInfo, UIKeyboardInteractive {

        private final byte[] password;

        public PasswordUserInfo(final byte[] password) {

            this.password = password;
        }

        @Override
        public byte[] getPassphrase() {
            return null;
        }

        @Override
        public byte[] getPassword() {
            return password;
        }

        @Override
        public boolean promptPassword(final String message) {
            return true;
        }

        @Override
        public boolean promptPassphrase(final String message) {
            return false;
        }

        @Override
        public boolean promptYesNo(final String message) {
            return true;
        }

        @Override
        public void showMessage(final String message) {
            LOGGER.log(Level.INFO, message);
        }

        @Override
        public String[] promptKeyboardInteractive(final String destination, final String name, final String instruction, final String[] prompt, final boolean[] echo) {
            //TODO fix this in barreleye
            return null;
        }
    }
}
