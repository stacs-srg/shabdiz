package uk.ac.standrews.cs.shabdiz.interfaces;

import java.util.SortedSet;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;

/**
 * Presents the local functionalities provided by a coordinator.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ICoordinator { // XXX Discuss whether a coordinator is a good name for this concept

    /**
     * Informs the coordinator of the existence of a host on which a worker will be deployed.
     *
     * @param host_descriptor the descriptor of a host on which a worker will be deployed
     */
    void addHost(HostDescriptor host_descriptor);

    /**
     * Deploys workers on hosts and returns the set of deployed workers. This method blocks until on each added host a worker is deployed.
     *
     * @return the set of deployed workers in order of deployment
     * @throws Exception if the attempt to deploy workers on hosts fails
     */
    SortedSet<IWorker> deployWorkersOnHosts() throws Exception;

    /**
     * Shuts down this coordinator. This method does <code>not</code> shot down any workers deployed by this coordinator; user may shot down workers by calling {@link IWorker#shutdown()}.
     */
    void shutdown();
}
