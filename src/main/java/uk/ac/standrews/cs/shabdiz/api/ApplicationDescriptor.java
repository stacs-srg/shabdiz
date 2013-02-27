package uk.ac.standrews.cs.shabdiz.api;

import java.util.Collection;

import uk.ac.standrews.cs.nds.rpc.interfaces.Pingable;

public interface ApplicationDescriptor<ApplicationReference extends Pingable> {

    Host getHost();

    ApplicationReference getApplicationReference();

    Collection<Process> getProcesses();

    ApplicationState getState();
}
