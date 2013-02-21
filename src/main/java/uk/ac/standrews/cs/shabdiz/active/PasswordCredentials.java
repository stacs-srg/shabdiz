package uk.ac.standrews.cs.shabdiz.active;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.barreleye.SSHClient;
import uk.ac.standrews.cs.barreleye.UIKeyboardInteractive;
import uk.ac.standrews.cs.barreleye.UserInfo;
import uk.ac.standrews.cs.barreleye.exception.SSHException;
import uk.ac.standrews.cs.nds.util.Input;
import uk.ac.standrews.cs.shabdiz.active.exceptions.InvalidCredentialsException;

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

    public char[] getPassword() {

        return password;
    }

    @Override
    public void authenticate(final Object obj) throws IOException {

        if (SSHClient.class.isInstance(obj)) {
            SSHClient.class.cast(obj).setUserInfo(createUserInfo());
        }
        else {
            throw new InvalidCredentialsException("cannot authenticate object " + obj);
        }
    }

    protected UserInfo createUserInfo() throws SSHException {

        return new PasswordUserInfo(getPasswordAsBytes());
    }

    protected byte[] getPasswordAsBytes() {

        return Input.toBytes(password);
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
