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
package uk.ac.standrews.cs.shabdiz;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;

/**
 * A factory for creating {@link LauncherCallbackRemoteProxy} objects.
 * This implementation caches a proxy instance for a given address.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
final class LauncherCallbackRemoteProxyFactory {

    private static final Map<InetSocketAddress, LauncherCallbackRemoteProxy> LAUNCHER_CALLBACK_PROXY_MAP = new Hashtable<InetSocketAddress, LauncherCallbackRemoteProxy>(); // Hashtable is used because it does not permit null key/values

    private LauncherCallbackRemoteProxyFactory() {

    }

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the cached proxy associated to a given address. Instantiates a new proxy if no such association exists.
     *
     * @param proxy_address the proxy address
     * @return the proxy associated to the given address
     */
    static synchronized LauncherCallbackRemoteProxy getProxy(final InetSocketAddress proxy_address) {

        final LauncherCallbackRemoteProxy proxy;

        if (LAUNCHER_CALLBACK_PROXY_MAP.containsKey(proxy_address)) {

            proxy = LAUNCHER_CALLBACK_PROXY_MAP.get(proxy_address);
        }
        else {

            proxy = new LauncherCallbackRemoteProxy(proxy_address);
            LAUNCHER_CALLBACK_PROXY_MAP.put(proxy_address, proxy);
        }

        return proxy;
    }
}