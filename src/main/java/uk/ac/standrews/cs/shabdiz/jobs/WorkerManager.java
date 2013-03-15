package uk.ac.standrews.cs.shabdiz.jobs;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.process.RemoteJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

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

        final String address_as_string = ProcessUtil.getValueFromProcessOutput(worker_process, WorkerMain.WORKER_REMOTE_ADDRESS_KEY, worker_deployment_timeout);
        return Marshaller.getAddress(address_as_string);
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
