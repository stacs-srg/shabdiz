package uk.ac.standrews.cs.shabdiz.api;

import java.beans.PropertyChangeListener;
import java.util.Collection;

import uk.ac.standrews.cs.nds.rpc.interfaces.Pingable;

public interface ApplicationDescriptor {

    void addPropertyChangeListener(String property_name, PropertyChangeListener listener);

    void removePropertyChangeListener(String property_name, PropertyChangeListener listener);

    Host getHost();

    Pingable getApplicationReference();

    Collection<Process> getProcesses();

    State getState();
}
