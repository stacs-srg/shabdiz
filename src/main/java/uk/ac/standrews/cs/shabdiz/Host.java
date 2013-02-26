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
package uk.ac.standrews.cs.shabdiz;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.credentials.Credentials;

public abstract class Host {

    private static final Logger LOGGER = Logger.getLogger(Host.class.getName());
    private final InetAddress address;
    private final boolean local;
    protected final Credentials credentials;

    public Host(final String name, final Credentials credentials) throws IOException {

        this(InetAddress.getByName(name), credentials);
    }

    public Host(final InetAddress address, final Credentials credentials) {

        this.address = address;
        this.credentials = credentials;
        local = NetworkUtil.isValidLocalAddress(address);
    }

    public abstract void upload(File source, String destination) throws IOException;

    public abstract void upload(Collection<File> sources, String destination) throws IOException;

    public abstract void download(String source, File destination) throws IOException;

    public abstract Process execute(String... command) throws IOException;

    public abstract Platform getPlatform() throws IOException;

    public InetAddress getAddress() {

        return address;
    }

    public boolean isLocal() {

        return local;
    }

    public void shutdown() {

        LOGGER.info("shutting down host " + address);
    }
}
