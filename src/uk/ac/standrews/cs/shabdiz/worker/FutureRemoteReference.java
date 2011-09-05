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
package uk.ac.standrews.cs.shabdiz.worker;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.worker.rpc.FutureRemoteProxyFactory;

/**
 * An implementation of a reference to the pending result of a remote computation.
 *
 * @param <Result> the type of result returned by the remote computation
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class FutureRemoteReference<Result extends Serializable> implements IFutureRemoteReference<Result>, Comparable<FutureRemoteReference<Result>> {

    private final UUID id;
    private final InetSocketAddress address;
    private final IFutureRemote<Result> remote;

    /**
     * Instantiates a new reference to the pending result of a remote computation with the given <code>id</code> on the given <code>address</code>.
     *
     * @param id the id of the remote computation
     * @param address the address of the worker that executes the computation
     */
    public FutureRemoteReference(final UUID id, final InetSocketAddress address) {

        this.id = id;
        this.address = address;

        remote = FutureRemoteProxyFactory.getProxy(id, address);
    }

    @Override
    public UUID getId() {

        return id;
    }

    @Override
    public InetSocketAddress getAddress() {

        return address;
    }

    @Override
    public IFutureRemote<Result> getRemote() {

        return remote;
    }

    @Override
    public int compareTo(final FutureRemoteReference<Result> other) {

        return id.compareTo(other.id);
    }

    @Override
    public int hashCode() {

        return (id == null ? 0 : id.hashCode()) + (address == null ? 0 : address.hashCode());
    }

    @Override
    public boolean equals(final Object object) {

        if (this == object) { return true; }
        if (object == null) { return false; }
        if (getClass() != object.getClass()) { return false; }

        @SuppressWarnings("rawtypes")
        final FutureRemoteReference other = (FutureRemoteReference) object;
        if (address == null) {
            if (other.address != null) { return false; }
        }
        else if (!address.equals(other.address)) { return false; }
        if (id == null) {
            if (other.id != null) { return false; }
        }
        else if (!id.equals(other.id)) { return false; }

        return true;
    }

}
