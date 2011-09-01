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
     * Blocks until workers are deployed on the hosts added using {@link #addHost(HostDescriptor)} and returns a set of deployed workers.
     *
     * @return the deployed workers
     * @throws Exception if unable to deploy the hosts
     */
    SortedSet<IWorker> deployWorkersOnHosts() throws Exception;

    /**
     * Shuts down this coordinator.
     */
    void shutdown();
}
