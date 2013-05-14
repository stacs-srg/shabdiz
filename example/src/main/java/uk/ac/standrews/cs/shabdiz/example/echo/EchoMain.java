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
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

/**
 * The entry point to Echo example.
 * Deploys a network of five Echo instances on the local machine in separate processes and awaits {@link ApplicationState#RUNNING running} state.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class EchoMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoMain.class);

    private EchoMain() {

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
        final LocalHost local_host = new LocalHost();

        // Add five instances to be deployed on the local host
        network.add(local_host);
        network.add(local_host);
        network.add(local_host);
        network.add(local_host);
        network.add(local_host);

        // Pick the first deployed instance as the termination candidate
        final ApplicationDescriptor kill_candidate = network.first();
        try {
            network.deployAll();

            LOGGER.info("Awaiting RUNNING state...");
            network.awaitAnyOfStates(ApplicationState.RUNNING);

            LOGGER.info("terminating a member");
            network.kill(kill_candidate);

            LOGGER.info("Awaiting AUTH state on the terminated instance...");
            kill_candidate.awaitAnyOfStates(ApplicationState.AUTH);

            LOGGER.info("About to kill all...");
            network.killAll();

            LOGGER.info("Awaiting AUTH state...");
            network.awaitAnyOfStates(ApplicationState.AUTH);

            LOGGER.info("Done.");
        }
        finally {
            LOGGER.info("shutting down...");
            network.shutdown();
            EchoApplicationManager.ECHO_PROXY_FACTORY.shutdown();
        }
    }
}
