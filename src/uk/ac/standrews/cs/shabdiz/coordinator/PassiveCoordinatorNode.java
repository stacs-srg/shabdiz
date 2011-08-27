package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.shabdiz.coordinator.rpc.CoordinatorServer;
import uk.ac.standrews.cs.shabdiz.interfaces.coordinator.ICoordinatorRemote;
import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.madface.exceptions.LibrariesOverwrittenException;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;

public class PassiveCoordinatorNode extends AbstractCoordinatorNode implements ICoordinatorRemote {

    private static final int EPHEMERAL_PORT = 0;

    private final InetSocketAddress local_address;
    private final CoordinatorServer server;
    private final Map<UUID, Serializable> notified_completions;
    private final Map<UUID, Exception> notified_exceptions;
    private final Set<UUID> cancelled_jobs;

    // -------------------------------------------------------------------------------------------------------------------------------

    public PassiveCoordinatorNode(final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        this(EPHEMERAL_PORT, application_lib_urls, try_registry_on_connection_error);
    }

    public PassiveCoordinatorNode(final int port, final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        super(application_lib_urls, try_registry_on_connection_error);

        local_address = NetworkUtil.getLocalIPv4InetSocketAddress(port);
        notified_completions = new ConcurrentSkipListMap<UUID, Serializable>();
        notified_exceptions = new ConcurrentSkipListMap<UUID, Exception>();
        cancelled_jobs = new HashSet<UUID>();

        server = new CoordinatorServer(this);
        expose();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public final boolean cancel(final UUID job_id, final boolean may_interrupt_if_running) throws RPCException {

        final boolean cancelled = super.cancel(job_id, may_interrupt_if_running);

        if (cancelled) {
            updateCancelledJobs(job_id);
        }

        return cancelled;
    }

    @Override
    public void addHost(final HostDescriptor host_descriptor) throws LibrariesOverwrittenException, AlreadyDeployedException {

        final Object[] application_deployment_params = new Object[]{local_address};
        host_descriptor.applicationDeploymentParams(application_deployment_params);

        super.addHost(host_descriptor);
    }

    @Override
    public synchronized boolean isCancelled(final UUID job_id) throws RPCException {

        if (!isSubmitted(job_id)) { return false; }
        if (!isDone(job_id)) { return false; }

        return cancelled_jobs.contains(job_id);
    }

    @Override
    public synchronized boolean isDone(final UUID job_id) {

        if (!isSubmitted(job_id)) { return false; }

        return notified_completions.containsKey(job_id) || notified_exceptions.containsKey(job_id) || cancelled_jobs.contains(job_id);
    }

    @Override
    public Serializable get(final UUID job_id) throws CancellationException, InterruptedException, ExecutionException, RPCException {

        if (!isSubmitted(job_id)) { return null; } // Check whether a job with the given id has been submitted

        while (!Thread.currentThread().isInterrupted()) {

            if (isDone(job_id)) {

                if (notified_completions.containsKey(job_id)) { return notified_completions.get(job_id); }

                final Exception exception = notified_exceptions.get(job_id);
                launchException(exception);
            }
        }

        throw new InterruptedException();
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
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void updateCancelledJobs(final UUID job_id) {

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

        if (exception instanceof CancellationException) { throw (CancellationException) exception; }
        if (exception instanceof InterruptedException) { throw (InterruptedException) exception; }
        if (exception instanceof ExecutionException) { throw (ExecutionException) exception; }

        throw new RPCException(exception);
    }
}
