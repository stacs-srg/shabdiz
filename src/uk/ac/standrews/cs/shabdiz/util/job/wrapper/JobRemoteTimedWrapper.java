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
 * For more information, see <http://beast.cs.st-andrews.ac.uk:8080/hudson/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.util.job.wrapper;

import java.io.Serializable;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;

/**
 * Wraps an {@link IJobRemote} in a timed job.
 *
 * @param <Result> the type of result returned by this job
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class JobRemoteTimedWrapper<Result extends Serializable> implements IJobRemote<Result> {

    private static final long serialVersionUID = 1478058445703561250L;

    private transient TimeoutExecutor timeout_executor;
    private final IJobRemote<Result> timed_job;

    private final Duration timeout;

    /**
     * Instantiates a new job remote timed wrapper.
     *
     * @param timed_job the timed_job
     * @param timeout the timeout
     */
    public JobRemoteTimedWrapper(final IJobRemote<Result> timed_job, final Duration timeout) {

        this.timed_job = timed_job;
        this.timeout = timeout;
    }

    @Override
    public Result call() throws Exception {

        timeout_executor = TimeoutExecutor.makeTimeoutExecutor(1, timeout, true, true, getClass().getSimpleName());
        try {
            return timeout_executor.executeWithTimeout(timed_job);
        }
        finally {
            timeout_executor.shutdown();
        }
    }

}
