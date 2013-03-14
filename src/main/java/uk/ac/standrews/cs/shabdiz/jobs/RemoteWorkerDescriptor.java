package uk.ac.standrews.cs.shabdiz.jobs;

import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.shabdiz.DefaultApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Worker;

public class RemoteWorkerDescriptor extends DefaultApplicationDescriptor {

    private final AtomicReference<Worker> application_reference;

    public RemoteWorkerDescriptor(final Host host, final WorkerManager worker_manager) {

        super(host, worker_manager);
        application_reference = new AtomicReference<Worker>();
    }

    public void setApplicationReference(final Worker worker) {

        application_reference.set(worker);
    }

    public Worker getApplicationReference() {

        return application_reference.get();
    }

}
