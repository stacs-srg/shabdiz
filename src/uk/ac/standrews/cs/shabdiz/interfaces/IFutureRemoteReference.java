package uk.ac.standrews.cs.shabdiz.interfaces;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Presents a reference to the pending result of a remote asynchronous computation.
 *
 * @param <Result> The result type returned by this Future's {@link IFutureRemoteReference#get()} method
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IFutureRemoteReference<Result extends Serializable> {

    /**
     * Gets the globally unique id associated to the value-returning job which its pending result is represented by <code>this</code> .
     *
     * @return the id of the submitted value-returning job
     */
    UUID getId();

    /**
     * Gets the address of the worker on which the result is pending.
     *
     * @return the address of the worker on which the result is pending
     */
    InetSocketAddress getAddress();

    /**
     * Gets the remote operations provided by the remote pending result.
     *
     * @return the interface to remote operations
     */
    IFutureRemote<Result> getRemote();
    
    Result get() throws  Exception;
}
