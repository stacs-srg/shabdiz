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

import uk.ac.standrews.cs.shabdiz.util.Input;

/**
 * Factory for {@link SSHPasswordCredentials}, {@link SSHPublicKeyCredentials} and utility methods for JSON serialisation and deserialisation of {@link SSHCredentials credentials}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class Credentials {

    private Credentials() {

    }

    /**
     * Initialises a credentials by prompting the user for information.
     * 
     * @param use_password whether the credentials is {@link SSHPasswordCredentials}
     * @return an instance of {@link SSHPasswordCredentials} if {@code use_password} is {@code true}, an instance of {@link SSHPublicKeyCredentials} otherwise
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static SSHCredentials newSSHCredential(final boolean use_password) throws IOException {

        final String username = Input.readLine("enter username: ");
        return use_password ? newSSHPasswordCredential(username) : newSSHPublicKeyCredential(username);
    }

    private static SSHCredentials newSSHPublicKeyCredential(final String username) throws IOException {

        final File private_key = new File(Input.readLine("enter full path to the private key"));
        final char[] passphrase = Input.readPassword("enter private key passphrase:");
        return new SSHPublicKeyCredentials(username, private_key, passphrase);
    }

    private static SSHPasswordCredentials newSSHPasswordCredential(final String username) {

        final char[] password = Input.readPassword("enter password:");
        return new SSHPasswordCredentials(username, password);
    }
}
