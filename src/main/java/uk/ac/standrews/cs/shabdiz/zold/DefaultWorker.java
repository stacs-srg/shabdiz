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
package uk.ac.standrews.cs.shabdiz.zold;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Future;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.zold.api.JobRemote;
import uk.ac.standrews.cs.shabdiz.zold.api.Worker;

/**
 * Implements a passive mechanism by which a {@link DefaultWorker} can be contacted.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class DefaultWorker implements Worker {

    private final InetSocketAddress worker_address;
    private final DefaultLauncher launcher;
    private final Process worker_process;
    private final WorkerRemoteProxy worker_proxy;

    /**
     * Instantiates a new worker which is contacted passively.
     * 
     * @param worker_remote the worker remote to wrap
     * @param launcher the launcher by which the remote correspondence of this worker is launched
     * @param worker_process
     */
    DefaultWorker(final DefaultLauncher launcher, final InetSocketAddress worker_address, final Process worker_process) {

        this.worker_address = worker_address;
        this.launcher = launcher;
        this.worker_process = worker_process;
        worker_proxy = WorkerRemoteProxyFactory.getProxy(worker_address);
        //        worker_proxy.ping()
    }

    @Override
    public InetSocketAddress getAddress() {

        return worker_address;
    }

    @Override
    public <Result extends Serializable> Future<Result> submit(final JobRemote<Result> job) throws RPCException {

        final UUID job_id = worker_proxy.submitJob(job);
        final FutureRemoteProxy<Result> future_remote = new FutureRemoteProxy<Result>(job_id, worker_address);
        launcher.notifyJobSubmission(future_remote);
        return future_remote;
    }

    @Override
    public void shutdown() throws RPCException {

        try {
            worker_proxy.shutdown();
        }
        finally {
            worker_process.destroy();
        }
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + (launcher == null ? 0 : launcher.hashCode());
        result = prime * result + (worker_address == null ? 0 : worker_address.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }

        final DefaultWorker other = (DefaultWorker) obj;

        if (worker_address == null) {
            if (other.worker_address != null) { return false; }
        }
        else if (!worker_address.equals(other.worker_address)) { return false; }

        if (launcher == null) {
            if (other.launcher != null) { return false; }
        }
        else if (!launcher.equals(other.launcher)) { return false; }

        return true;
    }

    @Override
    public int compareTo(final Worker other) {

        return getAddress().toString().compareTo(other.getAddress().toString());
    }
}
