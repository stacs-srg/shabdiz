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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.IOException;

/**
 * Presents password credentials of a {@link SSHHost}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class SSHPasswordCredentials extends SSHCredentials {

    protected final String password;

    /**
     * Instantiates a new SSH password credentials.
     *
     * @param password the password
     */
    public SSHPasswordCredentials(final char[] password) {

        this(String.valueOf(password));
    }

    /**
     * Instantiates a new SSH password credentials.
     *
     * @param password the password
     */
    public SSHPasswordCredentials(final String password) {

        super();
        this.password = password;
    }

    /**
     * Instantiates a new SSH password credentials.
     *
     * @param username the username
     * @param password the password
     */
    public SSHPasswordCredentials(final String username, final char[] password) {

        this(username, String.valueOf(password));
    }

    /**
     * Instantiates a new SSH password credentials.
     *
     * @param username the username
     * @param password the password
     */
    public SSHPasswordCredentials(final String username, final String password) {

        super(username);
        this.password = password;
    }

    @Override
    void authenticate(final JSch ssh_client, final Session session) throws IOException {

        session.setPassword(password);
    }
}
