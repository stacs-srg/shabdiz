package uk.ac.standrews.cs.shabdiz.interfaces.coordinator;

import java.util.SortedSet;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.exceptions.LibrariesOverwrittenException;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorker;

/**
 * Presents the local functionalities provided by a coordinator.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ICoordinatorNode {

    /**
     * Informs the coordinator of the existence of a host on which a worker will be deployed.
     *
     * @param host_descriptor the host descriptor
     * @throws LibrariesOverwrittenException 
     */
    void addHost(HostDescriptor host_descriptor) throws LibrariesOverwrittenException;

    /**
     * Blocks until workers are deployed on the hosts added using {@link #addHost(HostDescriptor)} and returns a set of deployed workers.
     *
     * @return the deployed workers
     * @throws Exception if unable to deploy the hosts
     */
    SortedSet<IWorker> deployWorkersOnHosts() throws Exception;

    /**
     * Kills the given worker.
     *
     * @param worker the worker to be killed
     * @throws Exception if unable to kill the worker
     */
    void killWorker(IWorker worker) throws Exception;

    /**
     * Kills all the workers which have been deployed by this coordinator.
     *
     * @throws Exception an error has occurred while killing the workers
     */
    void killAllWorkers() throws Exception;

    /**
     * Shuts down this coordinator.
     */
    void shutdown();
}
