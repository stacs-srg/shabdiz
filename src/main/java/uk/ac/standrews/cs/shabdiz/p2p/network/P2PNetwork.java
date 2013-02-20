/***************************************************************************
 * * nds Library * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group * University of St Andrews, Scotland * http://www-systems.cs.st-andrews.ac.uk/ * * This file is part of nds, a package of utility classes. * * nds is free software: you can redistribute it and/or modify * it under the terms of the GNU General Public License as published by * the Free Software Foundation, either version 3 of the License, or * (at your option) any later version. * * nds is distributed in the
 * hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License * along with nds. If not, see <http://www.gnu.org/licenses/>. * *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.p2p.network;

import java.util.Set;
import java.util.SortedSet;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.active.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.active.HostState;
import uk.ac.standrews.cs.shabdiz.active.MadfaceManagerFactory;
import uk.ac.standrews.cs.shabdiz.active.URL;
import uk.ac.standrews.cs.shabdiz.active.interfaces.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.active.interfaces.IMadfaceManager;

/**
 * Network comprising P2P nodes running on a set of specified hosts running Linux or OSX.
 * 
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 */
public class P2PNetwork implements INetwork {

    private final IMadfaceManager madface_manager;

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

        madface_manager = MadfaceManagerFactory.makeMadfaceManager();

        madface_manager.setHostScanning(true);
        madface_manager.configureApplication(application_manager);
        madface_manager.configureApplication(application_lib_urls);

        final IKey[] node_keys = key_distribution.generateKeys(host_descriptors.size());

        int node_index = 0;
        for (final HostDescriptor new_node_descriptor : host_descriptors) {

            final Object[] application_deployment_params = new Object[]{node_keys[node_index++]};
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
