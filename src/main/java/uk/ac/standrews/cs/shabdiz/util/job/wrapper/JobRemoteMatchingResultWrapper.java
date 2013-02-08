/*
 * shabdiz Library
 * Copyright (C) 2011 Distributed Systems Architecture Research Group
 * <http://www-systems.cs.st-andrews.ac.uk/>
 *
 * This file is part of shabdiz, a variation of the Chord protocol
 * <http://pdos.csail.mit.edu/chord/>, where each node strives to maintain
 * a list of all the nodes in the overlay in order to provide one-hop
 * routing.
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
package uk.ac.standrews.cs.shabdiz.util.job.wrapper;

import java.io.Serializable;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.interfaces.JobRemote;

/**
 * Retries a given job until it returns a matching result or the given timeout elapses.
 *
 * @param <Result> the type of result returned by this job
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class JobRemoteMatchingResultWrapper<Result extends Serializable> extends JobRemoteTimedWrapper<Result> {

    private static final long serialVersionUID = 8452981241994840258L;

    /**
     * Instantiates a new job remote retry wrapper.
     *
     * @param job the job to retry
     * @param matching_result the desired result which is expected to be returned by the condition
     * @param overall_timeout the overall timeout of retrying until the desired result is returned
     * @param loop_delay the delay between retries
     */
    public JobRemoteMatchingResultWrapper(final JobRemote<Result> job, final Result matching_result, final Duration overall_timeout, final Duration loop_delay) {

        super(new JobRemote<Result>() {

            private static final long serialVersionUID = 163279473093353752L;

            @Override
            public Result call() throws Exception {

                while (!Thread.currentThread().isInterrupted() && !job.call().equals(matching_result)) {
                    loop_delay.sleep();
                }
                return matching_result;
            }
        }, overall_timeout);
    }
}
