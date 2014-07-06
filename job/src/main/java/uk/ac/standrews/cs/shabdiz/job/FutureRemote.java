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

package uk.ac.standrews.cs.shabdiz.job;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presents a proxy to the pending result of a {@link Job job}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class FutureRemote<Result extends Serializable> extends CompletableFuture<Result> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FutureRemote.class);
    private final UUID job_id;
    private final WorkerRemote proxy;

    FutureRemote(final UUID job_id, final WorkerRemote proxy) {

        this.job_id = job_id;
        this.proxy = proxy;
    }

    @Override
    public boolean cancel(final boolean may_interrupt) {

        return super.cancel(may_interrupt) && cancelOnRemote(may_interrupt);
    }

    @Override
    public int hashCode() {

        return job_id.hashCode();
    }

    @Override
    public boolean equals(final Object other) {

        if (this == other) { return true; }
        if (!(other instanceof FutureRemote)) { return false; }
        final FutureRemote that = (FutureRemote) other;
        return job_id.equals(that.job_id);
    }

    protected UUID getJobID() {

        return job_id;
    }

    private boolean cancelOnRemote(final boolean may_interrupt) {

        try {
            return proxy.cancel(job_id, may_interrupt).get();
        }
        catch (final InterruptedException | ExecutionException e) {
            LOGGER.warn("failed to cancel job on remote", e);
            return false;
        }
    }
}
