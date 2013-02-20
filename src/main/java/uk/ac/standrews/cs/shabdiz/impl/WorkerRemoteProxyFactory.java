package uk.ac.standrews.cs.shabdiz.impl;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;

/**
 * A factory for retrieving {@link WorkerRemoteProxy} objects.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
final class WorkerRemoteProxyFactory {

    /** The Constant WORKER_NODE_PROXY_MAP. */
    private static final Map<InetSocketAddress, WorkerRemoteProxy> WORKER_NODE_PROXY_MAP = new Hashtable<InetSocketAddress, WorkerRemoteProxy>(); // Hashtable is used because it does not permit null key/values

    private WorkerRemoteProxyFactory() {

    }

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the cached proxy associated to a given address. Instantiates a new proxy if no such association exists.
     *
     * @param proxy_address the proxy address
     * @return the proxy associated to the given address
     */
    static synchronized WorkerRemoteProxy getProxy(final InetSocketAddress proxy_address) {

        WorkerRemoteProxy proxy = null;
        if (proxy_address !=null){

            if (WORKER_NODE_PROXY_MAP.containsKey(proxy_address)) {

                proxy = WORKER_NODE_PROXY_MAP.get(proxy_address);
            }
            else {

                proxy = new WorkerRemoteProxy(proxy_address);
                WORKER_NODE_PROXY_MAP.put(proxy_address, proxy);
            }
        }

        return proxy;
    }
}
