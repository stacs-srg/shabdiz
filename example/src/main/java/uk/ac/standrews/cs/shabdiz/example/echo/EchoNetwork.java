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

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.example.util.LogNewAndOldPropertyListener;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.HashCodeUtil;

/**
 * Presents a network of Echo service instances.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class EchoNetwork extends ApplicationNetwork {

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoNetwork.class);
    private static final LogNewAndOldPropertyListener PRINT_LISTENER = new LogNewAndOldPropertyListener();
    private final transient EchoApplicationManager manager;

    /** Instantiates a new Echo network under the name of {@code Echo Serice Network}. */
    public EchoNetwork() {

        this(new EchoApplicationManager());
    }

    public EchoNetwork(EchoApplicationManager manager) {

        super("Echo Service Network");
        this.manager = manager;
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

    /**
     * Attempts to add all the {@code hosts} to this network via {@link #add(Host)}.
     *
     * @param hosts the hosts to be added to this network
     */
    public void addAll(final Collection<? extends Host> hosts) {

        for (Host host : hosts) {
            add(host);
        }
    }

    @Override
    public int hashCode() {

        return HashCodeUtil.generate(super.hashCode(), manager.hashCode());
    }

    @Override
    public boolean equals(final Object other) {

        if (this == other) { return true; }
        if (!(other instanceof EchoNetwork) || !super.equals(other)) { return false; }
        final EchoNetwork that = (EchoNetwork) other;
        return manager.equals(that.manager);
    }
}
