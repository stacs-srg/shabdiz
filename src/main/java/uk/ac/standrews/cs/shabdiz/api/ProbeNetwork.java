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
package uk.ac.standrews.cs.shabdiz.api;

import java.util.Set;

/**
 * Maintains a set of {@link ProbeHook hooks} to application instances across multiple {@link Host hosts}.
 * 
 * @param <T> the type of {@link ProbeHook hooks} that are maintained by this network
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ProbeNetwork<T extends ProbeHook> extends Set<T> {

    /**
     * Causes the current thread to wait until all the {@link ProbeHook instances} managed by this network reach one of the given {@code states} at least once, unless the thread is {@link Thread#interrupt() interrupted}.
     * 
     * @param states the states which application instances must reach at least once
     * @throws InterruptedException if the current thread is {@link Thread#interrupt() interrupted} while waiting
     */
    void awaitAnyOfStates(ApplicationState... states) throws InterruptedException;

    /**
     * Adds the given {@code scanner} to the collection of this network's scanners.
     * This method has no effect if the given {@code scanner} has already been added.
     * 
     * @param scanner the scanner to add
     * @return true, if successfully added
     */
    boolean addScanner(Scanner<? extends T> scanner);

    /**
     * Removes the given {@code scanner} from the collection of this network's scanners.
     * This method has no effect if the given {@code scanner} does not exist in the collection of this network's scanners.
     * 
     * @param scanner the scanner to remove
     * @return true, if successfully removed
     */
    boolean removeScanner(Scanner<? extends T> scanner);

    /**
     * Sets the policy on whether the scanners of this network should be {@link Scanner#setEnabled(boolean) enabled}.
     * 
     * @param enabled if {@code true} enables all the scanners of this network, disables all the scanners otherwise
     */
    void setScanEnabled(boolean enabled);

    /**
     * Removes all the hooks that are maintained by this network.
     * After this method is called, this network is no longer usable.
     */
    void shutdown();
}
