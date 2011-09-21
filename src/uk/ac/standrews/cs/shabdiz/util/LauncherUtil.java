/*
 * shabdiz Library
 * Copyright (C) 2011 Distributed Systems Architecture Research Group
 * <http://www-systems.cs.st-andrews.ac.uk/>
 *
 * This file is part of shabdiz, a variation of the Chord protocol
 * <http://pdos.csail.mit.edu/chord/>, where each node strives to maintain
 * a list of all the nodes in the overlay in order to provide one-hop
 * routing.
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
 * For more information, see <http://beast.cs.st-andrews.ac.uk:8080/hudson/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.util;

import java.util.LinkedHashSet;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.impl.Launcher;
import uk.ac.standrews.cs.shabdiz.interfaces.ILauncher;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;

/**
 * Provides utility methods for a {@link Launcher}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class LauncherUtil {

    private LauncherUtil() {

    }

    // -------------------------------------------------------------------------------------------------------------------------------

    /**
     * Deploys a worker on each of the given hosts.
     *
     * @param launcher the launcher which is used to deploy workers
     * @param worker_hosts the set of hosts on which workers are deployed
     * @return the sets of deployed workers on hosts
     * @throws Exception if unable to deploy
     */
    public static LinkedHashSet<IWorker> deployWorkersOnHosts(final ILauncher launcher, final LinkedHashSet<HostDescriptor> worker_hosts) throws Exception {

        final LinkedHashSet<IWorker> deployed_workers = new LinkedHashSet<IWorker>();

        for (final HostDescriptor worker_host : worker_hosts) {

            deployed_workers.add(launcher.deployWorkerOnHost(worker_host));
        }

        return deployed_workers;
    }
}
