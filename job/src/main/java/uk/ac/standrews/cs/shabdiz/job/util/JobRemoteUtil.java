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
package uk.ac.standrews.cs.shabdiz.job.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.job.JobRemote;
import uk.ac.standrews.cs.shabdiz.job.wrapper.JobRemoteSequentialWrapper;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Utility to manage pending result of {@link JobRemote}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class JobRemoteUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRemoteUtil.class);

    private JobRemoteUtil() {

    }

    /**
     * Blocks until either the given timeout duration has elapsed or all the given futures are done.
     *
     * @param <Result> the type of pending result
     * @param futures the futures to wait for
     * @param timeout the timeout
     * @throws TimeoutException if the timeout duration has elapsed before all the futures are done
     * @throws InterruptedException if the blocking was interrupted
     * @throws ExecutionException the execution exception
     */
    public static <Result> void blockUntilFuturesAreDone(final Set<Future<Result>> futures, final Duration timeout) throws TimeoutException, InterruptedException, ExecutionException {

        TimeoutExecutorService.awaitCompletion(new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                blockUntilFuturesAreDone(futures);
                return null;
            }
        }, timeout);
    }

    /**
     * Blocks until all the given futures are done, either in a result or exception.
     *
     * @param <Result> the type of pending result
     * @param futures the futures to wait for
     */
    public static <Result> void blockUntilFuturesAreDone(final Set<Future<Result>> futures) {

        for (final Future<?> future : futures) {
            try {
                future.get();
            } catch (final Exception e) {
                LOGGER.trace("ignore error", e);
            }
        }
    }

    /**
     * Wraps a list of given jobs into a single job which executes the given jobs sequentially and returns their results in an array containing the result of each of the jobs.
     * The list of results is in the same oder as the given jobs to wrap.
     *
     * @param squential_jobs the jobs to execute sequentially
     * @return the single job which executes the given jobs sequentially
     */
    public static JobRemote<Serializable[]> wrapIntoASequentialJob(final JobRemote<? extends Serializable>... squential_jobs) {

        return new JobRemoteSequentialWrapper(squential_jobs);
    }
}
