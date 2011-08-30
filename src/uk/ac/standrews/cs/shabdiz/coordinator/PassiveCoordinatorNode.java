package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.madface.exceptions.LibrariesOverwrittenException;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.coordinator.rpc.CoordinatorRemoteServer;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.coordinator.ICoordinatorRemote;

/**
 * Passively coordinates the submission of jobs to a set of remote workers. Listens to the notifications of job completions from workers rather than actively contact them.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class PassiveCoordinatorNode extends AbstractCoordinatorNode implements ICoordinatorRemote {

    private static final int EPHEMERAL_PORT = 0;

    private final InetSocketAddress local_address;
    private final CoordinatorRemoteServer server;
    private final Map<UUID, Serializable> notified_completions;
    private final Map<UUID, Exception> notified_exceptions;
    private final Set<UUID> cancelled_jobs;

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Instantiates a new passive coordinator and  starts a local server which listens to the notifications from workers on an <i>ephemeral</i> port number.
     *
     * @param application_lib_urls the application library URLs
     * @param try_registry_on_connection_error whether to try to lookup a worker from registry upon connection error
     * @throws Exception if unable to start a coordinator node
     */
    public PassiveCoordinatorNode(final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        this(EPHEMERAL_PORT, application_lib_urls, try_registry_on_connection_error);
    }

    /**
     * Instantiates a new passive coordinator node and starts a local server which listens to the notifications from workers on the given port number.
     *
     * @param port the port on which to start the coordinator server
     * @param application_lib_urls the application library URLs
     * @param try_registry_on_connection_error  whether to try to lookup a worker from registry upon connection error
     * @throws Exception if unable to start a coordinator node
     */
    public PassiveCoordinatorNode(final int port, final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        super(application_lib_urls, try_registry_on_connection_error);

        local_address = NetworkUtil.getLocalIPv4InetSocketAddress(port);
        notified_completions = new ConcurrentSkipListMap<UUID, Serializable>();
        notified_exceptions = new ConcurrentSkipListMap<UUID, Exception>();
        cancelled_jobs = new HashSet<UUID>();

        server = new CoordinatorRemoteServer(this);
        expose();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addHost(final HostDescriptor host_descriptor) throws LibrariesOverwrittenException, AlreadyDeployedException {

        final Object[] application_deployment_params = new Object[]{local_address};
        host_descriptor.applicationDeploymentParams(application_deployment_params);

        super.addHost(host_descriptor);
    }

    @Override
    protected <Result extends Serializable> IFutureRemote<Result> getFutureRemote(final IFutureRemoteReference<Result> future_remote_reference) {

        final UUID job_id = future_remote_reference.getId();
        return new IFutureRemote<Result>() {

            @Override
            public boolean cancel(final boolean may_interrupt_if_running) throws RPCException {

                if (isDone()) { return false; }

                final boolean cancelled = future_remote_reference.getRemote().cancel(may_interrupt_if_running);

                if (cancelled) {
                    addCancelledJob(job_id);
                }

                return cancelled;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Result get() throws InterruptedException, ExecutionException, RPCException {

                while (!Thread.currentThread().isInterrupted()) {

                    if (isDone()) {

                        if (notified_completions.containsKey(job_id)) { return (Result) notified_completions.get(job_id); }
                        if (notified_exceptions.containsKey(job_id)) {

                            launchException(notified_exceptions.get(job_id));
                        }

                        throw new CancellationException();
                    }
                }

                throw new InterruptedException();
            }

            @Override
            public Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException, RPCException {

                return new FutureTask<Result>(new Callable<Result>() {

                    @Override
                    public Result call() throws Exception {

                        return get();
                    }
                }).get(timeout, unit);
            }

            @Override
            public boolean isCancelled() {

                if (!isDone()) { return false; }

                return cancelled_jobs.contains(job_id);
            }

            @Override
            public boolean isDone() {

                return notified_completions.containsKey(job_id) || notified_exceptions.containsKey(job_id) || cancelled_jobs.contains(job_id);
            }
        };
    }

    @Override
    public void notifyCompletion(final UUID job_id, final Serializable result) throws RPCException {

        notified_completions.put(job_id, result);
    }

    @Override
    public void notifyException(final UUID job_id, final Exception exception) throws RPCException {

        notified_exceptions.put(job_id, exception);
    }

    @Override
    public void shutdown() {

        unexpose();
        super.shutdown();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void addCancelledJob(final UUID job_id) {

        cancelled_jobs.add(job_id);
    }

    private void expose() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        server.setLocalAddress(local_address.getAddress());
        server.setPort(local_address.getPort());

        server.start(true);
    }

    private void unexpose() {

        try {
            server.stop();
        }
        catch (final IOException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "Unable to stop coordinator server, because: ", e.getMessage(), e);
        }
    }

    private void launchException(final Exception exception) throws InterruptedException, ExecutionException, RPCException {

        if (exception instanceof InterruptedException) { throw (InterruptedException) exception; }
        if (exception instanceof ExecutionException) { throw (ExecutionException) exception; }

        throw new RPCException(exception);
    }
}
