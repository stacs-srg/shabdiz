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
package uk.ac.standrews.cs.shabdiz.host;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.api.Host;

public abstract class AbstractHost implements Host {

    private static final Logger LOGGER = Logger.getLogger(AbstractHost.class.getName());
    private final InetAddress address;
    private final boolean local;

    public AbstractHost(final String name) throws IOException {

        this(InetAddress.getByName(name));
    }

    public AbstractHost(final InetAddress address) {

        this.address = address;
        local = NetworkUtil.isValidLocalAddress(address);
    }

    @Override
    public InetAddress getAddress() {

        return address;
    }

    @Override
    public boolean isLocal() {

        return local;
    }

    @Override
    public void close() throws IOException {

        LOGGER.fine("closing host " + address);
    }
}