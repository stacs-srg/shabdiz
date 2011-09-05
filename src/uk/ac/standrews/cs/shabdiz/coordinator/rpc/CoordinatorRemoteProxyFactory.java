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
package uk.ac.standrews.cs.shabdiz.coordinator.rpc;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;

/**
 * A factory for creating {@link CoordinatorRemoteProxy} objects.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class CoordinatorRemoteProxyFactory {

    private static final Map<InetSocketAddress, CoordinatorRemoteProxy> COORDINATOR_PROXY_MAP = new Hashtable<InetSocketAddress, CoordinatorRemoteProxy>(); // Hashtable is used because it does not permit null key/values

    private CoordinatorRemoteProxyFactory() {

    }

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the cached proxy associated to a given address. Instantiates a new proxy if not such association is cached.
     *
     * @param proxy_address the proxy address
     * @return the proxy associated to the given address
     */
    public static synchronized CoordinatorRemoteProxy getProxy(final InetSocketAddress proxy_address) {

        final CoordinatorRemoteProxy proxy;

        if (COORDINATOR_PROXY_MAP.containsKey(proxy_address)) {

            proxy = COORDINATOR_PROXY_MAP.get(proxy_address);
        }
        else {

            proxy = new CoordinatorRemoteProxy(proxy_address);
            COORDINATOR_PROXY_MAP.put(proxy_address, proxy);
        }

        return proxy;
    }
}
