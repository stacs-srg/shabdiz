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
