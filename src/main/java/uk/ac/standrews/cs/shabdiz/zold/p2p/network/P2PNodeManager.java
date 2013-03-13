/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.zold.p2p.network;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.zold.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.zold.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.zold.exceptions.DeploymentException;

/**
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public abstract class P2PNodeManager extends AbstractApplicationManager {

    private static final String LOCAL_HOSTNAME_SUFFIX = ".local";

    protected abstract P2PNodeFactory getP2PNodeFactory();

    private final boolean local_deployment_only;

    /**
     * Initializes a node manager.
     * 
     * @param local_deployment_only true if nodes are only to be deployed to the local node
     */
    public P2PNodeManager(final boolean local_deployment_only) {

        this.local_deployment_only = local_deployment_only;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public void deployApplication(final HostDescriptor host_descriptor) throws Exception {

        Diagnostic.traceNoSource(DiagnosticLevel.FULL, "deploying application to: " + host_descriptor.getHost());

        getP2PNodeFactory().createNode(host_descriptor, getKey(host_descriptor.getApplicationDeploymentParams()));

        // If locally deployed, assume deployment was successful and set the host descriptor state directly to speed things up.
        if (local_deployment_only) {
            host_descriptor.hostState(ApplicationState.RUNNING);
        }
    }

    protected String stripLocalSuffix(final String host_name) {

        return host_name.endsWith(LOCAL_HOSTNAME_SUFFIX) ? host_name.substring(0, host_name.length() - LOCAL_HOSTNAME_SUFFIX.length()) : host_name;
    }

    // -------------------------------------------------------------------------------------------------------

    private IKey getKey(final Object... args) throws DeploymentException {

        if (args == null || args.length == 0 || args[0] == null) { return null; }

        final Object arg = args[0];
        if (arg instanceof IKey) { return (IKey) arg; }
        throw new DeploymentException("argument not of type IKey");
    }
}