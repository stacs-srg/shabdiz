package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;

/**
 * A factory for creating {@link WorkerRemoteProxy} objects.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class WorkerRemoteProxyFactory {

    private static final Map<InetSocketAddress, WorkerRemoteProxy> WORKER_PROXY_MAP = new Hashtable<InetSocketAddress, WorkerRemoteProxy>(); // Hashtable is used because it does not permit null key/values

    private WorkerRemoteProxyFactory() {

    }

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the cached proxy associated to a given address. Instantiates a new proxy if not such association is cached.
     *
     * @param proxy_address the proxy address
     * @return the proxy associated to the given address
     */
    public static synchronized WorkerRemoteProxy getProxy(final InetSocketAddress proxy_address) {

        final WorkerRemoteProxy proxy;

        if (WORKER_PROXY_MAP.containsKey(proxy_address)) {

            proxy = WORKER_PROXY_MAP.get(proxy_address);
        }
        else {

            proxy = new WorkerRemoteProxy(proxy_address);
            WORKER_PROXY_MAP.put(proxy_address, proxy);
        }

        return proxy;
    }
}
