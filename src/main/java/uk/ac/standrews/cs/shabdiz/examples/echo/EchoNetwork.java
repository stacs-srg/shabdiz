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
package uk.ac.standrews.cs.shabdiz.examples.echo;

import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.examples.PrintNewAndOldPropertyListener;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

public class EchoNetwork extends ApplicationNetwork {

    private static final PrintNewAndOldPropertyListener PRINT_LISTENER = new PrintNewAndOldPropertyListener();

    public EchoNetwork() {

        super("Echo Service Network");
    }

    public static void main(final String[] args) throws TimeoutException, Exception {

        final EchoNetwork network = new EchoNetwork();
        final EchoApplicationManager application_manager = new EchoApplicationManager();
        final LocalHost local_host = new LocalHost();
        addEchoServiceDescriptor(network, local_host, application_manager);
        final EchoApplicationDescriptor kill_candidate = addEchoServiceDescriptor(network, local_host, application_manager);
        addEchoServiceDescriptor(network, local_host, application_manager);
        addEchoServiceDescriptor(network, local_host, application_manager);

        network.deployAll();
        System.out.print("Awaiting RUNNING state...");
        network.awaitAnyOfStates(ApplicationState.RUNNING);
        System.out.println();
        System.out.println("All instances are in RUNNING state");
        System.out.println("Killing a member");
        network.kill(kill_candidate);
        kill_candidate.awaitAnyOfStates(ApplicationState.AUTH);

        System.out.print("About to kill all..");
        network.killAll();
        System.out.print("Awaiting AUTH state...");
        network.awaitAnyOfStates(ApplicationState.AUTH);

        System.out.println("All done, shutting down");

        network.shutdown();
    }

    protected static EchoApplicationDescriptor addEchoServiceDescriptor(final EchoNetwork network, final Host host, final EchoApplicationManager application_manager) {

        final EchoApplicationDescriptor descriptor = new EchoApplicationDescriptor(host, application_manager);
        descriptor.addStateChangeListener(PRINT_LISTENER);
        network.add(descriptor);
        return descriptor;
    }

}
