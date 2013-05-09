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
package uk.ac.standrews.cs.shabdiz;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link ApplicationDescriptor} tests.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ApplicationDescriptorTest {

    private static final int CONCURRENCY_TEST_TIMEOUT_IN_SECONDS = 5;
    private static final int DESCRIPTORS_COUNT = 100;

    private List<ApplicationDescriptor> descriptors;
    private ExecutorService executor;

    /** Prepares the environment for a test. */
    @Before
    public void setUp() {

        executor = Executors.newCachedThreadPool();
        descriptors = new ArrayList<ApplicationDescriptor>();
        for (int i = 0; i < DESCRIPTORS_COUNT; i++) {
            descriptors.add(new ApplicationDescriptor(null));
        }
    }

    /** Cleans up after a test. */
    @After
    public void tearDown() {

        descriptors.clear();
        executor.shutdownNow();
    }

    /**
     * Tests {@link ApplicationDescriptor#awaitAnyOfStates(ApplicationState...)} when concurrent state changes occur.
     * 
     * @throws InterruptedException if interrupted while waiting for the test to complete
     */
    @Test
    public void testAwaitAnyOfStatesWithConcurrentStateChange() throws InterruptedException {

        final CountDownLatch start_latch = new CountDownLatch(1);
        final CountDownLatch end_latch = new CountDownLatch(DESCRIPTORS_COUNT * 2);

        for (final ApplicationDescriptor descriptor : descriptors) {
            executor.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    start_latch.await();
                    descriptor.awaitAnyOfStates(ApplicationState.RUNNING);
                    end_latch.countDown();
                    return null;
                }
            });

            executor.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    start_latch.await();
                    descriptor.setApplicationState(ApplicationState.RUNNING);
                    end_latch.countDown();
                    return null;
                }
            });
        }
        start_latch.countDown();
        if (!end_latch.await(CONCURRENCY_TEST_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) {
            Assert.fail();
        }
    }
}
