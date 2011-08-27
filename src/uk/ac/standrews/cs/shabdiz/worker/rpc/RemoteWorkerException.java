package uk.ac.standrews.cs.shabdiz.worker.rpc;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class RemoteWorkerException extends RPCException {

    private static final long serialVersionUID = 4506906155644771341L;

    /**
     * Instantiates a new remote worker exception.
     *
     * @param message the message
     */
    public RemoteWorkerException(final String message) {

        super(message);
    }

    /**
     * Instantiates a new remote worker exception from a given cause.
     *
     * @param cause the cause
     */
    public RemoteWorkerException(final Throwable cause) {

        super(cause);
    }
}
