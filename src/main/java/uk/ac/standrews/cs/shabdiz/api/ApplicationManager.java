package uk.ac.standrews.cs.shabdiz.api;

public interface ApplicationManager<ApplicationReference> {

    ApplicationState getApplicationState(ApplicationDescriptor descriptor);

    void kill(ApplicationDescriptor descriptor) throws Exception;

    ApplicationReference deploy(Host host) throws Exception;

}
