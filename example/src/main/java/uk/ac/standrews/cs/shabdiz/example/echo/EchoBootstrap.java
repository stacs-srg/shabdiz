package uk.ac.standrews.cs.shabdiz.example.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;

/**
 * This class contains a {@code main} method and can be used to start up a stand-alone Echo service.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class EchoBootstrap extends Bootstrap {

    static final String ECHO_SERVICE_ADDRESS_KEY = "ECHO_SERVICE_ADDRESS";

    /**
     * Starts a new instance of {@link DefaultEcho}.
     *
     * @param args expects the first entry to be the port number on which to listen for incoming connections
     * @throws NumberFormatException if the first entry in the arguments cannot be parsed to an integer
     * @throws IOException Signals that an I/O exception has occurred
     */
    public static void main(final String[] args) throws IOException {

        final EchoBootstrap bootstrap = new EchoBootstrap();
        bootstrap.deploy(args);
    }

    public static InetSocketAddress getAddressProperty(Properties properties) throws UnknownHostException {

        final String address_as_string = properties.getProperty(ECHO_SERVICE_ADDRESS_KEY);
        return address_as_string != null ? NetworkUtil.getAddressFromString(address_as_string) : null;
    }

    @Override
    protected void deploy(final String... args) throws IOException {

        final InetSocketAddress address = getAddress(args);
        final DefaultEcho echo_service = new DefaultEcho(address);
        setProperty(ECHO_SERVICE_ADDRESS_KEY, echo_service.getAddress());
    }

    private InetSocketAddress getAddress(final String[] args) throws UnknownHostException {

        final int port = args != null && args.length > 0 ? Integer.parseInt(args[0]) : 0;
        return NetworkUtil.getLocalIPv4InetSocketAddress(port);
    }
}
