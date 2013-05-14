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

import uk.ac.standrews.cs.shabdiz.platform.Platforms;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Presents credentials of a {@link SSHHost}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class SSHCredentials {

    static final File DEFAULT_SSH_HOME = new File(System.getProperty("user.home"), ".ssh");
    static final File DEFAULT_SSH_KNOWN_HOSTS_FILE = new File(DEFAULT_SSH_HOME, "known_hosts");
    private final String username;

    protected SSHCredentials() {

        this(Platforms.getCurrentUser());
    }

    protected SSHCredentials(final String username) {

        this.username = username;
    }

    protected String getKnownHostsFile() {

        return DEFAULT_SSH_KNOWN_HOSTS_FILE.getAbsolutePath();
    }

    /**
     * Authenticates a given a {@link JSch} and a {@link Session}.
     * 
     * @param ssh_client the SSH client to authenticate
     * @param ssh_session the ssh session to authenticate
     * @throws IOException Signals that an I/O exception has occurred.
     */
    abstract void authenticate(JSch ssh_client, Session ssh_session) throws IOException;

    /**
     * Gets the username.
     * 
     * @return the username
     */
    public String getUsername() {

        return username;
    }
}
