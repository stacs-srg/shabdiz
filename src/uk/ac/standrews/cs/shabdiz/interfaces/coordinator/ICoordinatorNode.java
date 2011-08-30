package uk.ac.standrews.cs.shabdiz.interfaces.coordinator;

import java.io.Serializable;
import java.util.SortedSet;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.exceptions.LibrariesOverwrittenException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.coordinator.AlreadyDeployedException;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.worker.IWorkerRemote;

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
     */
    void addHost(HostDescriptor host_descriptor);

    /**
     * Deploys a worker on each of the hosts added using {@link #addHost(HostDescriptor)}.
     *
     * @throws Exception if unable to deploy the hosts
     */
SortedSet<IWorkerRemote> deployWorkersOnHosts() throws Exception;

    /**
     * Submits a computation to be performed on a worker.
     *
     * @param <Result> the type of result which is returned by the job
     * @param worker the remote worker
     * @param job the computation to be performed remotely
     * @return the remote pending result of the computation
     * @throws RPCException if unable to contact the remote worker or if unable to submit the job
     */
    <Result extends Serializable> IFutureRemote<Result> submit(IWorkerRemote worker, IRemoteJob<Result> job) throws RPCException;

    get rid of submit, make a smart proxy -> some coordinated worker remote
    
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
