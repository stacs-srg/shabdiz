package uk.ac.standrews.cs.shabdiz.interfaces.worker;

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

    /** The remote method name for {@link #cancel(boolean)}. */
    String CANCEL_REMOTE_METHOD_NAME = "cancel";

    /** The remote method name for {@link #get()}. */
    String GET_REMOTE_METHOD_NAME = "get";

    /** The remote method name for {@link #get(long, TimeUnit)}. */
    String GET_WITH_TIMEOUT_REMOTE_METHOD_NAME = "getWithTimeout";

    /** The remote method name for {@link #isCancelled()}. */
    String IS_CANCELLED_REMOTE_METHOD_NAME = "isCancelled";

    /** The remote method name for {@link #isDone()}. */
    String IS_DONE_REMOTE_METHOD_NAME = "isDone";

    /**
     * Cancels the remote computation which is submitted to a remote worker.
     *
     * @param may_interrupt_if_running whether to interrupt the computation if it is running
     * @return true, if successfully cancelled
     * @throws RPCException  if unable to communicate with the worker which performs the computation
     * @see Future#cancel(boolean)
     */
    boolean cancel(final boolean may_interrupt_if_running) throws RPCException;

    /**
     * Waits if necessary for the remote computation to complete, and then retrieves its result.
     *
     * @return the result of the remote computation
     * @throws InterruptedException if the remote computation is interrupted
     * @throws ExecutionException if the execution is resulted in an exception
     * @throws RPCException  if unable to communicate with the worker which performs the computation
     * @see Future#get()
     */
    Result get() throws InterruptedException, ExecutionException, RPCException;

    /**
     * Waits if necessary for at most the given time for the computation which is submitted to a remote worker to complete, and then retrieves its result.
     *
     * @param timeout the timeout
     * @param unit the time unit for the given timeout
     * @return the result of the remote computation
     * @throws InterruptedException if the remote computation is interrupted
     * @throws ExecutionException if the execution is resulted in an exception
     * @throws TimeoutException if the timeout has reached before the retrieval of the computation
     * @throws RPCException if unable to communicate with the remote worker
     * @see Future#get(long, TimeUnit)
     */
    Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException, RPCException;

    /**
     * Checks if the remote computation was cancelled before it completed normally.
     *
     * @return true, if the remote computation is cancelled
     * @throws RPCException if unable to communicate with the worker which performs the computation
     * @see Future#isCancelled()
     */
    boolean isCancelled() throws RPCException;

    /**
     * Returns true if the remote computation is completed. Completion may be due to normal termination, an exception, or cancellation; in all of these cases, this method will return true.
     *
     * @return true, if the computation is done
     * @throws RPCException  if unable to communicate with the worker which performs the computation
     * @see Future#isDone()
     */
    boolean isDone() throws RPCException;
}
