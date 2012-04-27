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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import uk.ac.standrews.cs.shabdiz.impl.Launcher;

/**
 * Provides utility methods for a {@link Launcher}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class LauncherUtil {

    private LauncherUtil() {

    }

    /**
     * Waits for a collection of given futures until all have a result available. If one of the futures results  in exception, this method throws the exception immediately.
     *
     * @param <T> the generic type
     * @param futures the futures to wait for
     * @throws InterruptedException if one of the futures has interrupted
     * @throws ExecutionException if one of the futures has ended in exception
     */
    public static <T> void awaitFutures(final Collection<Future<T>> futures) throws InterruptedException, ExecutionException {

        for (final Future<T> future : futures) {

            future.get();
        }
    }

    /**
     * Gets the results of a collection of given futures. If one of the futures results  in exception, this method throws the exception immediately.
     *
     * @param <T> the generic type
     * @param futures the futures to wait for
     * @return the results of the given futures
     * @throws InterruptedException if one of the futures has interrupted
     * @throws ExecutionException if one of the futures has ended in exception
     */
    public static <T> Collection<T> awaitAndGetFutures(final Collection<Future<T>> futures) throws InterruptedException, ExecutionException {

        final List<T> results = new ArrayList<T>();
        for (final Future<T> future : futures) {

            results.add(future.get());
        }
        return results;
    }
}
