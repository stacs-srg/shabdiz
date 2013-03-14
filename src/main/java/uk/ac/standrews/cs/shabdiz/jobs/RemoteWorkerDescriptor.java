package uk.ac.standrews.cs.shabdiz.jobs;

import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Worker;

public class RemoteWorkerDescriptor extends AbstractApplicationDescriptor {

    private final AtomicReference<Worker> worker_proxy;

    public RemoteWorkerDescriptor(final Host host) {

        super(host);
        worker_proxy = new AtomicReference<Worker>();
    }

    @Override
    public void ping() throws RPCException {

        getApplicationReference().getAddress();
    }

    public Worker getApplicationReference() {

        return worker_proxy.get();
    }

    protected void setApplicationReference(final Worker worker) {

        worker_proxy.set(worker);
    }

    @Override
    public void deploy() {

        // TODO Auto-generated method stub

    }
}
