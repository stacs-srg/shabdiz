package uk.ac.standrews.cs.shabdiz.interfaces;

import java.io.Serializable;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents the remote functionalities provided by a coordinator.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ICoordinatorRemote {

    /**
     * Notifies the coordinator about the result of a submitted job.
     *
     * @param <Result> the type of result returned by the submitted job
     * @param future_reference the reference to the pending result of the submitted job
     * @param result the result of the completed job
     * @throws RPCException if unable to contact the correspondence
     */
    <Result extends Serializable> void notifyCompletion(IFutureRemoteReference<Result> future_reference, Result result) throws RPCException;

    /**
     * Notifies the coordinator about the exception resulted by a submitted job.
     *
     * @param <Result> the type of result returned by the submitted job
     * @param future_reference the reference to the pending result of the submitted job
     * @param exception the exception which occurred when trying to execute a job
     * @throws RPCException if unable to contact the correspondence
     */
    <Result extends Serializable> void notifyException(IFutureRemoteReference<Result> future_reference, Exception exception) throws RPCException;
}
