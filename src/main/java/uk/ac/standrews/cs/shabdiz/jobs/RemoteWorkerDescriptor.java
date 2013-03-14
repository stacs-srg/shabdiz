package uk.ac.standrews.cs.shabdiz.jobs;

import uk.ac.standrews.cs.shabdiz.DefaultApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Worker;

public class RemoteWorkerDescriptor extends DefaultApplicationDescriptor<Worker> {

    public RemoteWorkerDescriptor(final Host host, final WorkerManager application_manager) {

        super(host, application_manager);
    }

}

class WorkerManager implements ApplicationManager<Worker> {

    @Override
    public ApplicationState getApplicationState(final ApplicationDescriptor descriptor) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

    }

    @Override
    public Worker deploy(final Host host) throws Exception {

        return null;
    }
}
