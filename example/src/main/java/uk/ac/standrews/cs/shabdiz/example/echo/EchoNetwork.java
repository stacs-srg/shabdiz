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

import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.example.util.LogNewAndOldPropertyListener;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

/**
 * Presents a network of Echo service instances.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class EchoNetwork extends ApplicationNetwork {

    private static final long serialVersionUID = 1218798936967429750L;

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoNetwork.class);
    private static final LogNewAndOldPropertyListener PRINT_LISTENER = new LogNewAndOldPropertyListener();

    EchoNetwork() {

        super("Echo Service Network");
    }

    /**
     * Starts an Echo network of four instances all running on a single {@link LocalHost local hosts}.
     * All four instances are managed by a single instance of {@link EchoApplicationManager}.
     * 
     * @param args the arguments are ignored
     * @throws Exception if failure occurs during the deployment of the network
     */
    public static void main(final String[] args) throws Exception {

        final EchoNetwork network = new EchoNetwork();
        final EchoApplicationManager application_manager = new EchoApplicationManager();
        final LocalHost local_host = new LocalHost();
        addEchoServiceDescriptor(network, local_host, application_manager);
        final EchoApplicationDescriptor kill_candidate = addEchoServiceDescriptor(network, local_host, application_manager);
        addEchoServiceDescriptor(network, local_host, application_manager);
        addEchoServiceDescriptor(network, local_host, application_manager);

        network.deployAll();
        LOGGER.info("Awaiting RUNNING state...");
        network.awaitAnyOfStates(ApplicationState.RUNNING);
        LOGGER.info("All instances are in RUNNING state");
        LOGGER.info("Killing a member");
        network.kill(kill_candidate);
        kill_candidate.awaitAnyOfStates(ApplicationState.AUTH);

        LOGGER.info("About to kill all..");
        network.killAll();
        LOGGER.info("Awaiting AUTH state...");
        network.awaitAnyOfStates(ApplicationState.AUTH);

        LOGGER.info("All done, shutting down");
        network.shutdown();
    }

    protected static EchoApplicationDescriptor addEchoServiceDescriptor(final EchoNetwork network, final Host host, final EchoApplicationManager application_manager) {

        final EchoApplicationDescriptor descriptor = new EchoApplicationDescriptor(host, application_manager);
        descriptor.addStateChangeListener(PRINT_LISTENER);
        network.add(descriptor);
        return descriptor;
    }

}
