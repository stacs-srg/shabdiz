package uk.ac.standrews.cs.shabdiz.interfaces;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;

/**
 * Presents the local functionalities provided by a coordinator.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ICoordinator {
fix the comment
    /**
     * Deploys workers on hosts and returns the set of deployed workers. 
     * This method blocks until on each added host a worker is deployed.
     *
     * @return the set of deployed workers in order of deployment
     * @throws Exception if the attempt to deploy workers on hosts fails
     */
    IWorkerRemote deployWorkerOnHost(HostDescriptor host_descriptor) throws Exception;

    /**
     * Shuts down this coordinator. This method does <code>not</code> shot down any workers deployed by this coordinator.
     * User may shot down workers by calling {@link IWorkerRemote#shutdown()}.
     */
    void shutdown();
}
