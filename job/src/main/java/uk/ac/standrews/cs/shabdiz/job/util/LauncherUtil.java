/*
 * Copyright 2013 University of St Andrews School of Computer Science
 *
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
package uk.ac.standrews.cs.shabdiz.job.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import uk.ac.standrews.cs.shabdiz.job.WorkerNetwork;

/**
 * Provides utility methods for a {@link WorkerNetwork}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class LauncherUtil {

    private LauncherUtil() {

    }

    /**
     * Waits for a collection of given futures until all have a result available. If one of the futures results in exception, this method throws the exception immediately.
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
     * Gets the results of a collection of given futures. If one of the futures results in exception, this method throws the exception immediately.
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
