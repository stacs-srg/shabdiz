package uk.ac.standrews.cs.mcjob.coordinator;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.rpc.RPCException;

public class PassiveCoordinatorNode extends AbstractCoordinatorNode {

    private static final int EPHEMERAL_PORT = 0;

    // -------------------------------------------------------------------------------------------------------------------------------

    public PassiveCoordinatorNode(final Set<HostDescriptor> host_descriptors, final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        this(EPHEMERAL_PORT, host_descriptors, application_lib_urls, try_registry_on_connection_error);
    }

    public PassiveCoordinatorNode(final int port, final Set<HostDescriptor> host_descriptors, final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        super(port, host_descriptors, application_lib_urls, try_registry_on_connection_error);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------------------------------------------------------------

    private void launchException(final Exception exception) throws InterruptedException, ExecutionException, RPCException {

        if (exception instanceof CancellationException) { throw (CancellationException) exception; }
        if (exception instanceof InterruptedException) { throw (InterruptedException) exception; }
        if (exception instanceof ExecutionException) { throw (ExecutionException) exception; }

        throw new RPCException(exception);
    }
}
