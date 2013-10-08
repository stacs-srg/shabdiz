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

import java.io.IOException;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.HashCodeUtil;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;

/**
 * Provides the common functionality for a {@link Host}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class AbstractHost implements Host {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHost.class);
    private final InetAddress address;
    private final boolean local;
    private final String name;

    protected AbstractHost(final String name) throws IOException {

        this(InetAddress.getByName(name));
    }

    protected AbstractHost(final InetAddress address) {

        this.address = address;
        local = NetworkUtil.isValidLocalAddress(address);
        name = address.getHostName();
    }

    @Override
    public InetAddress getAddress() {

        return address;
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public boolean isLocal() {

        return local;
    }

    /**
     * Logs the closure of this host.
     *
     * @throws IOException {@inheritDoc}
     */
    @Override
    public void close() throws IOException {

        LOGGER.debug("closing host {}", address);
    }

    @Override
    public int hashCode() {

        return HashCodeUtil.generate(address.hashCode(), name.hashCode(), local ? 1 : 0);
    }

    @Override
    public boolean equals(final Object other) {

        if (this == other) { return true; }
        if (!(other instanceof Host)) { return false; }
        final Host that = (Host) other;
        return local == that.isLocal() && address.equals(that.getAddress()) && name.equals(that.getName());
    }

    @Override
    public String toString() {

        return getName();
    }
}
