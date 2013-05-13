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
package uk.ac.standrews.cs.shabdiz.example.echo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.example.util.LogNewAndOldPropertyListener;
import uk.ac.standrews.cs.shabdiz.host.Host;

/**
 * Presents a network of Echo service instances.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class EchoNetwork extends ApplicationNetwork {

    private static final long serialVersionUID = 1218798936967429750L;
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoNetwork.class);
    private static final LogNewAndOldPropertyListener PRINT_LISTENER = new LogNewAndOldPropertyListener();
    private final transient EchoApplicationManager manager;

    EchoNetwork() {

        super("Echo Service Network");
        manager = new EchoApplicationManager();
    }

    /**
     * Adds a new {@link ApplicationDescriptor} with the given {@code host} as its host, and {@link EchoApplicationManager} as its manager, to this network.
     *
     * @param host the host of the descriptor to be added
     * @return true, if successfully added
     */
    public boolean add(final Host host) {

        LOGGER.debug("adding an instance descriptor on host: {}", host);
        final ApplicationDescriptor descriptor = new ApplicationDescriptor(host, manager);
        descriptor.addStateChangeListener(PRINT_LISTENER);
        return add(descriptor);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof EchoNetwork)) return false;
        if (!super.equals(o)) return false;

        final EchoNetwork that = (EchoNetwork) o;

        if (manager != null ? !manager.equals(that.manager) : that.manager != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (manager != null ? manager.hashCode() : 0);
        return result;
    }
}