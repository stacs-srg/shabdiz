/*
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

import java.io.IOException;

import uk.ac.standrews.cs.shabdiz.AbstractApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.DefaultApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.examples.PrintNewAndOldPropertyListener;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

public class EchoNetwork extends AbstractApplicationNetwork<EchoApplicationDescriptor> {

    private static final PrintNewAndOldPropertyListener PRINT_LISTENER = new PrintNewAndOldPropertyListener();

    public EchoNetwork() {

        super("Echo Service Network");
    }

    public static void main(final String[] args) throws IOException, InterruptedException {

        final EchoNetwork network = new EchoNetwork();
        final EchoApplicationManager application_manager = new EchoApplicationManager();
        final LocalHost local_host = new LocalHost();
        addEchoServiceDescriptor(network, local_host, application_manager);
        addEchoServiceDescriptor(network, local_host, application_manager);
        addEchoServiceDescriptor(network, local_host, application_manager);
        addEchoServiceDescriptor(network, local_host, application_manager);
        //        addEchoServiceDescriptor(network, new SSHHost("localhost", Credentials.newSSHCredential(true)), application_manager);
        //        addEchoServiceDescriptor(network, new SSHHost("beast.cs.st-andrews.ac.uk", Credentials.newSSHCredential(true)), application_manager);
        //        addEchoServiceDescriptor(network, new SSHHost("blub.cs.st-andrews.ac.uk", SSHPublicKeyCredential.getDefaultRSACredentials(Input.readPassword("Enter public key password"))), application_manager);

        network.deployAll();
        System.out.print("Awaiting RUNNING state...");
        network.awaitAnyOfStates(ApplicationState.RUNNING);
        System.out.println();
        System.out.println("All instances are in RUNNING state");
        System.out.println();

        System.out.print("About to kill all..");
        network.killAll();
        System.out.print("Awaiting AUTH state...");
        network.awaitAnyOfStates(ApplicationState.AUTH);

        System.out.println("All done, shutting down");

        network.shutdown();
    }

    private static void addEchoServiceDescriptor(final EchoNetwork network, final Host host, final EchoApplicationManager application_manager) {

        final EchoApplicationDescriptor descriptor = new EchoApplicationDescriptor(host, application_manager);
        descriptor.addPropertyChangeListener(DefaultApplicationDescriptor.STATE_PROPERTY_NAME, PRINT_LISTENER);
        network.add(descriptor);
    }

}
