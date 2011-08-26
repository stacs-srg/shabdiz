package uk.ac.standrews.cs.mcjob.interfaces.coordinator;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import uk.ac.standrews.cs.mcjob.coordinator.AlreadyDeployedException;
import uk.ac.standrews.cs.mcjob.interfaces.IRemoteFuture;
import uk.ac.standrews.cs.mcjob.interfaces.IRemoteJob;
import uk.ac.standrews.cs.mcjob.interfaces.worker.IWorkerRemoteReference;
import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.exceptions.LibrariesOverwrittenException;
import uk.ac.standrews.cs.nds.rpc.RPCException;

public interface ICoordinatorNode {

    void addHost(HostDescriptor host_descriptor) throws LibrariesOverwrittenException, AlreadyDeployedException;

    void deployHosts() throws Exception;

    <Result extends Serializable> IRemoteFuture<Result> submit(IWorkerRemoteReference remote_reference, IRemoteJob<Result> job) throws RPCException;

    boolean cancel(final UUID job_id, final boolean may_interrupt_if_running) throws RPCException;

    boolean isCancelled(UUID job_id) throws RPCException;

    boolean isDone(UUID job_id) throws RPCException;

    Object get(UUID job_id) throws CancellationException, InterruptedException, ExecutionException, RPCException;

    Set<IWorkerRemoteReference> getNodes();

    void killWorker(IWorkerRemoteReference node) throws Exception;

    void killAllWorkers() throws Exception;

    void shutdown();

}
