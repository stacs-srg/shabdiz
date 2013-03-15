package uk.ac.standrews.cs.shabdiz.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.NamingThreadFactory;

public class TimeoutExecutorService extends ThreadPoolExecutor {

    private static final TimeoutExecutorService TIMEOUT_EXECUTOR_SERVICE = new TimeoutExecutorService();

    private TimeoutExecutorService() {

        super(0, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamingThreadFactory("TimeoutExecutorService_"));
    }

    public static <Result> Result awaitCompletion(final Callable<Result> task, final Duration timeout) throws InterruptedException, ExecutionException, TimeoutException {

        return awaitCompletion(task, timeout.getLength(), timeout.getTimeUnit());
    }

    public static <Result> Result awaitCompletion(final Callable<Result> task, final Long time, final TimeUnit time_unit) throws InterruptedException, ExecutionException, TimeoutException {

        final Future<Result> future_result = TIMEOUT_EXECUTOR_SERVICE.submit(task);
        try {
            return future_result.get(time, time_unit);
        }
        finally {
            future_result.cancel(true);
            TIMEOUT_EXECUTOR_SERVICE.purge();
        }
    }
}
