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

import com.staticiser.jetson.exception.JsonRpcException;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Future;

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

    void setWorkerProcessId(final Integer id) {

        worker_process_id = id;
    }

    public Integer getWorkerProcessId() {

        return worker_process_id;
    }

    @Override
    public InetSocketAddress getAddress() {

        return worker_address;
    }

    @Override
    public <Result extends Serializable> Future<Result> submit(final JobRemote<Result> job) throws JsonRpcException {

        synchronized (network) {
            final UUID job_id = proxy.submitJob(job);
            final PassiveFutureRemoteProxy<Result> future_remote = new PassiveFutureRemoteProxy<Result>(job_id, proxy);
            network.notifyJobSubmission(future_remote);
            return future_remote;
        }
    }

    @Override
    public void shutdown() throws JsonRpcException {

        try {
            proxy.shutdown();
        }
        finally {
            worker_process.destroy();
        }
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + (network == null ? 0 : network.hashCode());
        result = prime * result + (worker_address == null ? 0 : worker_address.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }

        final DefaultWorkerWrapper other = (DefaultWorkerWrapper) obj;

        if (worker_address == null) {
            if (other.worker_address != null) { return false; }
        }
        else if (!worker_address.equals(other.worker_address)) { return false; }

        if (network == null) {
            if (other.network != null) { return false; }
        }
        else if (!network.equals(other.network)) { return false; }

        return true;
    }

    @Override
    public int compareTo(final Worker other) {

        return getAddress().toString().compareTo(other.getAddress().toString());
    }
}
