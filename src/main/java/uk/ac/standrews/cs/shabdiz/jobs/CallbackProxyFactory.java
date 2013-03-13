package uk.ac.standrews.cs.shabdiz.jobs;

import java.io.IOException;
import java.net.InetSocketAddress;

import uk.ac.standrews.cs.jetson.JsonRpcProxyFactory;

public final class CallbackProxyFactory extends JsonRpcProxyFactory {

    private static final CallbackProxyFactory CALLBACK_PROXY_FACTORY_INSTANCE = new CallbackProxyFactory();

    private CallbackProxyFactory() {

        super(WorkerCallback.class, WorkerJsonFactory.getInstance());
    }

    public static WorkerCallback getProxy(final InetSocketAddress address) throws IllegalArgumentException, IOException {

        return CALLBACK_PROXY_FACTORY_INSTANCE.get(address);
    }

    public static CallbackProxyFactory getInstance() {

        return CALLBACK_PROXY_FACTORY_INSTANCE;
    }
}
