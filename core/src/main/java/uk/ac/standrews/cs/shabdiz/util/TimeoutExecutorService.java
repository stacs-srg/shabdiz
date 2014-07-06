/*
 * Copyright 2013 University of St Andrews School of Computer Science
 *
 * This file is part of Shabdiz.
 *
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A utility {@link ThreadPoolExecutor} that is used to execute tasks with timeout.
 * This class automatically terminates its idle threads and does not need to be shut down.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class TimeoutExecutorService extends ThreadPoolExecutor {

    private static final TimeoutExecutorService TIMEOUT_EXECUTOR_SERVICE_INSTANCE = new TimeoutExecutorService();
    private static final long IDLE_THREAD_TIMEOUT_IN_MILLISECONDS = 500L;
    private static final int UNLIMITED_RETRY_COUNT = Integer.MAX_VALUE;

    private TimeoutExecutorService() {

        super(0, Integer.MAX_VALUE, IDLE_THREAD_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), new FormattedNameThreadFactory("TimeoutExecutorService_%d"));
    }

    /**
     * Await completion of a given task for a given timeout.
     *
     * @param <Result> the type of the result that is returned by the task
     * @param task the task to await its completion
     * @param timeout the duration to wait for the task completion
     * @return the result of the task
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws TimeoutException the timeout exception
     */
    public static <Result> Result awaitCompletion(final Callable<Result> task, final Duration timeout) throws InterruptedException, ExecutionException, TimeoutException {

        return awaitCompletion(task, timeout.getLength(), timeout.getTimeUnit());
    }

    /**
     * Await completion of a given task for a given timeout.
     *
     * @param <Result> the type of the result that is returned by the task
     * @param task the task to await its execution
     * @param time the maximum duration to wait for the task completion
     * @param time_unit the unit of the maximum duration to wait for the task completion
     * @return the result of the task
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws TimeoutException the timeout exception
     */
    public static <Result> Result awaitCompletion(final Callable<Result> task, final long time, final TimeUnit time_unit) throws InterruptedException, ExecutionException, TimeoutException {

        final Future<Result> future_result = TIMEOUT_EXECUTOR_SERVICE_INSTANCE.submit(task);
        try {
            return future_result.get(time, time_unit);
        }
        finally {
            future_result.cancel(true);
            TIMEOUT_EXECUTOR_SERVICE_INSTANCE.purge();
        }
    }

    public static <Result> Result retry(final Callable<Result> task, final Duration timeout, final Duration retry_interval) throws InterruptedException, ExecutionException, TimeoutException {

        return retry(task, timeout.getLength(TimeUnit.NANOSECONDS), retry_interval.getLength(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
    }

    public static <Result> Result retry(final Callable<Result> task, final Duration timeout) throws InterruptedException, ExecutionException, TimeoutException {

        return retry(task, timeout.getLength(TimeUnit.NANOSECONDS), 0, TimeUnit.NANOSECONDS);
    }

    public static <Result> Result retry(final Callable<Result> task, final long timeout, final long retry_interval, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

        return retry(task, timeout, retry_interval, unit, UNLIMITED_RETRY_COUNT);
    }

    public static <Result> Result retry(final Callable<Result> task, final long timeout, final long retry_interval, final TimeUnit unit, int max_retry_count) throws InterruptedException, ExecutionException, TimeoutException {

        final TaskRetryWrapper<Result> retry_wrapper = new TaskRetryWrapper<Result>(task, max_retry_count);
        retry_wrapper.setRetryInterval(retry_interval, unit);

        final Future<Result> future_result = TIMEOUT_EXECUTOR_SERVICE_INSTANCE.submit(retry_wrapper);
        try {
            return future_result.get(timeout, unit);
        }
        finally {
            future_result.cancel(true);
            TIMEOUT_EXECUTOR_SERVICE_INSTANCE.purge();
        }
    }

    private static class TaskRetryWrapper<Result> implements Callable<Result> {

        private final Callable<Result> task;
        private final int max_retry_count;
        private int retry_count;
        private long interval_millis;

        private TaskRetryWrapper(Callable<Result> task, int max_retry_count) {

            this.task = task;
            this.max_retry_count = max_retry_count;
        }

        @Override
        public Result call() throws Exception {

            Exception error = null;
            while (!Thread.currentThread().isInterrupted() && !hasReachedMaxRetryCount()) {
                try {
                    return task.call();
                }
                catch (Exception e) {
                    error = e;
                    retry_count++;
                    delayRetryIfNecessary();
                }
            }
            assert error != null;
            throw error;
        }

        private void delayRetryIfNecessary() throws InterruptedException {

            if (isIntervalSpecified()) {
                Thread.sleep(interval_millis);
            }
        }

        private boolean isIntervalSpecified() {

            return interval_millis > 0;
        }

        void setRetryInterval(long interval, TimeUnit unit) {

            interval_millis = TimeUnit.MILLISECONDS.convert(interval, unit);
        }

        private boolean hasReachedMaxRetryCount() {

            return retry_count >= max_retry_count;
        }
    }
}
