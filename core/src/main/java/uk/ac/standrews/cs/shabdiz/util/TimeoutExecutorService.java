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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.staticiser.jetson.util.NamingThreadFactory;

/**
 * The Class TimeoutExecutorService.
 */
public class TimeoutExecutorService extends ThreadPoolExecutor {

    private static final TimeoutExecutorService TIMEOUT_EXECUTOR_SERVICE_INSTANCE = new TimeoutExecutorService();

    private TimeoutExecutorService() {

        super(0, Integer.MAX_VALUE, 500L, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new NamingThreadFactory("TimeoutExecutorService_"));
    }

    /**
     * Await completion.
     * 
     * @param <Result> the generic type
     * @param task the task
     * @param timeout the timeout
     * @return the result
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws TimeoutException the timeout exception
     */
    public static <Result> Result awaitCompletion(final Callable<Result> task, final Duration timeout) throws InterruptedException, ExecutionException, TimeoutException {

        return awaitCompletion(task, timeout.getLength(), timeout.getTimeUnit());
    }

    /**
     * Await completion.
     * 
     * @param <Result> the generic type
     * @param task the task
     * @param time the time
     * @param time_unit the time_unit
     * @return the result
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws TimeoutException the timeout exception
     * @throws RejectedExecutionException the rejected execution exception
     */
    public static <Result> Result awaitCompletion(final Callable<Result> task, final Long time, final TimeUnit time_unit) throws InterruptedException, ExecutionException, TimeoutException {

        final Future<Result> future_result = TIMEOUT_EXECUTOR_SERVICE_INSTANCE.submit(task);
        try {
            return future_result.get(time, time_unit);
        }
        finally {
            future_result.cancel(true);
            TIMEOUT_EXECUTOR_SERVICE_INSTANCE.purge();
        }
    }
}
