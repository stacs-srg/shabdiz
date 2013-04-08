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
package uk.ac.standrews.cs.shabdiz.util.job.wrapper;

import java.io.Serializable;
import java.util.concurrent.Callable;

import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.Timing;
import uk.ac.standrews.cs.shabdiz.jobs.JobRemote;

/**
 * Wraps a given job into a retry mechanism.
 * @see Timing#retry(Callable, Duration, Duration, boolean, DiagnosticLevel)
 *
 * @param <Result> the generic type
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class JobRemoteRetryWrapper<Result extends Serializable> implements JobRemote<Result> {

    private static final long serialVersionUID = 8452981241994840258L;
    private final JobRemote<Result> job;
    private final Duration overall_timeout;
    private final Duration loop_delay;
    private final boolean delay_is_fixed;
    private final DiagnosticLevel reporting_level;

    /**
     * Instantiates a new job remote retry wrapper.
     *
     * @param job the job
     * @param overall_timeout the overall_timeout
     * @param loop_delay the loop_delay
     * @param delay_is_fixed the delay_is_fixed
     * @see Timing#retry(Callable, Duration, Duration, boolean, DiagnosticLevel)
     */
    public JobRemoteRetryWrapper(final JobRemote<Result> job, final Duration overall_timeout, final Duration loop_delay, final boolean delay_is_fixed) {

        this(job, overall_timeout, loop_delay, delay_is_fixed, DiagnosticLevel.NONE);
    }

    /**
     * Instantiates a new job remote retry wrapper.
     *
     * @param job the job to retry
     * @param overall_timeout the overall_timeout
     * @param loop_delay the loop_delay
     * @param delay_is_fixed the delay_is_fixed
     * @param reporting_level the reporting_level
     * @see Timing#retry(Callable, Duration, Duration, boolean, DiagnosticLevel)
     */
    public JobRemoteRetryWrapper(final JobRemote<Result> job, final Duration overall_timeout, final Duration loop_delay, final boolean delay_is_fixed, final DiagnosticLevel reporting_level) {

        this.job = job;
        this.overall_timeout = overall_timeout;
        this.loop_delay = loop_delay;
        this.delay_is_fixed = delay_is_fixed;
        this.reporting_level = reporting_level;
    }

    @Override
    public Result call() throws Exception {

        return Timing.retry(job, overall_timeout, loop_delay, delay_is_fixed, reporting_level);
    }
}