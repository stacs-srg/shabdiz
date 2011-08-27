package uk.ac.standrews.cs.shabdiz.coordinator.rpc;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;

public final class CoordinatorProxyFactory {

    private static final Map<InetSocketAddress, CoordinatorProxy> COORDINATOR_PROXY_MAP = new Hashtable<InetSocketAddress, CoordinatorProxy>(); // Hashtable is used because it does not permit null key/values

    private CoordinatorProxyFactory() {

    }

    // -------------------------------------------------------------------------------------------------------------------------------

    public static synchronized CoordinatorProxy getProxy(final InetSocketAddress proxy_address) {

        final CoordinatorProxy proxy;

        if (COORDINATOR_PROXY_MAP.containsKey(proxy_address)) {

            proxy = COORDINATOR_PROXY_MAP.get(proxy_address);
        }
        else {

            proxy = new CoordinatorProxy(proxy_address);
            COORDINATOR_PROXY_MAP.put(proxy_address, proxy);
        }

        return proxy;
    }
}
