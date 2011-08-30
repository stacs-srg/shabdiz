package uk.ac.standrews.cs.shabdiz.interfaces;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Presents a reference to the pending remote result of a remote asynchronous computation.
 *
 * @param <Result> The result type returned by this Future's {@link IFutureRemoteReference#get()} method
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IFutureRemoteReference<Result extends Serializable> {

    String GET_ID_METHOD_NAME = "getId";
    String GET_ADDRESS_METHOD_NAME = "getAddress";
    String GET_REMOTE_METHOD_NAME = "getRemote";

    /**
     * Gets the globally unique id associated to the value-returning job which its pending result is represented by <code>this</code> .
     *
     * @return the id of the submitted value-returning job
     */
    UUID getId();

    /**
     * Gets the address of this remote future.
     *
     * @return the address
     */
    InetSocketAddress getAddress();

    /**
     * Gets the remote operations provided by the remote pending result.
     *
     * @return the interface to remote operations
     */
    IFutureRemote<Result> getRemote(); // XXX ask Al and Graham about a better javadoc for this method...
}
