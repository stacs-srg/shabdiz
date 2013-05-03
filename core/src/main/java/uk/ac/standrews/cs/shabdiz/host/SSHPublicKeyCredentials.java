/*
 * Copyright 2013 University of St Andrews School of Computer Science
 * 
 * This file is part of Shabdiz.
 * 
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.host;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * Presents public key credentials of a {@link SSHHost}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class SSHPublicKeyCredentials extends SSHPasswordCredentials {

    private static final Logger LOGGER = Logger.getLogger(SSHPublicKeyCredentials.class.getName());
    private static final File DEFAULT_SSH_RSA_PRIVATE_KEY = new File(DEFAULT_SSH_HOME, "id_rsa");

    private final File private_key;

    /**
     * Instantiates a new SSH public key credentials.
     * 
     * @param username the username
     * @param private_key the private_key
     * @param passphrase the passphrase
     */
    public SSHPublicKeyCredentials(final String username, final File private_key, final char[] passphrase) {

        super(username, passphrase);
        this.private_key = private_key;
    }

    /**
     * Instantiates a new sSH public key credentials.
     * 
     * @param private_key the private_key
     * @param passphrase the passphrase
     */
    public SSHPublicKeyCredentials(final File private_key, final char[] passphrase) {

        super(passphrase);
        this.private_key = private_key;
    }

    /**
     * Gets the default RSA credentials.
     * 
     * @param passphrase the passphrase
     * @return the default rsa credentials
     */
    public static SSHPublicKeyCredentials getDefaultRSACredentials(final char[] passphrase) {

        return new SSHPublicKeyCredentials(DEFAULT_SSH_RSA_PRIVATE_KEY, passphrase);
    }

    File getPrivateKey() {

        return private_key;
    }

    @Override
    public void authenticate(final JSch ssh_client, final Session session) throws IOException {

        addIdentity(ssh_client);
    }

    void addIdentity(final JSch ssh_client) throws IOException {

        try {
            ssh_client.addIdentity(private_key.getAbsolutePath());
        }
        catch (final JSchException e) {
            throw new IOException(e);
        }
    }

    protected UserInfo createUserInfo() {

        return new PassphraseUserInfo();
    }

    private final class PassphraseUserInfo implements UserInfo {

        @Override
        public String getPassphrase() {

            return password;
        }

        @Override
        public String getPassword() {

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
