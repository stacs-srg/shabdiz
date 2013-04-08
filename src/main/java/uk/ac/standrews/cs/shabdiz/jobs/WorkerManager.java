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
package uk.ac.standrews.cs.shabdiz.jobs;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.jetson.exception.JsonRpcException;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
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
        final Worker worker = worker_descriptor.getApplicationReference();
        worker.getAddress(); // makes a remote call
    }

    @Override
    public Worker deploy(final ApplicationDescriptor descriptor) throws Exception {

        final RemoteWorkerDescriptor worker_descriptor = (RemoteWorkerDescriptor) descriptor;
        final Process worker_process = worker_process_builder.start(worker_descriptor.getHost());
        final InetSocketAddress worker_address = new InetSocketAddress(worker_descriptor.getHost().getAddress(), getWorkerRemoteAddressFromProcessOutput(worker_process).getPort());
        final WorkerRemote worker_remote = WorkerRemoteProxyFactory.getProxy(worker_address);
        return new DefaultWorkerWrapper(network, worker_remote, worker_process, new InetSocketAddress(descriptor.getHost().getAddress(), worker_address.getPort()));
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {
            final RemoteWorkerDescriptor worker_descriptor = (RemoteWorkerDescriptor) descriptor;
            final Worker worker = worker_descriptor.getApplicationReference();
            if (worker != null) {
                worker.shutdown();
            }
        }
        catch (final JsonRpcException e) {
            //ignore; expected.
        }
        finally {
            super.kill(descriptor);
        }
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