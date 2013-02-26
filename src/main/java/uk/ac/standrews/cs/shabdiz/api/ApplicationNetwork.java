package uk.ac.standrews.cs.shabdiz.api;

import uk.ac.standrews.cs.nds.rpc.interfaces.IPingable;

public interface ApplicationNetwork<ApplicationReference extends IPingable> extends Iterable<ApplicationReference> {

    void awaitState(ApplicationState state);

    ApplicationReference deploy(Host host);

    void Kill(ApplicationReference application);

    void drop(ApplicationReference application);

    void shutdown();
}
