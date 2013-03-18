/*
 * This file is part of Shabdiz.
 * 
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.legacy.p2p.network;

import java.util.Set;
import java.util.SortedSet;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.legacy.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.legacy.MadfaceManagerFactory;
import uk.ac.standrews.cs.shabdiz.legacy.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.legacy.api.MadfaceManager;
import uk.ac.standrews.cs.shabdiz.util.URL;

/**
 * Network comprising P2P nodes running on a set of specified hosts running Linux or OSX.
 * 
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 */
public class P2PNetwork implements Network {

    private final MadfaceManager madface_manager;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new network.
     * 
     * @param host_descriptors a description of the target host for each node to be created
     * @param application_manager a manager for the required application
     * @param application_lib_urls
     * @param key_distribution the required key distribution
     * @throws Exception if there is an error during creation of the network
     */
    public P2PNetwork(final SortedSet<HostDescriptor> host_descriptors, final ApplicationManager application_manager, final Set<URL> application_lib_urls, final KeyDistribution key_distribution) throws Exception {

        madface_manager = MadfaceManagerFactory.DEFAULT_MADFACE_MANAGER_FACTORY.newMadfaceManager();

        madface_manager.setHostScanning(true);
        application_manager.setApplicationLibraryURLs(application_lib_urls);
        madface_manager.setApplicationManager(application_manager);

        final IKey[] node_keys = key_distribution.generateKeys(host_descriptors.size());
        int node_index = 0;
        for (final HostDescriptor new_node_descriptor : host_descriptors) {

            final Object[] application_deployment_params = new Object[]{node_keys[node_index++]};
            new_node_descriptor.applicationDeploymentParams(application_deployment_params);
            madface_manager.add(new_node_descriptor);
        }

        madface_manager.deployAll();
        madface_manager.waitForAllToReachState(ApplicationState.RUNNING);
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
