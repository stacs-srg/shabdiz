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

package uk.ac.standrews.cs.shabdiz.zold.p2p.network;

import java.util.SortedSet;

import uk.ac.standrews.cs.shabdiz.zold.HostDescriptor;

/**
 * Interface representing a set of nodes.
 * 
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 */
public interface Network {

    /**
     * Returns a new list containing the nodes.
     * 
     * @return the nodes in the network, sorted in ascending key order.
     */
    SortedSet<HostDescriptor> getNodes();

    /**
     * Kills a given node and removes it from the network.
     * 
     * @param node the node to be killed
     * @throws Exception if the node cannot be killed
     */
    void killNode(HostDescriptor node) throws Exception;

    /**
     * Kills all nodes and removes them from the network.
     * 
     * @throws Exception if one of the nodes cannot be killed
     */
    void killAllNodes() throws Exception;

    /**
     * Shuts down the network.
     */
    void shutdown();
}
