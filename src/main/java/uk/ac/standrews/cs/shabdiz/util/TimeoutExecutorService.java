/*
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
