package uk.ac.standrews.cs.shabdiz.interfaces.coordinator;

import java.io.Serializable;
import java.util.SortedSet;

import uk.ac.standrews.cs.shabdiz.coordinator.AlreadyDeployedException;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorkerRemote;
import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.exceptions.LibrariesOverwrittenException;
import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * Presents the local functionalities provided by a coordinator.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ICoordinatorNode {

    /**
     * Adds a host to the list of hosts to be deployed.
     *
     * @param host_descriptor the host descriptor
     * @throws LibrariesOverwrittenException if the host descriptor's application libraries set are overridden
     * @throws AlreadyDeployedException if {@link #deployHosts()} on this coordinator has already been called
     */
    void addHost(HostDescriptor host_descriptor) throws LibrariesOverwrittenException, AlreadyDeployedException;

    /**
     * Deploys a worker on each of the hosts added using {@link #addHost(HostDescriptor)}.
     *
     * @throws AlreadyDeployedException if {@link #deployHosts()} on this coordinator has already been called
     * @throws Exception if unable to deploy the hosts
     */
    void deployHosts() throws Exception;

    /**
     * Submits a computation to be performed on a worker.
     *
     * @param <Result> the type of result which is returned by the job
     * @param remote_reference the remote_reference
     * @param job the job
     * @return the i future remote
     * @throws RPCException the rPC exception
     */
    <Result extends Serializable> IFutureRemote<Result> submit(IWorkerRemote remote_reference, IRemoteJob<Result> job) throws RPCException;

    /**
     * Gets the workers which have been deployed by this coordinator.
     *
     * @return the workers
     */
    SortedSet<IWorkerRemote> getWorkers();

    /**
     * Kills the given worker.
     *
     * @param node the worker
     * @throws Exception if unable to kill the worker
     */
    void killWorker(IWorkerRemote worker) throws Exception;

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
