package uk.ac.standrews.cs.shabdiz.jobs;

import java.io.IOException;
import java.net.InetSocketAddress;

import uk.ac.standrews.cs.jetson.JsonRpcProxyFactory;

public final class WorkerRemoteProxyFactory extends JsonRpcProxyFactory {

    private static final WorkerRemoteProxyFactory WORKER_REMOTE_PROXY_FACTORY_INSTANCE = new WorkerRemoteProxyFactory();

    private WorkerRemoteProxyFactory() {

        super(WorkerRemote.class, WorkerJsonFactory.getInstance());
    }

    public static WorkerRemote getProxy(final InetSocketAddress address) throws IllegalArgumentException, IOException {

        return WORKER_REMOTE_PROXY_FACTORY_INSTANCE.get(address);
    }

    public static WorkerRemoteProxyFactory getInstance() {

        return WORKER_REMOTE_PROXY_FACTORY_INSTANCE;
    }
}
