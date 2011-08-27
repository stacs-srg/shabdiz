package uk.ac.standrews.cs.artisan.coordinator;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import uk.ac.standrews.cs.artisan.interfaces.worker.IWorkerRemote;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.rpc.RPCException;

public class ActiveCoordinatorNode extends AbstractCoordinatorNode {

    public ActiveCoordinatorNode(final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        super(application_lib_urls, try_registry_on_connection_error);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    public synchronized boolean isCancelled(final UUID job_id) throws RPCException {

        if (!isSubmitted(job_id)) { return false; }
        if (!isDone(job_id)) { return false; }

        return getRemoteWorker(job_id).isCancelled(job_id);
    }

    @Override
    public synchronized boolean isDone(final UUID job_id) throws RPCException {

        if (!isSubmitted(job_id)) { return false; }

        return getRemoteWorker(job_id).isDone(job_id);
    }

    @Override
    public Serializable get(final UUID job_id) throws CancellationException, InterruptedException, ExecutionException, RPCException {

        if (!isSubmitted(job_id)) { return null; }

        final IWorkerRemote remote_worker = getRemoteWorker(job_id);
        while (!Thread.currentThread().isInterrupted()) {

            if (remote_worker.isDone(job_id)) {
                try {
                    remote_worker.get(job_id);
                }
                catch (final RPCException e) {
                    final Throwable exception = e.getCause();
                    launchException(exception);
                }
            }
        }

        throw new InterruptedException();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void launchException(final Throwable exception) throws InterruptedException, ExecutionException, RPCException {

        if (exception instanceof CancellationException) { throw (CancellationException) exception; }
        if (exception instanceof InterruptedException) { throw (InterruptedException) exception; }
        if (exception instanceof ExecutionException) { throw (ExecutionException) exception; }

        throw new RPCException(exception);
    }
}
