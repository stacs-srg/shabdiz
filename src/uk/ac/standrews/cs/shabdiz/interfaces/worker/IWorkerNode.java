package uk.ac.standrews.cs.shabdiz.interfaces.worker;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.Future;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents the local functionalities provided by a worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IWorkerNode {

    /**
     * Gets this worker's address.
     *
     * @return this worker's address
     * @throws RPCException if unable to make the remote call
     */
    InetSocketAddress getAddress();

    /**
     * Gets the <code>id</code> of all jobs which have been submitted to this worker.
     *
     * @return the set of <code>id</code>s
     */
    SortedSet<UUID> getJobIds();

    /**
     * Gets the pending result of a job which its <code>id</code> matches the given <code>id</code>.
     *
     * @param job_id the job id
     * @return the pending result
     */
    Future<? extends Serializable> getFutureById(UUID job_id);

    /**
     * Handles the completion of a job.
     *
     * @param job_id the job_id
     * @param result the result
     */
    void handleCompletion(UUID job_id, Serializable result);

    /**
     * Handles the exception which has thrown as the result of a job execution.
     *
     * @param job_id the id of the job which has resulted in exception
     * @param exception the exception
     */
    void handleException(UUID job_id, Exception exception);
}
