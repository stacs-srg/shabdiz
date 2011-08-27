package uk.ac.standrews.cs.artisan.interfaces;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Presents the result of a remote asynchronous computation.
 *
 * @param <Result> The result type returned by this Future's {@link IFutureRemote#get()} method
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IFutureRemote<Result extends Serializable> extends Future<Result> {

    String GET_ID_METHOD_NAME = "getId";
    String CANCEL_METHOD_NAME = "cancel";
    String IS_CANCELLED_METHOD_NAME = "isCancelled";
    String IS_DONE_METHOD_NAME = "isDone";
    String GET_METHOD_NAME = "get";

    /**
     * Gets the globally unique id associated to the value-returning job which its pending result is represented by <code>this</code> .
     *
     * @return the id of the submitted value-returning job
     */
    UUID getId();
}
