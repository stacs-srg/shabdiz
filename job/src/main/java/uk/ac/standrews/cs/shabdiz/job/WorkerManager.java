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

import com.staticiser.jetson.exception.JsonRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.host.exec.JavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class WorkerManager extends AbstractApplicationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerManager.class);
    private static final Duration DEFAULT_WORKER_DEPLOYMENT_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final String DEFAULT_WORKER_JVM_ARGUMENTS = "-Xmx128m"; // add this for debug "-XX:+HeapDumpOnOutOfMemoryError"
    private static final Integer DEFAULT_WORKER_PORT = 0;
    private final JavaProcessBuilder worker_process_builder;
    private final WorkerNetwork network;
    private final WorkerRemoteProxyFactory proxy_factory;
    private volatile Duration worker_deployment_timeout = DEFAULT_WORKER_DEPLOYMENT_TIMEOUT;

    public WorkerManager(final WorkerNetwork network, final Set<File> classpath) {

        this.network = network;
        worker_process_builder = createRemoteJavaProcessBuiler(classpath, network.§getCallbackAddress());
        proxy_factory = new WorkerRemoteProxyFactory();
    }

    private JavaProcessBuilder createRemoteJavaProcessBuiler(final Set<File> classpath, final InetSocketAddress callback_address) {

        final JavaProcessBuilder process_builder = new JavaProcessBuilder(WorkerMain.class);
        final List<String> arguments = WorkerMain.constructCommandLineArguments(callback_address, DEFAULT_WORKER_PORT);
        process_builder.addCommandLineArguments(arguments);
        process_builder.addJVMArgument(DEFAULT_WORKER_JVM_ARGUMENTS);
        process_builder.addClasspath(classpath);
        process_builder.addCurrentJVMClasspath();
        return process_builder;
    }

    @Override
    public Worker deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final Process worker_process = worker_process_builder.start(host);

        final InetSocketAddress worker_address = new InetSocketAddress(host.getAddress(), getWorkerRemoteAddressFromProcessOutput(worker_process).getPort());
        final String runtime_mxbean_name = ProcessUtil.getValueFromProcessOutput(worker_process, WorkerMain.RUNTIME_MXBEAN_NAME_KEY, DEFAULT_WORKER_DEPLOYMENT_TIMEOUT);
        final WorkerRemote worker_remote = proxy_factory.get(worker_address);
        final InetSocketAddress worker_remote_address = new InetSocketAddress(host.getAddress(), worker_address.getPort());
        final DefaultWorkerWrapper worker_wrapper = new DefaultWorkerWrapper(network, worker_remote, worker_process, worker_remote_address);
        final Integer worker_pid = ProcessUtil.getPIDFromRuntimeMXBeanName(runtime_mxbean_name);
        worker_wrapper.setWorkerProcessId(worker_pid);
        LOGGER.info("started a worker on {}, pid: {}", worker_remote_address, worker_pid);
        return worker_wrapper;
    }

    private InetSocketAddress getWorkerRemoteAddressFromProcessOutput(final Process worker_process) throws UnknownHostException, IOException, InterruptedException, TimeoutException {

        final String address_as_string = ProcessUtil.getValueFromProcessOutput(worker_process, WorkerMain.WORKER_REMOTE_ADDRESS_KEY, worker_deployment_timeout);
        return NetworkUtil.getAddressFromString(address_as_string);
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {
            final DefaultWorkerWrapper worker = descriptor.getApplicationReference();
            if (worker != null) {
                final Integer process_id = worker.getWorkerProcessId();
                if (process_id != null) {
                    final Platform platform = descriptor.getHost().getPlatform();
                    final String kill_command = Commands.KILL_BY_PROCESS_ID.get(platform, String.valueOf(process_id));
                    final Process kill = descriptor.getHost().execute(kill_command);
                    ProcessUtil.waitForNormalTerminationAndGetOutput(kill);
                }
                try {
                    worker.shutdown();
                } catch (final JsonRpcException e) {
                    LOGGER.trace("ignoring expected error at the time of kill", e);
                }
            }
        } finally {
            super.kill(descriptor);
        }
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final Worker worker = descriptor.getApplicationReference();
        worker.getAddress(); // makes a remote call
    }

    /**
     * Sets the worker deployment timeout.
     *
     * @param duration the new worker deployment timeout
     * @throws NullPointerException if the given timeout is {@code null}
     */
    public void setWorkerDeploymentTimeout(final Duration duration) {

        if (duration == null) {
            throw new NullPointerException();
        }
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

    void shutdown() {

        proxy_factory.shutdown();
    }
}
