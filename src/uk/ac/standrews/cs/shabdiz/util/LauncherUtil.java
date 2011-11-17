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
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.util;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    public static IWorker[] deployWorkersOnHosts(final ILauncher launcher, final List<HostDescriptor> worker_hosts) throws Exception {

        final IWorker[] deployed_workers = new IWorker[worker_hosts.size()];

        int count = 0;
        for (final HostDescriptor worker_host : worker_hosts) {
            System.out.println("Deploying worker on " + worker_host.getHost() + "...");
            deployed_workers[count] = launcher.deployWorkerOnHost(worker_host);
            System.out.println("Done.");

            count++;
        }

        return deployed_workers;
    }

    /**
     * Waits for a set of given futures until all have a result available. If one of the futures results  in exception, this method throws the exception immediately.
     *
     * @param <T> the generic type
     * @param futures the futures to wait for
     * @throws InterruptedException if one of the futures has interrupted
     * @throws ExecutionException if one of the futures has ended in exception
     */
    public static <T> void waitForFutures(final Set<Future<T>> futures) throws InterruptedException, ExecutionException {

        for (final Future<?> ring_stable_futue : futures) {

            ring_stable_futue.get();
        }
    }
}
