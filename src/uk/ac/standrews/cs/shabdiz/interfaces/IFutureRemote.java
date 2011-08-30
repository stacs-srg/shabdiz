package uk.ac.standrews.cs.shabdiz.interfaces;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents the result of a remote asynchronous computation.
 *
 * @param <Result> The result type returned by this Future's {@link IFutureRemote#get()} method
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IFutureRemote<Result extends Serializable> {

    String CANCEL_METHOD_NAME = "cancel";
    String GET_METHOD_NAME = "get";
    String GET_WITH_TIMEOUT_METHOD_NAME = "getWithTimeout";
    String IS_CANCELLED_METHOD_NAME = "isCancelled";
    String IS_DONE_METHOD_NAME = "isDone";

    /**
     * @see Future#cancel(boolean)
     */
    boolean cancel(final boolean may_interrupt_if_running) throws RPCException;

    /**
     * @see Future#get()
     */
    Result get() throws InterruptedException, ExecutionException, RPCException;

    /**
     * @see Future#get(long, TimeUnit)
     */
    Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException, RPCException;

    /**
     * @see Future#isCancelled()
     */
    public boolean isCancelled() throws RPCException;

    /**
     * @see Future#isDone()
     */
    boolean isDone() throws RPCException;
}
