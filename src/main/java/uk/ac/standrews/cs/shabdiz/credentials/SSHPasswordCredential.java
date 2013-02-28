/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.credentials;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.barreleye.SSHClient;
import uk.ac.standrews.cs.barreleye.UIKeyboardInteractive;
import uk.ac.standrews.cs.barreleye.UserInfo;
import uk.ac.standrews.cs.barreleye.exception.SSHException;
import uk.ac.standrews.cs.nds.util.Input;

public class SSHPasswordCredential extends SSHCredential {

    private static final Logger LOGGER = Logger.getLogger(SSHPasswordCredential.class.getName());
    private transient final char[] password;

    public SSHPasswordCredential(final char[] password) {

        super();
        this.password = password;
    }

    public SSHPasswordCredential(final String username, final char[] password) {

        super(username);
        this.password = password;
    }

    char[] getPassword() {

        return password;
    }

    @Override
    public void authenticate(final SSHClient ssh_client) throws IOException {

        ssh_client.setUserInfo(createUserInfo());
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
