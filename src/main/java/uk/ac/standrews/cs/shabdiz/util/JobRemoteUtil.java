/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.util;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;
import uk.ac.standrews.cs.shabdiz.api.JobRemote;
import uk.ac.standrews.cs.shabdiz.util.job.wrapper.JobRemoteSequentialWrapper;

/**
 * Utility to manage pending result of {@link JobRemote}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class JobRemoteUtil {

    private JobRemoteUtil() {

    }

    // -------------------------------------------------------------------------------------------------------------------------------

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
            }
            catch (final Exception e) {
                // ignore
            }
        }
    }

    /**
     * Blocks until either the given timeout duration has elapsed or all the given futures are done.
     *
     * @param <Result> the type of pending result
     * @param futures the futures to wait for
     * @param timeout the timeout
     * @throws TimeoutException if the timeout duration has elapsed before all the futures are done
     * @throws InterruptedException if the blocking was interrupted
     */
    public static <Result> void blockUntilFuturesAreDone(final Set<Future<Result>> futures, final Duration timeout) throws TimeoutException, InterruptedException {

        final TimeoutExecutor timeout_executor = TimeoutExecutor.makeTimeoutExecutor(1, timeout, true, true, JobRemoteUtil.class.getSimpleName());

        try {
            timeout_executor.executeWithTimeout(new Runnable() {

                @Override
                public void run() {

                    blockUntilFuturesAreDone(futures);
                }
            });
        }
        finally {
            timeout_executor.shutdown();
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
