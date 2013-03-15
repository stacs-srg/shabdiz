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
