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

import com.staticiser.jetson.util.NamingThreadFactory;

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

    private TimeoutExecutorService() {

        super(0, Integer.MAX_VALUE, IDLE_THREAD_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new NamingThreadFactory("TimeoutExecutorService_"));
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
     * @param time the duration to wait for the task completion
     * @param time_unit the time unit of the duration to wait for the task completion
     * @return the result of the task
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws TimeoutException the timeout exception
     */
    public static <Result> Result awaitCompletion(final Callable<Result> task, final Long time, final TimeUnit time_unit) throws InterruptedException, ExecutionException, TimeoutException {

        final Future<Result> future_result = TIMEOUT_EXECUTOR_SERVICE_INSTANCE.submit(task);
        try {
            return future_result.get(time, time_unit);
        } finally {
            future_result.cancel(true);
            TIMEOUT_EXECUTOR_SERVICE_INSTANCE.purge();
        }
    }
}
