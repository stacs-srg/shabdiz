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
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Future;
import org.mashti.jetson.exception.RPCException;
import uk.ac.standrews.cs.shabdiz.util.HashCodeUtil;

/**
 * Implements a passive mechanism by which a {@link DefaultWorkerWrapper} can be contacted.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class DefaultWorkerWrapper implements Worker {

    private final WorkerNetwork network;
    private final Process worker_process;
    private final WorkerRemote proxy;
    private final InetSocketAddress worker_address;
    private volatile Integer worker_process_id;

    DefaultWorkerWrapper(final WorkerNetwork network, final WorkerRemote proxy, final Process worker_process, final InetSocketAddress worker_address) {

        this.network = network;
        this.proxy = proxy;
        this.worker_process = worker_process;
        this.worker_address = worker_address;
    }

    public Integer getWorkerProcessId() {

        return worker_process_id;
    }

    @Override
    public InetSocketAddress getAddress() {

        return worker_address;
    }

    @Override
    public synchronized <Result extends Serializable> Future<Result> submit(final Job<Result> job) throws RPCException {

        final UUID job_id = proxy.submitJob(job);
        final FutureRemoteProxy<Result> future_remote = new FutureRemoteProxy<Result>(job_id, proxy);
        network.notifyJobSubmission(future_remote);
        return future_remote;
    }

    @Override
    public void shutdown() throws RPCException {

        try {
            proxy.shutdown();
        }
        finally {
            worker_process.destroy();
        }
    }

    @Override
    public int hashCode() {

        return HashCodeUtil.generate(network.hashCode(), worker_address.hashCode(), proxy.hashCode());
    }

    @Override
    public boolean equals(final Object other) {

        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        final DefaultWorkerWrapper that = (DefaultWorkerWrapper) other;
        return worker_address.equals(that.worker_address) && proxy.equals(that.proxy) && network.equals(that.network);
    }

    @Override
    public int compareTo(final Worker other) {

        return getAddress().toString().compareTo(other.getAddress().toString());
    }

    void setWorkerProcessId(final Integer id) {

        worker_process_id = id;
    }
}
