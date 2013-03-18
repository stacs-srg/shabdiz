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

import uk.ac.standrews.cs.shabdiz.jobs.JobRemote;

/**
 * Wraps an array of {@link JobRemote}s into a single sequential {@link JobRemote}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class JobRemoteSequentialWrapper implements JobRemote<Serializable[]> {

    private static final long serialVersionUID = 818815948631765997L;

    private final JobRemote<? extends Serializable>[] squential_jobs;

    /**
     * Instantiates a new sequential job remote wrapper.
     * 
     * @param squential_jobs the squential_jobs
     */
    public JobRemoteSequentialWrapper(final JobRemote<? extends Serializable>... squential_jobs) {

        this.squential_jobs = squential_jobs;
    }

    @Override
    public Serializable[] call() throws Exception {

        final int jobs_length = squential_jobs.length;
        final Serializable[] results = new Serializable[jobs_length];

        for (int i = 0; i < jobs_length; i++) {

            final JobRemote<? extends Serializable> sequential_job = squential_jobs[i];
            results[i] = sequential_job.call();
        }

        return results;
    }
}
