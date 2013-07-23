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

import com.google.common.util.concurrent.AbstractFuture;
import java.io.Serializable;
import java.util.UUID;
import org.mashti.jetson.exception.RPCException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presents a proxy to the pending result of a remote computation.
 *
 * @param <Result> the type of pending result
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class FutureRemoteProxy<Result extends Serializable> extends AbstractFuture<Result> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FutureRemoteProxy.class);
    private final UUID job_id;
    private final WorkerRemote proxy;

    FutureRemoteProxy(final UUID job_id, final WorkerRemote proxy) {

        this.job_id = job_id;
        this.proxy = proxy;
    }

    @Override
    public boolean cancel(final boolean may_interrupt) {

        return super.cancel(may_interrupt) && cancelOnRemote(may_interrupt);
    }

    public boolean set(final Serializable result) {

        return super.set((Result) result);
    }

    @Override
    public boolean setException(final Throwable throwable) {

        return super.setException(throwable);
    }

    @Override
    public int hashCode() {

        return job_id.hashCode();
    }

    @Override
    public boolean equals(final Object other) {

        if (this == other) { return true; }
        if (!(other instanceof FutureRemoteProxy)) { return false; }
        final FutureRemoteProxy that = (FutureRemoteProxy) other;
        return job_id.equals(that.job_id);
    }

    protected UUID getJobID() {

        return job_id;
    }

    private boolean cancelOnRemote(final boolean may_interrupt) {

        try {
            return proxy.cancel(job_id, may_interrupt);
        }
        catch (final RPCException e) {
            LOGGER.warn("failed to cancel job", e);
            return false;
        }
    }
}
