package uk.ac.standrews.cs.shabdiz.interfaces;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;

/**
 * Presents the local functionalities provided by a coordinator.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ICoordinator {

    /**
     * Deploys worker on a described host and returns the reference to the deployed worker.
     * This method blocks until the worker is deployed.
     *
     * @param host_descriptor the descriptor of the host on which a worker is deployed
     * @return the reference to the deployed worker
     * @throws Exception if the attempt to deploy worker on host fails
     */
    IWorkerRemote deployWorkerOnHost(HostDescriptor host_descriptor) throws Exception;

    /**
     * Shuts down this coordinator. This method does <code>not</code> shot down any workers deployed by this coordinator.
     * User may shot down workers by calling {@link IWorkerRemote#shutdown()}.
     */
    void shutdown();
}
