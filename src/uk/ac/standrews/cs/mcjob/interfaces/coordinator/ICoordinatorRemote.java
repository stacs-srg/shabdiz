package uk.ac.standrews.cs.mcjob.interfaces.coordinator;

import java.io.Serializable;
import java.util.UUID;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Provides the interface for operations provided to executor nodes by a coordinator node.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ICoordinatorRemote {

    /** The method name of {@link #notifyCompletion(UUID, Object)}. */
    String NOTIFY_COMPLETION_METHOD_NAME = "notifyCompletion";

    /** The method name of {@link #notifyException(UUID, Exception)}. */
    String NOTIFY_EXCEPTION_METHOD_NAME = "notifyException";

    /**
     * Notifies job execution completion.
     * 
     * @param job_id the job id
     * @param result the result of the completed job
     * @throws RPCException if unable to contact the correspondence
     */
    void notifyCompletion(UUID job_id, Serializable result) throws RPCException;

    /**
     * Notifies job execution error.
     * 
     * @param job_id the job id
     * @param exception the exception which occurred when trying to execute a job
     * @throws RPCException if unable to contact the correspondence
     */
    void notifyException(UUID job_id, Exception exception) throws RPCException;
}
