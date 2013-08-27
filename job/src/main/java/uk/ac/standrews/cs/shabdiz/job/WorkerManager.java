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
package uk.ac.standrews.cs.shabdiz.job;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.mashti.jetson.ClientFactory;
import org.mashti.jetson.exception.RPCException;
import org.mashti.jetson.lean.LeanClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenManagedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

class WorkerManager extends AbstractApplicationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerManager.class);
    private static final Duration DEFAULT_WORKER_DEPLOYMENT_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final String DEFAULT_WORKER_JVM_ARGUMENTS = "-Xmx128m"; // add this for debug "-XX:+HeapDumpOnOutOfMemoryError"
    private static final Integer DEFAULT_WORKER_PORT = 0;
    private final MavenManagedJavaProcessBuilder worker_process_builder;
    private final WorkerNetwork network;
    private final ClientFactory<WorkerRemote> proxy_factory;
    private volatile Duration worker_deployment_timeout = DEFAULT_WORKER_DEPLOYMENT_TIMEOUT;
    private String[] arguments;

    public WorkerManager(final WorkerNetwork network) {

        this.network = network;
        final InetSocketAddress callback_address = network.getCallbackAddress();
        arguments = WorkerMain.constructCommandLineArguments(callback_address, DEFAULT_WORKER_PORT);
        worker_process_builder = createRemoteJavaProcessBuilder();
        proxy_factory = new LeanClientFactory<WorkerRemote>(WorkerRemote.class);
    }

    @Override
    public Worker deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final Process worker_process = worker_process_builder.start(host, arguments);
        final InetSocketAddress worker_address = new InetSocketAddress(host.getAddress(), getWorkerRemoteAddressFromProcessOutput(worker_process).getPort());
        final String runtime_mxbean_name = ProcessUtil.scanProcessOutput(worker_process, WorkerMain.RUNTIME_MX_BEAN_NAME_KEY, DEFAULT_WORKER_DEPLOYMENT_TIMEOUT);
        final WorkerRemote worker_remote = proxy_factory.get(worker_address);
        final InetSocketAddress worker_remote_address = new InetSocketAddress(host.getAddress(), worker_address.getPort());
        final Worker worker = new Worker(network, worker_remote, worker_process, worker_remote_address);
        final Integer worker_pid = ProcessUtil.getPIDFromRuntimeMXBeanName(runtime_mxbean_name);
        worker.setWorkerProcessId(worker_pid);
        LOGGER.info("started a worker on {}, pid: {}", worker_remote_address, worker_pid);
        //        final DefaultWorkerRemote worker_remote = new DefaultWorkerRemote(NetworkUtil.getLocalIPv4InetSocketAddress(0), network.getCallbackAddress());
        //        final Worker worker = new Worker(network, worker_remote, null, worker_remote.getAddress());
        return worker;
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        final Worker worker = descriptor.getApplicationReference();
        if (worker != null) {
            final Integer process_id = worker.getWorkerProcessId();
            if (process_id != null) {
                final Platform platform = descriptor.getHost().getPlatform();
                final String kill_command = Commands.KILL_BY_PROCESS_ID.get(platform, String.valueOf(process_id));
                final Process kill = descriptor.getHost().execute(kill_command);
                ProcessUtil.awaitNormalTerminationAndGetOutput(kill);
            }
            try {
                worker.shutdown();
            }
            catch (final RPCException e) {
                LOGGER.trace("ignoring expected error at the time of kill", e);
            }
        }
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

        worker_process_builder.setJVMArguments(jvm_arguments);
    }

    public void addMavenDependency(final String group_id, final String artifact_id, final String version, final String classifier) {

        worker_process_builder.addMavenDependency(group_id, artifact_id, version, classifier);
    }

    private static MavenManagedJavaProcessBuilder createRemoteJavaProcessBuilder() {

        final MavenManagedJavaProcessBuilder process_builder = new MavenManagedJavaProcessBuilder();
        process_builder.setMainClass(WorkerMain.class);
        process_builder.addJVMArgument(DEFAULT_WORKER_JVM_ARGUMENTS);
        process_builder.addMavenDependency("uk.ac.standrews.cs", "shabdiz-core", "1.0-SNAPSHOT");
        process_builder.addMavenDependency("uk.ac.standrews.cs", "shabdiz-job", "1.0-SNAPSHOT");
        return process_builder;
    }

    private InetSocketAddress getWorkerRemoteAddressFromProcessOutput(final Process worker_process) throws IOException, InterruptedException, TimeoutException {

        final String address_as_string = ProcessUtil.scanProcessOutput(worker_process, WorkerMain.WORKER_REMOTE_ADDRESS_KEY, worker_deployment_timeout);
        return NetworkUtil.getAddressFromString(address_as_string);
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final Worker worker = descriptor.getApplicationReference();
        worker.getAddress(); // makes a remote call
    }

    void shutdown() {

        proxy_factory.shutdown();
    }
}
