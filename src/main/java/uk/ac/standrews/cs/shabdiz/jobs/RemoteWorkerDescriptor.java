package uk.ac.standrews.cs.shabdiz.jobs;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.DefaultApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Worker;
import uk.ac.standrews.cs.shabdiz.process.RemoteJavaProcessBuilder;

public class RemoteWorkerDescriptor extends DefaultApplicationDescriptor {

    private final AtomicReference<Worker> application_reference;

    public RemoteWorkerDescriptor(final Host host, final WorkerManager worker_manager) {

        super(host, worker_manager);
        application_reference = new AtomicReference<Worker>();
    }

    public void setApplicationReference(final Worker worker) {

        application_reference.set(worker);
    }

    public Worker getApplicationReference() {

        return application_reference.get();
    }

}

class WorkerManager extends AbstractApplicationManager {

    private static final Duration DEFAULT_WORKER_DEPLOYMENT_TIMEOUT = new Duration(15, TimeUnit.SECONDS);
    private static final String DEFAULT_WORKER_JVM_ARGUMENTS = "-Xmx128m"; // add this for debug "-XX:+HeapDumpOnOutOfMemoryError"
    private static final Integer DEFAULT_WORKER_PORT = 0;
    private static final Integer DEFAULT_THREAD_POOL_SIZE = 5;
    private static final Duration DEFAULT_WORKER_SOCKET_READ_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    private final RemoteJavaProcessBuilder worker_process_builder;
    private volatile Duration worker_deployment_timeout = DEFAULT_WORKER_DEPLOYMENT_TIMEOUT;
    private final WorkerNetwork network;

    public WorkerManager(final WorkerNetwork network, final Set<File> classpath) {

        this.network = network;
        worker_process_builder = createRemoteJavaProcessBuiler(classpath, network.getCallbackAddress());
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final RemoteWorkerDescriptor worker_descriptor = (RemoteWorkerDescriptor) descriptor;
        worker_descriptor.getApplicationReference().getAddress(); // makes a remote call
    }

    @Override
    public void deploy(final ApplicationDescriptor descriptor) throws Exception {

        final RemoteWorkerDescriptor worker_descriptor = (RemoteWorkerDescriptor) descriptor;
        final Process worker_process = worker_process_builder.start(worker_descriptor.getHost());
        final InetSocketAddress worker_address = getWorkerRemoteAddressFromProcessOutput(worker_process);
        final WorkerRemote worker_remote = WorkerRemoteProxyFactory.getProxy(worker_address);
        final DefaultWorkerWrapper worker = new DefaultWorkerWrapper(network, worker_remote, worker_process, worker_address);

        worker_descriptor.setApplicationReference(worker);
    }

    private InetSocketAddress getWorkerRemoteAddressFromProcessOutput(final Process worker_process) throws UnknownHostException, IOException, InterruptedException, TimeoutException {

        final Future<InetSocketAddress> future_address = executeScanForRemoteWorkerAddress(worker_process);
        boolean scan_succeeded = false;
        try {
            final InetSocketAddress worker_address = future_address.get(worker_deployment_timeout.getLength(), worker_deployment_timeout.getTimeUnit());
            scan_succeeded = true;
            return worker_address;
        }
        catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            final Class<IOException> io_exception = IOException.class;
            throw io_exception.isInstance(cause) ? io_exception.cast(cause) : new IOException(cause);
        }
        finally {
            if (!scan_succeeded) {
                if (!future_address.isDone()) {
                    future_address.cancel(true);
                }
                worker_process.destroy();
            }
        }
    }

    private RemoteJavaProcessBuilder createRemoteJavaProcessBuiler(final Set<File> classpath, final InetSocketAddress callback_address) {

        final RemoteJavaProcessBuilder process_builder = new RemoteJavaProcessBuilder(WorkerMain.class);
        final List<String> arguments = WorkerMain.constructCommandLineArguments(callback_address, DEFAULT_WORKER_PORT, DEFAULT_THREAD_POOL_SIZE, DEFAULT_WORKER_SOCKET_READ_TIMEOUT);
        process_builder.addCommandLineArguments(arguments);
        process_builder.addJVMArgument(DEFAULT_WORKER_JVM_ARGUMENTS);
        process_builder.addClasspath(classpath);
        process_builder.addCurrentJVMClasspath();
        return process_builder;
    }

    private Future<InetSocketAddress> executeScanForRemoteWorkerAddress(final Process worker_process) {

        return network.getExecutor().submit(new Callable<InetSocketAddress>() {

            @Override
            public InetSocketAddress call() throws Exception {

                InetSocketAddress worker_address;
                final Scanner scanner = new Scanner(worker_process.getInputStream()); // Scanner is not closed on purpose. The stream belongs to Process instance.
                do {
                    final String output_line = scanner.nextLine();
                    worker_address = WorkerMain.parseOutputLine(output_line);
                }
                while (worker_address == null && !Thread.currentThread().isInterrupted());
                return worker_address;
            }
        });
    }

    /**
     * Sets the worker deployment timeout.
     * 
     * @param duration the new worker deployment timeout
     * @throws NullPointerException if the given timeout is {@code null}
     */
    public void setWorkerDeploymentTimeout(final Duration duration) {

        if (duration == null) { throw new NullPointerException(); }
        worker_deployment_timeout = duration;
    }

    /**
     * Sets the worker JVM arguments.
     * 
     * @param jvm_arguments the new worker JVM arguments
     * @throws NullPointerException if the given arguments is {@code null}
     */
    public void setWorkerJVMArguments(final String jvm_arguments) {

        worker_process_builder.replaceJVMArguments(jvm_arguments.trim());
    }

}
