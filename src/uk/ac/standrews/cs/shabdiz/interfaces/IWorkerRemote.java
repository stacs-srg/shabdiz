package uk.ac.standrews.cs.shabdiz.interfaces;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents the remote functionalities provided by a worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IWorkerRemote {

    /**
     * Submits a value-returning task for execution to a remote worker and returns the pending result of the task.
     *
     * @param <Result> the generic type
     * @param remote_job the job to submit
     * @return the reference to the pending result of the submitted task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the given remote job is <code>null</code>
     * @throws RPCException if unable to make the remote call
     * @see ExecutorService#submit(java.util.concurrent.Callable)
     */
    <Result extends Serializable> IFutureRemoteReference<Result> submit(IJobRemote<Result> remote_job) throws RPCException;

    /**
     * Shuts down this worker.
     * 
     * @throws RPCException if unable to make the remote call
     */
    void shutdown() throws RPCException;
}