package uk.ac.standrews.cs.mcjob.interfaces.worker;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

import uk.ac.standrews.cs.mcjob.interfaces.IRemoteJob;
import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * The Interface IMcJobRemote.
 */
public interface IWorkerRemote {

    String GET_ADDRESS_METHOD_NAME = "getAddress";
    String SUBMIT_METHOD_NAME = "submit";
    String CANCEL_METHOD_NAME = "cancel";
    String IS_CANCELLED_METHOD_NAME = "isCancelled";
    String IS_DONE_METHOD_NAME = "isDone";
    String GET_METHOD_NAME = "get";

    /**
     * Returns this node's address.
     *
     * @return this node's address
     * @throws RPCException if an error occurs during the remote call
     */
    InetSocketAddress getAddress() throws RPCException;

    /**
     * Submit.
     *
     * @param remote_job the job
     * @return the uUID
     * @throws RPCException the rPC exception
     */
    UUID submit(IRemoteJob<?> remote_job) throws RPCException;

    /**
     * Cancel.
     *
     * @param job_id the job_id
     * @param may_interrupt_if_running the may_interrupt_if_running
     * @return true, if successful
     * @throws RPCException the rPC exception
     */
    boolean cancel(final UUID job_id, final boolean may_interrupt_if_running) throws RPCException;

    /**
     * Checks if is cancelled.
     *
     * @param job_id the job_id
     * @return true, if is cancelled
     * @throws RPCException the rPC exception
     */
    boolean isCancelled(UUID job_id) throws RPCException;

    /**
     * Checks if is done.
     *
     * @param job_id the job_id
     * @return true, if is done
     * @throws RPCException the rPC exception
     */
    boolean isDone(UUID job_id) throws RPCException;

    Serializable get(UUID job_id) throws RPCException;

}
