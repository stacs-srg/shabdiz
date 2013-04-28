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
package uk.ac.standrews.cs.shabdiz.jobs;

import java.io.IOException;
import java.net.InetSocketAddress;

import uk.ac.standrews.cs.jetson.ClientFactory;

public final class WorkerRemoteProxyFactory extends ClientFactory<WorkerRemote> {

    private static final WorkerRemoteProxyFactory WORKER_REMOTE_PROXY_FACTORY_INSTANCE = new WorkerRemoteProxyFactory();

    private WorkerRemoteProxyFactory() {

        super(WorkerRemote.class, WorkerJsonFactory.getInstance());
    }

    public static WorkerRemote getProxy(final InetSocketAddress address) throws IllegalArgumentException, IOException {

        return WORKER_REMOTE_PROXY_FACTORY_INSTANCE.get(address);
    }

    public static WorkerRemoteProxyFactory getInstance() {

        return WORKER_REMOTE_PROXY_FACTORY_INSTANCE;
    }
}
