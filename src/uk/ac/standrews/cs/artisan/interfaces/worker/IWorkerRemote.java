package uk.ac.standrews.cs.artisan.interfaces.worker;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import uk.ac.standrews.cs.artisan.interfaces.IRemoteJob;
import uk.ac.standrews.cs.nds.madface.IPingable;
import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents the remotely available operations provided by a worker.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IWorkerRemote extends IPingable {

    String GET_ADDRESS_METHOD_NAME = "getAddress";
    String SUBMIT_METHOD_NAME = "submit";

    /**
     * Returns this worker's address.
     *
     * @return this worker's address
     * @throws RPCException if unable to make the remote call
     */
    InetSocketAddress getAddress() throws RPCException;

    /**
     * Submits a value-returning task for execution to a remote worker and returns a remote Future representing the pending results of the task. 
     *
     * @param <Result> the type of result returned by the job
     * @param remote_job the job to submit
     * @return a remote Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the given remote job is <code>null</code>
     * @throws RPCException if unable to make the remote call
     * @see ExecutorService#submit(java.util.concurrent.Callable)
     */
    <Result extends Serializable> IFutureRemote<Result> submit(IRemoteJob<Result> remote_job) throws RejectedExecutionException, NullPointerException, RPCException;
}
