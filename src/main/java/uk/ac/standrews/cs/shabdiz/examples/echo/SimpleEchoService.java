package uk.ac.standrews.cs.shabdiz.examples.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import uk.ac.standrews.cs.jetson.JsonRpcServer;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SimpleEchoService implements EchoService {

    private static final Logger LOGGER = Logger.getLogger(SimpleEchoService.class.getName());
    private final InetSocketAddress local_address;
    private final ExecutorService executor;
    private final JsonRpcServer server;

    static final String ECHO_SERVICE_ADDRESS_KEY = "ECHO_SERVICE_ADDRESS";

    public static void main(final String[] args) throws NumberFormatException, IOException {

        final String port_as_string = NetworkUtil.extractPortNumberAsString(args[0]);
        final InetSocketAddress local_address = NetworkUtil.getLocalIPv4InetSocketAddress(Integer.parseInt(port_as_string));
        final SimpleEchoService echo_service = new SimpleEchoService(local_address);
        LOGGER.info("started echo service on " + echo_service.local_address);
        ProcessUtil.printValue(System.out, ECHO_SERVICE_ADDRESS_KEY, echo_service.getAddress());
    }

    public SimpleEchoService(final InetSocketAddress local_address) throws IOException {

        executor = Executors.newCachedThreadPool();
        server = new JsonRpcServer(local_address, EchoService.class, this, new JsonFactory(new ObjectMapper()), executor);
        server.expose();
        this.local_address = server.getLocalSocketAddress();
    }

    public InetSocketAddress getAddress() {

        return local_address;
    }

    @Override
    public String echo(final String message) {

        return message;
    }

    @Override
    public void shutdown() {

        server.unexpose();
        executor.shutdownNow();
    }
}
