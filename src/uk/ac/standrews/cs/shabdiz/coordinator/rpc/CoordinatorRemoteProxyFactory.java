package uk.ac.standrews.cs.shabdiz.coordinator.rpc;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;

public final class CoordinatorRemoteProxyFactory {

    private static final Map<InetSocketAddress, CoordinatorRemoteProxy> COORDINATOR_PROXY_MAP = new Hashtable<InetSocketAddress, CoordinatorRemoteProxy>(); // Hashtable is used because it does not permit null key/values

    private CoordinatorRemoteProxyFactory() {

    }

    // -------------------------------------------------------------------------------------------------------------------------------

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
