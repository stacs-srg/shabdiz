package uk.ac.standrews.cs.shabdiz.coordinator;

import java.io.Serializable;
import java.util.Set;

import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;

/**
 * Actively coordinates a set of workers. Makes connections to the remote worker upon request and does not cache any local state.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ActiveCoordinatorNode extends AbstractCoordinatorNode {

    /**
     * Instantiates a new active coordinator node.
     *
     * @param application_lib_urls the application_lib_urls
     * @param try_registry_on_connection_error the try_registry_on_connection_error
     * @throws Exception the exception
     */
    public ActiveCoordinatorNode(final Set<URL> application_lib_urls, final boolean try_registry_on_connection_error) throws Exception {

        super(application_lib_urls, try_registry_on_connection_error);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    @Override
    protected <Result extends Serializable> IFutureRemote<Result> getFutureRemote(final IFutureRemoteReference<Result> future_remote_reference) {

        return future_remote_reference.getRemote(); // Since no local state is held, future remote porxy is returned.
    }
}
