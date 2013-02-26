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
package uk.ac.standrews.cs.shabdiz.util.job.wrapper;

import java.util.concurrent.Callable;

import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.Timing;
import uk.ac.standrews.cs.shabdiz.interfaces.JobRemote;
import uk.ac.standrews.cs.shabdiz.util.SerializableVoid;

/**
 * Repeats a given {@link JobRemote} until a timeout has elapsed.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class JobRemoteRepeatWrapper implements JobRemote<SerializableVoid> {

    private static final long serialVersionUID = 30120696504793434L;

    private transient Callable<Void> void_wrapper;
    private final JobRemote<SerializableVoid> job;
    private final Duration overall_timeout;
    private final Duration loop_delay;
    private final boolean delay_is_fixed;
    private final DiagnosticLevel reporting_level;

    /**
     * Instantiates a wrapper which repeats the given job until the given timeout has elapsed.
     * Sets the reporting level to {@link DiagnosticLevel#NONE}.
     *
     * @param job the job to repeat
     * @param overall_timeout the overall duration that the job is repeated
     * @param loop_delay the duration of the delay at each loop interval
     * @param delay_is_fixed true if the delay represents a fixed sleep period between the end of one repetition and the start of the next, false if it represents the minimum time between the start of one repetition and the start of the next
     * @see Timing#repeat(Callable, Duration, Duration, boolean, DiagnosticLevel)
     */
    public JobRemoteRepeatWrapper(final JobRemote<SerializableVoid> job, final Duration overall_timeout, final Duration loop_delay, final boolean delay_is_fixed) {

        this(job, overall_timeout, loop_delay, delay_is_fixed, DiagnosticLevel.NONE);
    }

    /**
     * Instantiates a wrapper which repeats the given job until the given timeout has elapsed.
     *
     * @param job the job to repeat
     * @param overall_timeout the overall duration that the job is repeated
     * @param loop_delay the duration of the delay at each loop interval
     * @param delay_is_fixed true if the delay represents a fixed sleep period between the end of one repetition and the start of the next, false if it represents the minimum time between the start of one repetition and the start of the next
     * @param reporting_level  the diagnostic reporting threshold
     * @see Timing#repeat(Callable, Duration, Duration, boolean, DiagnosticLevel)
     */
    public JobRemoteRepeatWrapper(final JobRemote<SerializableVoid> job, final Duration overall_timeout, final Duration loop_delay, final boolean delay_is_fixed, final DiagnosticLevel reporting_level) {

        this.job = job;
        this.overall_timeout = overall_timeout;
        this.loop_delay = loop_delay;
        this.delay_is_fixed = delay_is_fixed;
        this.reporting_level = reporting_level;
    }

    @Override
    public SerializableVoid call() throws Exception {

        void_wrapper = new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                job.call();

                return null;
            }
        };

        while (!Thread.currentThread().isInterrupted()) {
            Timing.repeat(void_wrapper, overall_timeout, loop_delay, delay_is_fixed, reporting_level);
        }
        return null;
    }
}
