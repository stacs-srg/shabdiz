/*
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
package uk.ac.standrews.cs.shabdiz.credentials;

import java.io.IOException;

import uk.ac.standrews.cs.barreleye.SSHClient;

public abstract class SSHCredential {

    private final String username;

    protected SSHCredential() {

        this(Credentials.getCurrentUser());
    }

    protected SSHCredential(final String username) {

        this.username = username;
    }

    /**
     * Authenticates a given {@link SSHClient}.
     * 
     * @param ssh_client the SSH client to authenticate
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public abstract void authenticate(SSHClient ssh_client) throws IOException;

    /**
     * Gets the username.
     * 
     * @return the username
     */
    public String getUsername() {

        return username;
    }
}
