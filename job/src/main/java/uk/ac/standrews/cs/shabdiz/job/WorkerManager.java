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

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.mashti.jetson.ClientFactory;
import org.mashti.jetson.exception.RPCException;
import org.mashti.jetson.lean.LeanClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

class WorkerManager extends AbstractApplicationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerManager.class);
    private static final Duration DEFAULT_WORKER_DEPLOYMENT_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final String DEFAULT_WORKER_JVM_ARGUMENTS = "-Xmx128m"; // add this for debug "-XX:+HeapDumpOnOutOfMemoryError"
    private static final Integer DEFAULT_WORKER_PORT = 0;
    private static final String SHABDIZ_GROUP_ID = "uk.ac.standrews.cs";
    private static final String SHABDIZ_VERSION = "1.0-SNAPSHOT";
    private final AgentBasedJavaProcessBuilder worker_process_builder;
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

    private static AgentBasedJavaProcessBuilder createRemoteJavaProcessBuilder() {

        final AgentBasedJavaProcessBuilder process_builder = new AgentBasedJavaProcessBuilder();
        process_builder.setMainClass(WorkerMain.class);
        process_builder.addJVMArgument(DEFAULT_WORKER_JVM_ARGUMENTS);
        process_builder.addMavenDependency(SHABDIZ_GROUP_ID, "shabdiz-core", SHABDIZ_VERSION);
        process_builder.addMavenDependency(SHABDIZ_GROUP_ID, "shabdiz-job", SHABDIZ_VERSION);
        //        process_builder.addFile(new File("target/shabdiz-job-1.0-SNAPSHOT.jar"));
        //        process_builder.addFile(new File("target/shabdiz-job-1.0-SNAPSHOT-tests.jar"));
        return process_builder;
    }

    @Override
    public Worker deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final Process worker_process = worker_process_builder.start(host, arguments);
        final Properties properties = Bootstrap.readProperties(WorkerMain.class, worker_process, worker_deployment_timeout);
        final InetSocketAddress worker_address = getWorkerAddressFromProperties(host, properties);
        final Integer pid = getProcessIDFromProperties(properties);
        final WorkerRemote worker_remote = proxy_factory.get(worker_address);
        final InetSocketAddress worker_remote_address = new InetSocketAddress(host.getAddress(), worker_address.getPort());
        final Worker worker = new Worker(network, worker_remote, worker_process, worker_remote_address);
        worker.setWorkerProcessId(pid);
        LOGGER.info("started a worker on {}, pid: {}", worker_remote_address, pid);
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

    private Integer getProcessIDFromProperties(final Properties properties) {

        final String pid_as_string = properties.getProperty(Bootstrap.PID_PROPERTY_KEY);
        return pid_as_string.equals("null") ? null : Integer.valueOf(pid_as_string);
    }

    private InetSocketAddress getWorkerAddressFromProperties(final Host host, final Properties properties) throws UnknownHostException {

        final int worker_port = NetworkUtil.getAddressFromString(properties.getProperty(WorkerMain.WORKER_REMOTE_ADDRESS_KEY)).getPort();
        return new InetSocketAddress(host.getAddress(), worker_port);
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

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final Worker worker = descriptor.getApplicationReference();
        worker.getAddress(); // makes a remote call
    }

    void shutdown() {

        proxy_factory.shutdown();
    }
}
