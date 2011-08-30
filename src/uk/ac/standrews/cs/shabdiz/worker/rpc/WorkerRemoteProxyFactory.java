package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;

public final class WorkerRemoteProxyFactory {

    private static final Map<InetSocketAddress, WorkerRemoteProxy> WORKER_PROXY_MAP = new Hashtable<InetSocketAddress, WorkerRemoteProxy>(); // Hashtable is used because it does not permit null key/values

    private WorkerRemoteProxyFactory() {

    }

    // -------------------------------------------------------------------------------------------------------------------------------

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
