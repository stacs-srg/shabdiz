package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

public final class FutureRemoteProxyFactory {

    private static final Map<UUID, FutureRemoteProxy<? extends Serializable>> FUTURE_PROXY_MAP = new Hashtable<UUID, FutureRemoteProxy<? extends Serializable>>(); // Hashtable is used because it does not permit null key/values

    private FutureRemoteProxyFactory() {

    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static synchronized <Result extends Serializable> FutureRemoteProxy<Result> getProxy(final UUID job_id, final InetSocketAddress proxy_address) {

        final FutureRemoteProxy<Result> proxy;

        if (FUTURE_PROXY_MAP.containsKey(job_id)) {

            proxy = (FutureRemoteProxy<Result>) FUTURE_PROXY_MAP.get(job_id);
        }
        else {

            proxy = new FutureRemoteProxy<Result>(job_id, proxy_address);
            FUTURE_PROXY_MAP.put(job_id, proxy);
        }

        return proxy;
    }
}
