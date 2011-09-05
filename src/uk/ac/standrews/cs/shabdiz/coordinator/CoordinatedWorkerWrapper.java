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
package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;
import uk.ac.standrews.cs.shabdiz.worker.FutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.rpc.WorkerRemoteProxy;

/**
 * A wrapper for {@link WorkerRemoteProxy} which passively coordinate the workers.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatedWorkerWrapper implements IWorkerRemote, Comparable<CoordinatedWorkerWrapper> {

    private final WorkerRemoteProxy worker_remote;
    private final Coordinator coordinator;

    /**
     * Instantiates a new coordinated worker wrapper.
     *
     * @param worker_remote the worker remote to wrap
     * @param coordinator the coordinator of the remote worker
     */
    CoordinatedWorkerWrapper(final Coordinator coordinator, final WorkerRemoteProxy worker_remote) {

        this.worker_remote = worker_remote;
        this.coordinator = coordinator;
    }

    @Override
    public <Result extends Serializable> IFutureRemoteReference<Result> submit(final IJobRemote<Result> remote_job) throws RPCException {

        final FutureRemoteReference<Result> real_future_remote = worker_remote.submit(remote_job);
        final CountDownLatch result_available = coordinator.getResultAvailableLatch(real_future_remote);

        return new CoordinatedFutureRemoteReferenceWrapper<Result>(coordinator, real_future_remote, result_available);

    }

    @Override
    public void shutdown() throws RPCException {

        worker_remote.shutdown();
    }

    @Override
    public int compareTo(final CoordinatedWorkerWrapper other) {

        if (equals(other)) { return 0; }

        final int compare_cached_addresses = worker_remote.getCachedAddress().toString().compareTo(other.worker_remote.getCachedAddress().toString());
        return compare_cached_addresses;
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + (coordinator == null ? 0 : coordinator.hashCode());
        result = prime * result + (worker_remote == null ? 0 : worker_remote.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }
        final CoordinatedWorkerWrapper other = (CoordinatedWorkerWrapper) obj;
        if (coordinator == null) {
            if (other.coordinator != null) { return false; }
        }
        else if (!coordinator.equals(other.coordinator)) { return false; }
        if (worker_remote == null) {
            if (other.worker_remote != null) { return false; }
        }
        else if (!worker_remote.equals(other.worker_remote)) { return false; }
        return true;
    }

}
