package uk.ac.standrews.cs.shabdiz.coordinator;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.SortedSet;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.HostState;
import uk.ac.standrews.cs.nds.madface.MadfaceManagerFactory;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.madface.interfaces.IMadfaceManager;
import uk.ac.standrews.cs.nds.p2p.network.INetwork;

/**
 * Presents a network of worker nodes. Deploys a worker on each of the given host descriptors.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerNetwork implements INetwork {

    private final IMadfaceManager madface_manager;

    /**
     * Package protected constructor of a new worker network.
     *
     * @param host_descriptors the hosts on which workers will be deployed
     * @param worker_manager the manager of a worker application
     * @param application_lib_urls the worker application library URLs
     * @param coordinator_address the address of the coordinator server
     * @throws Exception if unable to deploy the network of workers
     */
    WorkerNetwork(final SortedSet<HostDescriptor> host_descriptors, final WorkerManager worker_manager, final Set<URL> application_lib_urls, final InetSocketAddress coordinator_address) throws Exception {

        madface_manager = MadfaceManagerFactory.makeMadfaceManager();

        madface_manager.setHostScanning(true);
        madface_manager.configureApplication(worker_manager);
        madface_manager.configureApplication(application_lib_urls);

        for (final HostDescriptor new_node_descriptor : host_descriptors) {

            final Object[] application_deployment_params = new Object[]{coordinator_address};
            new_node_descriptor.applicationDeploymentParams(application_deployment_params);
            madface_manager.add(new_node_descriptor);
        }

        madface_manager.deployAll();
        madface_manager.waitForAllToReachState(HostState.RUNNING);
        madface_manager.setHostScanning(false);
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public SortedSet<HostDescriptor> getNodes() {

        return madface_manager.getHostDescriptors();
    }

    @Override
    public synchronized void killNode(final HostDescriptor node) throws Exception {

        madface_manager.kill(node, false);
        madface_manager.drop(node);
    }

    @Override
    public synchronized void killAllNodes() throws Exception {

        madface_manager.killAll(false);
        madface_manager.dropAll();
    }

    @Override
    public synchronized void shutdown() {

        madface_manager.shutdown();
    }
}
