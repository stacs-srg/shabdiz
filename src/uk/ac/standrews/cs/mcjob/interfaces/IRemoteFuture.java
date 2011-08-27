package uk.ac.standrews.cs.mcjob.interfaces;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.rpc.RPCException;

public interface IRemoteFuture<Result extends Serializable> {

    String CANCEL_METHOD_NAME = "cancel";
    String GET_METHOD_NAME = "get";
    String GET_TIMED_METHOD_NAME = "getTimed";
    String IS_CANCELLED_METHOD_NAME = "isCancelled";
    String IS_DONE_METHOD_NAME = "isDone";

    boolean cancel(final boolean may_interrupt_if_running) throws RPCException;

    Result get() throws InterruptedException, ExecutionException, RPCException;

    Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException, RPCException;

    boolean isCancelled() throws RPCException;

    boolean isDone() throws RPCException;
}
