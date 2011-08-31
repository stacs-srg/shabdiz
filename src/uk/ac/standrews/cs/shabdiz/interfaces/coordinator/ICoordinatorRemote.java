package uk.ac.standrews.cs.shabdiz.interfaces.coordinator;

import java.io.Serializable;
import java.util.UUID;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents the remote functionalities provided by a coordinator.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ICoordinatorRemote {

    /** The remote method name for {@link #notifyCompletion(UUID, Object)}. */
    String NOTIFY_COMPLETION_REMOTE_METHOD_NAME = "notifyCompletion";

    /** The remote method name for {@link #notifyException(UUID, Exception)}. */
    String NOTIFY_EXCEPTION_REMOTE_METHOD_NAME = "notifyException";

    /**
     * Notifies the coordinator about the result of a submitted job.
     * 
     * @param job_id the id of the submitted job
     * @param result the result of the completed job
     * @throws RPCException if unable to contact the correspondence
     */
    void notifyCompletion(UUID job_id, Serializable result) throws RPCException;

    /**
     * Notifies the coordinator about the exception resulted by a submitted job.
     * 
     * @param job_id the id of the submitted job
     * @param exception the exception which occurred when trying to execute a job
     * @throws RPCException if unable to contact the correspondence
     */
    void notifyException(UUID job_id, Exception exception) throws RPCException;
}
