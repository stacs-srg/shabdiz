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

import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;

/**
 * Wraps an array of {@link IJobRemote}s into a single sequential {@link IJobRemote}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class JobRemoteSequentialWrapper implements IJobRemote<Serializable[]> {

    private static final long serialVersionUID = 818815948631765997L;

    private final IJobRemote<? extends Serializable>[] squential_jobs;

    /**
     * Instantiates a new sequential job remote wrapper.
     *
     * @param squential_jobs the squential_jobs
     */
    public JobRemoteSequentialWrapper(final IJobRemote<? extends Serializable>... squential_jobs) {

        this.squential_jobs = squential_jobs;
    }

    @Override
    public Serializable[] call() throws Exception {

        final int jobs_length = squential_jobs.length;
        final Serializable[] results = new Serializable[jobs_length];

        for (int i = 0; i < jobs_length; i++) {

            final IJobRemote<? extends Serializable> sequential_job = squential_jobs[i];
            results[i] = sequential_job.call();
        }

        return results;
    }
}
