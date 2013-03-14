package uk.ac.standrews.cs.shabdiz.examples.echo;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.jetson.JsonRpcProxyFactory;
import uk.ac.standrews.cs.jetson.exception.JsonRpcException;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.process.RemoteJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.zold.util.ProcessUtil;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EchoApplicationManager extends AbstractApplicationManager {

    private final Random random;
    final JsonRpcProxyFactory proxy_factory;
    private final RemoteJavaProcessBuilder process_builder;
    private final ExecutorService executor;

    private static final Duration DEFAULT_DEPLOYMENT_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    EchoApplicationManager() {

        random = new Random();
        process_builder = new RemoteJavaProcessBuilder(SimpleEchoService.class);
        process_builder.addCommandLineArgument(":0");
        process_builder.addCurrentJVMClasspath();
        proxy_factory = new JsonRpcProxyFactory(EchoService.class, new JsonFactory(new ObjectMapper()));
        executor = Executors.newCachedThreadPool();
    }

    @Override
    public void deploy(final ApplicationDescriptor descriptor) throws Exception {

        final EchoApplicationDescriptor echo_descriptor = (EchoApplicationDescriptor) descriptor;
        final Host host = echo_descriptor.getHost();
        final Process echo_service_process = process_builder.start(host);
        final String address_as_string = ProcessUtil.getValueFromProcessOutput(echo_service_process, executor, SimpleEchoService.ECHO_SERVICE_ADDRESS_KEY, DEFAULT_DEPLOYMENT_TIMEOUT);
        final InetSocketAddress address = Marshaller.getAddress(address_as_string);
        final EchoService echo_proxy = proxy_factory.get(address);

        //        final SimpleEchoService echo_proxy = new SimpleEchoService(NetworkUtil.getInetSocketAddress("localhost", 52001));
        echo_descriptor.setApplicationReference(echo_proxy);
        echo_descriptor.setAddress(address);

    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final EchoApplicationDescriptor echo_descriptor = (EchoApplicationDescriptor) descriptor;
        //        System.out.println("attempting to call " + echo_descriptor);

        final String random_message = generateRandomString();

        final String echoed_message = echo_descriptor.getApplicationReference().echo(random_message);
        if (!random_message.equals(echoed_message)) { throw new Exception("expected " + random_message + ", but recieved " + echoed_message); }

    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        final EchoApplicationDescriptor echo_descriptor = (EchoApplicationDescriptor) descriptor;
        try {
            echo_descriptor.getApplicationReference().shutdown();
        }
        catch (final JsonRpcException e) {
            //expected;
        }
        finally {
            super.kill(descriptor);
        }
    }

    public void shutdown() {

        executor.shutdownNow();
    }

    private String generateRandomString() {

        synchronized (random) {
            return String.valueOf(random.nextLong());
        }
    }
}
