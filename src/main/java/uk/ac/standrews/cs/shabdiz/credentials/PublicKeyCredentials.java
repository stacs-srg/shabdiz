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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.barreleye.SSHClientFactory;
import uk.ac.standrews.cs.barreleye.UserInfo;
import uk.ac.standrews.cs.barreleye.exception.SSHException;
import uk.ac.standrews.cs.barreleye.userauth.IdentityManager;

public class PublicKeyCredentials extends PasswordCredentials {

    private static final Logger LOGGER = Logger.getLogger(PublicKeyCredentials.class.getName());
    static final File SSH_HOME = new File(System.getProperty("user.home"), ".ssh");
    static final File SSH_KNOWN_HOSTS = new File(PublicKeyCredentials.SSH_HOME, "known_hosts");
    private static final File SSH_RSA_PRIVATE_KEY = new File(SSH_HOME, "id_rsa");
    private transient final File private_key;

    public PublicKeyCredentials(final String username, final File private_key, final char[] passphrase) {

        super(username, passphrase);
        this.private_key = private_key;
    }

    public PublicKeyCredentials(final File private_key, final char[] passphrase) {

        super(passphrase);
        this.private_key = private_key;
    }

    public static PublicKeyCredentials getDefaultRSACredentials(final char[] passphrase) {

        return new PublicKeyCredentials(SSH_RSA_PRIVATE_KEY, passphrase);
    }

    public static void setSSHKnownHosts(final SSHClientFactory ssh_client_factory) throws SSHException {

        ssh_client_factory.setKnownHosts(SSH_KNOWN_HOSTS.getAbsolutePath());
    }

    public File getPrivateKey() {

        return private_key;
    }

    @Override
    protected UserInfo createUserInfo() throws SSHException {

        IdentityManager.getManager().addIdentity(private_key.getAbsolutePath());
        return new PassphraseUserInfo(getPasswordAsBytes());
    }

    private final class PassphraseUserInfo implements UserInfo {

        private final byte[] passphrase;

        public PassphraseUserInfo(final byte[] passphrase) {

            this.passphrase = passphrase;
        }

        @Override
        public byte[] getPassphrase() {

            return passphrase;
        }

        @Override
        public byte[] getPassword() {

            return null;
        }

        @Override
        public boolean promptPassword(final String message) {

            return false;
        }

        @Override
        public boolean promptPassphrase(final String message) {

            return true;
        }

        @Override
        public boolean promptYesNo(final String message) {

            return false;
        }

        @Override
        public void showMessage(final String message) {

            LOGGER.log(Level.INFO, message);
        }
    }
}